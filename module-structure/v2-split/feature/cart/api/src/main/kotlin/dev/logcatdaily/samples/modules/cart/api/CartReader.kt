package dev.logcatdaily.samples.modules.cart.api

import dev.logcatdaily.samples.modules.model.Money
import kotlinx.coroutines.flow.Flow

interface CartReader {
    val total: Flow<Money>
}
