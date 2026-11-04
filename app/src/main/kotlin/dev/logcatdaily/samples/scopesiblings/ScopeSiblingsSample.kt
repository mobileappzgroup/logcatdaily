package dev.logcatdaily.samples.scopesiblings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// variant: "coroutinescope", "supervisorscope" or "supervisornohandler" starts that run 1 s after
// the screen is drawn, so a recording catches it. Anything else waits for a tap.
@Composable
fun ScopeSiblingsSample(variant: String?) {
    val viewModel = remember { ScopeSiblingsViewModel() }
    val status by viewModel.status.collectAsState()

    LaunchedEffect(variant) {
        delay(1000)
        when (variant) {
            "coroutinescope" -> viewModel.runCoroutineScope()
            "supervisorscope" -> viewModel.runSupervisorScope()
            "supervisornohandler" -> viewModel.runSupervisorNoHandler()
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
        ) {
            Text(
                text = "Two uploads",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = status,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = { viewModel.runCoroutineScope() }) {
                Text("coroutineScope", fontSize = 20.sp)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { viewModel.runSupervisorScope() }) {
                Text("supervisorScope", fontSize = 20.sp)
            }
        }
    }
}
