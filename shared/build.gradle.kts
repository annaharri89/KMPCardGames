plugins {
    kotlin("multiplatform") version "2.0.21"
    id("com.android.library") version "8.5.2"
}

kotlin {
    androidTarget()
    jvm("desktop")
    js(IR) {
        browser()
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.example.kmpexample.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
}
