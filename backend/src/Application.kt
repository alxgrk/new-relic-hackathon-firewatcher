package de.alxgrk

import com.fasterxml.jackson.databind.SerializationFeature
import de.alxgrk.input.ActiveFireScheduler
import de.alxgrk.input.ActiveFires
import de.alxgrk.input.Sources
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.slf4j.event.Level

fun main(args: Array<String>): Unit = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    ActiveFireScheduler().scheduleAcquisition()

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
        header(HttpHeaders.Authorization)
        allowCredentials = true
        host("localhost")
    }

    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    routing {
        get("/active-fires") {
            val minRadiusKm = call.request.queryParameters["minRadiusKm"]?.toInt() ?: 0
            val maxRadiusKm = call.request.queryParameters["maxRadiusKm"]?.toInt() ?: 50
            require(minRadiusKm in 0..maxRadiusKm) { "minRadiusKm must be larger than 0, but not larger than maxRadiusKm" }

            val lat = call.request.queryParameters["lat"]!!.toBigDecimal()
            val lon = call.request.queryParameters["lon"]!!.toBigDecimal()
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
                            ref.haversinDistanceInKm.toDouble()
                        )
                    }
            )
        }
    }
}
