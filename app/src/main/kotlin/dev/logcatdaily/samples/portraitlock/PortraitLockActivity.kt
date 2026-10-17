package dev.logcatdaily.samples.portraitlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.logcatdaily.samples.ui.LogcatDailyTheme

// The manifest locks this activity to portrait in both variants.
// adb shell am start -n dev.logcatdaily.samples/.portraitlock.PortraitLockActivity --ez adaptive false
class PortraitLockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val adaptive = intent.getBooleanExtra("adaptive", false)
        setContent {
            LogcatDailyTheme {
                PortraitLockSample(broken = !adaptive)
            }
        }
    }
}
