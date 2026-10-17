package dev.logcatdaily.samples.processdeath

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.logcatdaily.samples.ui.Avatar

private const val TAG = "logcatdaily"
private val FOOD = listOf("Milk", "Eggs", "Bread", "Rice", "Tea")
private val PRICE = mapOf("Milk" to 34, "Eggs" to 78, "Bread" to 45, "Rice" to 120, "Tea" to 210)

@Composable
fun ProcessDeathSample(
    items: List<String>,
    onAdd: (String) -> Unit,
) {
    // prints once per start of the screen, so a second line with a new pid is the restart
    LaunchedEffect(Unit) {
        Log.d(TAG, "basket opened, items=${items.size}")
    }
    Box(Modifier.fillMaxSize().padding(top = 160.dp), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar("Nishant", size = 40.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "Basket (${items.size})",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Nishant's groceries",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Button(onClick = {
                val next = FOOD[items.size % FOOD.size]
                Log.d(TAG, "added $next")
                onAdd(next)
            }) { Text("Add") }
            items.forEach {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        it,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "₹${PRICE.getValue(it)}",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            if (items.isNotEmpty()) {
                Text(
                    "Total ₹${items.sumOf { PRICE.getValue(it) }}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
