plugins {
    id("com.android.application") version "8.5.2"
    kotlin("android") version "2.0.21"
}

android {
    namespace = "com.example.kmpexample.korge.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.kmpexample.korge"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation(project(":clients:korge"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.9.0-RC.2"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")
}
