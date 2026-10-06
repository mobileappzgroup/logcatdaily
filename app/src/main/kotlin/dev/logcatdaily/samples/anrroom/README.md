# anr-room

A notes screen with a draft field and a Save button, backed by a real Room
database. Tap Save in the broken version and the whole app stops answering.

## Symptom

One tap and the screen freezes, no ripple, no scroll. Android shows its own
"isn't responding" dialog after about 5 seconds, which is the input dispatch
timeout. The write keeps blocking for about 8 seconds, so the freeze outlasts
the dialog trigger.

## Trace

From the recording, tag `logcatdaily` plus the system lines around it, broken
variant:

```
logcatdaily: save started
InputDispatcher: Window 4d9a8c5 dev.logcatdaily.samples/dev.logcatdaily.samples.MainActivity is unresponsive: 4d9a8c5 dev.logcatdaily.samples/dev.logcatdaily.samples.MainActivity is not responding. Waited 5001ms for MotionEvent
ActivityManager: ANR in dev.logcatdaily.samples (dev.logcatdaily.samples/.MainActivity)
logcatdaily: save finished
InputDispatcher: 4d9a8c5 dev.logcatdaily.samples/dev.logcatdaily.samples.MainActivity spent 8028ms processing MotionEvent
```

`save started` at 2.1 s, unresponsive at 7.1 s, `save finished` at 10.2 s.
Same tap on the fixed variant, still about 8 s of work but off the main thread:

```
logcatdaily: save started
logcatdaily: save finished
```

Logcat says an ANR happened, the main thread stack says where. This one is
from a bugreport of a separate run for the long video, trimmed:

```
"main" prio=5 tid=1 Sleeping
  at java.lang.Thread.sleep(Native method)
  at dev.logcatdaily.samples.anrroom.NoteDao.insertHeavy(NoteDao.kt:34)
  at dev.logcatdaily.samples.anrroom.NoteDao.insertBlocking(NoteDao.kt:17)
  ...
  at dev.logcatdaily.samples.anrroom.AnrRoomSampleKt.BrokenNoteScreen$onSaveClick(AnrRoomSample.kt:98)
```

## Culprit

[`AnrRoomSample.kt:98`](AnrRoomSample.kt#L98)

```kotlin
db.noteDao().insertBlocking(Note(body = NOTE_BODY))
```

A non-suspend DAO call in the click handler, so the write runs on the main
thread. Room throws on that by default, so the sample turns the guard off with
`allowMainThreadQueries()` ([line 85](AnrRoomSample.kt#L85)), demo only. The
write is padded to about 8 s so the freeze is easy to see.

## Fix

[`NoteDao.kt:22`](NoteDao.kt#L22) and [`NotesViewModel.kt:31`](NotesViewModel.kt#L31)

```kotlin
open suspend fun insert(note: Note): Long = insertHeavy(note)

viewModelScope.launch {
    dao.insert(Note(body = body))
```

Room runs suspend DAO functions on its own executor, so the call leaves the
main thread with no `Dispatchers.IO` in the caller.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample anr-room --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample anr-room --es variant fixed
adb logcat -s logcatdaily
```

## Video

- Long: https://youtu.be/yy_3-QK38O4
- Short: https://youtube.com/shorts/Q84JGz0jiME
- Instagram reel: https://www.instagram.com/reel/DeJ80kYgU7O/
