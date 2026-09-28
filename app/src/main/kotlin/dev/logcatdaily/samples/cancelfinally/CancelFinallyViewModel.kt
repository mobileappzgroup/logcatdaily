package dev.logcatdaily.samples.cancelfinally

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "logcatdaily"
private const val TOTAL_STEPS = 5

// Work ticks every 400ms, then a finally block runs cleanup. Broken calls
// the suspend cleanup directly: by the time finally runs, the job is already
// cancelling, so that suspend call throws immediately and cleanup never
// completes. Fixed wraps it in withContext(NonCancellable) so cleanup is
// allowed to run to completion even though the job is cancelled.
class CancelFinallyViewModel(private val broken: Boolean) : ViewModel() {
    private val _status = MutableStateFlow("working 0/$TOTAL_STEPS")
    val status: StateFlow<String> = _status.asStateFlow()

    private var job: Job? = null

    fun start() {
        Log.d(TAG, "job started")
        job = viewModelScope.launch {
            try {
                repeat(TOTAL_STEPS) { i ->
                    delay(400)
                    _status.value = "working ${i + 1}/$TOTAL_STEPS"
                    Log.d(TAG, "tick ${i + 1}/$TOTAL_STEPS")
                }
                Log.d(TAG, "work finished")
            } finally {
                if (broken) {
                    cleanup()
                } else {
                    withContext(NonCancellable) { cleanup() }
                }
            }
        }
    }

    private suspend fun cleanup() {
        Log.d(TAG, "cleanup started")
        delay(500) // stand-in for a real suspend cleanup: flush a buffer, close a file
        Log.d(TAG, "cleanup done")
    }

    fun cancel() {
        Log.d(TAG, "cancel requested")
        job?.cancel()
    }
}
