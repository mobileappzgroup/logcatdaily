# data-copy

A note screen with a list of tags. The Draft is made with `copy()` from the Original. One button adds a tag to the Draft.

## Symptom

Tap Add tag to draft once. The Draft shows `android, kotlin`, and so does the Original. Nothing ever touched the Original. In Logcat the two lists have the same identity hash.

## Trace

From a real run (API 37 emulator), `adb logcat -s logcatdaily`, broken variant, one tap:

```
D logcatdaily: copy: original list=2eaf98d draft list=2eaf98d
D logcatdaily: draft.tags.add: original=[android, kotlin] draft=[android, kotlin] ids 2eaf98d 2eaf98d
```

Same steps on the fixed variant:

```
D logcatdaily: copy: original list=6236742 draft list=6236742
D logcatdaily: draft.copy(tags + kotlin): original=[android] draft=[android, kotlin] ids 6236742 3aa839
```

In the fixed variant the two lists start as one object too, but nothing edits it in place. The tap builds a new list (new id `3aa839`).

## Culprit

[`DataCopySample.kt:27`](DataCopySample.kt#L27) and [`DataCopySample.kt:48`](DataCopySample.kt#L48)

```kotlin
data class BrokenNote(val owner: String, val tags: MutableList<String>)
...
draft.tags.add("kotlin")
```

`copy()` is shallow: it copies the reference to the list, not the list. Draft and Original point at one `MutableList`, so `add` on one changes both.

## Fix

[`DataCopySample.kt:32`](DataCopySample.kt#L32) and [`DataCopySample.kt:59-61`](DataCopySample.kt#L59-L61)

```kotlin
data class FixedNote(val owner: String, val tags: List<String>)
...
draft = draft.copy(tags = draft.tags + "kotlin")
```

The field is a read-only `List`, so a change has to build a new list (`+` returns one). The Original keeps the old list. If you must keep a mutable list, copy it: `original.copy(tags = original.tags.toMutableList())`.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample data-copy --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample data-copy --es variant fixed
adb logcat -s logcatdaily
```
