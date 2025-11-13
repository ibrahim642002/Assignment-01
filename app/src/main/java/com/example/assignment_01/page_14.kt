package com.example.assignment_01

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class page_14 : AppCompatActivity() {

    private lateinit var storyImage: ImageView
    private lateinit var usernameText: TextView
    private lateinit var timeText: TextView
    private lateinit var database: FirebaseDatabase
    private lateinit var storiesRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page14)

        storyImage = findViewById(R.id.storyContent)
        usernameText = findViewById(R.id.usernameText)
        timeText = findViewById(R.id.timeText)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        storiesRef = database.getReference("stories")

        // Get story ID from intent
        val storyId = intent.getStringExtra("STORY_ID")

        if (storyId != null) {
            loadStoryFromDatabase(storyId)
        } else {
            Toast.makeText(this, "Story not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Close story on tap
        storyImage.setOnClickListener {
            finish()
        }

        // Auto close after 5 seconds (optional)
        storyImage.postDelayed({
            finish()
        }, 5000)
    }

    private fun loadStoryFromDatabase(storyId: String) {
        storiesRef.child(storyId).get()
            .addOnSuccessListener { snapshot ->
                val story = snapshot.getValue(Story::class.java)

                if (story != null) {
                    // Display username
                    usernameText.text = story.username

                    // Display time
                    timeText.text = getTimeAgo(story.timestamp)

                    // Display image from base64
                    try {
                        val imageBytes = Base64.decode(story.imageUrl, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        storyImage.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        Log.e("StoryViewer", "Failed to decode image: ${e.message}")
                        Toast.makeText(this, "Failed to load story image", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Story not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("StoryViewer", "Error loading story: ${exception.message}")
                Toast.makeText(this, "Error loading story", Toast.LENGTH_SHORT).show()
                finish()
            }
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
}