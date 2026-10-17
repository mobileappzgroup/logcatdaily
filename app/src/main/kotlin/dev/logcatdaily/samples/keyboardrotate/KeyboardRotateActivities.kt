package dev.logcatdaily.samples.keyboardrotate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import dev.logcatdaily.samples.ui.LogcatDailyTheme

// The broken Reply box with one of the two fixes that the Android 17 behavior-change page
// lists. The manifest entry of the first one sets android:windowSoftInputMode="stateAlwaysVisible".
// adb shell am start -n dev.logcatdaily.samples/.keyboardrotate.KeyboardRotateManifestActivity
open class KeyboardRotateManifestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LogcatDailyTheme { ReplyScreen() }
        }
    }
}

// Asks for the keyboard in onCreate() after the activity was recreated.
// adb shell am start -n dev.logcatdaily.samples/.keyboardrotate.KeyboardRotateOnCreateActivity
class KeyboardRotateOnCreateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LogcatDailyTheme { ReplyScreen() }
        }
        if (savedInstanceState != null) {
            WindowCompat.getInsetsController(window, window.decorView)
                .show(WindowInsetsCompat.Type.ime())
        }
    }
}

@androidx.compose.runtime.Composable
private fun ReplyScreen() {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        ReplyHeader()
        ReplyBox()
    }
}
