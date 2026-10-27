# process-death

A basket screen with an Add button. The list lives in a ViewModel. Android kills the app in the background, I come back from Recents, and the basket is empty.

## Symptom

I added Milk, Eggs and Bread, pressed Home, and waited a few seconds. Then I killed the process and reopened the app from Recents.
The broken version showed Basket (0). The fixed version showed all three items.

## Trace

From a real run (API 36 emulator), `adb logcat -s logcatdaily`, broken variant. The pid changes, so this is a new process:

```
D logcatdaily: basket opened, items=0   (pid 12060)
D logcatdaily: added Milk
D logcatdaily: added Eggs
D logcatdaily: added Bread
D logcatdaily: basket opened, items=0   (pid 12328)
```

Same steps on the fixed variant:

```
D logcatdaily: basket opened, items=0   (pid 12575)
D logcatdaily: added Milk
D logcatdaily: added Eggs
D logcatdaily: added Bread
D logcatdaily: basket opened, items=3   (pid 12778)
```

I checked `pidof` before and after: the old pid was gone each time.

### Runs for ViewModel vs SavedStateHandle (2026-10-27)

Same sample, four runs, `adb logcat -v threadtime -s logcatdaily` (columns: time, pid, tid). Three items added each time. No code change was needed.

Rotation (`user_rotation 1`), same pid, the activity is recreated and the ViewModel survives, so both variants keep 3:

```
broken: 15007  basket opened, items=0 -> added Milk, Eggs, Bread -> 15007  basket opened, items=3
fixed:  15147  basket opened, items=0 -> added Milk, Eggs, Bread -> 15147  basket opened, items=3
```

Process death (Home, `am kill`, reopen from Recents), new pid:

```
broken: 15653  basket opened, items=0 -> added Milk, Eggs, Bread -> 15748  basket opened, items=0
fixed:  15810  basket opened, items=0 -> added Milk, Eggs, Bread -> 15902  basket opened, items=3
```

Rotation alone does not separate a plain ViewModel from SavedStateHandle. Only process death does.

## Culprit

[`broken/BasketViewModel.kt:9`](broken/BasketViewModel.kt#L9)

```kotlin
private val _items = MutableStateFlow(listOf<String>())
```

The list only exists in memory. A ViewModel survives rotation, but it dies with the process.

## Fix

[`fixed/BasketViewModel.kt:12`](fixed/BasketViewModel.kt#L12)

```kotlin
val items = state.getStateFlow(KEY, listOf<String>())
```

`SavedStateHandle` is written into the saved instance state, so it comes back with the screen. It goes through a Bundle, so keep it small. List<String> worked in this sample. For anything big I would use Room or DataStore.

## Run it

Kill it with `am kill` while the app is in the background, then return from Recents. `am start` can make a fresh activity with no saved state, and `force-stop` clears the saved state too, so neither is process death.

```
adb shell am start -n dev.logcatdaily.samples/.processdeath.ProcessDeathActivity --es variant broken
adb shell input keyevent KEYCODE_HOME
adb shell am kill dev.logcatdaily.samples
adb logcat -s logcatdaily
```

`am kill` only took the process after about 4 seconds in the background on my emulator.

## Video

- Short: https://youtube.com/shorts/6niw64qmDjU
- Reel: https://www.instagram.com/reel/DezJeSeFPUn/
