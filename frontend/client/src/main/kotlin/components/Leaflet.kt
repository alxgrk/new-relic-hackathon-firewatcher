package client.components

import client.network.FireDataAPI.fetchFireData
import client.network.FireDataResource
import client.network.FireMarker
import client.scope
import kotlinx.coroutines.launch
import react.*
import react.dom.div

data class LatLon(val lat: Double, val lng: Double) {
    fun toArray() = arrayOf(lat, lng)
}

interface LeafletProps : RProps {
    var latLon: LatLon
}

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

    div {
        attrs.attributes["id"] = "mapid"

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
                marker(position = arrayOf(it.latitude, it.longitude)) {
                    popup {
                        +"Distance: ${it.distanceInKilometer}, Confidence: ${it.confidenceLevel}"
                    }
                }
            }
        }
    }
}

fun RBuilder.leaflet(latLon: LatLon, handler: RHandler<LeafletProps>) = child(Leaflet) {
    attrs {
        this.latLon = latLon
    }
    handler()
}
