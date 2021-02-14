package client.components

import kotlinx.css.marginTop
import kotlinx.css.paddingTop
import kotlinx.css.pct
import react.RProps
import react.dom.h1
import react.dom.h3
import react.dom.hGroup
import react.functionalComponent
import react.useState
import styled.css
import styled.styledDiv

val SearchArea = functionalComponent<RProps> {
    val (options, setOptions) = useState(arrayOf<String>())

    fun onRequestOptions(part: String) {
        console.log("Part: $part")
    }

    fun onSelect(selection: String) {
        console.log("Selected: $selection")
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
        autocompleteInput(options = options, onRequestOptions = { onRequestOptions(it) }, onSelect = { onSelect(it) })
    }
}
