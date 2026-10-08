package dev.logcatdaily.samples.newsfeed

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.InitializeAction
import androidx.paging.RemoteMediator.MediatorResult
import java.io.IOException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalPagingApi::class)
class FeedRemoteMediatorTest {
    private val api = FakeFeedApi()
    private val store = FakeFeedStore()
    private val mediator = FeedRemoteMediator(api, store, clock = { 5_000L })
    private val emptyState = PagingState<Int, FeedPostEntity>(emptyList(), null, PagingConfig(20), 0)

    private fun load(type: LoadType) = runBlocking { mediator.load(type, emptyState) }

    private fun ids(from: Int, to: Int) = (from downTo to).map { "p-%04d".format(it) }

    @Test
    fun initialize_launchesARefresh() {
        assertEquals(InitializeAction.LAUNCH_INITIAL_REFRESH, runBlocking { mediator.initialize() })
    }

    @Test
    fun refresh_writesTheFirstPageAndTheCursor() {
        val result = load(LoadType.REFRESH)

        assertFalse((result as MediatorResult.Success).endOfPaginationReached)
        assertEquals(ids(100, 81), store.ids())
        assertEquals("cur_81", store.saved?.nextCursor)
        assertEquals(5_000L, store.saved?.refreshedAt)
        assertEquals(listOf("first"), api.calls)
    }

    @Test
    fun postsAndCursorAreWrittenInOneCall() {
        load(LoadType.REFRESH)
        load(LoadType.APPEND)

        assertEquals(2, store.applyCalls)
    }

    @Test
    fun append_usesTheStoredCursor() {
        load(LoadType.REFRESH)
        load(LoadType.APPEND)

        assertEquals(listOf("first", "after=cur_81"), api.calls)
        assertEquals(ids(100, 61), store.ids())
        assertEquals("cur_61", store.saved?.nextCursor)
    }

    @Test
    fun append_keepsTheRefreshTime() {
        load(LoadType.REFRESH)
        val appendMediator = FeedRemoteMediator(api, store, clock = { 9_000L })
        runBlocking { appendMediator.load(LoadType.APPEND, emptyState) }

        assertEquals(5_000L, store.saved?.refreshedAt)
    }

    @Test
    fun append_withoutAnyState_startsWithARefresh() {
        val result = load(LoadType.APPEND)

        assertTrue(result is MediatorResult.Success)
        assertEquals(listOf("first"), api.calls)
        assertEquals(ids(100, 81), store.ids())
    }

    @Test
    fun lastPage_setsEndReachedAndEndsPagination() {
        api.server.deleteHead(80)
        // 20 posts left: one page, no cursor.
        val result = load(LoadType.REFRESH)

        assertTrue((result as MediatorResult.Success).endOfPaginationReached)
        assertNull(store.saved?.nextCursor)
        assertTrue(store.saved?.endReached == true)
    }

    @Test
    fun append_afterTheEnd_returnsEndWithoutCallingTheApi() {
        api.server.deleteHead(80)
        load(LoadType.REFRESH)
        api.calls.clear()

        val result = load(LoadType.APPEND)

        assertTrue((result as MediatorResult.Success).endOfPaginationReached)
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun prepend_returnsEndWithoutCallingTheApi() {
        val result = load(LoadType.PREPEND)

        assertTrue((result as MediatorResult.Success).endOfPaginationReached)
        assertTrue(api.calls.isEmpty())
        assertEquals(0, store.applyCalls)
    }

    @Test
    fun failedRefresh_returnsErrorAndLeavesTheStoreAsItWas() {
        load(LoadType.REFRESH)
        load(LoadType.APPEND)
        val idsBefore = store.ids()
        val stateBefore = store.saved
        val applyCallsBefore = store.applyCalls

        api.failures += UnknownHostException("offline")
        val result = load(LoadType.REFRESH)

        assertTrue((result as MediatorResult.Error).throwable is UnknownHostException)
        assertEquals(idsBefore, store.ids())
        assertEquals(stateBefore, store.saved)
        assertEquals(applyCallsBefore, store.applyCalls)
    }

    @Test
    fun failedFirstLoad_leavesAnEmptyStore() {
        api.failures += IOException("HTTP 503")
        val result = load(LoadType.REFRESH)

        assertTrue(result is MediatorResult.Error)
        assertTrue(store.posts.isEmpty())
        assertNull(store.saved)
    }

    @Test
    fun failedAppend_keeps40PostsAndTheCursor() {
        load(LoadType.REFRESH)
        load(LoadType.APPEND)

        api.failures += UnknownHostException("offline")
        val result = load(LoadType.APPEND)

        assertTrue(result is MediatorResult.Error)
        assertEquals(40, store.posts.size)
        assertEquals("cur_61", store.saved?.nextCursor)
    }

    @Test
    fun retryAfterAFailedAppend_loadsTheSamePage() {
        load(LoadType.REFRESH)
        api.failures += IOException("HTTP 500")
        load(LoadType.APPEND)

        load(LoadType.APPEND)

        assertEquals(listOf("first", "after=cur_81", "after=cur_81"), api.calls)
        assertEquals(ids(100, 61), store.ids())
    }

    @Test
    fun refresh_replacesTheOldRowsAndTheCursor() {
        load(LoadType.REFRESH)
        load(LoadType.APPEND)
        api.server.publish(2)

        load(LoadType.REFRESH)

        // Two pages saved before, only the newest page after.
        assertEquals(ids(102, 83), store.ids())
        assertEquals("cur_83", store.saved?.nextCursor)
    }

    @Test
    fun append_withABadCursor_endsInARefresh() {
        load(LoadType.REFRESH)
        load(LoadType.APPEND)
        api.server.publish(2)
        api.failures += BadCursorException("cur_61")

        val result = load(LoadType.APPEND)

        assertFalse((result as MediatorResult.Success).endOfPaginationReached)
        assertEquals(listOf("first", "after=cur_81", "after=cur_61", "first"), api.calls)
        assertEquals(ids(102, 83), store.ids())
        assertEquals("cur_83", store.saved?.nextCursor)
    }

    @Test
    fun append_withABadCursorEveryTime_endsInAnError() {
        load(LoadType.REFRESH)
        // The server rejects every cursor: the first 400 refreshes once, the
        // second 400 for the same cursor is an error and nothing is rewritten.
        api.failures += BadCursorException("cur_81")
        api.failures += null
        api.failures += BadCursorException("cur_81")
        val first = load(LoadType.APPEND)
        val applyCallsAfterFirst = store.applyCalls
        val idsAfterFirst = store.ids()

        assertTrue(first is MediatorResult.Success)
        assertEquals(listOf("first", "after=cur_81", "first"), api.calls)

        // The refresh left cur_81 as the stored cursor, so this APPEND sends it again.
        val second = load(LoadType.APPEND)

        assertTrue(second is MediatorResult.Error)
        assertEquals(applyCallsAfterFirst, store.applyCalls)
        assertEquals(idsAfterFirst, store.ids())
        assertEquals(listOf("first", "after=cur_81", "first", "after=cur_81"), api.calls)
    }

    @Test
    fun append_withABadCursorThenAFailedRefresh_returnsErrorAndKeepsThePosts() {
        load(LoadType.REFRESH)
        api.failures += BadCursorException("cur_81")
        api.failures += UnknownHostException("offline")

        val result = load(LoadType.APPEND)

        assertTrue(result is MediatorResult.Error)
        assertEquals(ids(100, 81), store.ids())
    }

    @Test(expected = CancellationException::class)
    fun cancellation_isNotTurnedIntoAnError() {
        api.failures += CancellationException("scope gone")

        load(LoadType.REFRESH)
    }
}
