@file:JsModule("react-autocomplete-input")
@file:JsNonModule

package client.components

import react.*

@JsName("default")
external val TextInput: RClass<TextInputProps>

external interface TextInputProps : RProps {
    var Component: String
    var defaultValue: String
    var disabled: String
    var maxOptions: Int
    var onSelect: (String) -> Unit
    var onRequestOptions: (String) -> Unit
    var matchAny: Boolean
    var offsetX: Int
    var offsetY: Int
    var options: Array<out String>
    var regex: String
    var requestOnlyIfNoOptions: Boolean
    var spaceRemovers: Array<String>
    var spacer: String
    var trigger: String
    var minChars: Int
    var value: String
    var passThroughEnter: Boolean
}