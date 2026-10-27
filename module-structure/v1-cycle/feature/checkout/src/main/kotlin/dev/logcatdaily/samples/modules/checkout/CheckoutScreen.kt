package dev.logcatdaily.samples.modules.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.logcatdaily.samples.modules.cart.CartTotals
import dev.logcatdaily.samples.modules.model.Money

@Composable
fun CheckoutScreen(modifier: Modifier = Modifier) {
    val total = CartTotals.total.collectAsState(initial = Money(0)).value
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Checkout")
        Text("Pay ${total.format()}")
    }
}
