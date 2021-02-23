package client.network

import client.components.LatLon
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.await

abstract class FireDataResource(val status: Status, val data: List<FireMarker>, val message: String?) {

    class Success(data: List<FireMarker>) : FireDataResource(Status.SUCCESS, data, null)

    class Error(message: String, data: List<FireMarker> = emptyList()) : FireDataResource(Status.ERROR, data, message)

    object Loading : FireDataResource(Status.LOADING, emptyList(), null)

    enum class Status {
        SUCCESS,
        ERROR,
        LOADING
    }
}

object FireDataAPI {

    suspend fun CoroutineScope.fetchFireData(
        reference: LatLon,
        minRadiusKm: Double? = null,
        maxRadiusKm: Double? = null
    ) =
        try {
            val result = getFires(reference, minRadiusKm, maxRadiusKm)
            FireDataResource.Success(result)
        } catch (e: Exception) {
            FireDataResource.Error(e.message ?: "unknown fetching error")
        }

    private suspend fun getFires(reference: LatLon, minRadiusKm: Double?, maxRadiusKm: Double?): List<FireMarker> {
        val apiUrl = js("API_URL").toString()
        val minParam = if (minRadiusKm != null) "&minRadiusKm=$minRadiusKm" else ""
        val maxParam = if (maxRadiusKm != null) "&maxRadiusKm=$maxRadiusKm" else ""
        val url = "${apiUrl}active-fires?lat=${reference.lat}&lon=${reference.lng}$minParam$maxParam"
        return window.fetch(url)
            .await()
            .json()
            .await()
            .unsafeCast<Array<FireMarker>>()
            .toList()
    }
}

data class FireMarker(
    val latitude: Double,
    val longitude: Double,
    val confidenceLevel: String,
    val distanceInKilometer: Double
)
