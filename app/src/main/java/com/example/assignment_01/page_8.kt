package com.example.assignment_01

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class page_8 : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var chatsRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page8)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        chatsRef = database.getReference("chats")

        // Get chat containers
        val chat1 = findViewById<RelativeLayout>(R.id.three)
        val chat2 = findViewById<RelativeLayout>(R.id.four)
        val chat3 = findViewById<RelativeLayout>(R.id.five)
        val chat4 = findViewById<RelativeLayout>(R.id.six)
        val chat5 = findViewById<RelativeLayout>(R.id.seven)
        val chat6 = findViewById<RelativeLayout>(R.id.eight)

        // Set click listeners - hardcoded for demo
        // In real app, you'd load these from Firebase
        chat1.setOnClickListener {
            openChat("joshua_I", "Joshua")
        }

        chat2.setOnClickListener {
            openChat("karennne", "Karen")
        }

        chat3.setOnClickListener {
            openChat("martini_rond", "Martini")
        }

        chat4.setOnClickListener {
            openChat("andrewww", "Andrew")
        }

        chat5.setOnClickListener {
            openChat("kiero_d", "Kiero")
        }

        chat6.setOnClickListener {
            openChat("maxjacobson", "Max")
        }
    }

    private fun openChat(userId: String, userName: String) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("USER_ID", userId)
        intent.putExtra("USER_NAME", userName)
        startActivity(intent)
    }
}