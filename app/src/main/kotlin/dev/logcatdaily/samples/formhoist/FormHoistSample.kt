package dev.logcatdaily.samples.formhoist

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TAG = "logcatdaily"

@Composable
fun FormHoistSample(broken: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Profile",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (broken) BrokenForm() else HoistedForm()
        }
    }
}

// Broken: each field keeps its own text. The form has nothing to read or reset.
@Composable
private fun BrokenForm() {
    BrokenField(label = "Name")
    BrokenField(label = "Email")
    ClearButton(onClick = { Log.d(TAG, "clear tapped") })
}

@Composable
private fun BrokenField(
    label: String,
) {
    var text by rememberSaveable {
        mutableStateOf("")
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
        },
        label = { Text(label) },
    )
}

// Fixed: the form owns the text. Fields get value down, onValueChange up.
@Composable
private fun HoistedForm() {
    var name by rememberSaveable {
        mutableStateOf("")
    }
    var email by rememberSaveable {
        mutableStateOf("")
    }

    HoistedField(
        label = "Name",
        value = name,
        onValueChange = { name = it },
    )
    HoistedField(
        label = "Email",
        value = email,
        onValueChange = { email = it },
    )
    ClearButton(onClick = {
        name = ""
        email = ""
    })

    Log.d(TAG, "form state name='$name' email='$email'")
}

@Composable
private fun HoistedField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
    )
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    Button(onClick = onClick) { Text("Clear") }
}
