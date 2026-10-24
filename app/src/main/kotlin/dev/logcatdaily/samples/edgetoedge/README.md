# edge-to-edge

An Orders screen with a green toolbar. My theme opts out of edge-to-edge. I raised targetSdk from 35 to 36, ran it on an Android 16 emulator, and the toolbar slid under the status bar.

## Symptom

Same code, two builds, one API 36 emulator.
At targetSdk 35 the toolbar sits below the status bar and the title "Orders" clears the clock.
At targetSdk 36 the toolbar draws under the status bar and "Orders" overlaps the clock.

## What you see

There is no log line for this one, so the proof is the build and the screen. The targetSdk of each APK I recorded:

```
t35-broken: build -PsampleTargetSdk=35
    versionCode=1 minSdk=26 targetSdk=35
t36-broken: default build
    versionCode=1 minSdk=26 targetSdk=36
t36-fixed: default build, enableEdgeToEdge + statusBarsPadding
    versionCode=1 minSdk=26 targetSdk=36
```

I ran targetSdk 35 and 36 on one Android 16 emulator. I did not run it on an Android 15 device.

## Culprit

[`values-v35/themes.xml:3`](../../../../../res/values-v35/themes.xml#L3)

```xml
<item name="android:windowOptOutEdgeToEdgeEnforcement">true</item>
```

The theme of this activity opts out of edge-to-edge. That works at targetSdk 35. For an app that targets 36 and runs on an Android 16 device, Google's Android 16 behavior changes page says the attribute is deprecated and disabled. The app is edge-to-edge whether the theme asks for it or not, and a toolbar with no inset handling draws behind the status bar.

[`Toolbars.kt:26`](Toolbars.kt#L26) is the toolbar with nothing handling the inset:

```kotlin
Row(
    bar()
        .height(56.dp),
) { Title() }
```

## Fix

[`Toolbars.kt:34`](Toolbars.kt#L34)

```kotlin
Row(
    bar()
        .statusBarsPadding()
        .height(56.dp),
) { Title() }
```

`Modifier.statusBarsPadding()` applies the status bar inset as padding, so the toolbar colour stays behind the status bar and the title moves below the clock. In the fixed variant I also call `enableEdgeToEdge()` in [`EdgeToEdgeActivity.kt:32`](EdgeToEdgeActivity.kt#L32).

I did not test `statusBarsPadding()` without `enableEdgeToEdge()`. I did not set the status icon colors myself. In the fixed run they stay dark on the green.
Material 3 `TopAppBar` and friends likely handle the inset already, per the docs, so this bites custom toolbars.

## Run it

The default build targets 36. Pass `sampleTargetSdk` to build the targetSdk 35 "before" APK (`app/build.gradle.kts`). Run these from `production/samples`, on an API 36 emulator.

```
./gradlew :app:installDebug -PsampleTargetSdk=35
adb shell am start -n dev.logcatdaily.samples/.edgetoedge.EdgeToEdgeActivity --es variant broken

./gradlew :app:installDebug
adb shell am start -n dev.logcatdaily.samples/.edgetoedge.EdgeToEdgeActivity --es variant broken
adb shell am start -n dev.logcatdaily.samples/.edgetoedge.EdgeToEdgeActivity --es variant fixed
```

Without `--es variant fixed` you get the broken toolbar.

## Play deadline

Rechecked on 2026-10-24 against the Play Console Help page and the Android developers target API page: new apps and app updates must target API 36 since 31 August 2026. Google says you can request an extension to 1 November 2026. Wear OS and Android Automotive OS apps need API 35, Android TV and Android XR apps need API 34. The Help page said the extension forms in Play Console were not available yet.

## Video

- Short: https://youtube.com/shorts/s41cDEDp6X4
- Reel: https://www.instagram.com/reel/De4TFh3CPU7/
