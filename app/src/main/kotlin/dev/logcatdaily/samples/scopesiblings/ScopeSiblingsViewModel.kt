package dev.logcatdaily.samples.scopesiblings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

private const val TAG = "logcatdaily"

// Two uploads run side by side. Upload A takes 3 steps of 400 ms. Upload B
// fails after 600 ms. The first two variants differ in the scope builder
// and in the handler on B; the third drops the handler to show the crash.
class ScopeSiblingsViewModel : ViewModel() {
    private val _status = MutableStateFlow("idle")
    val status: StateFlow<String> = _status.asStateFlow()

    // A child of supervisorScope is not covered by its parent's failure
    // handling, so the failure needs its own handler or it reaches the thread's
    // uncaught exception handler and kills the app.
    private val uploadBHandler = CoroutineExceptionHandler { _, e ->
        Log.d(TAG, "B threw ${e.message}")
    }

    fun runCoroutineScope() {
        viewModelScope.launch {
            try {
                coroutineScope {
                    launch { uploadA() }
                    launch {
                        delay(600)
                        Log.d(TAG, "B threw upload B failed")
                        throw IllegalStateException("upload B failed")
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "outer caught ${e.message}")
                _status.value = "coroutineScope: A cancelled"
            }
        }
    }

    fun runSupervisorScope() {
        viewModelScope.launch {
            try {
                supervisorScope {
                    launch { uploadA() }
                    launch(uploadBHandler) {
                        delay(600)
                        throw IllegalStateException("upload B failed")
                    }
                }
                Log.d(TAG, "outer finished, nothing thrown")
                _status.value = "supervisorScope: A finished"
            } catch (e: Exception) {
                Log.d(TAG, "outer caught ${e.message}")
            }
        }
    }

    // Same as runSupervisorScope, but B has no handler. Nothing catches B's
    // failure, so it reaches the uncaught exception handler.
    fun runSupervisorNoHandler() {
        viewModelScope.launch {
            try {
                supervisorScope {
                    launch { uploadA() }
                    launch {
                        delay(600)
                        throw IllegalStateException("upload B failed")
                    }
                }
                Log.d(TAG, "outer finished, nothing thrown")
                _status.value = "supervisorScope: A finished"
            } catch (e: Exception) {
                Log.d(TAG, "outer caught ${e.message}")
            }
        }
    }

    private suspend fun uploadA() {
        var step = 0
        try {
            while (step < 3) {
                delay(400)
                step++
                Log.d(TAG, "A step $step")
            }
            Log.d(TAG, "A done step $step")
        } catch (e: CancellationException) {
            Log.d(TAG, "A cancelled at step $step")
            throw e
        }
    }
}
