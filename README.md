# logcat daily samples

Sample Android code behind the logcat daily reels and Shorts.
Each folder reproduces one real Android bug or demo: broken first, then fixed.

Written and maintained by Nishant Chauhan ([LinkedIn](https://www.linkedin.com/in/nishant-chauhan-ab476026/)).

- Instagram: https://instagram.com/logcatdaily
- YouTube: https://youtube.com/@logcatdaily

Kotlin, Jetpack Compose, minSdk 26.

Every sample launches from the same activity, picked by intent extras:

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity \
  --es sample scroll-derived --es variant broken
```

`sample` is one of `scroll-derived`, `rotation-state`, `effect-key`, `anr-room`, `lazy-key`,
`cancel-swallow`, `cancel-loop`, `cancel-finally`, `form-hoist`.
`variant` is `broken` or `fixed`. Everything logs under the tag `logcatdaily`.

## scroll-derived

A 100-row list with a "scroll to top" button. Reading `firstVisibleItemIndex`
in the composable recomposes the screen on every row that scrolls past, 44
times in one short scroll. `derivedStateOf` brings it down to 2.
Trace, culprit line and fix: [scrollderived/README.md](app/src/main/kotlin/dev/logcatdaily/samples/scrollderived/README.md)

## rotation-state

A counter with a button. Rotate the phone and the broken version resets to
zero, because `remember` only survives recomposition, not the activity
getting recreated (which is what happens on a config change like rotation
by default).

Fix is `rememberSaveable`, which writes into the saved instance state bundle
so the value comes back after the rotation.
Trace, culprit line and fix: [rotationstate/README.md](app/src/main/kotlin/dev/logcatdaily/samples/rotationstate/README.md)

## effect-key

A profile screen whose `LaunchedEffect` is keyed on the ui state it writes to,
so it restarts itself every 0.7 s forever. Keying on `userId` runs it once.
Trace, culprit line and fix: [effectkey/README.md](app/src/main/kotlin/dev/logcatdaily/samples/effectkey/README.md)

## anr-room

A light-themed notes screen (seeded list, draft field, Save button), backed by a real Room database
(`Note`, `NoteDao`, `NoteDatabase`). Broken version opens the database with
`allowMainThreadQueries()` and calls a non-suspend `@Insert` straight from
the click handler, on the main thread. The write is a real transaction (the
note plus a batch of history rows, padded with a sleep) that takes about 8
seconds, so tapping Save freezes the app; tap it again while it's frozen and
you get a real system "isn't responding" dialog, because an input event went
unhandled for more than 5 seconds.

Fix is the same write behind a suspend `@Insert`, called from a `ViewModel`
via `viewModelScope.launch`. Room runs suspend DAO calls on its own executor,
off the main thread, with no `Dispatchers.IO` needed, so the UI stays free
to show a spinner while it saves.
Trace, culprit line and fix: [anrroom/README.md](app/src/main/kotlin/dev/logcatdaily/samples/anrroom/README.md)

## lazy-key

A task list with checkboxes, a Shuffle button, and a delete (X) button on
each row. Each row remembers its own checked state. Broken version calls
`items(tasks)` with no key, so Compose identifies each row by its position
in the list. Check a task, delete a task above it, and the list shifts up:
the checkmark stays behind at that row position, now showing whatever task
landed there.

Fix is `items(tasks, key = { it.id })`, so the checked state travels with
the task it belongs to, not the slot it happened to be sitting in.

## cancel-swallow

A 6-step "upload" that ticks once every 500ms in `viewModelScope`. Broken
wraps each step in `catch (e: Exception)`, the classic "keep retrying on any
failure" mistake. That also catches the `CancellationException` the coroutine
gets when Cancel is tapped, so the loop treats getting cancelled the same as
a network hiccup and just moves on to the next step anyway, right through to
"upload finished".

Fix checks `e is CancellationException` and rethrows it instead of retrying,
so cancelling actually stops the coroutine.

## cancel-loop

A CPU-bound counting loop on `Dispatchers.Default`, no suspend call inside
it. Broken never checks `isActive`, so there's no point where `cancel()` can
interrupt it: the tick count keeps climbing after Cancel is tapped, same as
if nothing happened, until it hits its own safety cap.

Fix checks `isActive` on every pass, so the loop notices the cancellation on
the very next spin and the tick count freezes immediately.

## cancel-finally

A 5-step save with a `finally` block that runs cleanup after the work loop.
Broken calls the suspend `cleanup()` directly from `finally`. By the time
`finally` runs the job is already cancelling, so that suspend call throws
immediately and cleanup logs "cleanup started" but never "cleanup done".

Fix wraps the same call in `withContext(NonCancellable) { cleanup() }`, so
cleanup is allowed to run to completion even though the job is cancelled.

## back-press

Not launched through MainActivity: it has its own activities, started directly so the screen is the task root.

```
adb shell am start -n dev.logcatdaily.samples/.backpress.BackPressActivity --es variant broken
```

A draft card with an Edit button that counts unsaved edits. `BackPressActivity` overrides
`onBackPressed()` to log and show a "Discard changes?" dialog. With `targetSdk = 36` on an
Android 16 device, back never reaches that override, so the app just closes with the edits lost.

`--es variant fixed` adds a Compose `BackHandler(enabled = edits > 0)`, which does run and opens the dialog.

`BackPressOptOutActivity` is the same screen and the same override, with
`android:enableOnBackInvokedCallback="false"` on its manifest entry. On the emulator the override runs again there.
Send back with `adb shell input keyevent KEYCODE_BACK`.

## form-hoist

A Profile card with Name and Email fields and a Clear button. Broken version gives each field its own
`rememberSaveable`, so the form has no state to write to and Clear does nothing to the text (it only logs).

Fix hoists both strings into the form: the fields take `value` and `onValueChange`, and Clear sets both to
an empty string. The fixed variant also logs the whole form state on every recomposition.

## interop

`fragment_profile.xml` hosts a `ComposeView` inside an XML layout. `ProfileFragment` binds it with view binding and
sets `ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed` before `setContent`, which is what the interop
docs recommend for a ComposeView in a Fragment's view.

```
adb shell am start -n dev.logcatdaily.samples/.interop.ProfileHostActivity
```
