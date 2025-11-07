package com.example.androidprojectadmin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import android.text.format.DateUtils
import androidx.core.content.ContextCompat
import com.google.firebase.Timestamp

class NewsAdminAdapter(
    private var newsList: List<NewsItem>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<NewsAdminAdapter.NewsViewHolder>() {

    interface OnItemClickListener {
        fun onEditClick(item: NewsItem)
        fun onDeleteClick(item: NewsItem)
    }

    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playerName: TextView = itemView.findViewById(R.id.tvPlayerNameAdmin)
        val fromTo: TextView = itemView.findViewById(R.id.tvFromToAdmin)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        val summary: TextView = itemView.findViewById(R.id.tvSummaryPreview)
        val timestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val milestoneStatus: TextView = itemView.findViewById(R.id.tvMilestoneStatus)
        val milestoneChip: MaterialCardView = itemView.findViewById(R.id.milestoneChip) // For changing colors

        val followCount: TextView = itemView.findViewById(R.id.tvFollowCount) // ADD THIS

        init {
            btnEdit.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onEditClick(newsList[position])
                }
            }
            btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(newsList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.news_item_admin, parent, false)
        return NewsViewHolder(view)
    }

    // In NewsAdminAdapter.kt

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val item = newsList[position]
        val context = holder.itemView.context

        // Set basic text
        holder.playerName.text = item.playerName
        holder.fromTo.text = item.fromTo

        // Set summary (and hide it if it's empty)
        if (item.summary.isNotBlank()) {
            holder.summary.visibility = View.VISIBLE
            holder.summary.text = item.summary
        } else {
            holder.summary.visibility = View.GONE
        }

        // Set relative timestamp
        holder.timestamp.text = getRelativeTime(item.timestamp)

        // Set Milestone Status text and color
        val status = item.milestoneStatus
        val isCanceled = status < 0
        val milestoneIndex = kotlin.math.abs(status) - 1
        val milestoneLabels = listOf("Rumor", "Talks", "Medical", "Contract", "Official")

        if (milestoneIndex in milestoneLabels.indices) {
            holder.milestoneStatus.text = milestoneLabels[milestoneIndex]
        }

        // Define colors for each status
        val statusColors = mapOf(
            1 to R.color.rumor_color,
            2 to R.color.talks_color,
            3 to R.color.warning_color, // medical
            4 to R.color.contract_color,
            5 to R.color.success_color // official
        )
        val statusLightColors = mapOf(
            1 to R.color.primary_light, // A generic light color for Rumor
            2 to R.color.info_light,
            3 to R.color.warning_light,
            4 to R.color.error_light, // A generic light color for Contract
            5 to R.color.success_light
        )

        val colorRes = if (isCanceled) R.color.error_color else statusColors[kotlin.math.abs(status)] ?: R.color.text_secondary
        val lightColorRes = if (isCanceled) R.color.error_light else statusLightColors[kotlin.math.abs(status)] ?: R.color.background_secondary

        holder.milestoneStatus.setTextColor(ContextCompat.getColor(context, colorRes))
        holder.milestoneChip.setCardBackgroundColor(ContextCompat.getColor(context, lightColorRes))
        holder.milestoneChip.strokeColor = ContextCompat.getColor(context, colorRes)

        holder.followCount.text = item.followCount.toString()
        holder.timestamp.text = getRelativeTime(item.timestamp)
    }

    override fun getItemCount() = newsList.size

    fun updateData(newNewsList: List<NewsItem>) {
        newsList = newNewsList
        notifyDataSetChanged()
    }

    private fun getRelativeTime(timestamp: Timestamp?): String {
        if (timestamp == null) return "No date"
        val now = System.currentTimeMillis()
        // Convert Timestamp to milliseconds before formatting
        return DateUtils.getRelativeTimeSpanString(timestamp.toDate().time, now, DateUtils.MINUTE_IN_MILLIS).toString()
    }
}