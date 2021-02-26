package de.alxgrk.push

import io.ktor.application.*
import mu.KotlinLogging
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import nl.martijndwars.webpush.Utils
import push.PushSubscription

private val logger = KotlinLogging.logger {}

object SubscriptionManager {

    private val subscriptions = mutableListOf<PushSubscription>()

    fun store(subscription: PushSubscription) {
        subscriptions.add(subscription)
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
            throw RuntimeException(response.toString())
        }

        logger.info { "Successfully sent notification to endpoint ${sub.endpoint}" }
    }
}
