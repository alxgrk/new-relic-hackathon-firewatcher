package client.persistence

import kotlinx.browser.window
import react.RBuilder
import react.useState

fun <T> RBuilder.useLocalStorage(key: String, initialValue: T): Pair<T, (dynamic) -> Unit> {

    // State to store our value
    // Pass initial state function to useState so logic is only executed once
    val (storedValue, setStoredValue) = useState {

        try {
            // Get from local storage by key
            val item = window.localStorage.getItem(key)

            // Parse stored json or if none return initialValue
            if (item != null) JSON.parse(item) else initialValue
        } catch (error: Exception) {
            // If error also return initialValue
            console.log(error)

            initialValue
        }
    }

    // Return a wrapped version of useState's setter function that ...
    // ... persists the new value to localStorage.
    val setValue = { valueOrFunction: dynamic -> // (value: T | ((val: T) => T))

        try {
            // Allow value to be a function so we have same API as useState
            val valueToStore: T = if (js("valueOrFunction instanceof Function") as Boolean)
                valueOrFunction(storedValue) as T
            else valueOrFunction as T

            // Save state
            setStoredValue(valueToStore)

            // Save to local storage
            window.localStorage.setItem(key, JSON.stringify(valueToStore))
        } catch (error: Exception) {
            // A more advanced implementation would handle the error case
            console.log(error)
        }
    }

    return storedValue to setValue
}
