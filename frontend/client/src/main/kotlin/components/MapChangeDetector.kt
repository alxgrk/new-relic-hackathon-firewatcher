package client.components

import react.*

interface MapChangeDetectorProps : RProps {
    var onCenterChanged: (LatLon) -> Unit
    var onRadiusChanged: (Double) -> Unit
}

val MapChangeDetector = functionalComponent<MapChangeDetectorProps> { props ->

    val map = useMap()
    useEffect(listOf()) {
        map.on("zoomend") {
            val mapHeightInMeters = map.getBounds().getSouthEast().distanceTo(map.getBounds().getNorthEast()).toDouble()
            props.onRadiusChanged((mapHeightInMeters / 1000).round())
        }
        map.on("moveend") {
            val center = map.getCenter()
            props.onCenterChanged(LatLon(center.lat.toDouble().round(5), center.lng.toDouble().round(5)))
        }
    }
}

fun RBuilder.mapChangeDetector(onCenterChanged: (LatLon) -> Unit, onRadiusChanged: (Double) -> Unit) = child(MapChangeDetector) {
    attrs {
        this.onCenterChanged = onCenterChanged
        this.onRadiusChanged = onRadiusChanged
    }
}
