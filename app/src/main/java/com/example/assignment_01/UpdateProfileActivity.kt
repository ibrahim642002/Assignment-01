package com.example.assignment_01

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.io.InputStream

class UpdateProfileActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 104

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var profileImagePreview: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageView

    private var selectedImageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_profile)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")

        profileImagePreview = findViewById(R.id.profileImagePreview)
        selectImageButton = findViewById(R.id.selectImageButton)
        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backButton)

        // Load current profile image
        loadCurrentProfileImage()

        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        saveButton.setOnClickListener {
            uploadProfileImage()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadCurrentProfileImage() {
        val currentUserId = mAuth.currentUser?.uid ?: return

        database.getReference("Users").child(currentUserId).get()
            .addOnSuccessListener { snapshot ->
                val profileImageBase64 = snapshot.child("profileImage").value?.toString() ?: ""

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

    private fun uploadProfileImage() {
        if (selectedImageBitmap == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = mAuth.currentUser?.uid ?: return

        saveButton.isEnabled = false
        Toast.makeText(this, "Updating profile picture...", Toast.LENGTH_SHORT).show()

        // Convert to base64
        val base64Image = bitmapToBase64(selectedImageBitmap!!)

        // Save to Firebase
        database.getReference("Users").child(currentUserId).child("profileImage")
            .setValue(base64Image)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
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