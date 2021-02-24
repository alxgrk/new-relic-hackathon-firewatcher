package client.components

import client.network.FireDataAPI.fetchFireData
import client.network.FireDataResource
import client.network.FireMarker
import client.scope
import kotlinext.js.jsObject
import kotlinx.coroutines.launch
import kotlinx.css.border
import kotlinx.css.padding
import react.*
import react.dom.article
import react.dom.footer
import styled.css
import styled.styledDiv

data class LatLon(val lat: Double, val lng: Double) {
    fun toArray() = arrayOf(lat.round(5), lng.round(5))
}

interface LeafletProps : RProps {
    var latLon: LatLon
}

fun Double.round(decimals: Int = 3): Double = this.asDynamic().toFixed(decimals).toString().toDouble()

val Leaflet = functionalComponent<LeafletProps> { props ->
    val (center, setCenter) = useState(props.latLon)
    val (maxRadius, setMaxRadius) = useState(50.0)
    val (fireMarkers, setFireMarkers) = useState(arrayOf<FireMarker>())

    useEffect(listOf(center, maxRadius)) {
        scope.launch {
            val resource = fetchFireData(center, 0.0, maxRadius)
            when (resource.status) {
                FireDataResource.Status.LOADING -> setFireMarkers(arrayOf()) // TODO
                FireDataResource.Status.ERROR -> setFireMarkers(arrayOf()) // TODO
                FireDataResource.Status.SUCCESS -> {
                    setFireMarkers(resource.data.toTypedArray())
                }
            }
        }
    }

    styledDiv {
        attrs.attributes["id"] = "mapid"
        css {
            ".leaflet-bar a" {
                padding = "0"
                border = "none"
            }
        }

        mapContainer(center = center.toArray(), zoom = 13) {
            tileLayer(
                attribution = "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors",
                url = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            )
            mapChangeDetector({ setCenter(it) }) { radius ->
                setMaxRadius(radius)
            }
            marker(position = props.latLon.toArray()) {
                popup {
                    +"Your Position"
                }
            }
            fireMarkers.forEach {
                marker(
                    position = arrayOf(it.latitude.round(5), it.longitude.round(5)),
                    icon = icon(
                        jsObject {
                            iconUrl = "fire-marker.png"
                        }
                    )
                ) {
                    popup {
                        +"Distance: ${it.distanceInKilometer.round()}, Confidence: ${it.confidenceLevel}"
                    }
                }
            }
        }
        article {
            val closest = fireMarkers.firstOrNull()
            if (closest != null) {
                +"The closest fire is ${closest.distanceInKilometer.round()}km away (${closest.latitude.round(5)},${closest.longitude.round(5)})."
            } else {
                +"No fires nearby."
            }
            footer {
                +"Subscribe to updates on new fires closer than ${maxRadius}km around your location (${center.lat.round()},${center.lng.round()})"
            }
        }
    }
}

fun RBuilder.leaflet(latLon: LatLon) = child(Leaflet) {
    attrs {
        this.latLon = latLon
    }
}
