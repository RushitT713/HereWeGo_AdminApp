package com.example.androidprojectadmin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity(), NewsAdminAdapter.OnItemClickListener {

    private lateinit var db: FirebaseFirestore
    private lateinit var newsRecyclerView: RecyclerView
    private lateinit var newsAdminAdapter: NewsAdminAdapter
    private var newsList = mutableListOf<NewsItem>()
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()

        newsRecyclerView = findViewById(R.id.rvNewsItems)
        newsRecyclerView.layoutManager = LinearLayoutManager(this)
        newsAdminAdapter = NewsAdminAdapter(newsList, this)
        newsRecyclerView.adapter = newsAdminAdapter

        val cardAddNews: CardView = findViewById(R.id.cardAddNews)
        cardAddNews.setOnClickListener {
            val intent = Intent(this, AddEditNewsActivity::class.java)
            startActivity(intent)
        }
        val cardStats: CardView = findViewById(R.id.cardStats)
        cardStats.setOnClickListener {
            val intent = Intent(this, AnalyticsActivity::class.java)
            startActivity(intent)
        }
        val btnLogout: ImageButton = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            logoutUser()
        }
        val cardSettings: CardView = findViewById(R.id.cardSettings)
        cardSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        setupFirestoreListener()
    }

    override fun onStop() {
        super.onStop()
        firestoreListener?.remove()
    }

    private fun setupFirestoreListener() {
        val query = db.collection("news_items")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        firestoreListener = query.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("Firestore", "Listen failed.", e)
                Toast.makeText(this, "Failed to fetch news items.", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val fetchedList = mutableListOf<NewsItem>()
                for (document in snapshots.documents) {
                    val item = document.toObject(NewsItem::class.java)
                    if (item != null) {
                        item.id = document.id
                        fetchedList.add(item)
                    }
                }
                newsList = fetchedList

                newsAdminAdapter.updateData(newsList)

                val tvTotalItems: TextView = findViewById(R.id.tvTotalItems)
                tvTotalItems.text = "${newsList.size} Items"

                val emptyState: View = findViewById(R.id.emptyState)
                emptyState.visibility = if (newsList.isEmpty()) View.VISIBLE else View.GONE

                Log.d("Firestore", "Data updated. Total items: ${newsList.size}")
            }
        }
    }

    override fun onEditClick(item: NewsItem) {
        val intent = Intent(this, AddEditNewsActivity::class.java)
        intent.putExtra("news_item_id", item.id)
        startActivity(intent)
    }

    override fun onDeleteClick(item: NewsItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete News Item")
            .setMessage("Are you sure you want to delete the news about ${item.playerName}?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Yes, Delete") { _, _ ->
                deleteItemFromFirestore(item)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteItemFromFirestore(item: NewsItem) {
        if (item.id.isEmpty()) {
            Toast.makeText(this, "Cannot delete item without ID.", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("news_items").document(item.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Item deleted successfully.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting item: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}