package com.example.assignment_01

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.FirebaseDatabase.*
import java.io.ByteArrayOutputStream
import java.io.InputStream



fun bitmapToBase64(bitmap: Bitmap): String {
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
    return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
}

class page_5 : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 101
    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storiesRef: DatabaseReference

    // Story circle ImageViews
    private lateinit var yourStory: ImageView
    private lateinit var story1: ImageView
    private lateinit var story2: ImageView
    private lateinit var story3: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page5)

        mAuth = FirebaseAuth.getInstance()
        database = getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        storiesRef = database.getReference("stories")

        // Initialize story circles
        yourStory = findViewById(R.id.you)
        story1 = findViewById(R.id.you1)
        story2 = findViewById(R.id.you2)
        story3 = findViewById(R.id.you3)

        val searchbtn = findViewById<ImageView>(R.id.search)
        val msgbtn = findViewById<ImageView>(R.id.msg)
        val profilebtn=findViewById<ImageView>(R.id.profile)
        val heartbtn=findViewById<ImageView>(R.id.heart)


        // Your story circle click
        yourStory.setOnClickListener {
            handleYourStoryClick()
        }

        // Load and display stories
        loadStories()

        searchbtn.setOnClickListener {
            val intent = Intent(this, page_6::class.java)
            startActivity(intent)
            finish()
        }

        msgbtn.setOnClickListener {
            val intent = Intent(this, page_8::class.java)
            startActivity(intent)
        }

        profilebtn.setOnClickListener {
            val intent = Intent(this, page_13::class.java)
            startActivity(intent)
        }

        heartbtn.setOnClickListener {
            val intent = Intent(this, page_11::class.java)
            startActivity(intent)
        }

        deleteExpiredStories()

        PresenceManager.initialize("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        PresenceManager.setUserOnline()
    }

    override fun onDestroy() {
        super.onDestroy()
        PresenceManager.setUserOffline()
    }

    private fun loadPosts() {
        val postsRef = database.getReference("posts")

        postsRef.orderByChild("timestamp").limitToLast(10)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val posts = mutableListOf<Post>()

                    for (postSnapshot in snapshot.children) {
                        val post = postSnapshot.getValue(Post::class.java)
                        if (post != null) {
                            posts.add(post)
                        }
                    }

                    // Reverse to show newest first
                    posts.reverse()

                    // Display posts in your feed
                    displayPosts(posts)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Posts", "Error loading posts: ${error.message}")
                }
            })
    }

    private fun displayPosts(posts: List<Post>) {
        // You can use RecyclerView or display in existing ImageViews
        // For now, let's update the main post image if posts exist
        if (posts.isNotEmpty()) {
            val latestPost = posts[0]
            val postImage = findViewById<ImageView>(R.id.pic) // Your main post image

            try {
                val imageBytes = Base64.decode(latestPost.imageUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                postImage?.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e("Posts", "Error loading post image: ${e.message}")
            }
        }
    }

    private fun handleYourStoryClick() {
        val currentUser = mAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if user already has an active story
        storiesRef.orderByChild("userId").equalTo(currentUser.uid).get()
            .addOnSuccessListener { snapshot ->
                var hasActiveStory = false
                var userStory: Story? = null

                for (storySnap in snapshot.children) {
                    val story = storySnap.getValue(Story::class.java)
                    if (story != null && !isExpired(story.timestamp)) {
                        hasActiveStory = true
                        userStory = story
                        break
                    }
                }

                if (hasActiveStory && userStory != null) {
                    // View your own story
                    viewStory(userStory)
                } else {
                    // No active story, open gallery
                    openGallery()
                }
            }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            val inputStream: InputStream? = imageUri?.let { contentResolver.openInputStream(it) }
            val bitmap = BitmapFactory.decodeStream(inputStream)
            uploadStory(bitmap)
        }
    }

    private fun uploadStory(bitmap: Bitmap) {
        val base64Image = bitmapToBase64(bitmap)
        val user = mAuth.currentUser
        val storyId = storiesRef.push().key ?: return

        // Get username from Users database
        database.getReference("Users").child(user?.uid ?: "").get()
            .addOnSuccessListener { snapshot ->
                val username = snapshot.child("username").value?.toString() ?: "User"

                val story = Story(
                    storyId = storyId,
                    userId = user?.uid ?: "",
                    username = username,
                    userProfileImage = "",
                    imageUrl = base64Image,
                    timestamp = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
                )

                storiesRef.child(storyId).setValue(story)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Story uploaded successfully!", Toast.LENGTH_SHORT).show()
                        loadStories()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun loadStories() {
        storiesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val stories = mutableListOf<Story>()

                for (storySnapshot in snapshot.children) {
                    val story = storySnapshot.getValue(Story::class.java)
                    if (story != null && !isExpired(story.timestamp)) {
                        stories.add(story)
                    }
                }

                // Sort by timestamp (newest first)
                stories.sortByDescending { it.timestamp }

                // Display up to 3 other users' stories (not yours)
                val currentUserId = mAuth.currentUser?.uid
                val otherStories = stories.filter { it.userId != currentUserId }

                // Set click listeners and images for story circles
                if (otherStories.isNotEmpty()) {
                    story1.setOnClickListener { viewStory(otherStories[0]) }
                    // Optionally load story thumbnail
                    loadStoryThumbnail(otherStories[0], story1)
                }

                if (otherStories.size > 1) {
                    story2.setOnClickListener { viewStory(otherStories[1]) }
                    loadStoryThumbnail(otherStories[1], story2)
                }

                if (otherStories.size > 2) {
                    story3.setOnClickListener { viewStory(otherStories[2]) }
                    loadStoryThumbnail(otherStories[2], story3)
                }

                Log.d("Stories", "Loaded ${stories.size} active stories")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Stories", "Error loading stories: ${error.message}")
            }
        })
    }

    private fun loadStoryThumbnail(story: Story, imageView: ImageView) {
        try {
            val imageBytes = Base64.decode(story.imageUrl, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("Stories", "Failed to load thumbnail: ${e.message}")
        }
    }

    private fun viewStory(story: Story) {
        // Only pass the story ID, load image from database in page_14
        val intent = Intent(this, page_14::class.java)
        intent.putExtra("STORY_ID", story.storyId)
        startActivity(intent)
    }

    private fun isExpired(timestamp: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - timestamp > 24 * 60 * 60 * 1000
    }

    private fun deleteExpiredStories() {
        storiesRef.get().addOnSuccessListener { snapshot ->
            for (storySnap in snapshot.children) {
                val story = storySnap.getValue(Story::class.java)
                if (story != null && isExpired(story.timestamp)) {
                    storiesRef.child(story.storyId).removeValue()
                    Log.d("Stories", "Deleted expired story: ${story.storyId}")
                }
            }
        }
    }
}