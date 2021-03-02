package client.network

import client.PushSubscription
import client.components.LatLon
import kotlinext.js.jsObject
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.RequestInit
import kotlin.js.json

abstract class PushNotificationResource(val status: Status, val message: String?) {

    object Success : PushNotificationResource(Status.SUCCESS, null)

    class Error(message: String) : PushNotificationResource(Status.ERROR, message)

    object Loading : PushNotificationResource(Status.LOADING, null)

    enum class Status {
        SUCCESS,
        ERROR,
        LOADING
    }
}

object PushNotificationAPI {

    suspend fun registerPushNotification(subscription: PushSubscription, latLon: LatLon, maxRadius: Double) =
        try {
            send(
                "register-push",
                subscription,
                mapOf("lat" to latLon.lat, "lon" to latLon.lng, "maxRadius" to maxRadius)
            )
            PushNotificationResource.Success
        } catch (e: Exception) {
            PushNotificationResource.Error(e.message ?: "unknown fetching error")
        }

    suspend fun unregisterPushNotification(subscription: PushSubscription) =
        try {
            send(
                "unregister-push",
                subscription,
                mapOf()
            )
            PushNotificationResource.Success
        } catch (e: Exception) {
            PushNotificationResource.Error(e.message ?: "unknown fetching error")
        }

    private suspend fun send(path: String, subscription: PushSubscription, queryParams: Map<String, Double>) {
        val apiUrl = js("API_URL").toString()
        val queryParamsAsString = if (queryParams.isEmpty())
            "" else
            queryParams.entries.joinToString(separator = "&", prefix = "?") { "${it.key}=${it.value}" }
        val url = "${apiUrl}$path$queryParamsAsString"
        val config = jsObject<RequestInit> {
            method = "POST"
            headers = json("Content-Type" to "application/json")
            body = JSON.stringify(subscription)
        }
        val response = window.fetch(url, config).await()
        if (!response.ok)
            throw RuntimeException("Could not register for push notifications: response code was ${response.status}")
    }
}
