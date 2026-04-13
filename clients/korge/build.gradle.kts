import korlibs.korge.gradle.*
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("com.soywiz.korge")
}

korge {
    id = "dev.harrisonsoftware.KMPCardGames.clients.korge"
    name = "KMP Solitaire"
    title = "KMP Playable V1 - Solitaire"
    icon = file("src/commonMain/resources/app-icon.png")
    preferredIphoneSimulatorVersion = 16
    jvmTarget = "17"
    androidSdk(compileSdk = 35, minSdk = 24, targetSdk = 35)
    orientation = Orientation.LANDSCAPE
    fullscreen = false
    androidLibrary = true
    androidCustomApplicationAttributes["android:label"] = "KMP Solitaire"
    androidCustomApplicationAttributes["android:icon"] = "@mipmap/ic_launcher"
    androidCustomApplicationAttributes["android:roundIcon"] = "@mipmap/ic_launcher_round"
    androidCustomApplicationAttributes["android:theme"] = "@style/Theme.KmpSolitaire"
    androidCustomApplicationAttributes["android:appCategory"] = "game"
    androidManifestApplicationChunk(
        """
        <activity
            android:name="dev.harrisonsoftware.KMPCardGames.clients.korge.SolitaireActivity"
            android:exported="true"
            android:screenOrientation="sensorLandscape"
            android:configChanges="orientation|screenSize|keyboardHidden|screenLayout|smallestScreenSize|uiMode">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        """.trimIndent(),
    )
    targetJvm()
    targetJs()
    targetIos()
    targetAndroid()
    dependencyProject(":shared")
}

dependencies {
    add("commonTestImplementation", kotlin("test"))
    add("androidMainImplementation", "androidx.core:core-ktx:1.15.0")
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

tasks.register("runIosSimulatorDebugDetached") {
    group = "run"
    description = "Install and launch iOS simulator app without console attachment."
    dependsOn("installIosSimulatorDebug")
    doLast {
        val iosSimulatorBundleId = "dev.harrisonsoftware.KMPCardGames.clients.korge.app-SimulatorArm64-Debug"
        logger.lifecycle("[IOS_SIM_RUN] Launching bundleId=$iosSimulatorBundleId on booted simulator")
        exec {
            commandLine("xcrun", "simctl", "launch", "booted", iosSimulatorBundleId)
        }
        logger.lifecycle("[IOS_SIM_RUN] Launched successfully in detached mode")
    }
}

tasks.named<Exec>("runIosSimulatorDebug") {
    dependsOn("runIosSimulatorDebugDetached")
    onlyIf { false }
}

fun Project.applyIosHostLandscapeOrientationLocks() {
    val plistFile = layout.buildDirectory.file("platforms/ios/app/Info.plist").get().asFile
    if (plistFile.exists()) {
        val originalText = plistFile.readText()
        val i = "\t\t"
        val portraitPhoneBlock =
            "${i}<string>UIInterfaceOrientationPortrait</string>\n" +
                "${i}<string>UIInterfaceOrientationLandscapeLeft</string>\n" +
                "${i}<string>UIInterfaceOrientationLandscapeRight</string>\n"
        val landscapePhoneOnlyBlock =
            "${i}<string>UIInterfaceOrientationLandscapeLeft</string>\n" +
                "${i}<string>UIInterfaceOrientationLandscapeRight</string>\n"
        val portraitIpadBlock =
            "${i}<string>UIInterfaceOrientationPortrait</string>\n" +
                "${i}<string>UIInterfaceOrientationPortraitUpsideDown</string>\n" +
                "${i}<string>UIInterfaceOrientationLandscapeLeft</string>\n" +
                "${i}<string>UIInterfaceOrientationLandscapeRight</string>\n"
        val landscapeIpadOnlyBlock =
            "${i}<string>UIInterfaceOrientationLandscapeLeft</string>\n" +
                "${i}<string>UIInterfaceOrientationLandscapeRight</string>\n"
        val stillAllowsPortrait =
            originalText.contains("UIInterfaceOrientationPortrait") ||
                originalText.contains("UIInterfaceOrientationPortraitUpsideDown")
        val patchedPlist = originalText
            .replace(portraitPhoneBlock, landscapePhoneOnlyBlock)
            .replace(portraitIpadBlock, landscapeIpadOnlyBlock)
        when {
            patchedPlist != originalText -> plistFile.writeText(patchedPlist)
            stillAllowsPortrait ->
                logger.lifecycle(
                    "[ios_orientation] KorGE Info.plist still allows portrait; template changed — update applyIosHostLandscapeOrientationLocks() (${plistFile.absolutePath})",
                )
            else -> Unit
        }
    }

    val mainM = layout.buildDirectory.file("platforms/ios/app/main.m").get().asFile
    if (mainM.exists()) {
        val originalMain = mainM.readText()
        if (!originalMain.contains("supportedInterfaceOrientationsForWindow")) {
            val terminateAndEnd = """
- (void)applicationWillTerminate:(UIApplication *)application {
    [[GameMainNewAppDelegate getNewAppDelegate] applicationWillTerminateApp: application];
}
@end
""".trimIndent()
            val withOrientation = """
- (void)applicationWillTerminate:(UIApplication *)application {
    [[GameMainNewAppDelegate getNewAppDelegate] applicationWillTerminateApp: application];
}
- (UIInterfaceOrientationMask)application:(UIApplication *)application supportedInterfaceOrientationsForWindow:(UIWindow *)window {
    return (UIInterfaceOrientationMaskLandscapeLeft | UIInterfaceOrientationMaskLandscapeRight);
}
@end
""".trimIndent()
            val patchedMain = originalMain.replace(terminateAndEnd, withOrientation)
            if (patchedMain != originalMain) {
                mainM.writeText(patchedMain)
            } else {
                logger.lifecycle(
                    "[ios_orientation] Could not insert supportedInterfaceOrientationsForWindow into main.m (${mainM.absolutePath})",
                )
            }
        }
    }
}

tasks.named("prepareKotlinNativeIosProject").configure {
    doLast { applyIosHostLandscapeOrientationLocks() }
}

tasks.withType<Exec>().configureEach {
    if (!name.startsWith("iosBuild")) return@configureEach
    if (!name.endsWith("Debug") && !name.endsWith("Release")) return@configureEach
    doFirst { applyIosHostLandscapeOrientationLocks() }
}

