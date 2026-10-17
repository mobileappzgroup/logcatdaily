# news-feed

Design: the Instagram or Twitter feed for an Android app. Scroll without
repeats or gaps while new posts land at the top and old ones are deleted, and
keep scrolling with no connection.

Video: https://youtu.be/aOWp1I2tnB0

## The question

Design the news feed. I build the read path: scroll, pull to refresh, new posts
at the top, offline. Out of scope: posting, likes, images, ranking, building the
feed on the server. A prefetch worker and a "N new posts" pill are drawn in the
video, not built. Offline, errors with tap to retry, and a feed that changes
while you page it are the constraints; the last one is the crux.

## App map

| box | file | trade-off |
|---|---|---|
| UI layer | [`FeedScreen.kt`](FeedScreen.kt), [`FeedViewModel.kt`](FeedViewModel.kt) | rows stay put, new posts sit above |
| Repository | [`FeedRepository.kt`](FeedRepository.kt) | one `Pager`, more setup than a list |
| Local source | [`FeedDatabase.kt`](FeedDatabase.kt), [`FeedDao.kt`](FeedDao.kt) | works offline, the cache needs a cap |
| Remote source | [`FeedApi.kt`](FeedApi.kt) | cursor: no jumping to page 7 |

The screen reads Room. Only the mediator writes it. `FeedApi` is plain
`HttpURLConnection` to stay dependency-free; in an app it is Retrofit, OkHttp or Ktor.

## API

| call | notes |
|---|---|
| `GET /feed?limit=20` | `{items, nextCursor}`, newest first, first cursor is `cur_81` |
| `GET /feed?limit=20&after=cur_81` | posts older than the cursor, opaque to the client. Bad or expired: 400 |
| `GET /feed?limit=20&offset=20` | no cursor in the reply, the `offset` arm only |

Mock server: `mock-feed-server/`, port 8081, 100 mock posts. Admin (POST): `/admin/reset`,
`publish?n=2`, `delete-head?n=2`, `drop-next-reply`, `expire-next-cursor`, `latency?ms=N`.

## Data model

`feed_posts`: `id` (`p-0082`), `seq` (server order), `authorId`, `author`, `text`,
`createdAt`, `likeCount` (mock, changes on the server).
`feed_state`: one row, `feedId` = `home`, `nextCursor`, `endReached`, `refreshedAt`
(written and carried forward, not used to decide anything). One row is enough because pages only go toward older posts.

## The crux

Offset paging breaks when the list changes ahead of the offset. Two posts land at
the top and page 2 starts two rows early: `p-0082` and `p-0081` show twice. Two are
deleted and page 2 starts two rows late: `p-0080` and `p-0079` never show. A
client-side `distinctBy` ([`OffsetFeed.kt`](OffsetFeed.kt#L61), a `filter` and a set
of ids, `PagingData` has no `distinctBy`) hides the first and cannot see the second.

A cursor is a position, "older than `cur_81`", so neither change moves it, as long
as `seq` only grows and the order does not change. A ranked feed needs the server
to pin the order per session. [`FeedRemoteMediator`](FeedRemoteMediator.kt#L32)
keeps the next cursor in `feed_state` in the same transaction as the posts
([`applyPage`](FeedStore.kt#L24)), after the response arrived. A failed load
returns an error and writes nothing. A 400 on APPEND runs one REFRESH instead of a
retry button, and a second 400 for the same cursor is an error. REFRESH replaces the
table instead of merging: after more than a page of new posts a merge that keeps the
old cursor leaves a hole, and one that resets the cursor downloads the saved pages again.

## Run the proof

```
./mock-feed-server/run.sh
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample news-feed --es variant cursor
adb logcat -s logcatdaily
```

`variant` is `cursor`, `offset` (Paging 3 with page keys) or `offset-dedupe`; the
line under the title says which. Publish two with `curl -X POST 'localhost:8081/admin/publish?n=2'`
and scroll on `offset`: the server prints `GET /feed limit=20 offset=20 -> p-0082..p-0063`,
while `curl 'localhost:8081/feed?limit=3&after=cur_81'` starts at `p-0080`. After
`delete-head?n=2` page 2 starts at `p-0078` on `offset-dedupe` and `p-0080` on `cursor`.
The cursor arm loads two pages on open: set `/admin/latency?ms=2000`, open the app,
and delete once the first `GET` shows in the server log.

```
FeedMediator REFRESH -> 20 posts, next=cur_81
FeedMediator APPEND after=cur_81 -> 20 posts, next=cur_61
FeedMediator APPEND after=cur_61 failed: ConnectException, kept 40 posts
FeedMediator APPEND after=cur_61 rejected (400), refreshing
```

Offline is a `ConnectException` here because the sample calls an IP. With a hostname
you would usually see `UnknownHostException`. Pinned: Paging 3.3.6, room-paging 2.8.4,
Room 2.8.4.

Tests: `./gradlew :app:testDebugUnitTest --tests 'dev.logcatdaily.samples.newsfeed.*'`.
They cover the mediator against an in-memory store (one write per load, a failed
fetch writes nothing) and the repeat, the skip and the cursor against a list that
acts like the server. The Room transaction itself is not unit tested.

## Deliberately simplified

- No cap on the cache. A real app limits it by count or age.
- Refresh drops pages saved deeper than the newest. Chosen, not forgotten.
- Newest first only. A ranked feed keeps this client, the server pins the ranking.
- No background arrival can move the list, because the sample has no new-posts signal; the pill is drawn, not built.
- After process death the list comes back from Room at the top, scroll position is
  not restored (tried once on the emulator). With a network the refresh then replaces
  it with the newest page.

## Video

- Long: https://youtu.be/aOWp1I2tnB0
- Short: added on its go-live day (Sun 2026-10-18)
