package dev.logcatdaily.samples.flowvsshared

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val TAG = "logcatdaily"

// One case per launch. Every case emits first, then starts a late collector, then emits more.
// The collector logs what it actually received, so the log is the evidence.
suspend fun runCase(case: String, scope: CoroutineScope, onLine: (String) -> Unit) {
    fun log(msg: String) {
        Log.d(TAG, msg)
        onLine(msg)
    }

    fun collect(name: String, flow: Flow<String>): Job = scope.launch {
        flow.collect { log("$name got $it") }
    }

    when (case) {
        // StateFlow keeps the latest value, so a collector that starts late still gets it.
        "state-late" -> {
            val state = MutableStateFlow("Nishant offline")
            log("state emit Nishant online")
            state.value = "Nishant online"
            delay(300)
            log("collector starts")
            val job = collect("collector", state)
            delay(300)
            log("state emit Nishant typing")
            state.value = "Nishant typing"
            delay(300)
            job.cancel()
        }
        // SharedFlow with replay 0 keeps nothing, so an event sent before collection is gone.
        "shared-late" -> {
            val events = MutableSharedFlow<String>()
            log("shared emit Nishant signed in")
            events.emit("Nishant signed in")
            delay(300)
            log("collector starts")
            val job = collect("collector", events)
            delay(300)
            log("shared emit Nishant signed out")
            events.emit("Nishant signed out")
            delay(300)
            job.cancel()
        }
        // StateFlow skips a value equal to the current one.
        "state-equal" -> {
            val state = MutableStateFlow("Nishant")
            val job = collect("collector", state)
            delay(300)
            for (v in listOf("Nishant", "Nishant", "Nishant online")) {
                log("state emit $v")
                state.value = v
                delay(300)
            }
            job.cancel()
        }
        // Same three values through a SharedFlow: nothing is dropped as equal.
        "shared-equal" -> {
            val events = MutableSharedFlow<String>()
            val job = collect("collector", events)
            delay(300)
            for (v in listOf("Nishant", "Nishant", "Nishant online")) {
                log("shared emit $v")
                events.emit(v)
                delay(300)
            }
            job.cancel()
        }
        else -> log("unknown case $case")
    }
    log("done")
}
