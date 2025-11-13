package com.example.assignment_01

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object PresenceManager {

    private lateinit var database: FirebaseDatabase
    private lateinit var presenceRef: DatabaseReference
    private var connectionRef: DatabaseReference? = null

    fun initialize(databaseUrl: String) {
        database = FirebaseDatabase.getInstance(databaseUrl)
        presenceRef = database.getReference(".info/connected")
    }

    fun setUserOnline() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userPresenceRef = database.getReference("Users").child(userId)

        presenceRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false

                if (connected) {
                    // When connected, set online
                    userPresenceRef.child("isOnline").setValue(true)
                    userPresenceRef.child("lastSeen").setValue(ServerValue.TIMESTAMP)

                    // When disconnected, set offline
                    userPresenceRef.child("isOnline").onDisconnect().setValue(false)
                    userPresenceRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP)

                    Log.d("Presence", "User is online")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Presence", "Error: ${error.message}")
            }
        })
    }

    fun setUserOffline() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userPresenceRef = database.getReference("Users").child(userId)

        userPresenceRef.child("isOnline").setValue(false)
        userPresenceRef.child("lastSeen").setValue(ServerValue.TIMESTAMP)
    }
}