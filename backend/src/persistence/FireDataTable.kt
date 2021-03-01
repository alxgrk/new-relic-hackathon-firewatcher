package persistence

import de.alxgrk.input.Sources
import org.jetbrains.exposed.dao.id.UUIDTable

object FireDataTable : UUIDTable() {
    val fireDataSource = enumeration("fireDataSource", Sources::class)
    val latitude = decimal("latitude", 9, 6)
    val longitude = decimal("longitude", 9, 6)
    val confidenceLevel = enumeration("confidenceLevel", Sources.ConfidenceLevel::class)

    init {
        index("coordinates", true, latitude, longitude)
    }
}
