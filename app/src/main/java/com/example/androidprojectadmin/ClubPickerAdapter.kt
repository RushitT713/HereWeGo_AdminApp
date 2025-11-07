package com.example.androidprojectadmin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

// Define a typealias for the Club data class from ClubDataProviderAdmin
// This assumes the structure is identical (name: String, logoUrl: String)
// Adjust if your Club definition needs to be different in Admin vs User app

class ClubPickerAdapter(
    private var clubs: List<Club>, // Use Club directly
    private val onClubSelected: (Club) -> Unit // Use Club directly
) : RecyclerView.Adapter<ClubPickerAdapter.ClubViewHolder>() {

    private var filteredClubs: List<Club> = clubs // Use Club directly

    inner class ClubViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val badge: ImageView = itemView.findViewById(R.id.imgClubBadgePicker)
        val name: TextView = itemView.findViewById(R.id.tvClubNamePicker)

        fun bind(club: Club) { // Use Club directly
            name.text = club.name
            badge.load(club.logoUrl) {
                placeholder(R.drawable.ic_soccer)
                error(R.drawable.ic_broken_image)
            }
            itemView.setOnClickListener {
                onClubSelected(club)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClubViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.dialog_list_item_club_picker, parent, false)
        return ClubViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClubViewHolder, position: Int) {
        holder.bind(filteredClubs[position])
    }

    override fun getItemCount(): Int = filteredClubs.size

    fun filter(query: String) {
        filteredClubs = if (query.isEmpty()) {
            clubs
        } else {
            clubs.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }
}