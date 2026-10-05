package dev.logcatdaily.samples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.logcatdaily.samples.anrroom.AnrRoomSample
import dev.logcatdaily.samples.cancelfinally.CancelFinallySample
import dev.logcatdaily.samples.cancelloop.CancelLoopSample
import dev.logcatdaily.samples.cancelswallow.CancelSwallowSample
import dev.logcatdaily.samples.effectkey.EffectKeySample
import dev.logcatdaily.samples.formhoist.FormHoistSample
import dev.logcatdaily.samples.lazykey.LazyKeySample
import dev.logcatdaily.samples.newlistparam.NewListParamSample
import dev.logcatdaily.samples.offlinechat.OfflineChatSample
import dev.logcatdaily.samples.readtoohigh.ReadTooHighSample
import dev.logcatdaily.samples.rotationstate.RotationStateSample
import dev.logcatdaily.samples.scrollderived.ScrollDerivedSample
import dev.logcatdaily.samples.ui.LogcatDailyTheme
import dev.logcatdaily.samples.wideparam.WideParamSample

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
                    "anr-room" -> AnrRoomSample(broken = broken)
                    "lazy-key" -> LazyKeySample(broken = broken)
                    "cancel-swallow" -> CancelSwallowSample(broken = broken)
                    "cancel-loop" -> CancelLoopSample(broken = broken)
                    "cancel-finally" -> CancelFinallySample(broken = broken)
                    "form-hoist" -> FormHoistSample(broken = broken)
                    "read-too-high" -> ReadTooHighSample(broken = broken)
                    "new-list-param" -> NewListParamSample(broken = broken)
                    "wide-param" -> WideParamSample(broken = broken)
                    "offline-chat" -> OfflineChatSample()
                    else -> ScrollDerivedSample(broken = broken)
                }
            }
        }
    }
}
