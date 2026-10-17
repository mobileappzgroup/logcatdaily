package dev.logcatdaily.samples.edgetoedge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.logcatdaily.samples.ui.Avatar
import dev.logcatdaily.samples.ui.LogcatDailyTheme

// The theme of this activity opts out of edge-to-edge (values-v35). That works at targetSdk 35
// and is disabled at targetSdk 36 on Android 16.
// adb shell am start -n dev.logcatdaily.samples/.edgetoedge.EdgeToEdgeActivity --es variant broken
class EdgeToEdgeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fixed = intent.getStringExtra("variant") == "fixed"
        if (fixed) enableEdgeToEdge()
        setContent {
            LogcatDailyTheme {
                Column(Modifier.fillMaxSize()) {
                    if (fixed) FixedToolbar() else BrokenToolbar()
                    LazyColumn(Modifier.weight(1f)) {
                        items((0 until 30).toList()) { n ->
                            OrderRow(n)
                        }
                    }
                }
            }
        }
    }
}

private val customers = listOf("Asha", "Ravi", "Meera", "Nishant", "Kabir", "Isha")
private val amounts = listOf(240, 180, 560, 95, 1320, 410, 275)

@Composable
private fun OrderRow(n: Int) {
    val customer = customers[n % customers.size]
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(customer, size = 40.dp)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                text = "Order #${1000 + n}",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            )
            Text(
                text = "$customer · ${9 + n / 6}:${(n * 7 % 60).toString().padStart(2, '0')}",
                color = MaterialTheme.colorScheme.outline,
                fontSize = 13.sp,
            )
        }
        Text(
            text = "₹${amounts[n % amounts.size]}",
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
    }
}
