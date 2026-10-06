# offline-chat

Design: an offline-first 1:1 chat for Android. Typing a message never waits
for the network, and a message is stored on the server once, even when the
reply to the send is lost.

Video: (added on go-live day)

## Question

Design a chat screen that works on a bad connection. I build the send path
and the catch-up path. Live receive is drawn in the video and not built here.

## Constraints

Must do:
- Send a message with no connection and show it right away as SENDING.
- Send it for real when the connection comes back, without the user doing anything.
- Show messages from the other side, in the server's order.

Must handle:
- The app dies after the tap, before the send.
- The server stores the message and the reply never arrives.
- Timeouts, 5xx and rate limits, which are worth another try, and a 4xx that is not.

Out of scope: groups, attachments, read receipts, typing and presence, auth, push.

## App map

Five boxes. The screen only talks to the repository.

- UI layer: `ChatScreen`, `ChatViewModel`. Reads Room through one flow.
- Repository: `MessageRepository`. The only place that knows about both sources.
- Local source: `ChatDatabase` (Room) with `MessageDao` and `OutboxDao`.
- Remote source: `ChatApi`, plain `HttpURLConnection`.
- Trigger: `SendWorker`, a WorkManager request that needs a connection.

Repository, Local source and Remote source are the Data layer. There is no
domain layer in this episode. The server is the mock in `mock-chat-server/`.

## API

| call | body | reply |
|---|---|---|
| `POST /conversations/{conversationId}/messages`, header `Idempotency-Key: <clientId>` | `{clientId, senderId, text}` | `{clientId, serverSeq, sentAt}` |
| `GET /conversations/{conversationId}/messages?after=N` | none | `[{seq, clientId, senderId, text, sentAt}]` |

The server dedupes on the key: a repeated key gets the first reply and
stores nothing. The sample uses one conversation, `c1`. The phone is
`nishant`.

## Data model

| table | columns |
|---|---|
| `messages` | `id`, `clientId`, `conversationId`, `senderId`, `text`, `status` (SENDING, SENT, FAILED), `createdAt` (local tap time), `sentAt` (server time, null until acked), `serverSeq` (null until acked, unique per conversation) |
| `outbox` | `clientId`, `attempts`, `lastError` |
| `sync_state` | `conversationId`, `lastSeq` |

`clientId` is not unique on purpose, see the debug toggle below.

## The crux

Three parts that only work together:

1. Outbox. The message and its outbox row are written in one transaction in
   [`send`](MessageRepository.kt#L45), so a message can never sit in SENDING
   with nobody to send it.
2. Send now, retry later. `send` tries once in the foreground. If that fails
   it queues a unique `SendWorker` for the message, which retries with
   backoff, 8 tries at most ([`attemptSend`](MessageRepository.kt#L87)). On
   chat open, [`resumePending`](MessageRepository.kt#L81) queues anything
   the process died on.
3. Idempotency key. The key is the `clientId`
   ([`ChatApi.kt:67`](ChatApi.kt#L67)). Retrying is only safe because the
   server answers a repeated key with the first reply.

Retry rule ([`failureFor`](ChatApi.kt#L28)): network errors, 5xx, 408, 425 and
429 retry. Any other 4xx is final and the bubble turns FAILED. Tap a FAILED
bubble to start over with attempts back at 0.

[`sync`](MessageRepository.kt#L123) asks for rows after `lastSeq` on chat open
and after each ack, and merges them by `serverSeq`: already stored is
skipped, a `clientId` that matches an unacked local row is adopted (seq and
`sentAt`), anything else becomes its own SENT message. Acked rows are shown
by `serverSeq`, pending ones last by `createdAt`.

## Run the proof

Start the mock on the Mac (port 8080). The emulator reaches it at `10.0.2.2`:

```
./mock-chat-server/run.sh
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample offline-chat
adb logcat -s logcatdaily
```

1. Airplane mode on, send a message. It stays SENDING.
2. Airplane mode off. `send attempt` shows up, the bubble turns SENT, the
   server prints one `stored` line.
3. Lost ack: restart with `./mock-chat-server/run.sh --drop-next-reply` (or
   `curl -X POST localhost:8080/admin/drop-next-reply`). The server stores the
   next message and drops the reply. The app logs `send failed`, the worker
   retries, the server prints `duplicate key` and nothing new is stored.
4. Switch on "debug: no idempotency key" and repeat step 3. The retry has no
   key, so the server stores the message twice.
5. Check what the other side gets:

```
curl 'http://localhost:8080/conversations/c1/messages?after=0'
```

With the key you see one row for the message. Without it, two. To play the
other phone and see a row that is not yours arrive on the next sync:

```
curl -X POST localhost:8080/conversations/c1/messages \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: friend-1' \
  -d '{"clientId":"friend-1","senderId":"friend","text":"hey"}'
```

`POST /admin/reset` empties the server.

## Why the debug toggle is there

"debug: no idempotency key" is not a feature. It sends the same request
without the header so I can show what the key is for. Deduping on `clientId`
in Room would hide the second bubble on my phone, but the server and the
other phone would still have two messages. That is why `clientId` is not
unique locally and why the check above is a curl against the server.

## Deliberately simplified

- No socket. Live receive in a real app is a WebSocket while the chat is
  open, or FCM when it is not, with `sync` running after every reconnect.
  This sample builds the pull only.
- No auth, so no token refresh and no 401 handling.
- No dependency injection. `ChatGraph` wires it by hand.
- Room uses a destructive migration. A schema change wipes the local data.
- The retry is a plain `OneTimeWorkRequest`. A real app would make a user send
  expedited work so Doze does not delay it.

Tests: `./gradlew :app:testDebugUnitTest` runs the merge outcomes, the send
outcomes with a fake `ChatApi` (success, network error, 429, 400, 8th failure,
lost ack with and without the key) and the tap to retry.
