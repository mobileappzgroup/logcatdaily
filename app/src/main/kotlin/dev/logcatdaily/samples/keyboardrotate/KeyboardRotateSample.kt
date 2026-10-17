package dev.logcatdaily.samples.keyboardrotate

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import dev.logcatdaily.samples.ui.Avatar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TAG = "logcatdaily"

@Composable
fun KeyboardRotateSample(variant: String) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
    ) {
        ReplyHeader()
        if (variant == "fixed") ReplyBoxFixed() else ReplyBox()
    }
}

// Title and the message being answered. Display only.
@Composable
internal fun ReplyHeader() {
    Text(
        text = "Reply",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 16.dp),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Avatar("Asha", size = 44.dp)
        Column(Modifier.padding(start = 12.dp)) {
            Row {
                Text(
                    text = "Asha",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "9:41",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Text(
                text = "Standup moved to 11. Same room, same agenda. Can you bring the sprint board?",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// Broken: the text is saved across rotation, nothing asks for the keyboard.
@Composable
internal fun ReplyBox() {
    var text by rememberSaveable {
        mutableStateOf("")
    }
    LogIme(text.length)
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Write a reply to Asha") },
        shape = RoundedCornerShape(16.dp),
        colors = replyFieldColors(),
    )
}

@Composable
private fun replyFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary,
)

// Fixed: remember that the user was typing and ask for focus again.
@Composable
private fun ReplyBoxFixed() {
    var text by rememberSaveable {
        mutableStateOf("")
    }
    var typing by rememberSaveable {
        mutableStateOf(false)
    }
    val wasTyping = remember { typing }
    val focus = remember {
        FocusRequester()
    }
    LaunchedEffect(Unit) {
        if (wasTyping) {
            focus.requestFocus()
        }
    }
    LogIme(text.length)
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .onFocusChanged {
                // rotation reports focus lost before the state is saved, so only set it
                if (it.isFocused) typing = true
            },
        placeholder = { Text("Write a reply to Asha") },
        shape = RoundedCornerShape(16.dp),
        colors = replyFieldColors(),
    )
}

// imeBottom > 0 while the on-screen keyboard is showing.
@Composable
private fun LogIme(textLength: Int) {
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val visible = imeBottom > 0
    LaunchedEffect(visible) {
        Log.d(TAG, "ime visible=$visible text=$textLength")
    }
}
