package client.components

import react.*
import react.dom.div

val Fog = functionalComponent<RProps> {
    div(classes = "fog fogLayerOne") {
        div(classes = "image01") {}
        div(classes = "image02") {}
    }
    div(classes = "fog fogLayerTwo") {
        div(classes = "image01") {}
        div(classes = "image02") {}
    }
    div(classes = "fog fogLayerThree") {
        div(classes = "image01") {}
        div(classes = "image02") {}
    }
}

fun RBuilder.fog() = child(Fog)