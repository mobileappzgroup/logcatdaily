package dev.logcatdaily.samples.processdeath

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.logcatdaily.samples.processdeath.broken.BasketViewModel as BrokenBasket
import dev.logcatdaily.samples.processdeath.fixed.BasketViewModel as FixedBasket
import dev.logcatdaily.samples.ui.LogcatDailyTheme

// The default ViewModel factory of a ComponentActivity hands a SavedStateHandle to a
// ViewModel that asks for one. This module has no lifecycle-viewmodel-compose, so the
// screen gets its ViewModel from the activity.
// adb shell am start -n dev.logcatdaily.samples/.processdeath.ProcessDeathActivity --es variant broken
class ProcessDeathActivity : ComponentActivity() {
    private val brokenVm by viewModels<BrokenBasket>()
    private val fixedVm by viewModels<FixedBasket>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val broken = intent.getStringExtra("variant") != "fixed"
        setContent {
            LogcatDailyTheme {
                if (broken) {
                    val items by brokenVm.items.collectAsState()
                    ProcessDeathSample(items) { brokenVm.add(it) }
                } else {
                    val items by fixedVm.items.collectAsState()
                    ProcessDeathSample(items) { fixedVm.add(it) }
                }
            }
        }
    }
}
