package com.example.assignment_01

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class signup : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        mAuth = FirebaseAuth.getInstance()

        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")

        // Getting all input fields
        val username = findViewById<EditText>(R.id.user2)
        val fname = findViewById<EditText>(R.id.fname2)
        val lname = findViewById<EditText>(R.id.lname2)
        val dob = findViewById<EditText>(R.id.dob2)
        val email = findViewById<EditText>(R.id.email2)
        val password = findViewById<EditText>(R.id.pass2)

        val signupBtn = findViewById<TextView>(R.id.signup)
        val backBtn = findViewById<ImageView>(R.id.back)

        signupBtn.setOnClickListener {
            Log.d("Signup", "Button clicked")

            val u = username.text.toString().trim()
            val f = fname.text.toString().trim()
            val l = lname.text.toString().trim()
            val d = dob.text.toString().trim()
            val e = email.text.toString().trim()
            val p = password.text.toString().trim()

            // Validation
            if (u.isEmpty() || f.isEmpty() || l.isEmpty() || d.isEmpty() || e.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("Signup", "Creating user")

            // Create user using Firebase Authentication
            mAuth.createUserWithEmailAndPassword(e, p)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("Signup", "Auth success")

                        val userId = mAuth.currentUser!!.uid
                        val db = database.getReference("Users")

                        val userProfile = mapOf(
                            "username" to u,
                            "firstName" to f,
                            "lastName" to l,
                            "dob" to d,
                            "email" to e
                        )

                        Log.d("Signup", "Saving to database")

                        db.child(userId).setValue(userProfile)
                            .addOnSuccessListener {
                                Log.d("Signup", "Database success!")
                                Toast.makeText(this@signup, "Account Created!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@signup, page_5::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Signup", "Database failed: ${exception.message}")
                                Toast.makeText(this@signup, "Database Error: ${exception.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Log.e("Signup", "Auth failed: ${task.exception?.message}")
                        Toast.makeText(
                            this@signup,
                            "Signup failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // Back button
        backBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}