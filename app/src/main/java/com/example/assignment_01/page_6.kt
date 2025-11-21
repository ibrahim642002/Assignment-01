package com.example.assignment_01

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class page_6 : AppCompatActivity() {

    private lateinit var searchbtn: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page6)

        searchbtn = findViewById(R.id.search)

        searchbtn.setOnClickListener {
            val intent = Intent(this, page_7::class.java)
            startActivity(intent)
        }

    }

}


