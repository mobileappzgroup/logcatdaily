# r8missing-standalone

A tiny app with a library module. The library calls slf4j, which it declares `compileOnly`. `assembleDebug` passes. `assembleRelease` with minify on fails in R8 with "Missing class". No emulator needed.

Versions: AGP 9.5.1, Gradle 9.6.0, R8 9.5.29, Kotlin 2.4.21, slf4j-api 2.0.16, compileSdk 36.

## Symptom

`./gradlew --no-daemon assembleDebug` is green. `./gradlew --no-daemon assembleRelease` stops in `:app:minifyReleaseWithR8` after 6 seconds.

## Trace

From a real run, release build, no custom rules:

```
ERROR: Missing classes detected while running R8. Please add the missing classes or apply additional keep rules that are generated in .../app/build/outputs/mapping/release/missing_rules.txt.
ERROR: R8: Missing class org.slf4j.Logger (referenced from: void dev.logcatdaily.samples.r8missing.lib.Report.send())
Missing class org.slf4j.LoggerFactory (referenced from: void dev.logcatdaily.samples.r8missing.lib.Report.send())
BUILD FAILED in 6s
```

Gradle wrote `missing_rules.txt` next to the mapping file:

```
-dontwarn org.slf4j.Logger
-dontwarn org.slf4j.LoggerFactory
```

## Culprit

[`Report.kt:7`](lib/src/main/kotlin/dev/logcatdaily/samples/r8missing/lib/Report.kt#L7)

```kotlin
LoggerFactory.getLogger("report").info("sent for $name")
```

The library compiles against slf4j because of [`lib/build.gradle.kts:9`](lib/build.gradle.kts#L9), `compileOnly`. That keeps slf4j out of the app, so at release time R8 sees a call to a class that is not there. Debug does not run R8, so it never looks.

I also tried the same call straight from the app module. That release build passed, no error. I did not dig into why, so this README only covers the library case.

## Fix

[`app/proguard-rules.pro.fixed-dontwarn`](app/proguard-rules.pro.fixed-dontwarn), copied into `app/proguard-rules.pro`:

```
-dontwarn org.slf4j.Logger
-dontwarn org.slf4j.LoggerFactory
```

Release then builds in 8 seconds. This only silences R8. The class is still not in the app.

The honest fix, when that code can run, is to add the dependency ([`app/build.gradle.kts.fixed-dependency`](app/build.gradle.kts.fixed-dependency)):

```kotlin
implementation("org.slf4j:slf4j-api:2.0.16")
```

Release builds in 13 seconds with no rules. Use `-dontwarn` for an optional class the code never reaches. I only ran the builds, not the app, so I did not watch the crash that a missing class would cause on a device.

## Run it

```
./gradlew --no-daemon assembleDebug
./gradlew --no-daemon assembleRelease
cat app/build/outputs/mapping/release/missing_rules.txt
```

## Video

- Short: 
- Reel: 
