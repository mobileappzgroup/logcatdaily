package dev.logcatdaily.samples.backpress

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.logcatdaily.samples.ui.LogcatDailyTheme

private const val TAG = "logcatdaily"

// The Activity case: an OnBackPressedCallback instead of the onBackPressed() override.
// The sample's own BackHandler is skipped (broken = true), so this callback is the only handler.
// adb shell am start -n dev.logcatdaily.samples/.backpress.BackPressCallbackActivity
class BackPressCallbackActivity : ComponentActivity() {
    private var showDiscard by mutableStateOf(false)

    private val discardCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "OnBackPressedCallback invoked")
            showDiscard = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, discardCallback)
        setContent {
            LogcatDailyTheme {
                BackPressSample(
                    broken = true,
                    legacyDialog = showDiscard,
                    onLegacyDialogDismiss = { showDiscard = false },
                    onEditsChanged = { discardCallback.isEnabled = it > 0 },
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop isFinishing=$isFinishing")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy isFinishing=$isFinishing")
    }
}
