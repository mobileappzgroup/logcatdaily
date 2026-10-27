package dev.logcatdaily.samples.modules.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.logcatdaily.samples.modules.cart.api.CartWriter
import dev.logcatdaily.samples.modules.model.DemoProducts
import dev.logcatdaily.samples.modules.ui.PrimaryButton

@Composable
fun CatalogScreen(cart: CartWriter, onOpenCart: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DemoProducts.all.forEach { product ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${product.name}  ${product.price.format()}")
                PrimaryButton("Add", onClick = { cart.add(product.id) })
            }
        }
        PrimaryButton("Cart", onClick = onOpenCart)
    }
}
