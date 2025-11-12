package com.example.androidprojectadmin

import com.google.firebase.Timestamp // <-- IMPORT THIS
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class NewsItem(
    var id: String = "",
    val playerName: String = "",
    val fromTo: String = "",
    val summary: String = "",
    val imageUrl: String = "",
    val timestamp: Timestamp? = null,
    val milestoneStatus: Int = 1,
    var isFollowed: Boolean = false,
    val followCount: Int = 0
) {
    constructor() : this("", "", "", "", "", null, 1, false, 0) // <-- CHANGE THIS
}