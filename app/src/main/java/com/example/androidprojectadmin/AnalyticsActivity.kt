package com.example.androidprojectadmin

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration // <-- ADD THIS IMPORT
import com.google.firebase.firestore.Query

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var firestoreListener: ListenerRegistration? = null // <-- ADD THIS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        db = FirebaseFirestore.getInstance()

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    // --- MANAGE THE LISTENER WITH THE ACTIVITY LIFECYCLE ---
    override fun onStart() {
        super.onStart()
        setupAnalyticsListener()
    }

    override fun onStop() {
        super.onStop()
        firestoreListener?.remove() // Detach the listener
    }

    private fun setupAnalyticsListener() {
        val tvTotalItemsCount: TextView = findViewById(R.id.tvTotalItemsCount)
        val tvMostFollowedPlayer: TextView = findViewById(R.id.tvMostFollowedPlayer)
        val tvMostFollowedTransfer: TextView = findViewById(R.id.tvMostFollowedTransfer)
        val tvMostFollowedCount: TextView = findViewById(R.id.tvMostFollowedCount)

        // --- THIS FUNCTION NOW LISTENS FOR REAL-TIME CHANGES ---
        val query = db.collection("news_items")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        firestoreListener = query.addSnapshotListener { snapshots, e ->
            if (e != null) {
                // Handle failure
                tvTotalItemsCount.text = "-"
                tvMostFollowedPlayer.text = "Error loading data"
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                // 1. Calculate Total News Items
                tvTotalItemsCount.text = snapshots.size().toString()

                // 2. Find the Most Followed Transfer
                val newsList = snapshots.toObjects(NewsItem::class.java)
                val mostFollowedItem = newsList.maxByOrNull { it.followCount }

                if (mostFollowedItem != null && mostFollowedItem.followCount > 0) {
                    tvMostFollowedPlayer.text = mostFollowedItem.playerName
                    tvMostFollowedTransfer.text = mostFollowedItem.fromTo
                    tvMostFollowedCount.text = "${mostFollowedItem.followCount} Follows"
                } else {
                    tvMostFollowedPlayer.text = "No followed items yet"
                    tvMostFollowedTransfer.text = ""
                    tvMostFollowedCount.text = ""
                }
            } else {
                // Handle empty state
                tvTotalItemsCount.text = "0"
                tvMostFollowedPlayer.text = "No data available"
                tvMostFollowedTransfer.text = ""
                tvMostFollowedCount.text = ""
            }
        }
    }
}