package dev.logcatdaily.samples.lateinitlazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// variant: broken (crashes), lazy, fixed
@Composable
fun LateinitLazyScreen(variant: String) {
    val session = remember { Session() }
    val taps = remember { mutableIntStateOf(0) }
    val label = if (variant == "lazy") "Read repo" else "Open config"
    val onTap: () -> Unit = {
        when (variant) {
            "lazy" -> {
                taps.intValue++
                session.readLazy()
            }
            "fixed" -> session.readFixed()
            else -> session.readBroken()
        }
    }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Session for Nishant", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(variant, fontSize = 40.sp, modifier = Modifier.padding(top = 24.dp))
        Button(
            onClick = onTap,
            modifier = Modifier.fillMaxWidth().height(72.dp).padding(top = 24.dp),
        ) {
            Text(label, fontSize = 28.sp)
        }
        if (variant == "lazy") {
            Text("reads: ${taps.intValue}", fontSize = 24.sp, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
