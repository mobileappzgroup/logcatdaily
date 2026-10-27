package dev.logcatdaily.samples.launchasync

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val TAG = "logcatdaily"

class LaunchAsyncViewModel : ViewModel() {
    val status = MutableStateFlow("idle")

    fun runLaunch() {
        status.value = "launch running"
        Log.d(TAG, "launch start")
        viewModelScope.launch {
            delay(300)
            Log.d(TAG, "launch throw")
            error("sync failed for Nishant")
        }
    }

    fun runAsyncNoAwait() {
        status.value = "async running"
        Log.d(TAG, "async start")
        viewModelScope.async {
            delay(300)
            Log.d(TAG, "async throw")
            error("sync failed for Nishant")
        }
        viewModelScope.launch {
            delay(1000)
            status.value = "async finished, no error shown"
            Log.d(TAG, "async no await: no crash, no error shown")
        }
    }

    fun runAsyncAwait() {
        status.value = "async running"
        Log.d(TAG, "async start")
        val deferred = viewModelScope.async {
            delay(300)
            Log.d(TAG, "async throw")
            error("sync failed for Nishant")
        }
        viewModelScope.launch {
            try {
                deferred.await()
            } catch (e: IllegalStateException) {
                status.value = "caught: ${e.message}"
                Log.d(TAG, "await caught $e")
            }
        }
    }
}
