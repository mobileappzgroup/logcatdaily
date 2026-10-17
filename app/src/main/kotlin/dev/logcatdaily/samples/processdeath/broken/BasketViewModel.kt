package dev.logcatdaily.samples.processdeath.broken

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BasketViewModel : ViewModel() {
    private val _items =
        MutableStateFlow(
            listOf<String>()
        )
    val items = _items.asStateFlow()

    fun add(item: String) {
        _items.update { it + item }
    }
}
