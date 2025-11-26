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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
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
    private lateinit var followsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference

    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyCircleAdapter: StoryCircleAdapter
    private val storyCirclesList = mutableListOf<StoryCircle>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page5)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        storiesRef = database.getReference("stories")
        followsRef = database.getReference("follows")
        usersRef = database.getReference("Users")

        // Initialize RecyclerView for stories
        storiesRecyclerView = findViewById(R.id.storiesRecyclerView)
        storiesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        storyCircleAdapter = StoryCircleAdapter(storyCirclesList) { storyCircle ->
            if (storyCircle.stories.isNotEmpty()) {
                viewUserStories(storyCircle.stories)
            } else {
                // If it's current user with no stories, open gallery
                if (storyCircle.userId == mAuth.currentUser?.uid) {
                    openGallery()
                }
            }
        }
        storiesRecyclerView.adapter = storyCircleAdapter

        val searchbtn = findViewById<ImageView>(R.id.search)
        val msgbtn = findViewById<ImageView>(R.id.msg)
        val profilebtn = findViewById<ImageView>(R.id.profile)
        val heartbtn = findViewById<ImageView>(R.id.heart)

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

        loadFollowingStories()
        deleteExpiredStories()

        PresenceManager.initialize("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        PresenceManager.setUserOnline()
    }

    override fun onDestroy() {
        super.onDestroy()
        PresenceManager.setUserOffline()
    }

    private fun loadFollowingStories() {
        val currentUserId = mAuth.currentUser?.uid ?: return

        followsRef.child(currentUserId).child("following")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(followingSnapshot: DataSnapshot) {
                    val followingUserIds = mutableListOf<String>()

                    followingUserIds.add(currentUserId)

                    for (userSnapshot in followingSnapshot.children) {
                        val userId = userSnapshot.key
                        if (userId != null) {
                            followingUserIds.add(userId)
                        }
                    }

                    Log.d("Stories", "Following ${followingUserIds.size - 1} users")
                    loadStoriesForUsers(followingUserIds)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Stories", "Error loading following: ${error.message}")
                }
            })
    }

    private fun loadStoriesForUsers(userIds: List<String>) {
        storiesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val storiesMap = mutableMapOf<String, MutableList<Story>>()

                for (storySnapshot in snapshot.children) {
                    val story = storySnapshot.getValue(Story::class.java)
                    if (story != null && !isExpired(story.timestamp) && userIds.contains(story.userId)) {
                        if (storiesMap.containsKey(story.userId)) {
                            storiesMap[story.userId]?.add(story)
                        } else {
                            storiesMap[story.userId] = mutableListOf(story)
                        }
                    }
                }

                loadStoryCircles(storiesMap, userIds)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Stories", "Error loading stories: ${error.message}")
            }
        })
    }

    private fun loadStoryCircles(storiesMap: Map<String, MutableList<Story>>, userIds: List<String>) {
        val storyCircles = mutableListOf<StoryCircle>()
        val currentUserId = mAuth.currentUser?.uid

        if (currentUserId != null) {
            usersRef.child(currentUserId).get()
                .addOnSuccessListener { userSnapshot ->
                    val username = userSnapshot.child("username").value?.toString() ?: "Your Story"
                    val profileImage = userSnapshot.child("profileImage").value?.toString() ?: ""

                    val myStories = storiesMap[currentUserId]?.sortedByDescending { it.timestamp } ?: emptyList()

                    val myStoryCircle = StoryCircle(
                        userId = currentUserId,
                        username = username,
                        userProfileImage = profileImage,
                        stories = myStories,
                        latestStoryTimestamp = myStories.firstOrNull()?.timestamp ?: 0L
                    )

                    storyCircles.add(myStoryCircle)
                    loadOtherUsersStoryCircles(storiesMap, userIds, storyCircles)
                }
        }
    }

    private fun loadOtherUsersStoryCircles(
        storiesMap: Map<String, MutableList<Story>>,
        userIds: List<String>,
        storyCircles: MutableList<StoryCircle>
    ) {
        val currentUserId = mAuth.currentUser?.uid
        val otherUserIds = userIds.filter { it != currentUserId && storiesMap.containsKey(it) }

        var loadedCount = 0

        for (userId in otherUserIds) {
            usersRef.child(userId).get()
                .addOnSuccessListener { userSnapshot ->
                    val username = userSnapshot.child("username").value?.toString() ?: "User"
                    val profileImage = userSnapshot.child("profileImage").value?.toString() ?: ""

                    val userStories = storiesMap[userId]?.sortedByDescending { it.timestamp } ?: emptyList()

                    if (userStories.isNotEmpty()) {
                        val storyCircle = StoryCircle(
                            userId = userId,
                            username = username,
                            userProfileImage = profileImage,
                            stories = userStories,
                            latestStoryTimestamp = userStories.first().timestamp
                        )

                        storyCircles.add(storyCircle)
                    }

                    loadedCount++

                    if (loadedCount == otherUserIds.size) {
                        val sortedCircles = storyCircles.sortedWith(compareByDescending<StoryCircle> {
                            it.userId == currentUserId
                        }.thenByDescending {
                            it.latestStoryTimestamp
                        })

                        storyCircleAdapter.updateStories(sortedCircles)
                        Log.d("Stories", "Loaded ${sortedCircles.size} story circles")
                    }
                }
                .addOnFailureListener {
                    loadedCount++
                    if (loadedCount == otherUserIds.size) {
                        val sortedCircles = storyCircles.sortedWith(compareByDescending<StoryCircle> {
                            it.userId == currentUserId
                        }.thenByDescending {
                            it.latestStoryTimestamp
                        })
                        storyCircleAdapter.updateStories(sortedCircles)
                    }
                }
        }

        if (otherUserIds.isEmpty()) {
            storyCircleAdapter.updateStories(storyCircles)
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
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun viewUserStories(stories: List<Story>) {
        val sortedStories = stories.sortedBy { it.timestamp }
        val storyIds = sortedStories.map { it.storyId }.toTypedArray()

        val intent = Intent(this, page_14::class.java)
        intent.putExtra("STORY_IDS", storyIds)
        intent.putExtra("CURRENT_INDEX", 0)
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