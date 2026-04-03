plugins {
    base
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven")
    }
}
