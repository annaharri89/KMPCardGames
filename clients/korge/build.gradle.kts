plugins {
    kotlin("multiplatform") version "2.0.21"
    id("com.android.library") version "8.5.2"
}

kotlin {
    androidTarget()
    jvm("desktop") {
        mainRun {
            mainClass = "MainKt"
        }
    }
    js(IR) {
        browser()
        binaries.executable()
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation("com.soywiz.korge:korge:5.1.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.example.kmpexample.clients.korge"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
}

tasks.configureEach {
    if (name == "desktopRun" && this is JavaExec) {
        jvmArgs(
            "--add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
            "--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED",
            "-Dsun.java2d.opengl=false",
        )
        if (project.findProperty("foxPuppetPreview") == "true") {
            systemProperty("foxPuppetPreview", "true")
        }
    }
}

