# news-feed

Design: the Instagram or Twitter feed for an Android app. Scroll without
repeats or gaps while new posts land, and keep scrolling with no connection.

Video: (added on go-live day)

## The question

Design the news feed. I build the read path: scroll, pull to refresh, new
posts at the top, offline. Out of scope: posting, likes, images, ranking, and
building the feed on the server. A prefetch worker and a "N new posts" pill are
drawn in the video, not built.

## Constraints

Offline: saved posts scroll with no signal, a failed fetch changes nothing.
Errors: tap to retry, no retry loop. The crux is the third one: the feed
changes while you page it.

## App map

| box | file | trade-off |
|---|---|---|
| UI layer | [`FeedScreen.kt`](FeedScreen.kt), [`FeedViewModel.kt`](FeedViewModel.kt) | rows stay put, new posts sit above |
| Repository | [`FeedRepository.kt`](FeedRepository.kt) | one `Pager`, more setup than a list |
| Local source | [`FeedDatabase.kt`](FeedDatabase.kt), [`FeedDao.kt`](FeedDao.kt) | works offline, the cache needs a cap |
| Remote source | [`FeedApi.kt`](FeedApi.kt) | cursor: no jumping to page 7 |

The screen reads Room. Only the mediator writes it.

## API

| call | notes |
|---|---|
| `GET /feed?limit=20` | `{items, nextCursor}`, newest first, first cursor is `cur_81` |
| `GET /feed?limit=20&after=cur_81` | posts older than the cursor. Opaque to the client. Bad or expired: 400 |
| `GET /feed?limit=20&offset=20` | no cursor in the reply, the `offset` arm only |

The mock server is `mock-feed-server/`, port 8081, 100 mock posts. Admin (POST):
`/admin/reset`, `publish?n=2`, `delete-head?n=2`, `drop-next-reply`,
`expire-next-cursor`, `latency?ms=N`.

## Data model

`feed_posts`: `id` (`p-0082`), `seq` (server order), `authorId`, `author`, `text`,
`createdAt`, `likeCount` (mock, changes on the server).
`feed_state`: one row, `feedId` = `home`, `nextCursor`, `endReached`, `refreshedAt`.
One row is enough because the feed only grows downward.

## The crux

Offset paging breaks when the list changes ahead of the offset. Two posts land
at the top and page 2 starts two rows early: `p-0082` and `p-0081` show twice.
Two are deleted and page 2 starts two rows late: `p-0080` and `p-0079` never
show. A client-side `distinctBy` ([`OffsetFeed.kt`](OffsetFeed.kt#L61)) hides the
first and cannot see the second.

A cursor is a position, "older than `cur_81`", so neither change moves it.
[`FeedRemoteMediator`](FeedRemoteMediator.kt#L27) keeps the next cursor in
`feed_state` in the same transaction as the posts ([`applyPage`](FeedStore.kt#L24)),
after the response arrived. A failed load returns an error and writes nothing.
A 400 on APPEND ([line 51](FeedRemoteMediator.kt#L51)) runs a REFRESH, not a
retry button. REFRESH replaces the table instead of merging: after more than a
page of new posts a merge leaves a hole.

## Run the proof

```
./mock-feed-server/run.sh
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample news-feed --es variant cursor
adb logcat -s logcatdaily
```

`variant` is `cursor`, `offset` (Paging 3 with page keys) or `offset-dedupe`.
The line under the title says which one runs. Publish two with
`curl -X POST 'localhost:8081/admin/publish?n=2'` and scroll on `offset`: the
server prints `GET /feed limit=20 offset=20 -> p-0082..p-0063`, while
`curl 'localhost:8081/feed?limit=3&after=cur_81'` starts at `p-0080`.
After `delete-head?n=2` page 2 starts at `p-0078` on `offset-dedupe` and at
`p-0080` on `cursor`. The cursor arm loads two pages on open, so set
`/admin/latency?ms=2000` first to delete between them.

```
FeedMediator REFRESH -> 20 posts, next=cur_81
FeedMediator APPEND after=cur_81 -> 20 posts, next=cur_61
FeedMediator APPEND after=cur_61 failed: ConnectException, kept 40 posts
FeedMediator APPEND after=cur_61 rejected (400), refreshing
```

Tests: `./gradlew :app:testDebugUnitTest --tests 'dev.logcatdaily.samples.newsfeed.*'`.
They cover the mediator (including a failed load leaving the store as it was) and
the repeat, the skip and the cursor against a list that acts like the server.

## Deliberately simplified

- No cap on the cache. A real app limits it by count or age.
- Refresh drops pages saved deeper than the newest. Chosen, not forgotten.
- Newest first only. A ranked feed keeps this client: the cursor stays opaque and
  the server pins the ranking per session.
- New posts are never inserted above what you read. The pill is drawn, not built.
- Scroll position is not restored after process death. Text only, mock data.

## Video

(added on go-live day)
