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
include(":feature:cart")
include(":feature:checkout")
include(":core:model")
include(":core:ui")
