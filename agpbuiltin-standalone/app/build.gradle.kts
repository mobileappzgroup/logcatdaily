plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "dev.logcatdaily.samples.agpbuiltin"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.logcatdaily.samples.agpbuiltin"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
