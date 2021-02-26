package client.components

import react.*

interface MapInteractionControllerProps : RProps {
    var mapLocked: Boolean
}

val MapInteractionController = functionalComponent<MapInteractionControllerProps> { props ->

    val map = useMap()
    useEffect(listOf(props.mapLocked)) {
        val mapAsDynamic = map.asDynamic()
        val handlers = mapAsDynamic._handlers
        if (props.mapLocked) {
            mapAsDynamic.zoomControl.disable()
            handlers.forEach { handler ->
                handler.disable()
            }
        } else {
            mapAsDynamic.zoomControl.enable()
            handlers.forEach { handler ->
                handler.enable()
            }
        }
        Unit
    }
}

fun RBuilder.mapInteractionController(mapLocked: Boolean) = child(MapInteractionController) {
    attrs {
        this.mapLocked = mapLocked
    }
}
