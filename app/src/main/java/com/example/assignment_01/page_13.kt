package com.example.assignment_01

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class page_13 : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var postsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private lateinit var followsRef: DatabaseReference

    private lateinit var usernameText: TextView
    private lateinit var fullNameText: TextView
    private lateinit var postsCountText: TextView
    private lateinit var followersCountText: TextView
    private lateinit var followingCountText: TextView
    private lateinit var photoGrid: GridLayout
    private lateinit var createPostBtn: Button
    private lateinit var profileImage: ImageView
    private lateinit var notificationIcon: ImageView
    private lateinit var menuIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page13)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        postsRef = database.getReference("posts")
        usersRef = database.getReference("Users")
        followsRef = database.getReference("follows")

        // Initialize views
        usernameText = findViewById(R.id.usernameText)
        fullNameText = findViewById(R.id.fullNameText)
        postsCountText = findViewById(R.id.postsCountText)
        followersCountText = findViewById(R.id.followersCountText)
        followingCountText = findViewById(R.id.followingCountText)
        photoGrid = findViewById(R.id.photoGrid)
        createPostBtn = findViewById(R.id.createPostBtn)
        profileImage = findViewById(R.id.profileImage)

        // Top bar icons
        notificationIcon = findViewById(R.id.notificationIcon)
        menuIcon = findViewById(R.id.menuIcon)

        // Initialize and set user online
        PresenceManager.initialize("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        PresenceManager.setUserOnline()

        // Load user info
        loadUserInfo()

        // Load user's posts
        loadUserPosts()

        // Load followers/following counts
        loadFollowCounts()

        // Create new post button
        createPostBtn.setOnClickListener {
            openUploadPost()
        }

        // Profile image click - update profile picture
        profileImage.setOnClickListener {
            openProfilePictureUpload()
        }

        // Notification icon - open follow requests
        notificationIcon.setOnClickListener {
            openFollowRequests()
        }

        // Get the LinearLayout containers for followers and following to make them clickable
        val followersContainer = followersCountText.parent as? LinearLayout
        val followingContainer = followingCountText.parent as? LinearLayout

        // Followers container click - view followers list
        followersContainer?.setOnClickListener {
            openFollowersList("followers")
        }

        // Following container click - view following list
        followingContainer?.setOnClickListener {
            openFollowersList("following")
        }

        // Also make the labels individually clickable as backup
        findViewById<TextView>(R.id.followersLabel)?.setOnClickListener {
            openFollowersList("followers")
        }

        findViewById<TextView>(R.id.followingLabel)?.setOnClickListener {
            openFollowersList("following")
        }
    }

    private fun loadUserInfo() {
        val currentUserId = mAuth.currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        usersRef.child(currentUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").value?.toString() ?: "User"
                val firstName = snapshot.child("firstName").value?.toString() ?: ""
                val lastName = snapshot.child("lastName").value?.toString() ?: ""
                val profileImageBase64 = snapshot.child("profileImage").value?.toString() ?: ""

                usernameText.text = username
                fullNameText.text = "$firstName $lastName"

                // Load profile image if exists
                if (profileImageBase64.isNotEmpty()) {
                    try {
                        val imageBytes = Base64.decode(profileImageBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        profileImage.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        Log.e("Profile", "Error loading profile image: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Profile", "Failed to load user info: ${error.message}")
            }
        })
    }

    private fun loadFollowCounts() {
        val currentUserId = mAuth.currentUser?.uid ?: return

        // Count followers
        followsRef.child(currentUserId).child("followers")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    followersCountText.text = snapshot.childrenCount.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Profile", "Error loading followers count: ${error.message}")
                }
            })

        // Count following
        followsRef.child(currentUserId).child("following")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    followingCountText.text = snapshot.childrenCount.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Profile", "Error loading following count: ${error.message}")
                }
            })
    }

    private fun loadUserPosts() {
        val currentUserId = mAuth.currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        postsRef.orderByChild("userId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val posts = mutableListOf<Post>()

                    for (postSnapshot in snapshot.children) {
                        val post = postSnapshot.getValue(Post::class.java)
                        if (post != null) {
                            posts.add(post)
                        }
                    }

                    // Sort by timestamp (newest first)
                    posts.sortByDescending { it.timestamp }

                    // Update posts count
                    postsCountText.text = posts.size.toString()

                    // Display posts in grid
                    displayPostsInGrid(posts)

                    Log.d("Profile", "Loaded ${posts.size} posts for user")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Profile", "Error loading posts: ${error.message}")
                    Toast.makeText(this@page_13, "Failed to load posts", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun displayPostsInGrid(posts: List<Post>) {
        // Get the number of existing static images (9 in your XML)
        val staticImageCount = 9

        // Remove only previously added dynamic images (if any)
        val currentChildCount = photoGrid.childCount
        if (currentChildCount > staticImageCount) {
            photoGrid.removeViews(0, currentChildCount - staticImageCount)
        }

        // Add new posts at the beginning
        for ((index, post) in posts.withIndex()) {
            val imageView = ImageView(this)

            // Set layout params to match existing grid items
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = resources.displayMetrics.widthPixels / 3 - 4 // Square based on screen width
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(2, 2, 2, 2)
            imageView.layoutParams = params

            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            // Load image from base64
            try {
                val imageBytes = Base64.decode(post.imageUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e("Profile", "Error decoding image: ${e.message}")
                imageView.setImageResource(R.drawable.img_78) // Fallback image
            }

            // Click listener to view post details
            imageView.setOnClickListener {
                viewPostDetails(post)
            }

            // Add at the beginning (index 0, 1, 2, ...)
            photoGrid.addView(imageView, index)
        }
    }

    private fun viewPostDetails(post: Post) {
        // Show post details in a toast for now
        Toast.makeText(
            this,
            "Caption: ${post.caption}\nLocation: ${post.location}",
            Toast.LENGTH_SHORT
        ).show()

        // Optional: Create a PostDetailActivity to show full post
        // val intent = Intent(this, PostDetailActivity::class.java)
        // intent.putExtra("POST_ID", post.postId)
        // startActivity(intent)
    }

    private fun openUploadPost() {
        val intent = Intent(this, UploadPostActivity::class.java)
        startActivity(intent)
    }

    private fun openProfilePictureUpload() {
        val intent = Intent(this, UpdateProfileActivity::class.java)
        startActivity(intent)
    }

    private fun openFollowRequests() {
        val intent = Intent(this, FollowRequestsActivity::class.java)
        startActivity(intent)
    }

    private fun openFollowersList(type: String) {
        val intent = Intent(this, FollowersListActivity::class.java)
        intent.putExtra("LIST_TYPE", type) // "followers" or "following"
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Reload posts when returning to profile
        loadUserPosts()
        // Set user online again
        PresenceManager.setUserOnline()
    }

    override fun onPause() {
        super.onPause()
        // Don't set offline on pause, only on destroy
    }

    override fun onDestroy() {
        super.onDestroy()
        // Set user offline when app is closed
        PresenceManager.setUserOffline()
    }
}