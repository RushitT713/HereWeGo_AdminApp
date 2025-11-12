package com.example.androidprojectadmin

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val SPLASH_DURATION: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_AndroidProject_Splash)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val footballImage: ImageView = findViewById(R.id.football_loader)

        val rotate = ObjectAnimator.ofFloat(footballImage, "rotation", 0f, 360f * 2)
        rotate.duration = SPLASH_DURATION
        rotate.repeatCount = ObjectAnimator.INFINITE
        rotate.start()

        Handler(Looper.getMainLooper()).postDelayed({
            val firebaseAuth = FirebaseAuth.getInstance()
            val currentUser = firebaseAuth.currentUser

            if (currentUser != null) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }

            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, SPLASH_DURATION)
    }
}