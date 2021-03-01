package de.alxgrk.persistence

import org.jetbrains.exposed.dao.id.UUIDTable

object PushSubscriptionTable : UUIDTable() {
    val endpoint = varchar("endpoint", 500)
    val auth = varchar("auth", 500)
    val key = varchar("key", 500)
    val latitude = decimal("latitude", 9, 6)
    val longitude = decimal("longitude", 9, 6)
    val maxRadiusKm = decimal("maxRadiusKm", 7, 3)
}
