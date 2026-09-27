package dev.logcatdaily.samples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.logcatdaily.samples.effectkey.EffectKeySample
import dev.logcatdaily.samples.rotationstate.RotationStateSample
import dev.logcatdaily.samples.scrollderived.ScrollDerivedSample
import dev.logcatdaily.samples.ui.LogcatDailyTheme

// Launch a sample by intent extra, e.g.
// adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample scroll-derived --es variant broken
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sample = intent.getStringExtra("sample") ?: "scroll-derived"
        val broken = intent.getStringExtra("variant") != "fixed"

        setContent {
            LogcatDailyTheme {
                when (sample) {
                    "rotation-state" -> RotationStateSample(broken = broken)
                    "effect-key" -> EffectKeySample(broken = broken)
                    else -> ScrollDerivedSample(broken = broken)
                }
            }
        }
    }
}
