package com.example.assignment_01

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
import java.io.InputStream

class UploadPostActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 103

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var postsRef: DatabaseReference

    private lateinit var imagePreview: ImageView
    private lateinit var selectImageText: TextView
    private lateinit var captionInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var selectImageButton: Button
    private lateinit var postButton: TextView
    private lateinit var backButton: ImageView

    private var selectedImageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_post)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        postsRef = database.getReference("posts")

        // Initialize views
        imagePreview = findViewById(R.id.imagePreview)
        selectImageText = findViewById(R.id.selectImageText)
        captionInput = findViewById(R.id.captionInput)
        locationInput = findViewById(R.id.locationInput)
        selectImageButton = findViewById(R.id.selectImageButton)
        postButton = findViewById(R.id.postButton)
        backButton = findViewById(R.id.backButton)

        // Click listeners
        imagePreview.setOnClickListener {
            openImagePicker()
        }

        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        postButton.setOnClickListener {
            uploadPost()
        }

        backButton.setOnClickListener {
            finish()
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

            // Display preview
            imagePreview.setImageBitmap(selectedImageBitmap)
            selectImageText.visibility = View.GONE
        }
    }

    private fun uploadPost() {
        if (selectedImageBitmap == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val caption = captionInput.text.toString().trim()
        val location = locationInput.text.toString().trim()

        if (caption.isEmpty()) {
            Toast.makeText(this, "Please write a caption", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        postButton.isEnabled = false
        Toast.makeText(this, "Uploading post...", Toast.LENGTH_SHORT).show()

        val currentUser = mAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            postButton.isEnabled = true
            return
        }

        // Get user info
        database.getReference("Users").child(currentUser.uid).get()
            .addOnSuccessListener { snapshot ->
                val username = snapshot.child("username").value?.toString() ?: "User"

                // Convert image to base64
                val base64Image = bitmapToBase64(selectedImageBitmap!!)

                // Create post
                val postId = postsRef.push().key ?: return@addOnSuccessListener

                val post = Post(
                    postId = postId,
                    userId = currentUser.uid,
                    username = username,
                    userProfileImage = "",
                    imageUrl = base64Image,
                    caption = caption,
                    location = location,
                    timestamp = System.currentTimeMillis(),
                    likes = 0,
                    comments = 0
                )

                // Save to Firebase
                postsRef.child(postId).setValue(post)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Post uploaded successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                        postButton.isEnabled = true
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to get user info", Toast.LENGTH_SHORT).show()
                postButton.isEnabled = true
            }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos) // Compress to 70% quality
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }
}