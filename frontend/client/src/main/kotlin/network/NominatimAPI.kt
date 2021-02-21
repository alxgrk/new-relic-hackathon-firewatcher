package client.network

import client.network.NominatimAPI.Place
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.await

abstract class NominatimResource(val status: Status, val data: List<Place>, val message: String?) {

    class Success(data: List<Place>) : NominatimResource(Status.SUCCESS, data, null)

    class Error(message: String, data: List<Place> = emptyList()) : NominatimResource(Status.ERROR, data, message)

    object Loading : NominatimResource(Status.LOADING, emptyList(), null)

    enum class Status {
        SUCCESS,
        ERROR,
        LOADING
    }
}

object NominatimAPI {

    suspend fun CoroutineScope.fetchPlaces(query: String) =
        try {
            if (query.isBlank())
                NominatimResource.Loading
            else {
                val result = search(query)
                NominatimResource.Success(result)
            }
        } catch (e: Exception) {
            NominatimResource.Error(e.message ?: "unknown fetching error")
        }

    private suspend fun search(query: String) =
        window.fetch("https://nominatim.openstreetmap.org/search?format=jsonv2&q=$query")
            .await()
            .json()
            .await()
            .unsafeCast<Array<Place>>()
            .toList()

    data class Place(
        val place_id: Long,
        val licence: String,
        val boundingbox: List<String>,
        val lat: String,
        val lon: String,
        val display_name: String,
        val icon: String
    )
}
