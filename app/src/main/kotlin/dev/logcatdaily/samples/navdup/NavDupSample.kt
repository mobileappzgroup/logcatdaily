package dev.logcatdaily.samples.navdup

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private const val TAG = "logcatdaily"

// currentBackStack is library-internal API. Used here only to print the whole stack.
@SuppressLint("RestrictedApi")
@Composable
fun NavDupSample(broken: Boolean) {
    val nav = rememberNavController()

    // Log the whole back stack every time it changes.
    LaunchedEffect(nav) {
        nav.currentBackStack.collect { stack ->
            Log.d(TAG, "stack: " + stack.mapNotNull { it.label() }.joinToString(" > "))
        }
    }

    NavHost(nav, startDestination = "list") {
        composable("list") { ListScreen { open(nav, broken) } }
        composable("detail/{id}") { DetailScreen(it.label() ?: "") }
    }
}

private fun open(nav: NavController, broken: Boolean) {
    Log.d(TAG, "navigate detail/7")
    if (broken) {
        nav.navigate("detail/7")
    } else {
        // Top entry is already detail/*: reuse it instead of pushing. Does not stop two
        // taps that open two different destinations; guard those with a RESUMED check.
        nav.navigate("detail/7") { launchSingleTop = true }
    }
}

private fun NavBackStackEntry.label(): String? =
    arguments?.getString("id")?.let { "detail/$it" } ?: destination.route?.takeIf { it == "list" }

@Composable
private fun ListScreen(onOpen: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Inbox", fontSize = 44.sp, fontWeight = FontWeight.Bold)
        Text("1 new message", fontSize = 26.sp, modifier = Modifier.padding(top = 8.dp, bottom = 40.dp))
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth().height(88.dp)) {
            Text("Open message", fontSize = 28.sp)
        }
    }
}

@Composable
private fun DetailScreen(route: String) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Message", fontSize = 44.sp, fontWeight = FontWeight.Bold)
        Text(route, fontSize = 28.sp, modifier = Modifier.padding(top = 8.dp))
    }
}
