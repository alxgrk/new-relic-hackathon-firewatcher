package client.components

import client.PushManagerState
import client.network.FireDataAPI.fetchFireData
import client.network.FireDataResource
import client.network.FireMarker
import client.persistence.useLocalStorage
import client.scope
import kotlinext.js.jsObject
import kotlinx.coroutines.launch
import kotlinx.css.border
import kotlinx.css.padding
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*
import styled.css
import styled.styledDiv
import kotlin.math.pow
import kotlin.math.roundToInt

data class LatLon(val lat: Double, val lng: Double) {
    fun toArray() = arrayOf(lat.round(5), lng.round(5))
}

interface LeafletProps : RProps {
    var pushManagerState: PushManagerState
    var onSubscribe: (PushManagerState.NotSubscribed) -> Unit
    var onUnsubscribe: (PushManagerState.Subscribed) -> Unit
    var shouldClear: Boolean
    var latLon: LatLon
}

fun Double.round(decimals: Int = 3): Double = this.asDynamic().toFixed(decimals).toString().toDouble()

fun radiusToZoomLevel(maxRadius: Double): Int = (15.92648614 * (maxRadius.pow(-0.131722466))).roundToInt()

val Leaflet = functionalComponent<LeafletProps> { props ->
    val (center, setCenter) = useLocalStorage("leaflet-center", props.latLon)
    val (maxRadius, setMaxRadius) = useLocalStorage("leaflet-radius", 25.0)
    val (mapLocked, setMapLocked) = useLocalStorage("leaflet-map-lock", false)
    val (fireMarkers, setFireMarkers) = useState(arrayOf<FireMarker>())

    useEffect(listOf(props.shouldClear)) {
        if (props.shouldClear) {
            setCenter(props.latLon)
            setMaxRadius(25.0)
            setMapLocked(false)
            (props.pushManagerState as? PushManagerState.Subscribed)?.let {
                props.onUnsubscribe(it)
            }
        }
    }

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

        mapContainer(
            center = arrayOf(center.lat.round(5), center.lng.round(5)),
            zoom = radiusToZoomLevel(maxRadius),
            mapLocked
        ) {
            // map interactions
            mapChangeDetector({ setCenter(it) }) { radius ->
                setMaxRadius(radius)
            }
            mapInteractionController(mapLocked)

            // UI components
            tileLayer(
                attribution = "&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors",
                url = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            )
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
        figure {
            val closest = fireMarkers.firstOrNull()
            if (closest != null) {
                h4 {
                    +"The closest fire is ${closest.distanceInKilometer.round()}km away (${closest.latitude.round(5)},${
                    closest.longitude.round(
                        5
                    )
                    })."
                }
            } else {
                h4 {
                    +"No fires nearby."
                }
            }

            when (props.pushManagerState) {
                is PushManagerState.NotSubscribed -> {
                    button {
                        attrs {
                            onClickFunction = {
                                setMapLocked(true)
                                props.onSubscribe(props.pushManagerState as PushManagerState.NotSubscribed)
                            }
                        }
                        +"Subscribe"
                    }
                    p {
                        +"Subscribe to updates on new fires closer than ${maxRadius}km around your location (${center.lat.round()}, ${center.lng.round()})"
                    }
                }
                is PushManagerState.Subscribed -> {
                    button {
                        attrs {
                            onClickFunction = {
                                setMapLocked(false)
                                props.onUnsubscribe(props.pushManagerState as PushManagerState.Subscribed)
                            }
                        }
                        +"Unsubscribe"
                    }
                    p {
                        +"Subscribed to updates on new fires closer than ${maxRadius}km around your location (${center.lat.round()}, ${center.lng.round()})"
                    }
                }
                PushManagerState.NotSupported -> h2 {
                    +"Push API is not supported on this browser"
                }
                PushManagerState.Loading -> loadingComponent()
            }
        }
    }
}

fun RBuilder.leaflet(
    pushManagerState: PushManagerState,
    onSubscribe: (PushManagerState.NotSubscribed) -> Unit,
    onUnsubscribe: (PushManagerState.Subscribed) -> Unit,
    shouldClear: Boolean,
    latLon: LatLon
) = child(Leaflet) {
    attrs {
        this.pushManagerState = pushManagerState
        this.onSubscribe = onSubscribe
        this.onUnsubscribe = onUnsubscribe
        this.shouldClear = shouldClear
        this.latLon = latLon
    }
}
