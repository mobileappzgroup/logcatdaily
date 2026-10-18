# file-upload

Design: upload a big file from an Android app so a dropped connection or a killed
app carries on from where it stopped, never from byte 0, and never sends a chunk
the server already has.

## The question

Design file upload with resume. I build the upload path for one 24 MB file: a queue
in Room, WorkManager, 1 MB chunks, a server session. Out of scope: the file picker
and permissions, compressing or processing after the upload, several uploads at
once, sign-in. A user-initiated transfer job and direct upload to object storage
are drawn in the video, not built.

| constraint | what it means here | the crux? |
|---|---|---|
| connection drops mid-upload | airplane mode at about 55 percent | yes |
| app killed mid-upload | `am force-stop`, then open the app | |
| retry without duplicates | a repeated create or chunk must be harmless | |
| wasted bytes | bytes sent over file size, 1.0 is no waste | |

## App map

| box | file | trade-off |
|---|---|---|
| UI layer | [`UploadsScreen.kt`](UploadsScreen.kt), [`UploadsViewModel.kt`](UploadsViewModel.kt) | the bar moves per chunk, not per byte |
| Repository | [`UploadRepository.kt`](UploadRepository.kt), [`UploadRunner.kt`](UploadRunner.kt) | many states, more to test |
| Local source | [`UploadDatabase.kt`](UploadDatabase.kt), [`UploadDao.kt`](UploadDao.kt) | survives kills, old rows need cleanup |
| Remote source | [`UploadApi.kt`](UploadApi.kt) | small chunks: safe but chatty |
| Trigger | [`UploadWorker.kt`](UploadWorker.kt), [`UploadNotifications.kt`](UploadNotifications.kt) | reliable, but may start late |

The screen reads Room only. The work request carries the upload id, never a path or an offset.
`UploadApi` is plain `HttpURLConnection` to stay dependency-free. Hand-wired in [`UploadGraph.kt`](UploadGraph.kt).

## API

Shaped like the core of tus (HEAD for the offset, append at an offset, 409 on a
mismatch). Not compliant: no `Tus-Resumable` header, and the whole-file `sha256` is ours.

| call | notes |
|---|---|
| `POST /files` | `Upload-Length`, `Idempotency-Key`, `X-Content-SHA256`. 201 and `Location: /files/s-0001`; the same key again gives 200 and the same session |
| `HEAD /files/{id}` | 200 with `Upload-Offset`; unknown session 404 |
| `PATCH /files/{id}` | `Upload-Offset` and the chunk. 204 with the new offset; wrong offset 409; last chunk with a different hash 422. Sent as `POST` with `X-HTTP-Method-Override: PATCH` ([`UploadApi.kt#L47`](UploadApi.kt#L47)) |
| `DELETE /files/{id}` | 204, the partial file is removed |
| `POST /upload` | the whole file in one request (the `single` variant) |

Mock server: `mock-upload-server/`, port 8083, reads bodies at 700 KB/s. Admin (POST): `/admin/reset`,
`throttle?kbps=N`, `drop-reply-at?offset=N`, `drop-create-reply`; GET `/admin/stats`, `/admin/files`.

## Data model

`uploads` ([`UploadEntity.kt`](UploadEntity.kt)): `id` (`u-0001`), `idempotencyKey` (a UUID saved
with the row before any request), `fileName`, `sourcePath` (app-private), `totalBytes`, `sha256`,
`sessionUrl`, `confirmedBytes` (what the app believes the server has), `state`, `attempts`, `lastError`, `createdAt`.

## The crux

A dropped connection can lose the chunk or only its reply. On the emulator an airplane
cut lost the reply: the server saved the 14th chunk about 1 s after the phone went offline.
The obvious fix, "count my own bytes in Room", sends that chunk again at its own count and gets
409 forever, with WorkManager waiting 10, 20, 40 s between tries. The fix that holds
asks the server first ([`askOffset`](UploadRunner.kt#L146), [`ResumeDecision`](ResumeDecision.kt#L12)):
`HEAD` says 14680064, the app had 13631488, it goes on from the server's number.

## Run the proof

```
mock-upload-server/run.sh
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample file-upload --es variant counter
adb shell cmd connectivity airplane-mode enable    # at about 13 MB, off again after 5 s
curl localhost:8083/admin/stats
```

`variant` is `single`, `counter` or `server`. What I saw: `single` sent 39,491,344 bytes for the
25,165,824-byte file (1.57 times). `counter` logged `PATCH offset=13631488 -> 409 conflict` three times.
`server` logged `server offset=14680064, mine=13631488`, finished with `DONE sha256 matches`, and the
server received 25,165,824 bytes: the file once. Tests: `./gradlew :app:testDebugUnitTest --tests
'dev.logcatdaily.samples.fileupload.*'` (chunk plan, resume decision, retry rules, runner variants, wire format).

## Deliberately simplified

- One upload at a time and one chunk at a time. Parallel chunks need part numbers, like S3 multipart.
- A whole-file `sha256` at the end only. A real app also checks each chunk, so a bad byte at 10 percent is found at 10 percent.
- A 404 on `HEAD` (session expired) ends FAILED. A real app starts a new session from 0.
- Plain WorkManager with a `dataSync` foreground service. For an upload the user starts, Google's docs recommend the user-initiated transfer job; there is no Jetpack library for it.
- No auth, http to a mock on the Mac. A real app uses https, sends tokens in a header, and treats the session address as a secret.
- The file is 24 MiB of generated bytes, created in app-private storage, not a picked file.
- Bytes go through the server. A real backend can hand out a signed address so the app uploads straight to object storage; the offset rule is the same there.
