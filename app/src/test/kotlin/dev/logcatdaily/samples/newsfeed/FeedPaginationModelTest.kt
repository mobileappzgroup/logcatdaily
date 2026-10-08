package dev.logcatdaily.samples.newsfeed

import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The claim the video makes: insert above is a repeat, delete above is a skip,
// and a client-side dedupe only sees the first. Tested without Android, against
// a list that behaves like the server.
class FeedPaginationModelTest {
    private val api = FakeFeedApi()
    private val source = OffsetPagingSource(api)

    private fun ids(from: Int, to: Int) = (from downTo to).map { "p-%04d".format(it) }

    private fun loadFirst(): List<String> = runBlocking {
        val result = source.load(LoadParams.Refresh(null, 20, false)) as LoadResult.Page
        result.data.map { it.id }
    }

    private fun loadSecond(): List<String> = runBlocking {
        val result = source.load(LoadParams.Append(20, 20, false)) as LoadResult.Page
        result.data.map { it.id }
    }

    private fun cursorSecondPage(): List<String> = runBlocking {
        val first = api.page(null, 20)
        api.page(first.nextCursor, 20).items.map { it.id }
    }

    @Test
    fun offset_afterTwoInserts_repeatsTwoPosts() {
        val first = loadFirst()
        api.server.publish(2)
        val second = loadSecond()

        assertEquals(listOf("p-0082", "p-0081"), first.intersect(second.toSet()).sortedDescending())
        assertEquals("p-0082", second.first())
    }

    @Test
    fun offset_afterTwoDeletes_skipsTwoPosts() {
        val first = loadFirst()
        api.server.deleteHead(2)
        val second = loadSecond()

        // p-0080 and p-0079 exist on the server and are never shown.
        assertEquals("p-0078", second.first())
        assertTrue((first + second).none { it == "p-0080" || it == "p-0079" })
        assertTrue(api.server.posts.any { it.id == "p-0080" })
    }

    @Test
    fun distinctBy_removesTheRepeat() {
        val shown = loadFirst().let { first ->
            api.server.publish(2)
            first + loadSecond()
        }

        assertEquals(40, shown.size)
        assertEquals(38, shown.distinct().size)
    }

    @Test
    fun distinctBy_doesNotSeeTheSkip() {
        val shown = loadFirst().let { first ->
            api.server.deleteHead(2)
            first + loadSecond()
        }

        // Nothing is repeated, so there is nothing for distinct to remove, and
        // the list still jumps from p-0081 to p-0078.
        assertEquals(shown, shown.distinct())
        assertEquals(listOf("p-0081", "p-0078"), shown.subList(19, 21))
    }

    @Test
    fun cursor_afterTwoInserts_repeatsNothing() {
        val first = runBlocking { api.page(null, 20) }
        api.server.publish(2)
        val second = runBlocking { api.page(first.nextCursor, 20) }

        assertEquals(ids(80, 61), second.items.map { it.id })
        assertTrue(first.items.map { it.id }.intersect(second.items.map { it.id }.toSet()).isEmpty())
    }

    @Test
    fun cursor_afterTwoDeletes_skipsNothing() {
        val first = runBlocking { api.page(null, 20) }
        api.server.deleteHead(2)
        val second = runBlocking { api.page(first.nextCursor, 20) }

        assertEquals("p-0080", second.items.first().id)
        assertEquals(ids(80, 61), second.items.map { it.id })
    }

    @Test
    fun cursor_staysValidWhenItsOwnRowIsDeleted() {
        val first = runBlocking { api.page(null, 20) }
        // The cursor means "older than p-0081". Deleting p-0081 does not break it.
        api.server.posts.removeAll { it.id == "p-0081" }
        val second = runBlocking { api.page(first.nextCursor, 20) }

        assertEquals("p-0080", second.items.first().id)
    }

    @Test
    fun cursorPaging_walksAllHundredPostsOnce() {
        val seen = mutableListOf<String>()
        var cursor: String? = null
        do {
            val page = runBlocking { api.page(cursor, 20) }
            seen += page.items.map { it.id }
            cursor = page.nextCursor
        } while (cursor != null)

        assertEquals(ids(100, 1), seen)
    }

    @Test
    fun offsetSource_shortPageIsTheLastOne() {
        api.server.deleteHead(70)
        val result = runBlocking { source.load(LoadParams.Refresh(null, 20, false)) } as LoadResult.Page
        val second = runBlocking { source.load(LoadParams.Append(20, 20, false)) } as LoadResult.Page

        assertEquals(20, result.data.size)
        assertEquals(20, result.nextKey)
        assertEquals(10, second.data.size)
        assertEquals(null, second.nextKey)
    }

    @Test
    fun offsetSource_networkTroubleIsAnErrorResult() {
        api.failures += java.io.IOException("offline")
        val result = runBlocking { source.load(LoadParams.Refresh(null, 20, false)) }

        assertTrue(result is LoadResult.Error)
    }
}
