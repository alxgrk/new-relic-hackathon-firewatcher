package de.alxgrk

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import de.alxgrk.input.ActiveFireScheduler
import de.alxgrk.input.ActiveFires
import de.alxgrk.input.Sources
import de.alxgrk.monitoring.NewRelic
import de.alxgrk.monitoring.NewRelic.createGlobalRegistry
import de.alxgrk.push.NewFirePublisher
import de.alxgrk.push.NewFirePublisher.listen
import de.alxgrk.push.SubscriptionManager
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.metrics.micrometer.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import kotlinx.coroutines.*
import org.slf4j.event.Level
import persistence.DatabaseFactory
import push.PushSubscription
import java.math.RoundingMode
import kotlin.time.ExperimentalTime

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@ExperimentalTime
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    createGlobalRegistry()
    with(DatabaseFactory) { init() }
    ActiveFireScheduler().scheduleAcquisition()
    launch(Dispatchers.IO) {
        with(NewFirePublisher) { listen() }
    }

    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024) // condition
        }
    }

    install(AutoHeadResponse)

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.ContentType)
        header(HttpHeaders.Authorization)
        allowCredentials = true
        host("localhost:8080")
        host("alxgrk.github.io", schemes = listOf("https"))
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }

    install(MicrometerMetrics) {
        registry = NewRelic.registry
        distributionStatisticConfig = DistributionStatisticConfig.Builder()
            .percentilesHistogram(true)
            .build()
    }

    routing {

        post("register-push") {
            val subscription = call.receive<PushSubscription>()

            val lat = call.parameters["lat"]!!.toBigDecimal()
            val lon = call.parameters["lon"]!!.toBigDecimal()
            val maxRadiusKm = call.parameters["maxRadius"]!!.toDouble()

            with(SubscriptionManager) {
                store(subscription, Sources.Coordinate(lat, lon), maxRadiusKm)
                GlobalScope.launch {
                    delay(5000)
                    sendPushMessage(subscription, "Test Notification".toByteArray(Charsets.UTF_8))
                }
            }

            call.respond(HttpStatusCode.OK, "")
        }

        post("unregister-push") {
            val subscription = call.receive<PushSubscription>()

            SubscriptionManager.remove(subscription)

            call.respond(HttpStatusCode.OK, "")
        }

        get("/active-fires") {
            val minRadiusKm = call.parameters["minRadiusKm"]?.toDouble() ?: 0.0
            val maxRadiusKm = call.parameters["maxRadiusKm"]?.toDouble() ?: 50.0
            require(minRadiusKm in 0.0..maxRadiusKm) { "minRadiusKm must be larger than 0, but not larger than maxRadiusKm" }

            val lat = call.parameters["lat"]!!.toBigDecimal()
            val lon = call.parameters["lon"]!!.toBigDecimal()
            val reference = Sources.Coordinate(lat, lon)

            data class ResponseItem(
                val latitude: Double,
                val longitude: Double,
                val confidenceLevel: Sources.ConfidenceLevel,
                val distanceInKilometer: Double
            )

            call.respond(
                ActiveFires.findSortedByCoordinates(reference)
                    .entries
                    .filter { (ref, _) -> ref.haversinDistanceInKm in minRadiusKm.toBigDecimal()..maxRadiusKm.toBigDecimal() }
                    .map { (ref, c) ->
                        ResponseItem(
                            ref.coordinate.latitude.toDouble(),
                            ref.coordinate.longitude.toDouble(),
                            c,
                            ref.haversinDistanceInKm.setScale(3, RoundingMode.HALF_UP).toDouble()
                        )
                    }
            )
        }
    }
}
