package client

import client.components.fog
import client.components.loadingComponent
import client.components.searchArea
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import react.*
import react.dom.main

val scope = MainScope()

val App = functionalComponent<RProps> {

    val (serviceWorkerState, setServiceWorkerState) = useState<ServiceWorkerState>(ServiceWorkerState.Loading)
    val (pushManagerState, setPushManagerState) = useState<PushManagerState>(PushManagerState.Loading)

    suspend fun loadServiceWorkerState() {
        try {
            val swRegistration = window.navigator.serviceWorker.register("/serviceWorker.js").await()
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
            // use your own VAPID public key
            val publicKey = "BLceSSynHW5gDWDz-SK5mmQgUSAOzs_yXMPtDO0AmNsRjUllTZsdmDU4_gKvTr_q1hA8ZX19xLbGe28Bkyvwm3E"
            pushManager.subscribe(
                PushSubscriptionOptions(userVisibleOnly = true, applicationServerKey = publicKey)
            ).await()

            // send subscription to server

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
