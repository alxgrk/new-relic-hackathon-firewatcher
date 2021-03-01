package de.alxgrk.push

import de.alxgrk.input.ActiveFires
import de.alxgrk.monitoring.NewRelic
import de.alxgrk.persistence.PushSubscriptionRepo
import io.ktor.application.*

object NewFirePublisher {

    private val pushSubscriptionRepo = PushSubscriptionRepo()

    private val dangerousFiresDistribution = NewRelic.registry.summary("dangerousFires")

    suspend fun Application.listen() {
        for ((coord, confidenceLevel) in ActiveFires.newFiresChannel) {
            val matching = pushSubscriptionRepo.findMatching(coord)

            if (matching.isEmpty()) continue

            dangerousFiresDistribution.record(matching.size.toDouble())

            matching.forEach {
                with(SubscriptionManager) {
                    sendPushMessage(
                        it,
                        "Detected new fire at (${coord.latitude}, ${coord.longitude}) with confidence $confidenceLevel."
                            .toByteArray(Charsets.UTF_8)
                    )
                }
            }
        }
    }
}
