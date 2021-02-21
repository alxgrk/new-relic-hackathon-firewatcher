package serviceWorker

import kotlinx.coroutines.*
import org.w3c.fetch.Response
import org.w3c.notifications.NotificationEvent
import org.w3c.notifications.NotificationOptions
import org.w3c.workers.*

external val self: ServiceWorkerGlobalScope
val scope = MainScope()

fun main() {
    installServiceWorker()
}

const val MAIN_CACHE = "mainCache"

fun installServiceWorker() {
    val offlineContent = arrayOf(
        "/index.html",
        "/client.js",
        "/android-chrome-192x192.png",
        "/android-chrome-512x512.png",
        "/apple-touch-icon.png",
        "/autocomplete.css",
        "/browserconfig.xml",
        "/favicon-16x16.png",
        "/favicon-32x32.png",
        "/favicon.ico",
        "/fog.css",
        "/manifest.webmanifest",
        "/mstile-150x150.png",
        "/safari-pinned-tab.svg"
    )

    self.addEventListener(
        "install",
        { event ->
            event as InstallEvent
            console.log("I am installed.")
            scope.async {
                val cache = self.caches.open(MAIN_CACHE).await()
                cache.addAll(offlineContent).await()
                console.log("Offline cache loaded.")
            }.let {
                event.waitUntil(it.asPromise())
            }
        }
    )

    // using the "Network falling back to cache" strategy (https://developers.google.com/web/ilt/pwa/caching-files-with-service-worker#network_falling_back_to_the_cache)
    self.addEventListener(
        "fetch",
        { event ->
            event as FetchEvent
            if (event.request.url.contains("http").not()) return@addEventListener

            scope.async {
                val cache = self.caches.open(MAIN_CACHE).await()
                try {
                    val response = self.fetch(event.request).await()
                    cache.put(event.request, response.clone()).await()
                    return@async response
                } catch (e: Throwable) {
                    return@async self.caches.match(event.request).await().unsafeCast<Response>()
                }
            }.let {
                event.respondWith(it.asPromise())
            }
        }
    )

    self.addEventListener(
        "push",
        { event ->
            event as PushEvent
            console.log("Push received.")
            val payloadString = event.data.text()

            event.waitUntil(
                self.registration.showNotification(
                    title = "New Fire Alert",
                    options = NotificationOptions(
                        tag = "tag",
                        body = payloadString,
                        icon = "/android-chrome-192x192.png",
                        badge = "/android-chrome-192x192.png"
                    )
                )
            )
        }
    )

    self.addEventListener(
        "notificationclick",
        { event ->
            event as NotificationEvent
            console.log("Notification click received.")
            event.notification.close()
            val selfUrlFromWebpackDefine = js("SELF_URL").toString()
            event.waitUntil(
                self.clients.openWindow(selfUrlFromWebpackDefine)
            )
        }
    )
}
