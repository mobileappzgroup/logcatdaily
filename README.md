# logcat daily samples

Sample Android code behind the logcat daily reels and Shorts.
Each folder reproduces one real Android bug or demo: broken first, then fixed.

- Instagram: https://instagram.com/logcatdaily
- YouTube: https://youtube.com/@logcatdaily

Kotlin, Jetpack Compose, minSdk 26.

Every sample launches from the same activity, picked by intent extras:

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity \
  --es sample scroll-derived --es variant broken
```

`sample` is one of `scroll-derived`, `rotation-state`, `effect-key`.
`variant` is `broken` or `fixed`. Everything logs under the tag `logcatdaily`.

## scroll-derived

A 100-row list with a "scroll to top" button that should only show up once
you've scrolled past the first row.

Broken version reads `listState.firstVisibleItemIndex` straight in the
composable. That reads a snapshot value from the scroll state, so the whole
composable recomposes on every index change while you scroll, not just the
one time the button needs to show or hide. Watch the logcat counter, it spams.

Fix is `derivedStateOf`: wrap the comparison so only the boolean result is
observed. Now it only recomposes when the button actually needs to flip.

## rotation-state

A counter with a button. Rotate the phone and the broken version resets to
zero, because `remember` only survives recomposition, not the activity
getting recreated (which is what happens on a config change like rotation
by default).

Fix is `rememberSaveable`, which writes into the saved instance state bundle
so the value comes back after the rotation.

## effect-key

A tiny "profile" screen that loads on start. Broken version keys its
`LaunchedEffect` on the whole ui state object, and the effect itself updates
that object when the load finishes. New object, new key, so Compose tears
down and restarts the effect again. It loops forever without you touching
anything.

Fix is keying on `userId` instead, which doesn't change once the screen is
up, so the effect runs once and stays done.
