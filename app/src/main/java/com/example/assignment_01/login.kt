package com.example.assignment_01

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginBtn = findViewById<TextView>(R.id.login)
        val swBtn= findViewById<TextView>(R.id.sw)
        val signBtn= findViewById<TextView>(R.id.signup)

        loginBtn.setOnClickListener {
            val intent = Intent(this, page_5::class.java)
            startActivity(intent)
        }

        swBtn.setOnClickListener {
            val intent = Intent(this, login2::class.java)
            startActivity(intent)
        }

        signBtn.setOnClickListener {
            val intent = Intent(this, signup::class.java)
            startActivity(intent)
        }

        }
    }
