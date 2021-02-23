@file:JsModule("leaflet")
@file:JsNonModule

package client.components

external class Map {
    fun getCenter(): LatLng
    fun getBounds(): LatLngBounds
    fun on(type: String, handler: () -> Unit): Map
}

external class LatLngBounds {
    fun getNorthEast(): LatLng
    fun getSouthEast(): LatLng
}

external class LatLng {
    val lat: Number
    val lng: Number
    fun distanceTo(otherLatLng: LatLng): Number
}