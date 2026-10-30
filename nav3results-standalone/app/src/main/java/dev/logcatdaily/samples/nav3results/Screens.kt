package dev.logcatdaily.samples.nav3results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(picked: String?, onOpenPicker: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Picked: ${picked ?: "nothing"}", fontSize = 32.sp)
        Button(onClick = onOpenPicker, modifier = Modifier.padding(top = 24.dp)) {
            Text("Open picker")
        }
    }
}

@Composable
fun PickerScreen(onPick: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Pick a name", fontSize = 28.sp)
        Button(onClick = { onPick("Nishant") }, modifier = Modifier.padding(top = 24.dp)) {
            Text("Nishant")
        }
    }
}
