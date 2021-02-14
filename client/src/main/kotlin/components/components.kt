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

fun RBuilder.autocompleteInput(options: Array<String>, onRequestOptions: (String) -> Unit, onSelect: (String) -> Unit) = TextInput {
    attrs {
        this.trigger = ""
        this.minChars = 3
        this.options = options
        this.requestOnlyIfNoOptions = false
        this.onRequestOptions = onRequestOptions
        this.onSelect = onSelect
        this.Component = "input"
    }
}

fun RBuilder.searchArea(props: RProps, handler: RHandler<RProps>) = child(SearchArea, props, handler)
