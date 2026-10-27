pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "module-structure"
include(":app")
include(":feature:catalog")
include(":feature:cart:api")
include(":feature:cart:impl")
include(":feature:checkout:api")
include(":feature:checkout:impl")
include(":core:model")
include(":core:ui")
