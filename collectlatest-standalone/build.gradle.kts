plugins {
    kotlin("jvm") version "2.2.21"
    application
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

kotlin { jvmToolchain(21) }

application {
    mainClass.set("SearchKt")
}

tasks.named<JavaExec>("run") {
    args = (project.findProperty("variant") as String? ?: "collect").split(",")
}
