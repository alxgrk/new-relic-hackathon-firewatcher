package client.network

import client.PushSubscription
import kotlinext.js.jsObject
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
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

    suspend fun registerPushNotification(subscription: PushSubscription) =
        try {
            send("register-push", subscription)
            PushNotificationResource.Success
        } catch (e: Exception) {
            PushNotificationResource.Error(e.message ?: "unknown fetching error")
        }

    suspend fun unregisterPushNotification(subscription: PushSubscription) =
        try {
            send("unregister-push", subscription)
            PushNotificationResource.Success
        } catch (e: Exception) {
            PushNotificationResource.Error(e.message ?: "unknown fetching error")
        }

    private suspend fun send(path: String, subscription: PushSubscription) {
        val apiUrl = js("API_URL").toString()
        val url = "${apiUrl}$path"
        val config = jsObject<RequestInit> {
            method = "post"
            headers = json("Content-Type" to "application/json")
            body = JSON.stringify(subscription)
        }
        val response = window.fetch(url, config).await()
        if (!response.ok)
            throw RuntimeException("Could not register for push notifications: response code was ${response.status}")
    }
}
