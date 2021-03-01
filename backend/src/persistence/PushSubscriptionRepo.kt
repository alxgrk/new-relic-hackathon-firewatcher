package de.alxgrk.persistence

import de.alxgrk.input.Sources
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import push.Keys
import push.PushSubscription

class PushSubscriptionRepo {

    fun create(subscription: PushSubscription, coordinate: Sources.Coordinate, maxRadiusKm: Double) {
        transaction {
            PushSubscriptionTable.insert {
                it[this.endpoint] = subscription.endpoint
                it[this.auth] = subscription.keys.auth
                it[this.key] = subscription.keys.p256dh
                it[this.latitude] = coordinate.latitude
                it[this.longitude] = coordinate.longitude
                it[this.maxRadiusKm] = maxRadiusKm.toBigDecimal()
            }
        }
    }

    fun findMatching(newFireCoord: Sources.Coordinate): List<PushSubscription> = transaction {
        PushSubscriptionTable.selectAll()
            .filter {
                val lat = it[PushSubscriptionTable.latitude]
                val lon = it[PushSubscriptionTable.longitude]
                Sources.Coordinate(lat, lon).haversinDistance(newFireCoord) < it[PushSubscriptionTable.maxRadiusKm]
            }
            .map {
                PushSubscription(
                    it[PushSubscriptionTable.endpoint],
                    Keys(it[PushSubscriptionTable.auth], it[PushSubscriptionTable.key])
                )
            }
    }

    fun remove(subscription: PushSubscription) {
        transaction {
            PushSubscriptionTable.deleteWhere { equal(subscription) }
        }
    }

    fun count(): Long = transaction {
        PushSubscriptionTable.slice(PushSubscriptionTable.id.countDistinct()).selectAll().count()
    }

    private fun SqlExpressionBuilder.equal(pushSubscription: PushSubscription) =
        (PushSubscriptionTable.endpoint eq pushSubscription.endpoint) and (PushSubscriptionTable.auth eq pushSubscription.keys.auth) and (PushSubscriptionTable.key eq pushSubscription.keys.p256dh)
}
