# effect-key

A small profile screen that loads once when it opens. A counter on screen
shows how many loads have run.

## Symptom

Open the broken version and do nothing. The "loads triggered" counter keeps
climbing on its own, one load every 0.7 seconds, with no input at all. 14
loads in the first 11 seconds of the recording.

## Trace

From the recording, tag `logcatdaily`, broken variant:

```
effect started #1
loaded #1
effect started #2
loaded #2
...
loaded #13
effect started #14
```

27 lines in 11 s, never stops. Same screen on the fixed variant:

```
effect started #1
loaded #1
```

## Culprit

[`EffectKeySample.kt:57`](EffectKeySample.kt#L57)

```kotlin
LaunchedEffect(uiState) {
```

The effect is keyed on the whole ui state object, and the effect body writes
a new ui state (a copy with a fresh `loadedAt`) when the load finishes. New
object, new key. Compose sees the key change, tears the effect down and
starts it again. The effect restarts itself forever.

## Fix

[`EffectKeySample.kt:70`](EffectKeySample.kt#L70)

```kotlin
LaunchedEffect(userId) {
```

`userId` does not change once the screen is up, so the effect runs once and
stays done. The key should be the thing that, when it changes, should restart
the work. Not the thing the work produces.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample effect-key --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample effect-key --es variant fixed
adb logcat -s logcatdaily
```

## Video

- Short: https://youtube.com/shorts/W6qb3bkd_os
- Reel: https://www.instagram.com/logcatdaily/reel/Dd37M9JCsrW/
