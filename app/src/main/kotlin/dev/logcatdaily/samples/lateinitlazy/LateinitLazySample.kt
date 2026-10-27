package dev.logcatdaily.samples.lateinitlazy

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TAG = "logcatdaily"

class Config(val user: String)

class Session {
    lateinit var config: Config

    val repo by lazy {
        Log.d(TAG, "repo: init")
        "UserRepo(Nishant)"
    }

    fun load() {
        config = Config("Nishant")
    }

    fun readBroken() {
        Log.d(TAG, "reading config")
        Log.d(TAG, "user=${config.user}")
    }

    fun readLazy() {
        Log.d(TAG, "first access")
        Log.d(TAG, "got ${repo}")
        Log.d(TAG, "second access")
        Log.d(TAG, "got ${repo}")
    }

    fun readFixed() {
        val ready = ::config.isInitialized
        Log.d(TAG, "isInitialized=$ready")
        if (ready) Log.d(TAG, "user=${config.user}")
        else Log.d(TAG, "skip, load() not called")
        load()
        Log.d(TAG, "isInitialized=${::config.isInitialized}")
        Log.d(TAG, "user=${config.user}")
    }
}

