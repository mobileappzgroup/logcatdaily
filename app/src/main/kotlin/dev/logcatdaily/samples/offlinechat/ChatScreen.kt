package dev.logcatdaily.samples.offlinechat

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider

private val SendingColor = Color(0xFFFFB454)
private val SentColor = Color(0xFF16C79A)
private val FailedColor = Color(0xFFFF5C5C)

@Composable
fun OfflineChatSample() {
    val activity = LocalContext.current as ComponentActivity
    val viewModel = remember { ViewModelProvider(activity)[ChatViewModel::class.java] }
    val messages by viewModel.messages.collectAsState()
    val sendWithoutKey by viewModel.sendWithoutKey.collectAsState()

    ChatScreen(
        messages = messages,
        sendWithoutKey = sendWithoutKey,
        onSendWithoutKeyChange = viewModel::setSendWithoutKey,
        onSend = viewModel::send,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<Message>,
    sendWithoutKey: Boolean,
    onSendWithoutKeyChange: (Boolean) -> Unit,
    onSend: (String) -> Unit,
    onRetry: (String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Chat", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = {
            Composer(
                draft = draft,
                onDraftChange = { draft = it },
                onSendClick = {
                    onSend(draft)
                    draft = ""
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DebugToggle(sendWithoutKey, onSendWithoutKeyChange)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(messages, key = { it.id }) { MessageRow(it, onRetry) }
            }
        }
    }
}

@Composable
private fun DebugToggle(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "debug: no idempotency key",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun MessageRow(message: Message, onRetry: (String) -> Unit) {
    val failed = message.status == MessageStatus.FAILED
    Surface(
        // A FAILED bubble is a button: tap to send it again.
        modifier = Modifier.fillMaxWidth().clickable(enabled = failed) { onRetry(message.clientId) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                text = message.text,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(message.status)
                Spacer(Modifier.weight(1f))
                // Same short id the mock server prints, so a row on screen can be
                // matched to a line in the server log.
                val seq = message.serverSeq?.let { " seq=$it" } ?: ""
                Text(
                    text = message.clientId.take(8) + seq,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: MessageStatus) {
    val color = when (status) {
        MessageStatus.SENDING -> SendingColor
        MessageStatus.SENT -> SentColor
        MessageStatus.FAILED -> FailedColor
    }
    Text(
        text = status.name,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF0B0E13),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 14.dp, vertical = 4.dp),
    )
}

@Composable
private fun Composer(draft: String, onDraftChange: (String) -> Unit, onSendClick: () -> Unit) {
    Surface(
        modifier = Modifier.imePadding(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                placeholder = { Text("Message", fontSize = 18.sp) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
            )
            Spacer(Modifier.width(12.dp))
            Button(onClick = onSendClick, modifier = Modifier.height(56.dp)) {
                Text("Send", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
