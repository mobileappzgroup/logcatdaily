# equalsidentity-standalone

A JVM main() with a Kotlin data class. Two objects with the same values compare equal with `==` and not equal with `===`. No emulator, no Android.

Versions: Gradle 8.14.3, Kotlin 2.4.21, JVM toolchain 17.

## Symptom

Two `User("Nishant", 30)` objects. `a == b` prints true, `a === b` prints false.

## Trace

From a real run, `./gradlew --no-daemon run`:

```
a == b  : true
a === b : false
a === c : true
a.hashCode() == b.hashCode() : true
set size : 1
a : User(name=Nishant, age=30)
after a.lastSeen = today, a == b : true
Plain p == q : false
```

## Culprit

[`Main.kt:11`](src/main/kotlin/Main.kt#L11)

```kotlin
println("a == b  : ${a == b}")
```

Not a bug, a mix-up. `==` calls `equals`, and a data class builds `equals` from the primary constructor properties. `===` asks whether both names point at the same object, and `a` and `b` are two objects (lines 8 and 9).

## Fix

[`Main.kt:12`](src/main/kotlin/Main.kt#L12)

```kotlin
println("a === b : ${a === b}")
```

Use `==` for "same values" and `===` for "same object". Line 10 points `c` at `a`, so `a === c` is true.

Two things the run also shows. `lastSeen` sits in the class body, so it is not part of equals: changing it left `a == b` true (line 19). A plain class has no generated equals, so `p == q` is false (line 23).

## Run it

```
./gradlew --no-daemon run
```

## Video

- Short: https://youtube.com/shorts/YYlLJs37m5Y
- Reel: https://www.instagram.com/reel/Dfcj7w2gNTC/
