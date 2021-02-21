package client.components

import client.network.NominatimAPI
import client.network.NominatimAPI.fetchPlaces
import client.network.NominatimResource.Status.*
import client.scope
import kotlinx.coroutines.*
import kotlinx.css.paddingTop
import kotlinx.css.pct
import react.*
import react.dom.*
import styled.css
import styled.styledDiv

val SearchArea = functionalComponent<RProps> { props ->
    val (query, setQuery) = useState("")
    val (selected, setSelected) = useState<NominatimAPI.Place?>(null)
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
                paddingTop = 10.pct
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
        autocompleteInput(
            selected != null,
            options,
            onRequestOptions = onRequestOptions,
            onSelect = onSelect
        )
        if (selected != null) {
            leaflet(latLng = LatLng(selected.lat.toDouble(), selected.lon.toDouble())) {}
        }
    }
}
