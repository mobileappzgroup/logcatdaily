package dev.logcatdaily.samples.supervisorcrash

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val TAG = "logcatdaily"

val status = MutableStateFlow("waiting")

fun brokenScope() =
    CoroutineScope(SupervisorJob())

fun startJobs(scope: CoroutineScope) {
    scope.launch { error("boom") }
    scope.launch { sibling() }
}

private val handler =
    CoroutineExceptionHandler { _, e ->
        Log.d(TAG, "caught $e")
    }

fun fixedScope() =
    CoroutineScope(
        SupervisorJob() + handler,
    )

private suspend fun sibling() {
    delay(500)
    status.value = "sibling done"
    Log.d(TAG, "sibling done")
}
