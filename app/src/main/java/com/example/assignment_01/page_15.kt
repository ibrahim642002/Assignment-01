package com.example.assignment_01

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.io.InputStream

class page_15 : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 105

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var profileImagePreview: ImageView
    private lateinit var usernameInput: EditText
    private lateinit var firstNameInput: EditText
    private lateinit var lastNameInput: EditText
    private lateinit var bioInput: EditText
    private lateinit var selectImageButton: Button
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageView

    private var selectedImageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page15)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")

        // Initialize views
        profileImagePreview = findViewById(R.id.profileImagePreview)
        usernameInput = findViewById(R.id.usernameInput)
        firstNameInput = findViewById(R.id.firstNameInput)
        lastNameInput = findViewById(R.id.lastNameInput)
        bioInput = findViewById(R.id.bioInput)
        selectImageButton = findViewById(R.id.selectImageButton)
        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backButton)

        // Load current profile data
        loadCurrentProfile()

        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        saveButton.setOnClickListener {
            saveProfile()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadCurrentProfile() {
        val currentUserId = mAuth.currentUser?.uid ?: return

        database.getReference("Users").child(currentUserId).get()
            .addOnSuccessListener { snapshot ->
                val username = snapshot.child("username").value?.toString() ?: ""
                val firstName = snapshot.child("firstName").value?.toString() ?: ""
                val lastName = snapshot.child("lastName").value?.toString() ?: ""
                val bio = snapshot.child("bio").value?.toString() ?: ""
                val profileImageBase64 = snapshot.child("profileImage").value?.toString() ?: ""

                usernameInput.setText(username)
                firstNameInput.setText(firstName)
                lastNameInput.setText(lastName)
                bioInput.setText(bio)

                // Load profile image
                if (profileImageBase64.isNotEmpty()) {
                    try {
                        val imageBytes = Base64.decode(profileImageBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        profileImagePreview.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            val inputStream: InputStream? = imageUri?.let { contentResolver.openInputStream(it) }
            selectedImageBitmap = BitmapFactory.decodeStream(inputStream)

            profileImagePreview.setImageBitmap(selectedImageBitmap)
        }
    }

    private fun saveProfile() {
        val currentUserId = mAuth.currentUser?.uid ?: return

        val username = usernameInput.text.toString().trim()
        val firstName = firstNameInput.text.toString().trim()
        val lastName = lastNameInput.text.toString().trim()
        val bio = bioInput.text.toString().trim()

        if (username.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        saveButton.isEnabled = false
        Toast.makeText(this, "Saving profile...", Toast.LENGTH_SHORT).show()

        val updates = mutableMapOf<String, Any>(
            "username" to username.lowercase(), // Store username in lowercase for search
            "firstName" to firstName,
            "lastName" to lastName,
            "bio" to bio
        )

        // Add profile image if selected
        if (selectedImageBitmap != null) {
            val base64Image = bitmapToBase64(selectedImageBitmap!!)
            updates["profileImage"] = base64Image
        }

        // Save to Firebase
        database.getReference("Users").child(currentUserId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                saveButton.isEnabled = true
            }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }
}