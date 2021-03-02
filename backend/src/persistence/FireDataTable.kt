package persistence

import de.alxgrk.input.Sources
import org.jetbrains.exposed.dao.id.UUIDTable

object FireDataTable : UUIDTable() {
    val fireDataSource = enumeration("fireDataSource", Sources::class)
    val latitude = decimal("latitude", 8, 2)
    val longitude = decimal("longitude", 8, 2)
    val confidenceLevel = enumeration("confidenceLevel", Sources.ConfidenceLevel::class)

    init {
        index("coordinatesPerSource", true, fireDataSource, latitude, longitude)
    }
}
