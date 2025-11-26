package com.example.assignment_01

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
import java.io.InputStream

class page_14 : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 201

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storiesRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference

    private lateinit var storyImageView: ImageView
    private lateinit var storyProfileImage: ImageView
    private lateinit var usernameText: TextView
    private lateinit var timeText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var closeButton: ImageView
    private lateinit var addStoryButton: ImageView

    private var storyIds: Array<String> = arrayOf()
    private var currentStoryIndex = 0
    private var currentStoryUserId: String = ""
    private val storyDuration = 5000L
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page14)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        storiesRef = database.getReference("stories")
        usersRef = database.getReference("Users")

        // Initialize views
        storyImageView = findViewById(R.id.storyImageView)
        storyProfileImage = findViewById(R.id.storyProfileImage)
        usernameText = findViewById(R.id.storyUsername)
        timeText = findViewById(R.id.storyTimeText)
        progressBar = findViewById(R.id.storyProgressBar)
        closeButton = findViewById(R.id.closeStoryButton)
        addStoryButton = findViewById(R.id.addStoryButton)

        // Get story IDs from intent
        storyIds = intent.getStringArrayExtra("STORY_IDS") ?: arrayOf()
        currentStoryIndex = intent.getIntExtra("CURRENT_INDEX", 0)

        if (storyIds.isEmpty()) {
            Toast.makeText(this, "No stories to show", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Close button
        closeButton.setOnClickListener {
            finish()
        }

        // Add story button (img_87) - Opens gallery
        addStoryButton.setOnClickListener {
            // Only allow if viewing own story
            if (currentStoryUserId == mAuth.currentUser?.uid) {
                openGalleryToAddStory()
            } else {
                Toast.makeText(this, "You can only add to your own stories", Toast.LENGTH_SHORT).show()
            }
        }

        // Tap left side = previous story
        findViewById<View>(R.id.leftTapArea).setOnClickListener {
            previousStory()
        }

        // Tap right side = next story
        findViewById<View>(R.id.rightTapArea).setOnClickListener {
            nextStory()
        }

        // Load first story
        loadStory(currentStoryIndex)
    }

    private fun loadStory(index: Int) {
        if (index < 0 || index >= storyIds.size) {
            finish()
            return
        }

        val storyId = storyIds[index]

        storiesRef.child(storyId).get()
            .addOnSuccessListener { snapshot ->
                val story = snapshot.getValue(Story::class.java)
                if (story != null) {
                    currentStoryUserId = story.userId
                    displayStory(story)
                    updateAddStoryButtonVisibility()
                    startStoryTimer()
                } else {
                    nextStory()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load story", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayStory(story: Story) {
        // Set username
        usernameText.text = story.username

        // Set time ago
        timeText.text = getTimeAgo(story.timestamp)

        // Load story image
        try {
            val imageBytes = Base64.decode(story.imageUrl, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            storyImageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("StoryViewer", "Error loading story image: ${e.message}")
            nextStory()
            return
        }

        // Load profile image if available
        if (story.userProfileImage.isNotEmpty()) {
            try {
                val profileImageBytes = Base64.decode(story.userProfileImage, Base64.DEFAULT)
                val profileBitmap = BitmapFactory.decodeByteArray(profileImageBytes, 0, profileImageBytes.size)
                storyProfileImage.setImageBitmap(profileBitmap)
            } catch (e: Exception) {
                Log.e("StoryViewer", "Error loading profile image: ${e.message}")
            }
        }

        // Update progress bar
        progressBar.progress = ((currentStoryIndex + 1) * 100) / storyIds.size
    }

    private fun updateAddStoryButtonVisibility() {
        // Show add story button only if viewing own story
        if (currentStoryUserId == mAuth.currentUser?.uid) {
            addStoryButton.visibility = View.VISIBLE
        } else {
            addStoryButton.visibility = View.GONE
        }
    }

    private fun openGalleryToAddStory() {
        // Pause story timer while selecting image
        handler.removeCallbacksAndMessages(null)

        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            val inputStream: InputStream? = imageUri?.let { contentResolver.openInputStream(it) }
            val bitmap = BitmapFactory.decodeStream(inputStream)
            uploadNewStory(bitmap)
        } else {
            // Resume story timer if user cancelled
            startStoryTimer()
        }
    }

    private fun uploadNewStory(bitmap: Bitmap) {
        val base64Image = bitmapToBase64(bitmap)
        val user = mAuth.currentUser
        val storyId = storiesRef.push().key ?: return

        // Show loading
        Toast.makeText(this, "Uploading story...", Toast.LENGTH_SHORT).show()

        usersRef.child(user?.uid ?: "").get()
            .addOnSuccessListener { snapshot ->
                val username = snapshot.child("username").value?.toString() ?: "User"
                val profileImage = snapshot.child("profileImage").value?.toString() ?: ""

                val story = Story(
                    storyId = storyId,
                    userId = user?.uid ?: "",
                    username = username,
                    userProfileImage = profileImage,
                    imageUrl = base64Image,
                    timestamp = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
                )

                storiesRef.child(storyId).setValue(story)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Story added successfully!", Toast.LENGTH_SHORT).show()

                        // Close viewer to refresh stories
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                        // Resume story timer
                        startStoryTimer()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to get user info", Toast.LENGTH_SHORT).show()
                startStoryTimer()
            }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> "${diff / 86400000}d ago"
        }
    }

    private fun startStoryTimer() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            nextStory()
        }, storyDuration)
    }

    private fun nextStory() {
        handler.removeCallbacksAndMessages(null)
        currentStoryIndex++
        if (currentStoryIndex < storyIds.size) {
            loadStory(currentStoryIndex)
        } else {
            finish()
        }
    }

    private fun previousStory() {
        handler.removeCallbacksAndMessages(null)
        currentStoryIndex--
        if (currentStoryIndex >= 0) {
            loadStory(currentStoryIndex)
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}