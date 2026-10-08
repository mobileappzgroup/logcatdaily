package dev.logcatdaily.samples.newsfeed

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val HOME_FEED_ID = "home"

// What the screen draws. The server sends this shape, Room stores it as
// FeedPostEntity.
data class FeedPost(
    val id: String,
    val seq: Int,
    val authorId: String,
    val author: String,
    val text: String,
    val createdAt: Long,
    val likeCount: Int,
)

// seq is the server's order, newest first. likeCount is display only and
// changes on the server, so a saved count can be stale until the next refresh.
@Entity(tableName = "feed_posts", indices = [Index("seq")])
data class FeedPostEntity(
    @PrimaryKey val id: String,
    val seq: Int,
    val authorId: String,
    val author: String,
    val text: String,
    val createdAt: Long,
    val likeCount: Int,
)

// One row per feed. The feed only grows downward, so a single "next page"
// pointer is enough. refreshedAt is local time.
@Entity(tableName = "feed_state")
data class FeedState(
    @PrimaryKey val feedId: String = HOME_FEED_ID,
    val nextCursor: String?,
    val endReached: Boolean,
    val refreshedAt: Long,
)

fun FeedPost.toEntity() = FeedPostEntity(id, seq, authorId, author, text, createdAt, likeCount)

fun FeedPostEntity.toPost() = FeedPost(id, seq, authorId, author, text, createdAt, likeCount)
