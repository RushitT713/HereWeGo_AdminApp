package com.example.androidprojectadmin

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.concurrent.TimeUnit

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var firestoreListener: ListenerRegistration? = null

    // Total Stats
    private lateinit var tvTotalItemsCount: TextView
    private lateinit var tvTotalFollows: TextView
    private lateinit var tvAvgFollows: TextView

    // Most Followed
    private lateinit var tvMostFollowedPlayer: TextView
    private lateinit var tvMostFollowedTransfer: TextView
    private lateinit var tvMostFollowedCount: TextView

    // Status Distribution
    private lateinit var tvRumorCount: TextView
    private lateinit var tvTalksCount: TextView
    private lateinit var tvMedicalCount: TextView
    private lateinit var tvContractCount: TextView
    private lateinit var tvOfficialCount: TextView
    private lateinit var tvCanceledCount: TextView

    // Recent Activity
    private lateinit var tvItemsToday: TextView
    private lateinit var tvItemsThisWeek: TextView
    private lateinit var tvItemsThisMonth: TextView

    // Popular Clubs
    private lateinit var tvTopDepartingClub: TextView
    private lateinit var tvTopDepartingCount: TextView
    private lateinit var tvTopArrivingClub: TextView
    private lateinit var tvTopArrivingCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        db = FirebaseFirestore.getInstance()
        bindViews()

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun bindViews() {
        // Total Stats
        tvTotalItemsCount = findViewById(R.id.tvTotalItemsCount)
        tvTotalFollows = findViewById(R.id.tvTotalFollows)
        tvAvgFollows = findViewById(R.id.tvAvgFollows)

        // Most Followed
        tvMostFollowedPlayer = findViewById(R.id.tvMostFollowedPlayer)
        tvMostFollowedTransfer = findViewById(R.id.tvMostFollowedTransfer)
        tvMostFollowedCount = findViewById(R.id.tvMostFollowedCount)

        // Status Distribution
        tvRumorCount = findViewById(R.id.tvRumorCount)
        tvTalksCount = findViewById(R.id.tvTalksCount)
        tvMedicalCount = findViewById(R.id.tvMedicalCount)
        tvContractCount = findViewById(R.id.tvContractCount)
        tvOfficialCount = findViewById(R.id.tvOfficialCount)
        tvCanceledCount = findViewById(R.id.tvCanceledCount)

        // Recent Activity
        tvItemsToday = findViewById(R.id.tvItemsToday)
        tvItemsThisWeek = findViewById(R.id.tvItemsThisWeek)
        tvItemsThisMonth = findViewById(R.id.tvItemsThisMonth)

        // Popular Clubs
        tvTopDepartingClub = findViewById(R.id.tvTopDepartingClub)
        tvTopDepartingCount = findViewById(R.id.tvTopDepartingCount)
        tvTopArrivingClub = findViewById(R.id.tvTopArrivingClub)
        tvTopArrivingCount = findViewById(R.id.tvTopArrivingCount)
    }

    override fun onStart() {
        super.onStart()
        setupAnalyticsListener()
    }

    override fun onStop() {
        super.onStop()
        firestoreListener?.remove()
    }

    private fun setupAnalyticsListener() {
        val query = db.collection("news_items")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        firestoreListener = query.addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null || snapshots.isEmpty) {
                setEmptyState()
                return@addSnapshotListener
            }

            val newsList = snapshots.toObjects(NewsItem::class.java)
            calculateAnalytics(newsList)
        }
    }

    private fun calculateAnalytics(newsList: List<NewsItem>) {
        // Total Stats
        val totalItems = newsList.size
        val totalFollows = newsList.sumOf { it.followCount }
        val avgFollows = if (totalItems > 0) totalFollows / totalItems else 0

        tvTotalItemsCount.text = totalItems.toString()
        tvTotalFollows.text = formatNumber(totalFollows)
        tvAvgFollows.text = formatNumber(avgFollows)

        // Most Followed
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

        // Status Distribution
        calculateStatusDistribution(newsList)

        // Recent Activity
        calculateRecentActivity(newsList)

        // Popular Clubs
        calculatePopularClubs(newsList)
    }

    private fun calculateStatusDistribution(newsList: List<NewsItem>) {
        var rumorCount = 0
        var talksCount = 0
        var medicalCount = 0
        var contractCount = 0
        var officialCount = 0
        var canceledCount = 0

        newsList.forEach { item ->
            val absStatus = kotlin.math.abs(item.milestoneStatus)
            if (item.milestoneStatus < 0) {
                canceledCount++
            } else {
                when (absStatus) {
                    1 -> rumorCount++
                    2 -> talksCount++
                    3 -> medicalCount++
                    4 -> contractCount++
                    5 -> officialCount++
                }
            }
        }

        tvRumorCount.text = rumorCount.toString()
        tvTalksCount.text = talksCount.toString()
        tvMedicalCount.text = medicalCount.toString()
        tvContractCount.text = contractCount.toString()
        tvOfficialCount.text = officialCount.toString()
        tvCanceledCount.text = canceledCount.toString()
    }

    private fun calculateRecentActivity(newsList: List<NewsItem>) {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - TimeUnit.DAYS.toMillis(1)
        val oneWeekAgo = now - TimeUnit.DAYS.toMillis(7)
        val oneMonthAgo = now - TimeUnit.DAYS.toMillis(30)

        var itemsToday = 0
        var itemsThisWeek = 0
        var itemsThisMonth = 0

        newsList.forEach { item ->
            val itemTime = item.timestamp?.toDate()?.time ?: 0

            if (itemTime >= oneDayAgo) {
                itemsToday++
            }
            if (itemTime >= oneWeekAgo) {
                itemsThisWeek++
            }
            if (itemTime >= oneMonthAgo) {
                itemsThisMonth++
            }
        }

        tvItemsToday.text = itemsToday.toString()
        tvItemsThisWeek.text = itemsThisWeek.toString()
        tvItemsThisMonth.text = itemsThisMonth.toString()
    }

    private fun calculatePopularClubs(newsList: List<NewsItem>) {
        val departingClubs = mutableMapOf<String, Int>()
        val arrivingClubs = mutableMapOf<String, Int>()

        newsList.forEach { item ->
            val clubs = item.fromTo.split("→").map { it.trim() }

            if (clubs.isNotEmpty() && clubs[0].isNotEmpty()) {
                val fromClub = clubs[0]
                departingClubs[fromClub] = (departingClubs[fromClub] ?: 0) + 1
            }

            if (clubs.size > 1 && clubs[1].isNotEmpty()) {
                val toClub = clubs[1]
                arrivingClubs[toClub] = (arrivingClubs[toClub] ?: 0) + 1
            }
        }

        // Top Departing Club
        val topDeparting = departingClubs.maxByOrNull { it.value }
        if (topDeparting != null) {
            tvTopDepartingClub.text = topDeparting.key
            tvTopDepartingCount.text = "${topDeparting.value} transfers"
        } else {
            tvTopDepartingClub.text = "N/A"
            tvTopDepartingCount.text = ""
        }

        // Top Arriving Club
        val topArriving = arrivingClubs.maxByOrNull { it.value }
        if (topArriving != null) {
            tvTopArrivingClub.text = topArriving.key
            tvTopArrivingCount.text = "${topArriving.value} transfers"
        } else {
            tvTopArrivingClub.text = "N/A"
            tvTopArrivingCount.text = ""
        }
    }

    private fun setEmptyState() {
        tvTotalItemsCount.text = "0"
        tvTotalFollows.text = "0"
        tvAvgFollows.text = "0"
        tvMostFollowedPlayer.text = "No data available"
        tvMostFollowedTransfer.text = ""
        tvMostFollowedCount.text = ""

        tvRumorCount.text = "0"
        tvTalksCount.text = "0"
        tvMedicalCount.text = "0"
        tvContractCount.text = "0"
        tvOfficialCount.text = "0"
        tvCanceledCount.text = "0"

        tvItemsToday.text = "0"
        tvItemsThisWeek.text = "0"
        tvItemsThisMonth.text = "0"

        tvTopDepartingClub.text = "N/A"
        tvTopDepartingCount.text = ""
        tvTopArrivingClub.text = "N/A"
        tvTopArrivingCount.text = ""
    }

    private fun formatNumber(number: Int): String {
        return when {
            number >= 1000000 -> String.format("%.1fM", number / 1000000.0)
            number >= 1000 -> String.format("%.1fK", number / 1000.0)
            else -> number.toString()
        }
    }
}