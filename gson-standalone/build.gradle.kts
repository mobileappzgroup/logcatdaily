plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    application
}

kotlin { jvmToolchain(21) }

application { mainClass.set("dev.logcatdaily.samples.gson.MainKt") }

dependencies {
    implementation("com.google.code.gson:gson:2.15.0")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
