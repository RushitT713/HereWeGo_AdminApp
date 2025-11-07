package com.example.androidprojectadmin

import android.app.Application
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
    }
}