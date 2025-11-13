package com.example.assignment_01

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ChatActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 102

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var messagesRef: DatabaseReference

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private val messagesList = mutableListOf<Message>()

    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView
    private lateinit var attachImageBtn: ImageView
    private lateinit var chatUserName: TextView

    private var otherUserId: String = ""
    private var otherUserName: String = ""
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        mAuth = FirebaseAuth.getInstance()
        currentUserId = mAuth.currentUser?.uid ?: ""
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        messagesRef = database.getReference("messages")

        // Get chat user info
        otherUserId = intent.getStringExtra("USER_ID") ?: ""
        otherUserName = intent.getStringExtra("USER_NAME") ?: ""

        // Initialize views
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        attachImageBtn = findViewById(R.id.attachImageBtn)
        chatUserName = findViewById(R.id.chatUserName)
        val backButton = findViewById<ImageView>(R.id.backButton)

        chatUserName.text = otherUserName

        // Setup RecyclerView
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messageAdapter = MessageAdapter(messagesList) { message ->
            showEditDeleteDialog(message)
        }
        messagesRecyclerView.adapter = messageAdapter

        // Load messages
        loadMessages()

        // Send text message
        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendTextMessage(text)
                messageInput.text.clear()
            }
        }

        // Attach image
        attachImageBtn.setOnClickListener {
            openImagePicker()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadMessages() {
        val chatId = getChatId(currentUserId, otherUserId)

        messagesRef.child(chatId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()

                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        messages.add(message)
                    }
                }

                messages.sortBy { it.timestamp }
                messageAdapter.updateMessages(messages)
                messagesRecyclerView.scrollToPosition(messages.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatActivity, "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendTextMessage(text: String) {
        val messageId = messagesRef.push().key ?: return
        val chatId = getChatId(currentUserId, otherUserId)

        database.getReference("Users").child(currentUserId).get()
            .addOnSuccessListener { snapshot ->
                val senderName = snapshot.child("username").value?.toString() ?: "User"

                val message = Message(
                    messageId = messageId,
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    senderName = senderName,
                    messageText = text,
                    messageType = "text",
                    timestamp = System.currentTimeMillis()
                )

                messagesRef.child(chatId).child(messageId).setValue(message)
                    .addOnSuccessListener {
                        // Message sent
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
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
            val bitmap = BitmapFactory.decodeStream(inputStream)
            sendImageMessage(bitmap)
        }
    }

    private fun sendImageMessage(bitmap: Bitmap) {
        val messageId = messagesRef.push().key ?: return
        val chatId = getChatId(currentUserId, otherUserId)

        // Compress bitmap
        val base64Image = bitmapToBase64(bitmap)

        database.getReference("Users").child(currentUserId).get()
            .addOnSuccessListener { snapshot ->
                val senderName = snapshot.child("username").value?.toString() ?: "User"

                val message = Message(
                    messageId = messageId,
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    senderName = senderName,
                    messageText = "",
                    imageUrl = base64Image,
                    messageType = "image",
                    timestamp = System.currentTimeMillis()
                )

                messagesRef.child(chatId).child(messageId).setValue(message)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Image sent", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to send image", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun showEditDeleteDialog(message: Message) {
        val options = arrayOf("Edit", "Delete")

        AlertDialog.Builder(this)
            .setTitle("Message Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialog(message)
                    1 -> deleteMessage(message)
                }
            }
            .show()
    }

    private fun showEditDialog(message: Message) {
        val input = EditText(this)
        input.setText(message.messageText)

        AlertDialog.Builder(this)
            .setTitle("Edit Message")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty()) {
                    editMessage(message, newText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editMessage(message: Message, newText: String) {
        val chatId = getChatId(currentUserId, otherUserId)

        val updates = mapOf(
            "messageText" to newText,
            "isEdited" to true
        )

        messagesRef.child(chatId).child(message.messageId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Message edited", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to edit message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteMessage(message: Message) {
        val chatId = getChatId(currentUserId, otherUserId)

        val updates = mapOf(
            "isDeleted" to true,
            "messageText" to ""
        )

        messagesRef.child(chatId).child(message.messageId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos) // Lower quality for smaller size
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }
}