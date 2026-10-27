plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
}
