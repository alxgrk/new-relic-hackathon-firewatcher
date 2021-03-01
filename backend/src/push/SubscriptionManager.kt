package de.alxgrk.push

import de.alxgrk.input.Sources
import de.alxgrk.monitoring.NewRelic
import de.alxgrk.persistence.PushSubscriptionRepo
import io.ktor.application.*
import mu.KotlinLogging
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import nl.martijndwars.webpush.Utils
import push.PushSubscription

private val logger = KotlinLogging.logger {}

object SubscriptionManager {

    private val subscriptions = PushSubscriptionRepo()

    private val notificationsSent = NewRelic.registry.counter("notificationsSent")
    private val notificationsFailed = NewRelic.registry.counter("notificationsFailed")

    fun store(subscription: PushSubscription, coordinate: Sources.Coordinate, maxRadiusKm: Double) {
        subscriptions.create(subscription, coordinate, maxRadiusKm)
    }

    fun remove(subscription: PushSubscription) {
        subscriptions.remove(subscription)
    }

    fun Application.sendPushMessage(sub: PushSubscription, payload: ByteArray?) {

        // Create a notification with the endpoint, userPublicKey from the subscription and a custom payload
        val notification = Notification(
            sub.endpoint,
            sub.getUserPublicKey(),
            sub.keys.authAsBytes,
            payload
        )

        // Instantiate the push service, no need to use an API key for Push API
        val pushService = PushService()
        environment.config.propertyOrNull("ktor.environment.vapidPublicKeyBase64")?.getString()?.let {
            pushService.publicKey = Utils.loadPublicKey(it)
        }
        environment.config.propertyOrNull("ktor.environment.vapidPrivateKeyBase64")?.getString()?.let {
            pushService.privateKey = Utils.loadPrivateKey(it)
        }

        // Send the notification
        val response = pushService.send(notification)!!
        if (response.statusLine.statusCode != 201) {
            notificationsFailed.increment()
            throw RuntimeException(response.toString())
        }

        notificationsSent.increment()
        logger.info { "Successfully sent notification to endpoint ${sub.endpoint}" }
    }
}
