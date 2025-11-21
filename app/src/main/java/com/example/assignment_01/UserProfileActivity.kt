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
    private lateinit var notificationsRef: DatabaseReference
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
    private var targetUsername: String = ""
    private var currentUserId: String = ""
    private var currentUsername: String = ""
    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        mAuth = FirebaseAuth.getInstance()
        currentUserId = mAuth.currentUser?.uid ?: ""
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        usersRef = database.getReference("Users")
        followsRef = database.getReference("follows")
        notificationsRef = database.getReference("notifications")
        postsRef = database.getReference("posts")

        targetUserId = intent.getStringExtra("USER_ID") ?: ""

        if (targetUserId.isEmpty()) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        loadCurrentUsername()
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

    private fun loadCurrentUsername() {
        usersRef.child(currentUserId).child("username").get()
            .addOnSuccessListener { snapshot ->
                currentUsername = snapshot.value?.toString() ?: "User"
            }
    }

    private fun loadUserProfile() {
        usersRef.child(targetUserId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                targetUsername = snapshot.child("username").value?.toString() ?: "User"
                val firstName = snapshot.child("firstName").value?.toString() ?: ""
                val lastName = snapshot.child("lastName").value?.toString() ?: ""
                val bio = snapshot.child("bio").value?.toString() ?: "No bio yet"
                val profileImageBase64 = snapshot.child("profileImage").value?.toString() ?: ""

                usernameText.text = targetUsername
                fullNameText.text = "$firstName $lastName"
                bioText.text = bio

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
        followsRef.child(currentUserId).child("following").child(targetUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isFollowing = snapshot.exists()
                    updateFollowButton()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserProfile", "Error: ${error.message}")
                }
            })
    }

    private fun updateFollowButton() {
        if (isFollowing) {
            followButton.text = "Following"
            followButton.setBackgroundColor(resources.getColor(android.R.color.darker_gray))
            messageButton.visibility = View.VISIBLE
        } else {
            followButton.text = "Follow"
            followButton.setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark))
            messageButton.visibility = View.GONE
        }
    }

    private fun handleFollowAction() {
        if (isFollowing) {
            unfollowUser()
        } else {
            followUser()
        }
    }

    private fun followUser() {
        // Add to following/followers immediately (no request needed)
        val updates = mutableMapOf<String, Any>()
        updates["follows/$currentUserId/following/$targetUserId"] = true
        updates["follows/$targetUserId/followers/$currentUserId"] = true

        database.reference.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Following $targetUsername", Toast.LENGTH_SHORT).show()

                // Send notification to target user
                sendFollowNotification()

                // Send notification to current user
                sendSelfNotification()

                isFollowing = true
                updateFollowButton()
                loadFollowCounts()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to follow", Toast.LENGTH_SHORT).show()
            }
    }

    private fun unfollowUser() {
        val updates = mutableMapOf<String, Any?>()
        updates["follows/$currentUserId/following/$targetUserId"] = null
        updates["follows/$targetUserId/followers/$currentUserId"] = null

        database.reference.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Unfollowed $targetUsername", Toast.LENGTH_SHORT).show()
                isFollowing = false
                updateFollowButton()
                loadFollowCounts()
            }
    }

    private fun sendFollowNotification() {
        val notificationId = notificationsRef.push().key ?: return

        val notification = mapOf(
            "notificationId" to notificationId,
            "toUserId" to targetUserId,
            "fromUserId" to currentUserId,
            "fromUsername" to currentUsername,
            "type" to "follow",
            "message" to "$currentUsername started following you",
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )

        notificationsRef.child(targetUserId).child(notificationId).setValue(notification)
            .addOnSuccessListener {
                Log.d("Notification", "Follow notification sent to $targetUsername")
            }
    }

    private fun sendSelfNotification() {
        val notificationId = notificationsRef.push().key ?: return

        val notification = mapOf(
            "notificationId" to notificationId,
            "toUserId" to currentUserId,
            "fromUserId" to targetUserId,
            "fromUsername" to targetUsername,
            "type" to "following",
            "message" to "You started following $targetUsername",
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )

        notificationsRef.child(currentUserId).child(notificationId).setValue(notification)
            .addOnSuccessListener {
                Log.d("Notification", "Self notification created")
            }
    }

    private fun loadFollowCounts() {
        followsRef.child(targetUserId).child("followers")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    followersCountText.text = snapshot.childrenCount.toString()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

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

        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("USER_ID", targetUserId)
        intent.putExtra("USER_NAME", targetUsername)
        startActivity(intent)
    }
}