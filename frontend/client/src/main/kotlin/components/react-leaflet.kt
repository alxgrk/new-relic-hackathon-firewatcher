@file:JsModule("react-leaflet")
@file:JsNonModule

package client.components

import react.RClass
import react.RProps

external val MapContainer: RClass<MapContainerProps>

external interface MapContainerProps : RProps {
    var center: Array<Double>
    var zoom: Number
    var minZoom: Number
    var maxZoom: Number
    var attributionControl: Boolean
    var zoomControl: Boolean
}

external val TileLayer: RClass<TileLayerProps>

external interface TileLayerProps : RProps {
    var attribution: String
    var url: String
}

external val Marker: RClass<MarkerProps>

external interface MarkerProps : RProps {
    var position: Array<Double>
}

external val Popup: RClass<RProps>

external fun useMap(): Map