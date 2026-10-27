package dev.logcatdaily.samples.modules

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.logcatdaily.samples.modules.cart.CartScreen
import dev.logcatdaily.samples.modules.catalog.CatalogScreen
import dev.logcatdaily.samples.modules.checkout.CHECKOUT_ROUTE
import dev.logcatdaily.samples.modules.checkout.CheckoutScreen
import dev.logcatdaily.samples.modules.ui.AppTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val navController = rememberNavController()
                Scaffold(topBar = { TopAppBar(title = { Text("Shop") }) }) { padding ->
                    NavHost(navController, startDestination = "catalog", modifier = Modifier.padding(padding)) {
                        composable("catalog") {
                            CatalogScreen(onOpenCart = { navController.navigate("cart") })
                        }
                        composable("cart") { CartScreen(navController) }
                        composable(CHECKOUT_ROUTE) { CheckoutScreen() }
                    }
                }
            }
        }
    }
}
