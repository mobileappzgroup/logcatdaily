# module-structure

One tiny shop app (catalog, cart, checkout) in three layouts. Each folder is its own Gradle build.

| folder | what it is |
|---|---|
| v0-single | everything in `:app` |
| v1-cycle | split into feature modules, cart and checkout depend on each other, the build fails |
| v2-split | cart and checkout split into `api` and `impl`, `:app` wires them, the build works |

Run one of them:

    cd v2-split
    ./gradlew :app:assembleDebug

In v1-cycle you get `Circular dependency between the following tasks:`. `v1-cycle-add-dependency.patch` is the change (one import, one dependency line in `feature/checkout`) that turns the working version of that layout into the failing one.

Gradle 8.14.3, AGP 8.13.2, Kotlin 2.2.20, JDK 21.
