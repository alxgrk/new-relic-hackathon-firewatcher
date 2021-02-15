package client.components

import client.network.NominatimAPI.fetchPlaces
import client.network.NominatimResource.Status.*
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

val SearchArea = functionalComponent<RProps> { props ->
    val (disabled, setDisabled) = useState(false)
    val (part, setPart) = useState("")
    val (options, setOptions) = useState(arrayOf<String>())

    fun onRequestOptions(part: String) {
        console.log("Part: $part")
        if (part.length >= 3) {
            setPart(part)
        }
    }

    fun onSelect(selection: String) {
        console.log("Selected: $selection")
        setDisabled(true)
    }

    useEffectWithCleanup(listOf(part)) {
        val job = scope.launch {
            val resource = fetchPlaces(part)
            when (resource.status) {
                LOADING -> setOptions(arrayOf()) // TODO
                ERROR -> setOptions(arrayOf()) // TODO
                SUCCESS -> setOptions(resource.data.map { it.display_name }.toTypedArray())
            }
        }
        return@useEffectWithCleanup {
            if (job.isActive) job.cancel()
        }
    }

    styledDiv {
        css {
            "h1" {
                paddingTop = 20.pct
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
            disabled,
            options,
            onRequestOptions = { onRequestOptions(it) },
            onSelect = { onSelect(it) }
        )
        if (disabled) {
            
        }
    }
}
