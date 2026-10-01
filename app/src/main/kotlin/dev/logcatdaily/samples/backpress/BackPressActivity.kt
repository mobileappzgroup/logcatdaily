package dev.logcatdaily.samples.backpress

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.logcatdaily.samples.ui.LogcatDailyTheme

private const val TAG = "logcatdaily"

// adb shell am start -n dev.logcatdaily.samples/.backpress.BackPressActivity --es variant broken
open class BackPressActivity : ComponentActivity() {
    private var showDiscard by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val broken = intent.getStringExtra("variant") != "fixed"
        setContent {
            LogcatDailyTheme {
                BackPressSample(
                    broken = broken,
                    legacyDialog = showDiscard,
                    onLegacyDialogDismiss = { showDiscard = false },
                )
            }
        }
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed() called")
        showDiscard = true
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
