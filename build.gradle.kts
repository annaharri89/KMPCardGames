plugins {
    base
    kotlin("multiplatform") version "2.0.21" apply false
    kotlin("android") version "2.0.21" apply false
    id("com.android.library") version "8.6.1" apply false
    id("com.android.application") version "8.6.1" apply false
    id("com.soywiz.korge") version "6.0.0" apply false
    id("com.soywiz.korge.library") version "6.0.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven")
    }
}
