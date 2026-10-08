package dev.logcatdaily.samples.newsfeed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

// The cursor arm. Room is the only thing this screen reads.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(posts: LazyPagingItems<FeedPost>, modeTag: String) {
    val listState = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    val refresh = posts.loadState.refresh
    var pulled by remember { mutableStateOf(false) }
    var topAtPull by remember { mutableStateOf<String?>(null) }

    // A failed refresh keeps the saved posts. With posts on screen it is a
    // snackbar, with an empty list the whole screen asks for a retry.
    // The error can land before Room has handed over the saved rows, so the
    // effect also waits for the first row.
    val refreshFailed = refresh is LoadState.Error
    val hasPosts = posts.itemCount > 0
    LaunchedEffect(refreshFailed, hasPosts) {
        if (refreshFailed && hasPosts) snackbar.showSnackbar(REFRESH_FAILED)
    }

    // With itemKey the list holds on to the old first row when posts land
    // above it. After a pull, jump to the top once the new first post is in
    // the list. Room hands the rows over a moment after the state flips.
    LaunchedEffect(refresh) {
        if (!pulled) return@LaunchedEffect
        when (refresh) {
            is LoadState.NotLoading -> {
                pulled = false
                val changed = withTimeoutOrNull(1_000) {
                    snapshotFlow { posts.itemSnapshotList.firstOrNull()?.id }.first { it != topAtPull }
                    true
                } == true
                if (changed) listState.scrollToItem(0)
            }
            is LoadState.Error -> pulled = false
            else -> Unit
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { FeedTopBar(modeTag) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val refreshing = refresh is LoadState.Loading && posts.itemCount > 0
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = refreshing,
            state = pullState,
            // The default indicator is dark grey on dark grey. Accent colour so it shows in a recording.
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = MaterialTheme.colorScheme.primary,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            },
            onRefresh = {
                topAtPull = posts.itemSnapshotList.firstOrNull()?.id
                pulled = true
                posts.refresh()
            },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            if (posts.itemCount == 0 && refresh !is LoadState.NotLoading) {
                EmptyState(refresh) { posts.retry() }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(count = posts.itemCount, key = posts.itemKey { it.id }) { index ->
                        posts[index]?.let { PostRow(it) }
                    }
                    item { Footer(posts.loadState.append) { posts.retry() } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTopBar(modeTag: String) {
    TopAppBar(
        title = {
            Column {
                Text("Feed", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(modeTag, fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
fun PostRow(post: FeedPost) {
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(post.author, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(post.text, fontSize = 18.sp, maxLines = 2, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "${post.id}  seq ${post.seq}",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surface)
    }
}

@Composable
fun Footer(append: LoadState, onRetry: () -> Unit) {
    val modifier = Modifier.fillMaxWidth().padding(20.dp)
    when {
        append is LoadState.Loading -> Text(LOADING, modifier = modifier, color = MaterialTheme.colorScheme.outline)
        append is LoadState.Error ->
            Text(LOAD_MORE_FAILED, modifier = modifier.clickable(onClick = onRetry), color = MaterialTheme.colorScheme.secondary)
        append.endOfPaginationReached ->
            Text(CAUGHT_UP, modifier = modifier, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun EmptyState(refresh: LoadState, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (refresh is LoadState.Error) {
            Text(
                text = LOAD_FIRST_FAILED,
                modifier = Modifier.clickable(onClick = onRetry).padding(20.dp),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.secondary,
            )
        } else {
            Text(LOADING, color = MaterialTheme.colorScheme.outline)
        }
    }
}

// Strings from names.md v2.
private const val LOADING = "Loading..."
private const val LOAD_MORE_FAILED = "Can't load more. Tap to retry."
private const val CAUGHT_UP = "You're all caught up."
private const val LOAD_FIRST_FAILED = "Can't load posts. Tap to retry."
private const val REFRESH_FAILED = "Couldn't refresh. Showing saved posts."
