package dev.logcatdaily.samples.newsfeed

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import androidx.paging.filter
import java.io.IOException
import kotlinx.coroutines.flow.map

// The mid answer, the arm that breaks. Paging 3 with page keys: the key is the
// offset, there is no Room and no cursor. Offset 20 means "the 21st post right
// now", so a post that lands above shifts every later page by one.
class OffsetPagingSource(private val api: FeedApi) : PagingSource<Int, FeedPost>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FeedPost> {
        val offset = params.key ?: 0
        return try {
            val page = api.pageByOffset(offset, params.loadSize)
            // A short page is the last one.
            val next = if (page.items.size < params.loadSize) null else offset + params.loadSize
            LoadResult.Page(data = page.items, prevKey = null, nextKey = next)
        } catch (e: IOException) {
            LoadResult.Error(e)
        }
    }

    // A reload starts from the top.
    override fun getRefreshKey(state: PagingState<Int, FeedPost>): Int? = null
}

// dedupe = false shows the repeat. dedupe = true hides it, which is the fix a
// quick answer reaches for: the repeat goes away and a skipped post is never noticed.
class OffsetFeedViewModel(api: FeedApi, dedupe: Boolean) : ViewModel() {
    val feed = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            prefetchDistance = 5,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { OffsetPagingSource(api) },
    ).flow
        .map { data ->
            if (!dedupe) {
                data
            } else {
                // Stateful: PagingData has no distinctBy, so keep the ids seen so far.
                // One set per generation of the list, a refresh starts a new one.
                val seen = HashSet<String>()
                data.filter { seen.add(it.id) }
            }
        }
        .cachedIn(viewModelScope)
}

// No item key on purpose. A repeated id would be a duplicate key and a
// duplicate key crashes LazyColumn, so the repeat would never reach the eye.
// That crash has its own reel.
@Composable
fun OffsetFeedScreen(posts: LazyPagingItems<FeedPost>, modeTag: String) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { FeedTopBar(modeTag) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(count = posts.itemCount) { index ->
                posts[index]?.let { PostRow(it) }
            }
            item { Footer(posts.loadState.append) { posts.retry() } }
        }
    }
}
