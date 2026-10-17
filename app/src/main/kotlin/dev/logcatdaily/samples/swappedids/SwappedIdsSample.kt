package dev.logcatdaily.samples.swappedids

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.logcatdaily.samples.swappedids.longids.openMyOrderBroken
import dev.logcatdaily.samples.ui.Avatar

private const val TAG = "logcatdaily"

// Demo data. The signed-in user is 3, the order they want is 1042.
class Order(val id: Long, val owner: String, val items: String, val total: String = "", val placed: String = "")

internal val orders = mapOf(
    3L to Order(3L, "Asha", "1 filter coffee", "₹45", "Today, 8:15"),
    1042L to Order(1042L, "Nishant", "2 masala dosa", "₹180", "Today, 9:02"),
)

internal const val SIGNED_IN_USER = 3L
internal const val WANTED_ORDER = 1042L

@JvmInline
value class OrderId(
    val value: Long,
)

@JvmInline
value class UserId(
    val value: Long,
)

fun loadOrder(
    orderId: OrderId,
    userId: UserId,
): Order = find(orderId, userId)

private fun find(
    orderId: OrderId,
    userId: UserId,
): Order {
    Log.d(TAG, "loadOrder orderId=${orderId.value} userId=${userId.value}")
    return orders.getValue(orderId.value)
}

internal fun openMyOrder(): Order {
    val userId = UserId(SIGNED_IN_USER)
    val orderId = OrderId(WANTED_ORDER)
    return loadOrder(orderId, userId)
}

@Composable
fun SwappedIdsSample(broken: Boolean) {
    var order by remember { mutableStateOf<Order?>(null) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar("Nishant", size = 52.dp)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(
                        text = "Nishant (user $SIGNED_IN_USER)",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Signed in",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Button(onClick = {
                order = if (broken) openMyOrderBroken() else openMyOrder()
            }) { Text("Open my order") }
            OrderCard(order)
        }
    }
}

@Composable
private fun OrderCard(order: Order?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (order != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Order #${order.id}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = order.total,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(order.owner, size = 28.dp)
                Text(
                    text = order.owner,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Text(text = order.items, fontSize = 16.sp, color = MaterialTheme.colorScheme.outline)
            Text(text = order.placed, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
