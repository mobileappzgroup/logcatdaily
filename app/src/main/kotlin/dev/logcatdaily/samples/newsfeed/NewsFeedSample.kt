package dev.logcatdaily.samples.newsfeed

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.collectAsLazyPagingItems

private const val CURSOR = "cursor"
private const val OFFSET = "offset"
private const val OFFSET_DEDUPE = "offset-dedupe"

// Launch with --es sample news-feed --es variant cursor | offset | offset-dedupe.
// Anything else is the cursor arm.
@Composable
fun NewsFeedSample(variant: String?) {
    val activity = LocalContext.current as ComponentActivity
    when (variant) {
        OFFSET, OFFSET_DEDUPE -> {
            val dedupe = variant == OFFSET_DEDUPE
            val viewModel = remember {
                ViewModelProvider(activity, object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        OffsetFeedViewModel(FeedApi(), dedupe) as T
                })[OffsetFeedViewModel::class.java]
            }
            OffsetFeedScreen(
                posts = viewModel.feed.collectAsLazyPagingItems(),
                modeTag = if (dedupe) "offset + distinctBy" else "offset: Paging 3, page keys",
            )
        }
        else -> {
            val viewModel = remember { ViewModelProvider(activity)[FeedViewModel::class.java] }
            FeedScreen(posts = viewModel.feed.collectAsLazyPagingItems(), modeTag = CURSOR)
        }
    }
}
