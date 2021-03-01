package de.alxgrk.input

import com.google.common.collect.Sets
import de.alxgrk.input.Sources.ConfidenceLevel
import de.alxgrk.input.Sources.Coordinate
import de.alxgrk.monitoring.NewRelic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import persistence.FireData
import persistence.FireDataRepo
import java.math.BigDecimal
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

object ActiveFires {
    private val mutex = Mutex()

    private val fireDataRepo = FireDataRepo()

    private val referenceCache = LRUCache(20)

    val newFiresChannel = Channel<Pair<Coordinate, ConfidenceLevel>>(UNLIMITED)

    val activeFiresCacheSize = NewRelic.registry.gauge("activeFiresCacheSize", referenceCache) { it.size.toDouble() }
    val activeFiresCacheMisses = NewRelic.registry.counter("activeFiresCacheMisses")
    val activeFirePersitingTime = NewRelic.registry.summary("activeFirePersitingTimeInMs")

    suspend fun findSortedByCoordinates(reference: Coordinate): TreeMap<ReferenceCoordinate, ConfidenceLevel> =
        referenceCache[reference]
            ?: treeMapByHaversinDistance()
                .also { treeMap ->
                    activeFiresCacheMisses.increment()
                    mutex.withLock {
                        fireDataRepo.getAll().forEach { (_, coord, level) ->
                            val referenceCoordinate = ReferenceCoordinate(reference, coord)
                            treeMap[referenceCoordinate] = level
                        }
                        referenceCache[reference] = treeMap
                    }
                }

    @ExperimentalTime
    suspend fun store(newFires: Map<Coordinate, Pair<Sources, ConfidenceLevel>>) {
        mutex.withLock {
            val oldKeys = fireDataRepo.getAllCoordinates().toSet()
            val newKeys = newFires.keys
            val firesOngoing = Sets.intersection(oldKeys, newKeys)
            val firesRemoved = Sets.difference(oldKeys, newKeys)
            val firesAdded = Sets.difference(newKeys, oldKeys)

            NewRelic.registry.gauge("firesOngoing", firesOngoing.size.toDouble())
            NewRelic.registry.gauge("firesRemoved", firesRemoved.size.toDouble())
            NewRelic.registry.gauge("firesAdded", firesAdded.size.toDouble())

            fireDataRepo.removeAll(firesRemoved)
            (firesAdded + firesOngoing).forEach {
                val (source, confidenceLevel) = newFires[it]!!

                val (_, duration) = measureTimedValue {
                    fireDataRepo.create(FireData(source, it, confidenceLevel))
                }
                activeFirePersitingTime.record(duration.inMilliseconds)

                newFiresChannel.send(it to confidenceLevel)
            }
        }
    }

    private fun treeMapByHaversinDistance() = TreeMap<ReferenceCoordinate, ConfidenceLevel> { o1, o2 ->
        o1.haversinDistanceInKm.compareTo(o2.haversinDistanceInKm)
    }

    fun size(): Long = fireDataRepo.count()
}

data class ReferenceCoordinate(
    val reference: Coordinate,
    val coordinate: Coordinate,
    val haversinDistanceInKm: BigDecimal = reference.haversinDistance(coordinate) / BigDecimal(1000)
)

class LRUCache(private val maxSize: Int) :
    LinkedHashMap<Coordinate, TreeMap<ReferenceCoordinate, ConfidenceLevel>>(maxSize, 0.75f, true) {

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Coordinate, TreeMap<ReferenceCoordinate, ConfidenceLevel>>?): Boolean {
        return this.size > maxSize
    }
}
