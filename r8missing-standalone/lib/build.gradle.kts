plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
}

kotlin { jvmToolchain(17) }

dependencies {
    compileOnly("org.slf4j:slf4j-api:2.0.16")
}
