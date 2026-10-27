package dev.logcatdaily.samples.modules.cart.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.logcatdaily.samples.modules.cart.api.CartReader
import dev.logcatdaily.samples.modules.checkout.api.CheckoutLauncher
import dev.logcatdaily.samples.modules.model.Money
import dev.logcatdaily.samples.modules.ui.PrimaryButton

@Composable
fun CartScreen(reader: CartReader, checkout: CheckoutLauncher, modifier: Modifier = Modifier) {
    val total = reader.total.collectAsState(initial = Money(0)).value
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Nishant's cart")
        Text("Total ${total.format()}")
        PrimaryButton("Checkout", onClick = checkout::open)
    }
}
