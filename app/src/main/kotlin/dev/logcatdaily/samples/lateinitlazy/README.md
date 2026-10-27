# lateinit-lazy

A session screen with one button. `lateinit var config` is read before anything set it, and a `by lazy` property is read twice. Variants: `broken`, `lazy`, `fixed`.

## Symptom

Tap Open config on the broken variant and the app dies on the first tap. The lazy variant never crashes: the initializer runs on the first read and the second read reuses the value.

## Trace

From a real run (API 37 emulator), `adb logcat -s logcatdaily`, broken variant, one tap:

```
D logcatdaily: reading config
E AndroidRuntime: FATAL EXCEPTION: main
E AndroidRuntime: kotlin.UninitializedPropertyAccessException: lateinit property config has not been initialized
E AndroidRuntime: 	at dev.logcatdaily.samples.lateinitlazy.Session.getConfig(LateinitLazySample.kt:23)
E AndroidRuntime: 	at dev.logcatdaily.samples.lateinitlazy.Session.readBroken(LateinitLazySample.kt:36)
```

The crash comes 149 ms after the log line. Same tap on the fixed variant:

```
D logcatdaily: isInitialized=false
D logcatdaily: skip, load() not called
D logcatdaily: isInitialized=true
D logcatdaily: user=Nishant
```

Lazy variant, two taps on Read repo:

```
D logcatdaily: first access
D logcatdaily: repo: init
D logcatdaily: got UserRepo(Nishant)
D logcatdaily: second access
D logcatdaily: got UserRepo(Nishant)
```

`repo: init` appears once. The next tap after that prints `first access` and `got ...` with no init line.

## Culprit

[`LateinitLazySample.kt:36`](LateinitLazySample.kt#L36)

```kotlin
Log.d(TAG, "user=${config.user}")
```

`config` is declared `lateinit` at [line 23](LateinitLazySample.kt#L23) and only `load()` sets it. Nothing called `load()` first, so the getter throws. The compiler cannot see the order, which is why this builds fine.

## Fix

[`LateinitLazySample.kt:47`](LateinitLazySample.kt#L47)

```kotlin
val ready = ::config.isInitialized
```

`isInitialized` asks the property instead of reading it. If `load()` can really come late, check first or load first. If a value can be built when it is first needed, `by lazy` ([line 25](LateinitLazySample.kt#L25)) removes the order problem.

## Run it

```
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample lateinit-lazy --es variant broken
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample lateinit-lazy --es variant lazy
adb shell am start -n dev.logcatdaily.samples/.MainActivity --es sample lateinit-lazy --es variant fixed
adb logcat -s logcatdaily
```
