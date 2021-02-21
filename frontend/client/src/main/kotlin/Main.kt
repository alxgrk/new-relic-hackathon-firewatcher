package client

import kotlinext.js.jsObject
import kotlinx.browser.document
import kotlinx.css.*
import react.dom.render
import styled.injectGlobal

fun main() {
    val styles = CSSBuilder().apply {
        html {
            width = 100.pct
            height = 100.vh
        }
        body {
            margin = "0"
            padding = "0"
            color = Color.lightGrey
            backgroundColor = Color("#121212")
            width = 100.pct
            height = 100.vh
            overflowX = Overflow.hidden
        }
        "#root" {
            textAlign = TextAlign.center
        }
        "#mapid" {
            height = 40.vh
        }
        "#mapid div" {
            height = 100.pct
        }
        ".front" {
            position = Position.relative
        }
        h1 {
            margin = "0"
        }

        val primaryColor = Color("#fb8c00")
        val primaryHoverColor = Color("#ff9800")
        val primaryFocusColor = Color("rgba(251,140,0,0.25)")
        val primaryInverseColor = Color("#FFF")
        "[data-theme=\"dark\"]" {
            setCustomProperty("primary", primaryColor)
            setCustomProperty("primary-hover", primaryHoverColor)
            setCustomProperty("primary-focus", primaryFocusColor)
            setCustomProperty("primary-inverse", primaryInverseColor)
        }
        root {
            setCustomProperty("primary-border", primaryColor)
            setCustomProperty("primary-hover-border", primaryHoverColor)
            setCustomProperty("input-hover-border", primaryColor)
            setCustomProperty("input-focus", primaryFocusColor)
            setCustomProperty("input-inverse", primaryInverseColor)
        }
    }

    injectGlobal(styles.toString())

    render(document.getElementById("root")) {
        App(props = jsObject(), handler = {})
    }
}
