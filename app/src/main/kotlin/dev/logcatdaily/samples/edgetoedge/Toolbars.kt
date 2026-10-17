package dev.logcatdaily.samples.edgetoedge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun bar() = Modifier
    .fillMaxWidth()
    .background(MaterialTheme.colorScheme.primary)

@Composable
fun BrokenToolbar() {
    Row(
        bar()
            .height(56.dp),
    ) { Title() }
}

@Composable
fun FixedToolbar() {
    Row(
        bar()
            .statusBarsPadding()
            .height(56.dp),
    ) { Title() }
}

@Composable
private fun Title() {
    Text(
        text = "Orders",
        color = MaterialTheme.colorScheme.onPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxHeight()
            .wrapContentHeight(Alignment.CenterVertically),
    )
}
