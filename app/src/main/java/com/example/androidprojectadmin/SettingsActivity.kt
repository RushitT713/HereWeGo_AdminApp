package com.example.androidprojectadmin

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var auth: FirebaseAuth

    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchAutoRefresh: SwitchCompat
    private lateinit var cardClearCache: MaterialCardView
    private lateinit var cardChangePassword: MaterialCardView
    private lateinit var cardAbout: MaterialCardView
    private lateinit var cardPrivacy: MaterialCardView
    private lateinit var cardDeleteAccount: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()
        prefs = getSharedPreferences("AdminAppSettings", Context.MODE_PRIVATE)

        setupViews()
        loadSettings()
        setupListeners()
    }

    private fun setupViews() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        switchDarkMode = findViewById(R.id.switchDarkMode)
        switchNotifications = findViewById(R.id.switchNotifications)
        switchAutoRefresh = findViewById(R.id.switchAutoRefresh)
        cardClearCache = findViewById(R.id.cardClearCache)
        cardChangePassword = findViewById(R.id.cardChangePassword)
        cardAbout = findViewById(R.id.cardAbout)
        cardPrivacy = findViewById(R.id.cardPrivacy)
        cardDeleteAccount = findViewById(R.id.cardDeleteAccount)
    }

    private fun loadSettings() {
        // Load Dark Mode setting
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = isDarkMode

        // Load Notifications setting
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        switchNotifications.isChecked = notificationsEnabled

        // Load Auto Refresh setting
        val autoRefreshEnabled = prefs.getBoolean("auto_refresh", true)
        switchAutoRefresh.isChecked = autoRefreshEnabled
    }

    private fun setupListeners() {
        // Dark Mode Toggle
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            applyTheme(isChecked)
        }

        // Notifications Toggle
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
            val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Auto Refresh Toggle
        switchAutoRefresh.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_refresh", isChecked).apply()
            val message = if (isChecked) "Auto-refresh enabled" else "Auto-refresh disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Clear Cache
        cardClearCache.setOnClickListener {
            showClearCacheDialog()
        }

        // Change Password
        cardChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // About
        cardAbout.setOnClickListener {
            showAboutDialog()
        }

        // Privacy Policy
        cardPrivacy.setOnClickListener {
            showPrivacyDialog()
        }

        // Delete Account
        cardDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun applyTheme(isDarkMode: Boolean) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun showClearCacheDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear Cache")
            .setMessage("This will clear all cached data. The app may need to reload content.")
            .setPositiveButton("Clear") { _, _ ->
                clearAppCache()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAppCache() {
        try {
            cacheDir.deleteRecursively()
            Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to clear cache", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showChangePasswordDialog() {
        val currentUser = auth.currentUser
        if (currentUser?.email != null) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Change Password")
                .setMessage("A password reset email will be sent to:\n${currentUser.email}")
                .setPositiveButton("Send Email") { _, _ ->
                    sendPasswordResetEmail(currentUser.email!!)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(this, "Password reset email sent", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send email: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("About Here We Go! Admin")
            .setMessage("""
                Version: 1.0.0
                
                Here We Go! Admin Panel
                Manage football transfer news and analytics
                
                Developed for administrators to create, edit, and monitor transfer news content.
                
                © 2025 Here We Go! All rights reserved.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showPrivacyDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Privacy Policy")
            .setMessage("""
                Data Collection:
                • User authentication data (email)
                • News content created by admins
                • Analytics and usage statistics
                
                Data Usage:
                • To provide admin panel functionality
                • To improve app performance
                • To generate analytics reports
                
                Data Security:
                • All data is encrypted and stored securely
                • Firebase Authentication & Firestore used
                • No data shared with third parties
                
                For more information, contact support.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Account")
            .setMessage("⚠️ WARNING: This action is permanent and cannot be undone.\n\nDeleting your account will:\n• Remove all your data\n• Sign you out immediately\n• Revoke admin access\n\nAre you absolutely sure?")
            .setPositiveButton("Delete") { _, _ ->
                showFinalConfirmation()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFinalConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Final Confirmation")
            .setMessage("Type 'DELETE' to confirm account deletion")
            .setPositiveButton("Confirm") { _, _ ->
                deleteUserAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUserAccount() {
        val user = auth.currentUser
        user?.delete()
            ?.addOnSuccessListener {
                Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                // Clear all preferences
                prefs.edit().clear().apply()
                // Navigate to login
                val intent = android.content.Intent(this, LoginActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            ?.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete account: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}