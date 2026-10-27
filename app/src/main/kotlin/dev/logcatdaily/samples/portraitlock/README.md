# portrait-lock

An inbox screen in an activity that the manifest locks to portrait. On a tablet the lock is ignored and the screen rotates anyway.

## Symptom

On the phone emulator the lock holds. On the tablet emulator (API 37, 2560x1600, 800 dp smallest width) the activity still rotates to landscape, and the single-column inbox stretches across the wide screen.

## Trace

From real runs, `adb logcat -s logcatdaily`. Phone, broken variant:

```
D logcatdaily: requested=portrait orientation=portrait sw=411dp
```

Tablet, broken variant, then a rotation:

```
D logcatdaily: requested=unspecified orientation=portrait sw=800dp
D logcatdaily: requested=unspecified orientation=landscape sw=800dp
```

The tablet reports `requested=unspecified` even though the manifest says portrait. The system log says why:

```
I ActivityTaskManager: Ignoring manifest-declared fixed orientation SCREEN_ORIENTATION_PORTRAIT of ActivityRecord{... dev.logcatdaily.samples/.portraitlock.PortraitLockActivity ...} since target sdk 36
```

Same steps on the fixed variant give the same two lines, because the lock is ignored the same way. What changes is the layout: width 800 dp picks the two-pane inbox.

## Culprit

[`AndroidManifest.xml:42-43`](../../../../../AndroidManifest.xml#L42-L43)

```xml
android:screenOrientation=
    "portrait"
```

In this run, with target SDK 36 on the 800 dp tablet, the system ignores the fixed orientation request (the log line above). The sample then draws [`OnePane()`](PortraitLockSample.kt#L53) whatever the width, so the layout assumed a portrait phone, which the tablet did not give it.

## Fix

[`PortraitLockSample.kt:55`](PortraitLockSample.kt#L55) and [`PortraitLockSample.kt:61-64`](PortraitLockSample.kt#L61-L64)

```kotlin
BoxWithConstraints(Modifier.fillMaxSize()) { AdaptiveInbox() }
...
if (maxWidth >= 600.dp) TwoPane() else OnePane()
```

The layout follows the width the window really has, so rotation on a tablet is fine. The lock stays in the manifest in both variants; it does nothing on this tablet.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.portraitlock.PortraitLockActivity --ez adaptive false
adb shell am start -n dev.logcatdaily.samples/.portraitlock.PortraitLockActivity --ez adaptive true
adb logcat -s logcatdaily
```
