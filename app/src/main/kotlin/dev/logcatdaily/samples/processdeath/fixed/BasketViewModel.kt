package dev.logcatdaily.samples.processdeath.fixed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

private const val KEY = "items"

class BasketViewModel(
    val state:
        SavedStateHandle,
) : ViewModel() {
    val items =
        state.getStateFlow(
            KEY,
            listOf<String>(),
        )

    fun add(item: String) {
        state[KEY] = items.value + item
    }
}
