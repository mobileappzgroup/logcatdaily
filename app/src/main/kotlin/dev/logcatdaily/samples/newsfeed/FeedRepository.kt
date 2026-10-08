package dev.logcatdaily.samples.newsfeed

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FeedRepository(
    private val dao: FeedDao,
    private val mediator: FeedRemoteMediator,
) {
    // initialLoadSize defaults to 3 x pageSize. The mediator always asks the
    // server for PAGE_SIZE, so this only sets how many saved rows the first read takes.
    @OptIn(ExperimentalPagingApi::class)
    fun feed(): Flow<PagingData<FeedPost>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            prefetchDistance = 5,
            enablePlaceholders = false,
        ),
        remoteMediator = mediator,
        pagingSourceFactory = { dao.pagingSource() },
    ).flow.map { data -> data.map { it.toPost() } }
}
