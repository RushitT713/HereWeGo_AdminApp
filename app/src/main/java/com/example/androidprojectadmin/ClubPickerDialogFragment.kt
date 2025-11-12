package com.example.androidprojectadmin

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ClubPickerDialogFragment(
    private val onClubSelected: (Club) -> Unit
) : DialogFragment() {
    private lateinit var clubAdapter: ClubPickerAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText

    private val allClubs: List<Club> by lazy { ClubDataProviderAdmin.clubs }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_club_picker, null)

        recyclerView = view.findViewById(R.id.rvClubPicker)
        searchEditText = view.findViewById(R.id.etClubSearchPicker)

        setupRecyclerView()
        setupSearch()

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Select Club")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
    }

    private fun setupRecyclerView() {
        clubAdapter = ClubPickerAdapter(allClubs) { selectedClub ->
            onClubSelected(selectedClub)
            dismiss()
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = clubAdapter
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clubAdapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    companion object {
        const val TAG = "ClubPickerDialog"
    }
}