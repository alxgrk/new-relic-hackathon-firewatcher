package client.components

import react.RBuilder

fun RBuilder.loadingComponent() = LoadingSpinner {
    attrs {
        type = "MutatingDots"
        color = "green"
        height = 10
        width = 10
    }
}

fun RBuilder.autocompleteInput(
    value: String?,
    disabled: Boolean,
    options: Array<String>,
    onRequestOptions: (String) -> Unit,
    onSelect: (String) -> Unit
) = TextInput {
    attrs {
        if (value != null)
            this.value = value
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

fun RBuilder.mapContainer(
    center: Array<Double>,
    zoom: Number,
    mapLocked: Boolean,
    handler: RBuilder.() -> Unit
) = MapContainer {
    attrs {
        this.center = center
        this.zoom = zoom
        this.minZoom = 7
        this.maxZoom = 16
        this.attributionControl = true
        this.zoomControl = true
    }
    handler()
}

fun RBuilder.tileLayer(attribution: String, url: String) = TileLayer {
    attrs {
        this.attribution = attribution
        this.url = url
    }
}

fun RBuilder.marker(position: Array<Double>, icon: Icon? = null, handler: RBuilder.() -> Unit) = Marker {
    attrs {
        this.position = position
        if (icon != null) this.icon = icon
    }
    handler()
}

fun RBuilder.popup(handler: RBuilder.() -> Unit) = Popup {
    handler()
}
