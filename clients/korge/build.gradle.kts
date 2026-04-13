import korlibs.korge.gradle.*
import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("com.soywiz.korge.library")
}

korge {
    id = "dev.harrisonsoftware.KMPCardGames.clients.korge"
    name = "KorgeClient"
    title = "KMP Playable V1 - Solitaire"
    icon = file("src/commonMain/resources/app-icon.png")
    preferredIphoneSimulatorVersion = 16
    jvmTarget = "17"
    androidSdk(compileSdk = 35, minSdk = 24, targetSdk = 35)
    targetJvm()
    targetJs()
    targetIos()
    targetAndroid()
    dependencyProject(":shared")
}

dependencies {
    add("commonTestImplementation", kotlin("test"))
}

extensions.configure<KotlinMultiplatformExtension>("kotlin") {
    sourceSets.named("jvmMain") {
        kotlin.srcDir("src/desktopMain/kotlin")
    }
}

gradle.taskGraph.whenReady {
    val jvmRun = tasks.findByName("jvmRun") as? JavaExec ?: return@whenReady
    if (jvmRun !in gradle.taskGraph.allTasks) return@whenReady
    jvmRun.mainClass.set("MainKt")
    jvmRun.jvmArgs(
        "--add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
        "--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED",
        "-Dsun.java2d.opengl=false",
    )
    when (project.findProperty("foxPuppetPreview")?.toString()) {
        "true", "1" -> jvmRun.systemProperty("foxPuppetPreview", "true")
        "heart" -> jvmRun.systemProperty("foxPuppetPreview", "heart")
        "queen" -> jvmRun.systemProperty("foxPuppetPreview", "queen")
        else -> Unit
    }
}

tasks.register("runJvm") { dependsOn("jvmRun") }
tasks.register("desktopRun") { dependsOn("jvmRun") }
