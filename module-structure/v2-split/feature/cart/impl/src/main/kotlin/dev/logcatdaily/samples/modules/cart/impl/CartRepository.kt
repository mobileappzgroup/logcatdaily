package dev.logcatdaily.samples.modules.cart.impl

import dev.logcatdaily.samples.modules.cart.api.CartReader
import dev.logcatdaily.samples.modules.cart.api.CartWriter
import dev.logcatdaily.samples.modules.model.DemoProducts
import dev.logcatdaily.samples.modules.model.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class CartRepository : CartReader, CartWriter {
    private val ids = MutableStateFlow<List<String>>(emptyList())

    override val total: Flow<Money> = ids.map { list ->
        Money(list.sumOf { id -> DemoProducts.all.firstOrNull { it.id == id }?.price?.cents ?: 0L })
    }

    override fun add(productId: String) {
        ids.update { it + productId }
    }
}
