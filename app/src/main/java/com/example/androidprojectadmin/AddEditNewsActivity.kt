package com.example.androidprojectadmin

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.* // Keep general import for Button, ProgressBar etc.
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat // For color resources
import coil.load
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton // Import FAB if used
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import android.text.method.ScrollingMovementMethod

class AddEditNewsActivity : AppCompatActivity() {
    private lateinit var etPlayerName: TextInputEditText
    private lateinit var etSummary: TextInputEditText
    private lateinit var sliderMilestone: Slider
    private lateinit var tvMilestoneLabel: TextView
    private lateinit var cbCanceled: MaterialCheckBox
    private lateinit var btnSave: ExtendedFloatingActionButton
    private lateinit var btnSelectImage: Button
    private lateinit var imgPreview: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var toolbar: MaterialToolbar

    private lateinit var layoutFromClub: LinearLayout
    private lateinit var imgFromClub: ImageView
    private lateinit var tvFromClub: TextView
    private lateinit var layoutToClub: LinearLayout
    private lateinit var imgToClub: ImageView
    private lateinit var tvToClub: TextView

    private lateinit var db: FirebaseFirestore
    private val milestoneLabels = listOf("Rumor", "Talks", "Medical", "Contract", "Official")
    private var currentNewsItemId: String? = null
    private var imageUri: Uri? = null
    private var existingImageUrl: String? = null

    private var selectedFromClub: Club? = null
    private var selectedToClub: Club? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            imgPreview.load(it) {
                crossfade(true)
                placeholder(R.drawable.ic_soccer)
            }
            existingImageUrl = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_news)

        db = FirebaseFirestore.getInstance()
        bindViews()
        setupListeners()

        currentNewsItemId = intent.getStringExtra("news_item_id")
        if (currentNewsItemId != null) {
            toolbar.title = "Edit News Item"
            btnSave.text = "Update News Item"
            loadNewsItemData(currentNewsItemId!!)
        } else {
            toolbar.title = "Add News Item"
            btnSave.text = "Save News Item"
        }
    }

    private fun bindViews() {
        etPlayerName = findViewById(R.id.etPlayerName)
        etSummary = findViewById(R.id.etSummary)
        etSummary.movementMethod = ScrollingMovementMethod()
        sliderMilestone = findViewById(R.id.sliderMilestone)
        tvMilestoneLabel = findViewById(R.id.tvMilestoneLabel)
        cbCanceled = findViewById(R.id.cbCanceled)
        // --- END FIX ---
        btnSave = findViewById(R.id.btnSave)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        imgPreview = findViewById(R.id.imgPreview)
        progressBar = findViewById(R.id.progressBar)
        toolbar = findViewById(R.id.toolbar)

        layoutFromClub = findViewById(R.id.layoutFromClub)
        imgFromClub = findViewById(R.id.imgFromClub)
        tvFromClub = findViewById(R.id.tvFromClub)
        layoutToClub = findViewById(R.id.layoutToClub)
        imgToClub = findViewById(R.id.imgToClub)
        tvToClub = findViewById(R.id.tvToClub)
    }

    private fun setupListeners() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
        sliderMilestone.addOnChangeListener { _, value, _ ->
            val statusIndex = value.toInt() - 1
            if (statusIndex in milestoneLabels.indices) {
                tvMilestoneLabel.text = milestoneLabels[statusIndex]
            }
        }
        imgPreview.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        btnSave.setOnClickListener {
            handleSaveOrUpdate()
        }

        layoutFromClub.setOnClickListener {
            showClubPicker { club ->
                selectedFromClub = club
                updateClubSelectionUI(club, imgFromClub, tvFromClub, "Select From Club")
            }
        }
        layoutToClub.setOnClickListener {
            showClubPicker { club ->
                selectedToClub = club
                updateClubSelectionUI(club, imgToClub, tvToClub, "Select To Club")
            }
        }
    }

    private fun showClubPicker(onSelected: (Club) -> Unit) {
        ClubPickerDialogFragment(onSelected).show(supportFragmentManager, ClubPickerDialogFragment.TAG)
    }

    private fun updateClubSelectionUI(club: Club?, imageView: ImageView, textView: TextView, defaultHint: String) {
        if (club != null) {
            textView.text = club.name
            textView.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            imageView.load(club.logoUrl) {
                placeholder(R.drawable.ic_soccer)
                error(R.drawable.ic_broken_image)
            }
        } else {
            textView.text = defaultHint
            textView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            imageView.setImageResource(R.drawable.ic_soccer)
        }
    }

    private fun loadNewsItemData(id: String) {
        progressBar.visibility = View.VISIBLE
        db.collection("news_items").document(id).get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                if (document != null && document.exists()) {
                    etPlayerName.setText(document.getString("playerName"))
                    etSummary.setText(document.getString("summary"))

                    val fromToString = document.getString("fromTo") ?: ""
                    val clubNames = fromToString.split("→").map { it.trim() }.filter { it.isNotEmpty() }

                    selectedFromClub = null
                    selectedToClub = null

                    if (clubNames.isNotEmpty()) {
                        selectedFromClub = ClubDataProviderAdmin.clubs.find { it.name.equals(clubNames[0], ignoreCase = true) }
                    }
                    if (clubNames.size > 1) {
                        selectedToClub = ClubDataProviderAdmin.clubs.find { it.name.equals(clubNames[1], ignoreCase = true) }
                    }
                    updateClubSelectionUI(selectedFromClub, imgFromClub, tvFromClub, "Select From Club")
                    updateClubSelectionUI(selectedToClub, imgToClub, tvToClub, "Select To Club")

                    val status = document.getLong("milestoneStatus")?.toInt() ?: 1
                    sliderMilestone.value = kotlin.math.abs(status).toFloat()
                    val statusIndex = kotlin.math.abs(status) - 1
                    if (statusIndex in milestoneLabels.indices) {
                        tvMilestoneLabel.text = milestoneLabels[statusIndex]
                    }
                    cbCanceled.isChecked = status < 0
                    existingImageUrl = document.getString("imageUrl")
                    if (!existingImageUrl.isNullOrEmpty()) {
                        imgPreview.load(existingImageUrl) { placeholder(R.drawable.ic_soccer) }
                    } else {
                        imgPreview.setImageResource(R.drawable.ic_soccer)
                    }
                } else {
                    Toast.makeText(this, "News item not found.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to load data: ${it.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun handleSaveOrUpdate() {
        val playerName = etPlayerName.text.toString().trim()
        if (playerName.isEmpty()) {
            etPlayerName.error = "Player name is required"
            return
        }

        if (selectedFromClub == null && selectedToClub == null) {
            Toast.makeText(this, "Please select 'From' and/or 'To' club", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        if (imageUri != null) {
            uploadImageAndSaveData()
        } else {
            saveDataToFirestore(existingImageUrl ?: "")
        }
    }

    private fun uploadImageAndSaveData() {
        MediaManager.get().upload(imageUri).callback(object : UploadCallback {
            override fun onStart(requestId: String) { Log.d("Cloudinary", "Upload started") }
            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                val imageUrl = resultData["secure_url"] as? String ?: ""
                Log.d("Cloudinary", "Upload success: $imageUrl")
                runOnUiThread { saveDataToFirestore(imageUrl) }
            }
            override fun onError(requestId: String, error: ErrorInfo) {
                Log.e("Cloudinary", "Upload error: ${error.description}")
                runOnUiThread {
                    Toast.makeText(baseContext, "Image upload failed: ${error.description}", Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                }
            }
            override fun onReschedule(requestId: String, error: ErrorInfo) {}
        }).dispatch()
    }

    private fun saveDataToFirestore(imageUrl: String) {
        val sliderValue = sliderMilestone.value.toInt()
        val milestoneStatus = if (cbCanceled.isChecked) -sliderValue else sliderValue

        val fromClubName = selectedFromClub?.name ?: ""
        val toClubName = selectedToClub?.name ?: ""
        val fromToString = when {
            fromClubName.isNotEmpty() && toClubName.isNotEmpty() -> "$fromClubName → $toClubName"
            fromClubName.isNotEmpty() -> fromClubName
            toClubName.isNotEmpty() -> "→ $toClubName"
            else -> ""
        }

        val newsData = hashMapOf(
            "playerName" to etPlayerName.text.toString().trim(),
            "fromTo" to fromToString,
            "summary" to etSummary.text.toString().trim(),
            "milestoneStatus" to milestoneStatus,
            "imageUrl" to imageUrl,
            "timestamp" to Timestamp.now(),
        )

        val task = if (currentNewsItemId != null) {
            db.collection("news_items").document(currentNewsItemId!!)
                .update(newsData as Map<String, Any>)
        } else {
            // Add new document with initial followCount
            val initialNewsData = newsData + mapOf("followCount" to 0L)
            db.collection("news_items").add(initialNewsData)
        }

        task.addOnSuccessListener { result ->
            val successMessage = if (currentNewsItemId != null) "News updated successfully!" else "News added successfully!"
            val docId = currentNewsItemId ?: (result as? com.google.firebase.firestore.DocumentReference)?.id
            Log.d("Firestore", "$successMessage: ${docId ?: "N/A"}")

            runOnUiThread {
                progressBar.visibility = View.GONE
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
                finish()
            }
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Error saving data", e)
            runOnUiThread {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                Toast.makeText(this, "Error saving data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

}
    

