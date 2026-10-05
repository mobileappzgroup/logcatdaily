package dev.logcatdaily.samples.readtoohigh

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TAG = "logcatdaily"

@Composable
fun ReadTooHighSample(broken: Boolean) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val count = remember { intArrayOf(0) }
    SideEffect { Log.d(TAG, "Screen recompose #${++count[0]}") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scroll)
    ) {
        Box(Modifier.fillMaxWidth().height(220.dp).clipToBounds()) {
            // the header moves at half the scroll speed
            val parallax = if (broken) {
                Modifier.offset(y = with(density) { (scroll.value / 2).toDp() })
            } else {
                Modifier.offset { IntOffset(0, scroll.value / 2) }
            }
            Header(parallax)
        }
        repeat(30) { Row(it) }
    }
}

@Composable
private fun Header(modifier: Modifier) {
    val count = remember { intArrayOf(0) }
    SideEffect { Log.d(TAG, "Header recompose #${++count[0]}") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            text = "Release notes",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.statusBarsPadding().padding(24.dp),
        )
    }
}

@Composable
private fun Row(index: Int) {
    val count = remember { intArrayOf(0) }
    SideEffect { Log.d(TAG, "Row $index recompose #${++count[0]}") }

    Text(
        text = "Build 4.2.$index fixed a crash",
        fontSize = 20.sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
    )
}
