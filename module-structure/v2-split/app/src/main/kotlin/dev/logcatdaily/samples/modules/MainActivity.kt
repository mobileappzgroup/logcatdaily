package dev.logcatdaily.samples.modules

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.logcatdaily.samples.modules.cart.impl.CartRepository
import dev.logcatdaily.samples.modules.cart.impl.CartScreen
import dev.logcatdaily.samples.modules.catalog.CatalogScreen
import dev.logcatdaily.samples.modules.checkout.impl.CheckoutScreen
import dev.logcatdaily.samples.modules.ui.AppTheme

class CartHolder : ViewModel() {
    val repository = CartRepository()
}

class MainActivity : ComponentActivity() {
    private val holder: CartHolder by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val navController = rememberNavController()
                val graph = remember(navController) { AppGraph(holder.repository, navController) }
                Scaffold(topBar = { TopAppBar(title = { Text("Shop") }) }) { padding ->
                    NavHost(navController, startDestination = "catalog", modifier = Modifier.padding(padding)) {
                        composable("catalog") {
                            CatalogScreen(graph.cartWriter, onOpenCart = { navController.navigate("cart") })
                        }
                        composable("cart") { CartScreen(graph.cartReader, graph.checkoutLauncher) }
                        composable("checkout") { CheckoutScreen(graph.cartReader) }
                    }
                }
            }
        }
    }
}
