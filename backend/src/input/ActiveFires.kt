package de.alxgrk.input

import com.google.common.collect.Sets
import de.alxgrk.input.Sources.ConfidenceLevel
import de.alxgrk.input.Sources.Coordinate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.*

object ActiveFires {

    private val mutex = Mutex()

    private val backingMap = HashMap<Coordinate, ConfidenceLevel>()

    private val referenceCache = LRUCache(100)

    val newFiresChannel = Channel<Pair<Coordinate, ConfidenceLevel>>(UNLIMITED)

    suspend fun store(newFires: Map<Coordinate, ConfidenceLevel>) {
        mutex.withLock {
            val oldKeys = backingMap.keys
            val newKeys = newFires.keys
            val firesOngoing = Sets.intersection(oldKeys, newKeys)
            val firesRemoved = Sets.difference(oldKeys, newKeys)
            val firesAdded = Sets.difference(newKeys, oldKeys)

            firesRemoved.forEach { backingMap.remove(it) }
            (firesAdded + firesOngoing).forEach {
                val confidenceLevel = newFires[it]!!
                backingMap[it] = confidenceLevel
                newFiresChannel.send(it to confidenceLevel)
            }
        }
    }

    suspend fun findSortedByCoordinates(reference: Coordinate): TreeMap<ReferenceCoordinate, ConfidenceLevel> =
        referenceCache[reference]
            ?: treeMapByHaversinDistance()
                .also { treeMap ->
                    mutex.withLock {
                        backingMap.forEach { (k, v) ->
                            val referenceCoordinate = ReferenceCoordinate(reference, k)
                            treeMap[referenceCoordinate] = v
                        }
                        referenceCache[reference] = treeMap
                    }
                }

    private fun treeMapByHaversinDistance() = TreeMap<ReferenceCoordinate, ConfidenceLevel> { o1, o2 ->
        o1.haversinDistanceInKm.compareTo(o2.haversinDistanceInKm)
    }

    fun size(): Int = backingMap.size
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
