package com.example.assignment_01

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FollowRequestsActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var followRequestsRef: DatabaseReference
    private lateinit var followsRef: DatabaseReference

    private lateinit var requestsContainer: LinearLayout
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follow_requests)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        followRequestsRef = database.getReference("followRequests")
        followsRef = database.getReference("follows")

        requestsContainer = findViewById(R.id.requestsContainer)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            finish()
        }

        loadFollowRequests()
    }

    private fun loadFollowRequests() {
        val currentUserId = mAuth.currentUser?.uid ?: return

        followRequestsRef.orderByChild("toUserId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    requestsContainer.removeAllViews()

                    for (requestSnapshot in snapshot.children) {
                        val request = requestSnapshot.getValue(FollowRequest::class.java)
                        if (request != null && request.status == "pending") {
                            addRequestView(request)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FollowRequests", "Error: ${error.message}")
                }
            })
    }

    private fun addRequestView(request: FollowRequest) {
        val requestView = LayoutInflater.from(this).inflate(R.layout.item_follow_request, null)

        val usernameText = requestView.findViewById<TextView>(R.id.usernameText)
        val timeText = requestView.findViewById<TextView>(R.id.timeText)
        val acceptButton = requestView.findViewById<Button>(R.id.acceptButton)
        val rejectButton = requestView.findViewById<Button>(R.id.rejectButton)

        usernameText.text = request.fromUsername
        timeText.text = getTimeAgo(request.timestamp)

        acceptButton.setOnClickListener {
            acceptRequest(request)
        }

        rejectButton.setOnClickListener {
            rejectRequest(request)
        }

        requestsContainer.addView(requestView)
    }

    private fun acceptRequest(request: FollowRequest) {
        val currentUserId = mAuth.currentUser?.uid ?: return

        // Add to followers/following
        followsRef.child(request.fromUserId).child("following").child(currentUserId).setValue(true)
        followsRef.child(currentUserId).child("followers").child(request.fromUserId).setValue(true)

        // Update request status
        followRequestsRef.child(request.requestId).child("status").setValue("accepted")
            .addOnSuccessListener {
                Toast.makeText(this, "Follow request accepted", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectRequest(request: FollowRequest) {
        followRequestsRef.child(request.requestId).child("status").setValue("rejected")
            .addOnSuccessListener {
                Toast.makeText(this, "Follow request rejected", Toast.LENGTH_SHORT).show()
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