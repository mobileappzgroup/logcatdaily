# lazy-key

A task list with a checkbox, a delete button and a Shuffle button on each
row. Check one task, delete another, and the wrong row stays checked.

## Symptom

I checked "Update compileSdk to 36" (task 2), then deleted task 1. The list
moved up and "Update compileSdk to 36" now shows unchecked, while "Review PR
#42" (task 3) is checked, which I never touched.

## Trace

From the recording, tag `logcatdaily`, broken variant:

```
logcatdaily: composing item id=2 label="Update compileSdk to 36" checked=true
logcatdaily: composing item id=2 label="Update compileSdk to 36" checked=false
logcatdaily: composing item id=3 label="Review PR #42" checked=true
logcatdaily: composing item id=4 label="Reply to Play Store review" checked=false
...
logcatdaily: composing item id=8 label="Ship dark mode" checked=false
```

The first line is the check, 1.74 s before the delete. After the delete, id=2
reads `checked=false` and id=3 reads `checked=true`. The state stayed at its
position, the task moved. Rows 4 to 8 recompose too, since each position now
holds a different task. Same steps on the fixed variant:

```
logcatdaily: composing item id=2 label="Update compileSdk to 36" checked=true
```

That is the last line of the capture. The delete is not logged, and in this
run no row recomposes after it, because every keyed row keeps both its task
and its remembered state.

## Culprit

[`LazyKeySample.kt:109`](LazyKeySample.kt#L109)

```kotlin
items(tasks) { task -> TaskRow(task, onDelete = { deleteTask(task.id) }) }
```

With no key, `items` identifies each row by its position. `TaskRow` holds
`var checked by remember { mutableStateOf(false) }` ([line 120](LazyKeySample.kt#L120)),
and that remembered value belongs to the slot, not to the task. Remove a task
above it and the tasks shift up one slot while the checked state stays where
it was.

## Fix

[`LazyKeySample.kt:111`](LazyKeySample.kt#L111)

```kotlin
items(tasks, key = { it.id }) { task -> TaskRow(task, onDelete = { deleteTask(task.id) }) }
```

Now each row is identified by `task.id`, so its remembered state moves with
the task. The key has to be unique per item: a duplicate key throws an
`IllegalArgumentException` when the list composes the second item. It also has
to be stable, so an id and not the index or a mutable field. With
`rememberSaveable` the key must also be saveable in a `Bundle`.
`Modifier.animateItem()` needs keys too, so Compose can find the new position
of an item that moved.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample lazy-key --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample lazy-key --es variant fixed
adb logcat -s logcatdaily
```

## Video

- Short: https://youtube.com/shorts/FdDZRwn8Ois
- Reel: https://www.instagram.com/reel/DeMhqFpAf90/
