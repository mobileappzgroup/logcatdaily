package dev.logcatdaily.samples.modules.cart

import dev.logcatdaily.samples.modules.model.DemoProducts
import dev.logcatdaily.samples.modules.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

object CartTotals {
    private val ids = MutableStateFlow<List<String>>(emptyList())

    val total: Flow<Money> = ids.map { list ->
        Money(list.sumOf { id -> DemoProducts.all.firstOrNull { it.id == id }?.price?.cents ?: 0L })
    }

    fun add(productId: String) {
        ids.update { it + productId }
    }
}
