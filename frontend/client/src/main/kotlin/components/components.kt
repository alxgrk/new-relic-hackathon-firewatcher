package client.components

import react.RBuilder
import react.RHandler
import react.RProps
import react.child

fun RBuilder.loadingComponent() = LoadingSpinner {
    attrs {
        type = "MutatingDots"
        color = "green"
        height = 10
        width = 10
    }
}

fun RBuilder.fog() = child(Fog)

fun RBuilder.autocompleteInput(
    disabled: Boolean,
    options: Array<String>,
    onRequestOptions: (String) -> Unit,
    onSelect: (String) -> Unit
) = TextInput {
    attrs {
        this.trigger = ""
        this.minChars = 3
        this.options = options
        this.requestOnlyIfNoOptions = false
        this.onRequestOptions = onRequestOptions
        this.onSelect = onSelect
        this.Component = "input"
        this.disabled = disabled
    }
}

fun RBuilder.searchArea(props: RProps, handler: RHandler<RProps>) = child(SearchArea, props, handler)

fun RBuilder.leaflet(latLng: LatLng, handler: RHandler<LeafletProps>) = child(Leaflet) {
    attrs {
        this.latLng = latLng
    }
    handler()
}

fun RBuilder.mapContainer(center: Array<Double>, zoom: Number, handler: RBuilder.() -> Unit) = MapContainer {
    attrs {
        this.center = center
        this.zoom = zoom
    }
    handler()
}

fun RBuilder.tileLayer(attribution: String, url: String, handler: RBuilder.() -> Unit) = TileLayer {
    attrs {
        this.attribution = attribution
        this.url = url
    }
    handler()
}

fun RBuilder.marker(position: Array<Double>, handler: RBuilder.() -> Unit) = Marker {
    attrs {
        this.position = position
    }
    handler()
}

fun RBuilder.popup(handler: RBuilder.() -> Unit) = Popup {
    handler()
}
