package app.spotlightguard.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Dernier arbre de vues vu dans Snapchat. Gardé en mémoire uniquement. */
object DiagnosticStore {
    private val _dump = MutableStateFlow("")
    val dump: StateFlow<String> = _dump.asStateFlow()

    fun set(text: String) {
        _dump.value = text
    }
}
