package com.example.assignment_01

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FollowersListActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var followsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference

    private lateinit var titleText: TextView
    private lateinit var listContainer: LinearLayout
    private lateinit var backButton: ImageView

    private var listType = "followers" // "followers" or "following"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers_list)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        followsRef = database.getReference("follows")
        usersRef = database.getReference("Users")

        listType = intent.getStringExtra("LIST_TYPE") ?: "followers"

        titleText = findViewById(R.id.titleText)
        listContainer = findViewById(R.id.listContainer)
        backButton = findViewById(R.id.backButton)

        titleText.text = if (listType == "followers") "Followers" else "Following"

        backButton.setOnClickListener {
            finish()
        }

        loadList()
    }

    private fun loadList() {
        val currentUserId = mAuth.currentUser?.uid ?: return

        followsRef.child(currentUserId).child(listType)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listContainer.removeAllViews()

                    for (userSnapshot in snapshot.children) {
                        val userId = userSnapshot.key ?: continue
                        loadUserInfo(userId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FollowersList", "Error: ${error.message}")
                }
            })
    }

    private fun loadUserInfo(userId: String) {
        usersRef.child(userId).get().addOnSuccessListener { snapshot ->
            val username = snapshot.child("username").value?.toString() ?: "User"
            val firstName = snapshot.child("firstName").value?.toString() ?: ""
            val lastName = snapshot.child("lastName").value?.toString() ?: ""
            val profileImage = snapshot.child("profileImage").value?.toString() ?: ""
            val isOnline = snapshot.child("isOnline").value as? Boolean ?: false

            addUserView(userId, username, "$firstName $lastName", profileImage, isOnline)
        }
    }

    private fun addUserView(userId: String, username: String, fullName: String, profileImage: String, isOnline: Boolean) {
        val userView = LayoutInflater.from(this).inflate(R.layout.item_search_result, null)

        val profileImageView = userView.findViewById<ImageView>(R.id.profileImage)
        val usernameText = userView.findViewById<TextView>(R.id.usernameText)
        val fullNameText = userView.findViewById<TextView>(R.id.fullNameText)
        val onlineIndicator = userView.findViewById<View>(R.id.onlineIndicator)

        usernameText.text = username
        fullNameText.text = fullName

        if (profileImage.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(profileImage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                profileImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e("FollowersList", "Error loading image: ${e.message}")
            }
        }

        onlineIndicator.visibility = if (isOnline) View.VISIBLE else View.GONE

        userView.setOnClickListener {
            val intent = Intent(this, UserProfile::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        listContainer.addView(userView)
    }
}