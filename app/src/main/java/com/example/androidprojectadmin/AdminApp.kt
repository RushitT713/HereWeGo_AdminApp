package com.example.androidprojectadmin

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.cloudinary.android.MediaManager

class AdminApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = mapOf(
            "cloud_name" to "dygikcty7",
            "api_key" to "998648148777889",
            "api_secret" to "rpZsPaBbnJLTyfqX4SN6Q7-bwt8"
        )

        MediaManager.init(this, config)
        val prefs = getSharedPreferences("AdminAppSettings", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}