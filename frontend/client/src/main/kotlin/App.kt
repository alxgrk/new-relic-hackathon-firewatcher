package client

import client.components.fog
import client.components.loadingComponent
import client.components.searchArea
import client.network.PushNotificationAPI
import client.network.PushNotificationAPI.registerPushNotification
import client.network.PushNotificationAPI.unregisterPushNotification
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set
import react.*
import react.dom.main

val scope = MainScope()

val App = functionalComponent<RProps> {

    val (serviceWorkerState, setServiceWorkerState) = useState<ServiceWorkerState>(ServiceWorkerState.Loading)
    val (pushManagerState, setPushManagerState) = useState<PushManagerState>(PushManagerState.Loading)

    suspend fun loadServiceWorkerState() {
        try {
            val swRegistration = window.navigator.serviceWorker.register("./serviceWorker.js").await()
            console.log("Successfully registered a service worker.", swRegistration)
            setServiceWorkerState(ServiceWorkerState.Registered(swRegistration = swRegistration))
        } catch (e: Exception) {
            console.warn(e.message)
            setServiceWorkerState(ServiceWorkerState.Failed(errorMessage = e.message))
        }
    }

    suspend fun loadPushManagerState(pushManager: PushManager?) {
        if (pushManager != null) {
            pushManager.getSubscription().await().let {
                setPushManagerState(
                    if (it != null) {
                        PushManagerState.Subscribed(pushManager = pushManager)
                    } else {
                        PushManagerState.NotSubscribed(pushManager = pushManager)
                    }
                )
            }
        } else {
            setPushManagerState(PushManagerState.NotSupported)
        }
    }

    useEffect(dependencies = listOf()) {
        scope.launch {
            loadServiceWorkerState()
        }
    }

    useEffect(dependencies = listOf(serviceWorkerState)) {
        scope.launch {
            if (serviceWorkerState is ServiceWorkerState.Registered) {
                loadPushManagerState(serviceWorkerState.swRegistration.pushManager)
            }
        }
    }

    fun subscribeUser(pushManager: PushManager) = scope.launch {
        try {
            val publicKey = urlBase64ToUint8Array("BL9pmkZIZqcl3vDmdwvR7wvBSZvxxsHrBLbrPkZgC7BXguEtHAAVaW2ukBGxN6l9B925UzG8lcrn1vGHWRBmt2k=")
            val subscription = pushManager.subscribe(
                PushSubscriptionOptions(userVisibleOnly = true, applicationServerKey = publicKey)
            ).await()

            // send subscription to server
            registerPushNotification(subscription)

            setPushManagerState(PushManagerState.Subscribed(pushManager))
            console.log("User subscribed")
        } catch (e: Exception) {
            console.warn("Subscription denied - ${e.message}")
        }
    }

    fun unsubscribeUser(pushManager: PushManager) {
        scope.launch {
            val subscription = pushManager.getSubscription().await()
            if (subscription != null) {
                try {
                    // send subscription to server
                    unregisterPushNotification(subscription)

                    subscription.unsubscribe().await()

                    setPushManagerState(PushManagerState.NotSubscribed(pushManager))
                    console.log("User unsubscribed")
                } catch (e: Exception) {
                    console.error("User unsubscription failed: ${e.message}")
                }
            }
        }
    }

    when (serviceWorkerState) {
        is ServiceWorkerState.Registered -> {

            fog()

            main("front container") {
                attrs {
                    attributes["data-theme"] = "dark"
                }

                searchArea(
                    pushManagerState,
                    { subscribeUser(it.pushManager) },
                    { unsubscribeUser(it.pushManager) }
                )
            }
        }
        is ServiceWorkerState.Failed -> window.alert("Error in registering service worker: ${serviceWorkerState.errorMessage}")
        ServiceWorkerState.Loading -> loadingComponent()
    }
}

fun RBuilder.App(props: RProps, handler: RHandler<RProps>) = child(App, props, handler)

fun urlBase64ToUint8Array(base64String: String): Uint8Array {
    val padding = "=".repeat((4 - base64String.length % 4) % 4)
    val base64 = (base64String + padding)
        .replace("-", "+")
        .replace("_", "/")

    val rawData = window.atob(base64)
    val outArray = Uint8Array(rawData.length)

    for (i in 0..rawData.length) {
        outArray[i] = rawData.asDynamic().charCodeAt(i)
    }
    return outArray
}

