package client.components

import client.PushManagerState
import client.network.NominatimAPI
import client.network.NominatimAPI.fetchPlaces
import client.network.NominatimResource.Status.*
import client.persistence.useLocalStorage
import client.scope
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.css.paddingTop
import kotlinx.css.pct
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*
import styled.css
import styled.styledDiv

interface SearchAreaProps : RProps {
    var pushManagerState: PushManagerState
    var onSubscribe: (PushManagerState.NotSubscribed) -> Unit
    var onUnsubscribe: (PushManagerState.Subscribed) -> Unit
}

val SearchArea = functionalComponent<SearchAreaProps> { props ->
    val (query, setQuery) = useState("")
    val (shouldClear, setShouldClear) = useState(false)
    val (selected, setSelected) = useLocalStorage<NominatimAPI.Place?>("searcharea-selected", null)
    val (options, setOptions) = useState(arrayOf<String>())
    val (places, setPlaces) = useState(arrayOf<NominatimAPI.Place>())

    val onRequestOptions = { part: String ->
        console.log("Part: $part")
        if (part.length >= 3) {
            setQuery(part)
        }
    }

    val onSelect = { selection: String ->
        console.log("Selected: $selection")
        setSelected(places.toList().first { selection.startsWith(it.display_name, ignoreCase = true) })
    }

    useEffectWithCleanup(listOf(query)) {
        val job = scope.launch {
            val resource = fetchPlaces(query)
            when (resource.status) {
                LOADING -> setOptions(arrayOf()) // TODO
                ERROR -> setOptions(arrayOf()) // TODO
                SUCCESS -> {
                    setPlaces(resource.data.toTypedArray())
                    setOptions(resource.data.map { it.display_name }.toTypedArray())
                }
            }
        }
        return@useEffectWithCleanup {
            if (job.isActive) job.cancel()
        }
    }

    styledDiv {
        css {
            "h1" {
                paddingTop = 5.pct
            }
        }
        hGroup {
            h1 {
                +"\uD83D\uDD25 Firewatcher \uD83D\uDD25"
            }
            h3 {
                +"Better be warned when it gets hot nearby."
            }
        }
        div("grid") {
            autocompleteInput(
                value = selected?.display_name,
                disabled = selected != null,
                options = options,
                onRequestOptions = onRequestOptions,
                onSelect = onSelect
            )
            if (selected != null) {
                a(classes = "button") {
                    attrs {
                        onClickFunction = {
                            setQuery("")
                            setShouldClear(true)
                            window.setTimeout(
                                {
                                    setShouldClear(false)
                                    setSelected(null)
                                },
                                500
                            )
                            setOptions(arrayOf())
                            setPlaces(arrayOf())
                            (props.pushManagerState as? PushManagerState.Subscribed)?.let {
                                props.onUnsubscribe(it)
                            }
                        }
                    }
                    +"Clear"
                }
            }
        }
        if (selected != null) {
            leaflet(
                props.pushManagerState,
                props.onSubscribe,
                props.onUnsubscribe,
                shouldClear,
                latLon = LatLon(selected.lat.toDouble(), selected.lon.toDouble())
            )
        }
    }
}

fun RBuilder.searchArea(
    pushManagerState: PushManagerState,
    onSubscribe: (PushManagerState.NotSubscribed) -> Unit,
    onUnsubscribe: (PushManagerState.Subscribed) -> Unit
) = child(SearchArea) {
    attrs {
        this.pushManagerState = pushManagerState
        this.onSubscribe = onSubscribe
        this.onUnsubscribe = onUnsubscribe
    }
}
