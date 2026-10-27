package dev.logcatdaily.samples.modules

import androidx.navigation.NavController
import dev.logcatdaily.samples.modules.cart.api.CartReader
import dev.logcatdaily.samples.modules.cart.api.CartWriter
import dev.logcatdaily.samples.modules.cart.impl.CartRepository
import dev.logcatdaily.samples.modules.checkout.api.CheckoutLauncher

class AppGraph(cart: CartRepository, navController: NavController) {
    val cartReader: CartReader = cart
    val cartWriter: CartWriter = cart
    val checkoutLauncher = object : CheckoutLauncher {
        override fun open() = navController.navigate("checkout")
    }
}
