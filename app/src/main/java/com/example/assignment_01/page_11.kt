package com.example.assignment_01

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class page_11 : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var notificationsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference

    private lateinit var scrollView: ScrollView
    private lateinit var notificationsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page11)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        notificationsRef = database.getReference("notifications")
        usersRef = database.getReference("Users")

        // Find the ScrollView and the first LinearLayout inside it
        scrollView = findViewById(R.id.scrollView)
        notificationsContainer = scrollView.findViewById<LinearLayout>(R.id.scrollView)
            .getChildAt(0) as LinearLayout

        // Clear all static notifications
        notificationsContainer.removeAllViews()

        // Load real notifications
        loadNotifications()
    }

    private fun loadNotifications() {
        val currentUserId = mAuth.currentUser?.uid ?: return

        notificationsRef.child(currentUserId)
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    notificationsContainer.removeAllViews()

                    val notifications = mutableListOf<Map<String, Any?>>()

                    for (notifSnapshot in snapshot.children) {
                        val notif = notifSnapshot.value as? Map<String, Any?> ?: continue
                        notifications.add(notif)
                    }

                    // Show newest first
                    notifications.sortByDescending { it["timestamp"] as? Long ?: 0L }

                    if (notifications.isEmpty()) {
                        showNoNotifications()
                    } else {
                        notifications.forEach { notif ->
                            addNotificationView(notif)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Notifications", "Error: ${error.message}")
                    Toast.makeText(this@page_11, "Failed to load notifications", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showNoNotifications() {
        val cardView = androidx.cardview.widget.CardView(this)
        cardView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 16)
        }
        cardView.cardElevation = 0f
        cardView.setCardBackgroundColor(resources.getColor(android.R.color.white))

        val textView = TextView(this)
        textView.text = "No notifications yet\n\nFollow someone to get started!"
        textView.textSize = 16f
        textView.setPadding(32, 64, 32, 64)
        textView.gravity = android.view.Gravity.CENTER
        textView.setTextColor(resources.getColor(android.R.color.darker_gray))

        cardView.addView(textView)
        notificationsContainer.addView(cardView)
    }

    private fun addNotificationView(notification: Map<String, Any?>) {
        val cardView = androidx.cardview.widget.CardView(this)
        cardView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 8)
        }
        cardView.cardElevation = 0f
        cardView.setCardBackgroundColor(resources.getColor(android.R.color.white))

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.HORIZONTAL
        mainLayout.setPadding(24, 24, 24, 24)
        mainLayout.gravity = android.view.Gravity.CENTER_VERTICAL

        // Profile Image (Circular)
        val profileCardView = androidx.cardview.widget.CardView(this)
        profileCardView.layoutParams = LinearLayout.LayoutParams(80, 80)
        profileCardView.radius = 40f
        profileCardView.cardElevation = 2f

        val profileImage = ImageView(this)
        profileImage.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        profileImage.scaleType = ImageView.ScaleType.CENTER_CROP
        profileCardView.addView(profileImage)

        // Text content
        val textLayout = LinearLayout(this)
        textLayout.orientation = LinearLayout.VERTICAL
        textLayout.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            setMargins(24, 0, 24, 0)
        }

        val messageText = TextView(this)
        messageText.text = notification["message"]?.toString() ?: ""
        messageText.textSize = 14f
        messageText.setTextColor(resources.getColor(android.R.color.black))

        val timeText = TextView(this)
        val timestamp = notification["timestamp"] as? Long ?: 0L
        timeText.text = getTimeAgo(timestamp)
        timeText.textSize = 12f
        timeText.setTextColor(resources.getColor(android.R.color.darker_gray))
        timeText.setPadding(0, 8, 0, 0)

        textLayout.addView(messageText)
        textLayout.addView(timeText)

        // Assemble layout
        mainLayout.addView(profileCardView)
        mainLayout.addView(textLayout)

        cardView.addView(mainLayout)

        // Load profile image
        val fromUserId = notification["fromUserId"]?.toString() ?: ""
        loadUserImage(fromUserId, profileImage)

        // Set background color for unread
        val isRead = notification["isRead"] as? Boolean ?: false
        if (!isRead) {
            cardView.setCardBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
        }

        // Click to mark as read and view profile
        cardView.setOnClickListener {
            markAsRead(notification["notificationId"]?.toString() ?: "")
            openUserProfile(fromUserId)
        }

        notificationsContainer.addView(cardView)
    }

    private fun loadUserImage(userId: String, imageView: ImageView) {
        usersRef.child(userId).child("profileImage").get()
            .addOnSuccessListener { snapshot ->
                val profileImageBase64 = snapshot.value?.toString() ?: ""
                if (profileImageBase64.isNotEmpty()) {
                    try {
                        val imageBytes = Base64.decode(profileImageBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        imageView.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        Log.e("Notifications", "Error loading image: ${e.message}")
                        imageView.setImageResource(R.drawable.img_3)
                    }
                } else {
                    imageView.setImageResource(R.drawable.img_3)
                }
            }
            .addOnFailureListener {
                imageView.setImageResource(R.drawable.img_3)
            }
    }

    private fun markAsRead(notificationId: String) {
        val currentUserId = mAuth.currentUser?.uid ?: return
        if (notificationId.isNotEmpty()) {
            notificationsRef.child(currentUserId).child(notificationId).child("isRead").setValue(true)
        }
    }

    private fun openUserProfile(userId: String) {
        if (userId.isNotEmpty()) {
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> "${diff / 604800000}w ago"
        }
    }
}