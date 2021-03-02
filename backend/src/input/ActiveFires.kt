package de.alxgrk.input

import com.google.common.collect.Sets
import de.alxgrk.input.Sources.ConfidenceLevel
import de.alxgrk.input.Sources.Coordinate
import de.alxgrk.monitoring.NewRelic
import io.micrometer.core.instrument.ImmutableTag
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import persistence.FireData
import persistence.FireDataRepo
import java.math.BigDecimal
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

private val logger = KotlinLogging.logger {}

object ActiveFires {
    private val mutex = Mutex()

    private val fireDataRepo = FireDataRepo()

    val newFiresChannel = Channel<Pair<Coordinate, ConfidenceLevel>>(UNLIMITED)

    private val activeFirePersitingTime = NewRelic.registry.summary("activeFirePersitingTimeInMs")

    suspend fun findSortedByCoordinates(reference: Coordinate): TreeMap<ReferenceCoordinate, ConfidenceLevel> =
        treeMapByHaversinDistance()
            .also { treeMap ->
                mutex.withLock {
                    fireDataRepo.getAll().forEach { (_, coord, level) ->
                        val referenceCoordinate = ReferenceCoordinate(reference, coord)
                        treeMap[referenceCoordinate] = level
                    }
                }
            }

    @ExperimentalTime
    suspend fun store(newFires: Map<Sources, MutableMap<Coordinate, ConfidenceLevel>>) {
        mutex.withLock {
            val currentFires = fireDataRepo.getAllCoordinatesForSource()
            newFires.entries.forEach { (source, data) ->
                val currentRows = currentFires[source]?.toMap() ?: mapOf()
                val currentCoords = currentRows.keys
                val newCoords = data.keys.toSortedSet { c1, c2 -> c1.latitude.compareTo(c2.latitude) }
                val firesOngoing = Sets.intersection(currentCoords, newCoords)
                val firesRemoved = Sets.difference(currentCoords, newCoords)
                val firesAdded = Sets.difference(newCoords, currentCoords)

                NewRelic.registry.gauge(
                    "firesOngoing",
                    listOf(ImmutableTag("source", source.name)),
                    firesOngoing.size.toDouble()
                )
                NewRelic.registry.gauge(
                    "firesRemoved",
                    listOf(ImmutableTag("source", source.name)),
                    firesRemoved.size.toDouble()
                )
                NewRelic.registry.gauge(
                    "firesAdded",
                    listOf(ImmutableTag("source", source.name)), firesAdded.size.toDouble()
                )

                val deletions = fireDataRepo.removeAll(firesRemoved.map { currentRows[it]!! })
                logger.debug { "Successfully deleted $deletions rows for source $source." }
                firesAdded.forEach {
                    val confidenceLevel = data[it]!!

                    val (_, duration) = measureTimedValue {
                        fireDataRepo.create(FireData(source, it, confidenceLevel))
                    }
                    activeFirePersitingTime.record(duration.inMilliseconds)

                    newFiresChannel.send(it to confidenceLevel)
                }
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
