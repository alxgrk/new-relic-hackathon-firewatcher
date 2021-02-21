package client.components

import kotlinx.css.height
import kotlinx.css.pct
import kotlinx.css.vh
import react.RProps
import react.dom.div
import react.functionalComponent
import styled.css
import styled.styledDiv

data class LatLng(val lat: Double, val lng: Double) {
    fun toArray() = arrayOf(lat, lng)
}

interface LeafletProps : RProps {
    var latLng: LatLng
}

val Leaflet = functionalComponent<LeafletProps> { props ->

    div {
        attrs.attributes["id"] = "mapid"

        mapContainer(center = props.latLng.toArray(), zoom = 13) {
            tileLayer(
                attribution = "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors",
                url = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            ) {
                marker(position = props.latLng.toArray()) {
                    popup {
                        +"A pretty CSS3 popup . < br / > Easily customizable."
                    }
                }
            }
        }
    }
}
