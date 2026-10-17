package dev.logcatdaily.samples.swappedids.longids

import android.util.Log
import dev.logcatdaily.samples.swappedids.Order
import dev.logcatdaily.samples.swappedids.SIGNED_IN_USER
import dev.logcatdaily.samples.swappedids.WANTED_ORDER
import dev.logcatdaily.samples.swappedids.orders

private const val TAG = "logcatdaily"

// Broken: both ids are plain Long, so the compiler cannot see a swap.
internal fun openMyOrderBroken(): Order {
    val userId = SIGNED_IN_USER
    val orderId = WANTED_ORDER
    return loadOrder(userId, orderId)
}

private fun loadOrder(
    orderId: Long,
    userId: Long,
): Order = find(orderId, userId)

private fun find(
    orderId: Long,
    userId: Long,
): Order {
    Log.d(TAG, "loadOrder orderId=$orderId userId=$userId")
    return orders.getValue(orderId)
}
