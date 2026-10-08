package dev.logcatdaily.samples.newsfeed

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.paging.RemoteMediator.InitializeAction
import androidx.paging.RemoteMediator.MediatorResult
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "logcatdaily"
const val PAGE_SIZE = 20

// The only writer of feed_posts and feed_state. The screen reads Room and
// nothing else.
@OptIn(ExperimentalPagingApi::class)
class FeedRemoteMediator(
    private val api: FeedApi,
    private val store: FeedStore,
    private val clock: () -> Long = System::currentTimeMillis,
) : RemoteMediator<Int, FeedPostEntity>() {

    // The last cursor the server rejected. A second 400 for the same cursor
    // is an error, so a server that rejects every cursor cannot loop us.
    private var rejectedCursor: String? = null

    // Each time the ViewModel is created (cold open or new process, not rotation)
    // it tries to refresh. Until it succeeds the saved posts stay on screen.
    override suspend fun initialize(): InitializeAction = InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(loadType: LoadType, state: PagingState<Int, FeedPostEntity>): MediatorResult =
        when (loadType) {
            // New posts arrive through REFRESH, the feed only grows downward.
            LoadType.PREPEND -> MediatorResult.Success(endOfPaginationReached = true)
            LoadType.REFRESH -> refresh()
            LoadType.APPEND -> append()
        }

    private suspend fun refresh(): MediatorResult = try {
        fetchAndStore(refresh = true, cursor = null)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        failed("REFRESH", e)
    }

    private suspend fun append(): MediatorResult {
        val saved = store.state() ?: return refresh()
        if (saved.endReached) return MediatorResult.Success(endOfPaginationReached = true)
        val cursor = saved.nextCursor ?: return MediatorResult.Success(endOfPaginationReached = true)
        return try {
            fetchAndStore(refresh = false, cursor = cursor)
        } catch (e: CancellationException) {
            throw e
        } catch (e: BadCursorException) {
            // No retry button for a cursor that will never work: start the feed
            // over, once per cursor.
            if (rejectedCursor == cursor) return failed("APPEND after=$cursor", e)
            rejectedCursor = cursor
            Log.d(TAG, "FeedMediator APPEND after=$cursor rejected (400), refreshing")
            refresh()
        } catch (e: Exception) {
            failed("APPEND after=$cursor", e)
        }
    }

    // The response is in hand before anything is written. One call, one
    // transaction: posts and cursor move together.
    private suspend fun fetchAndStore(refresh: Boolean, cursor: String?): MediatorResult {
        val page = api.page(cursor, PAGE_SIZE)
        store.applyPage(refresh, page.items.map { it.toEntity() }, page.nextCursor, clock())
        if (!refresh) rejectedCursor = null
        val what = if (refresh) "REFRESH" else "APPEND after=$cursor"
        Log.d(TAG, "FeedMediator $what -> ${page.items.size} posts, next=${page.nextCursor}")
        return MediatorResult.Success(endOfPaginationReached = page.nextCursor == null)
    }

    // Errors are returned, never thrown, and nothing is cleared.
    private suspend fun failed(what: String, e: Exception): MediatorResult {
        Log.d(TAG, "FeedMediator $what failed: ${e.javaClass.simpleName}, kept ${store.postCount()} posts")
        return MediatorResult.Error(e)
    }
}
