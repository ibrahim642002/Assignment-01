package com.example.assignment_01

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserProfileActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference
    private lateinit var followsRef: DatabaseReference
    private lateinit var followRequestsRef: DatabaseReference
    private lateinit var postsRef: DatabaseReference

    private lateinit var profileImage: ImageView
    private lateinit var usernameText: TextView
    private lateinit var fullNameText: TextView
    private lateinit var bioText: TextView
    private lateinit var postsCountText: TextView
    private lateinit var followersCountText: TextView
    private lateinit var followingCountText: TextView
    private lateinit var followButton: Button
    private lateinit var messageButton: Button
    private lateinit var backButton: ImageView
    private lateinit var photoGrid: LinearLayout

    private var targetUserId: String = ""
    private var currentUserId: String = ""
    private var isFollowing = false
    private var isRequestPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        mAuth = FirebaseAuth.getInstance()
        currentUserId = mAuth.currentUser?.uid ?: ""
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        usersRef = database.getReference("Users")
        followsRef = database.getReference("follows")
        followRequestsRef = database.getReference("followRequests")
        postsRef = database.getReference("posts")

        targetUserId = intent.getStringExtra("USER_ID") ?: ""

        if (targetUserId.isEmpty()) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        loadUserProfile()
        checkFollowStatus()
        loadFollowCounts()
        loadUserPosts()

        followButton.setOnClickListener {
            handleFollowAction()
        }

        messageButton.setOnClickListener {
            openChat()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        profileImage = findViewById(R.id.profileImage)
        usernameText = findViewById(R.id.usernameText)
        fullNameText = findViewById(R.id.fullNameText)
        bioText = findViewById(R.id.bioText)
        postsCountText = findViewById(R.id.postsCountText)
        followersCountText = findViewById(R.id.followersCountText)
        followingCountText = findViewById(R.id.followingCountText)
        followButton = findViewById(R.id.followButton)
        messageButton = findViewById(R.id.messageButton)
        backButton = findViewById(R.id.backButton)
        photoGrid = findViewById(R.id.photoGridContainer)
    }

    private fun loadUserProfile() {
        usersRef.child(targetUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").value?.toString() ?: "User"
                val firstName = snapshot.child("firstName").value?.toString() ?: ""
                val lastName = snapshot.child("lastName").value?.toString() ?: ""
                val bio = snapshot.child("bio").value?.toString() ?: "No bio yet"
                val profileImageBase64 = snapshot.child("profileImage").value?.toString() ?: ""

                usernameText.text = username
                fullNameText.text = "$firstName $lastName"
                bioText.text = bio

                // Load profile image (circular)
                if (profileImageBase64.isNotEmpty()) {
                    try {
                        val imageBytes = Base64.decode(profileImageBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        profileImage.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        Log.e("UserProfile", "Error loading profile image: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserProfile", "Error: ${error.message}")
            }
        })
    }

    private fun checkFollowStatus() {
        // Check if already following
        followsRef.child(currentUserId).child("following").child(targetUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isFollowing = snapshot.exists()
                    updateFollowButton()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserProfile", "Error: ${error.message}")
                }
            })

        // Check if follow request is pending
        followRequestsRef.orderByChild("fromUserId").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isRequestPending = false
                    for (requestSnapshot in snapshot.children) {
                        val request = requestSnapshot.getValue(FollowRequest::class.java)
                        if (request != null && request.toUserId == targetUserId && request.status == "pending") {
                            isRequestPending = true
                            break
                        }
                    }
                    updateFollowButton()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserProfile", "Error: ${error.message}")
                }
            })
    }

    private fun updateFollowButton() {
        when {
            isFollowing -> {
                followButton.text = "Following"
                followButton.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                messageButton.visibility = View.VISIBLE
            }
            isRequestPending -> {
                followButton.text = "Requested"
                followButton.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
                messageButton.visibility = View.GONE
            }
            else -> {
                followButton.text = "Follow"
                followButton.setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark))
                messageButton.visibility = View.GONE
            }
        }
    }

    private fun handleFollowAction() {
        when {
            isFollowing -> {
                // Unfollow
                unfollowUser()
            }
            isRequestPending -> {
                Toast.makeText(this, "Follow request already sent", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Send follow request
                sendFollowRequest()
            }
        }
    }

    private fun sendFollowRequest() {
        val requestId = followRequestsRef.push().key ?: return

        usersRef.child(currentUserId).get().addOnSuccessListener { snapshot ->
            val currentUsername = snapshot.child("username").value?.toString() ?: "User"

            val followRequest = FollowRequest(
                requestId = requestId,
                fromUserId = currentUserId,
                fromUsername = currentUsername,
                toUserId = targetUserId,
                timestamp = System.currentTimeMillis(),
                status = "pending"
            )

            followRequestsRef.child(requestId).setValue(followRequest)
                .addOnSuccessListener {
                    Toast.makeText(this, "Follow request sent!", Toast.LENGTH_SHORT).show()
                    isRequestPending = true
                    updateFollowButton()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun unfollowUser() {
        // Remove from following/followers
        followsRef.child(currentUserId).child("following").child(targetUserId).removeValue()
        followsRef.child(targetUserId).child("followers").child(currentUserId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Unfollowed", Toast.LENGTH_SHORT).show()
                isFollowing = false
                updateFollowButton()
                loadFollowCounts()
            }
    }

    private fun loadFollowCounts() {
        // Followers count
        followsRef.child(targetUserId).child("followers")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    followersCountText.text = snapshot.childrenCount.toString()
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Following count
        followsRef.child(targetUserId).child("following")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    followingCountText.text = snapshot.childrenCount.toString()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadUserPosts() {
        postsRef.orderByChild("userId").equalTo(targetUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    postsCountText.text = snapshot.childrenCount.toString()

                    // Display posts in grid (simplified - you can enhance this)
                    for (postSnapshot in snapshot.children) {
                        val post = postSnapshot.getValue(Post::class.java)
                        if (post != null) {
                            addPostToGrid(post)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun addPostToGrid(post: Post) {
        val imageView = ImageView(this)
        val size = resources.displayMetrics.widthPixels / 3 - 4
        imageView.layoutParams = LinearLayout.LayoutParams(size, size)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setPadding(2, 2, 2, 2)

        try {
            val imageBytes = Base64.decode(post.imageUrl, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("UserProfile", "Error loading post image: ${e.message}")
        }

        photoGrid.addView(imageView)
    }

    private fun openChat() {
        if (!isFollowing) {
            Toast.makeText(this, "You must follow this user to chat", Toast.LENGTH_SHORT).show()
            return
        }

        usersRef.child(targetUserId).get().addOnSuccessListener { snapshot ->
            val username = snapshot.child("username").value?.toString() ?: "User"

            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("USER_ID", targetUserId)
            intent.putExtra("USER_NAME", username)
            startActivity(intent)
        }
    }
}