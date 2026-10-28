package com.neon2048.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class NeonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Prefs.nightMode(this))
    }
}
