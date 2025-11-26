package com.example.assignment_01

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class page_11 : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var notificationsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private lateinit var followsRef: DatabaseReference

    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private val notificationsList = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page11)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        notificationsRef = database.getReference("notifications")
        usersRef = database.getReference("Users")
        followsRef = database.getReference("follows")

        // Initialize RecyclerView
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        notificationsRecyclerView.layoutManager = LinearLayoutManager(this)

        notificationAdapter = NotificationAdapter(
            notificationsList,
            onFollowBackClick = { notification ->
                followBackUser(notification)
            },
            onNotificationClick = { notification ->
                handleNotificationClick(notification)
            }
        )
        notificationsRecyclerView.adapter = notificationAdapter

        // Load notifications
        loadNotifications()
    }

    private fun loadNotifications() {
        val currentUserId = mAuth.currentUser?.uid ?: return

        notificationsRef.child(currentUserId)
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notifications = mutableListOf<Notification>()

                    for (notificationSnapshot in snapshot.children) {
                        val notification = notificationSnapshot.getValue(Notification::class.java)
                        if (notification != null) {
                            // Load user profile image for each notification
                            loadUserProfileImage(notification) { updatedNotification ->
                                notifications.add(updatedNotification)

                                // Sort by newest first after all loaded
                                if (notifications.size == snapshot.childrenCount.toInt()) {
                                    notifications.sortByDescending { it.timestamp }
                                    notificationAdapter.updateNotifications(notifications)
                                }
                            }
                        }
                    }

                    if (snapshot.childrenCount == 0L) {
                        // No notifications
                        notificationAdapter.updateNotifications(emptyList())
                    }

                    Log.d("Notifications", "Loaded ${snapshot.childrenCount} notifications")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Notifications", "Error: ${error.message}")
                    Toast.makeText(this@page_11, "Failed to load notifications", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadUserProfileImage(notification: Notification, callback: (Notification) -> Unit) {
        usersRef.child(notification.fromUserId).get()
            .addOnSuccessListener { snapshot ->
                val profileImage = snapshot.child("profileImage").value?.toString() ?: ""
                val updatedNotification = notification.copy(fromUserProfileImage = profileImage)
                callback(updatedNotification)
            }
            .addOnFailureListener {
                callback(notification)
            }
    }

    private fun followBackUser(notification: Notification) {
        val currentUserId = mAuth.currentUser?.uid ?: return
        val targetUserId = notification.fromUserId

        // Check if already following
        followsRef.child(currentUserId).child("following").child(targetUserId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Toast.makeText(this, "Already following ${notification.fromUsername}", Toast.LENGTH_SHORT).show()
                } else {
                    // Follow the user
                    val updates = mutableMapOf<String, Any>()
                    updates["follows/$currentUserId/following/$targetUserId"] = true
                    updates["follows/$targetUserId/followers/$currentUserId"] = true

                    database.reference.updateChildren(updates)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Following ${notification.fromUsername}", Toast.LENGTH_SHORT).show()

                            // Send notification back
                            sendFollowNotification(targetUserId, notification.fromUsername)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to follow", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun sendFollowNotification(targetUserId: String, targetUsername: String) {
        val currentUserId = mAuth.currentUser?.uid ?: return

        usersRef.child(currentUserId).child("username").get()
            .addOnSuccessListener { snapshot ->
                val currentUsername = snapshot.value?.toString() ?: "User"

                val notificationId = notificationsRef.push().key ?: return@addOnSuccessListener

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
            }
    }

    private fun handleNotificationClick(notification: Notification) {
        // Mark as read
        notificationsRef.child(mAuth.currentUser?.uid ?: return)
            .child(notification.notificationId)
            .child("isRead")
            .setValue(true)

        // Navigate based on notification type
        when (notification.type) {
            "follow" -> {
                // Open user profile
                val intent = Intent(this, UserProfileActivity::class.java)
                intent.putExtra("USER_ID", notification.fromUserId)
                startActivity(intent)
            }
            // Add more cases for other notification types
        }
    }
}