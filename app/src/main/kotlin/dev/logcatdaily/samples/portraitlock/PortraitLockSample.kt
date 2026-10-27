package dev.logcatdaily.samples.portraitlock

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.logcatdaily.samples.ui.Avatar

private const val TAG = "logcatdaily"

private class Mail(val name: String, val line: String, val time: String, val unread: Boolean = false)

private val inbox = listOf(
    Mail("Asha", "Standup moved to 11", "9:41", unread = true),
    Mail("Ravi", "Can you review my PR?", "9:12", unread = true),
    Mail("Meera", "Lunch at 1?", "8:50"),
    Mail("Nishant", "Build is green", "Yesterday"),
    Mail("Kabir", "Release notes draft", "Yesterday"),
)

@Composable
fun PortraitLockSample(broken: Boolean) {
    LogOrientation()
    if (broken) {
        OnePane()
    } else {
        BoxWithConstraints(Modifier.fillMaxSize()) { AdaptiveInbox() }
    }
}

@Composable
private fun BoxWithConstraintsScope.AdaptiveInbox() {
    if (maxWidth >= 600.dp)
        TwoPane()
    else
        OnePane()
}

// requested is the manifest lock as the activity reports it, orientation is
// what the screen actually is. Words, because the two use different constants.
@Composable
private fun LogOrientation() {
    val activity = LocalContext.current as Activity
    val config = LocalConfiguration.current
    SideEffect {
        val requested = when (activity.requestedOrientation) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> "portrait"
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> "landscape"
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED -> "unspecified"
            else -> "other(${activity.requestedOrientation})"
        }
        val orientation = when (config.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> "portrait"
            Configuration.ORIENTATION_LANDSCAPE -> "landscape"
            else -> "other"
        }
        Log.d(TAG, "requested=$requested orientation=$orientation sw=${config.smallestScreenWidthDp}dp")
    }
}

@Composable
private fun OnePane() {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        Title()
        InboxList(Modifier.fillMaxWidth())
    }
}

@Composable
private fun TwoPane() {
    Row(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        Column(Modifier.width(360.dp).fillMaxHeight()) {
            Title()
            InboxList(Modifier.fillMaxWidth())
        }
        Column(
            Modifier
                .padding(start = 24.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar("Asha", size = 48.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        text = "Asha",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "9:41",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Text(
                text = "Standup moved to 11. Same room, same agenda.",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun Title() {
    Text(
        text = "Inbox",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

@Composable
private fun InboxList(modifier: Modifier) {
    Column(modifier) {
        inbox.forEach { mail ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(mail.name)
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(
                        text = mail.name,
                        fontSize = 18.sp,
                        fontWeight = if (mail.unread) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = mail.line,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = mail.time,
                        fontSize = 13.sp,
                        color = if (mail.unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                    if (mail.unread) {
                        Box(
                            Modifier
                                .padding(top = 8.dp)
                                .size(10.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }
                }
            }
        }
    }
}
