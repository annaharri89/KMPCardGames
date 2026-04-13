package com.example.kmpexample.korge.android

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import app.playableClientKorgeModule
import korlibs.korge.android.KorgeAndroidView
import korlibs.math.geom.Size

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        @Suppress("DEPRECATION")
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val foxPuppetPreviewFromIntent =
            intent.getStringExtra(EXTRA_FOX_PUPPET_PREVIEW)?.lowercase()?.trim()
        val korgeAndroidView = KorgeAndroidView(this)
        setContentView(korgeAndroidView)
        korgeAndroidView.loadModule(
            playableClientKorgeModule(
                windowSize = Size(1280, 720),
                title = "KMP Playable V1 - Solitaire",
                foxPuppetPreviewFromEnvironment = null,
                foxPuppetPreviewFromJvmOrBridge = foxPuppetPreviewFromIntent,
            ),
        )
    }

    companion object {
        const val EXTRA_FOX_PUPPET_PREVIEW = "foxPuppetPreview"
    }
}
