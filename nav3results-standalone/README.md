# nav3results-standalone

Two screens in Navigation 3 1.2.0: Home shows "Picked: <value>", Picker sends a name back and closes. The variant is chosen with an intent extra. The question: what is still on Home after the process is killed?

Versions: Gradle 9.8.1, AGP 9.1.0, Kotlin 2.4.21, navigation3 1.2.0, compileSdk 37. Emulator: API 36.

## Symptom

Pick "Nishant", Home shows it. Press Home, `adb shell am kill`, reopen from recents. The activity comes back (restored=true, new pid) and the back stack is restored. With the recipe pattern (ResultEffect writes into a ViewModel field) Home shows "Picked: nothing".

## Trace

Tag `logcatdaily`, variant `bus`:

```
onCreate variant=bus restored=false pid=4528
Picker set Nishant via bus
Home shows picked=Nishant
onCreate variant=bus restored=true pid=4626
Home shows picked=null
```

Same steps, variant `vm` (SavedStateHandle) and `key` (NavKey):

```
onCreate variant=vm restored=true pid=4882
Home shows picked=Nishant
onCreate variant=key restored=true pid=5000
Home shows picked=Nishant
```

## Culprit

[`MainActivity.kt:55`](app/src/main/java/dev/logcatdaily/samples/nav3results/MainActivity.kt#L55)

```kotlin
ResultEffect<String> { vm.picked = it }
```

ResultEventBus lives in memory only. The result is delivered once and the receiver decides where it lands. Here it lands in a plain ViewModel field, which dies with the process. Note: writing the same result into `rememberSaveable` (variant `bussaved`, line 60) survives, so the bus is not the whole story, the place you store the value is.

## Fix

[`MainActivity.kt:76`](app/src/main/java/dev/logcatdaily/samples/nav3results/MainActivity.kt#L76)

```kotlin
else -> backStack[0] = Home(name)
```

The value travels in the serializable NavKey, which the saved back stack restores. A ViewModel with SavedStateHandle ([line 32](app/src/main/java/dev/logcatdaily/samples/nav3results/MainActivity.kt#L32)) works too.

## Run it

```
adb shell am start -n dev.logcatdaily.samples.nav3results/.MainActivity --es variant bus
adb shell am start -n dev.logcatdaily.samples.nav3results/.MainActivity --es variant key
adb logcat -s logcatdaily
```

Variants: bus, bussaved, vm, key. After picking: Home button, `adb shell am kill dev.logcatdaily.samples.nav3results`, reopen from recents.
