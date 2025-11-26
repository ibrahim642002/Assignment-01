package com.example.assignment_01

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
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
    private lateinit var usersRef: DatabaseReference
    private lateinit var notificationsRef: DatabaseReference

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
    private var currentUserName: String = ""

    // Screenshot detection
    private var screenshotObserver: ContentObserver? = null
    private val screenshotHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        mAuth = FirebaseAuth.getInstance()
        currentUserId = mAuth.currentUser?.uid ?: ""
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        messagesRef = database.getReference("messages")
        usersRef = database.getReference("Users")
        notificationsRef = database.getReference("notifications")

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

        // Load current user's name
        loadCurrentUserName()

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

        // Video call button
        val videoCallButton = findViewById<ImageView>(R.id.videoCallButton)
        videoCallButton.setOnClickListener {
            startVideoCall()
        }

        // Start screenshot detection
        startScreenshotDetection()
    }

    private fun loadCurrentUserName() {
        usersRef.child(currentUserId).child("username").get()
            .addOnSuccessListener { snapshot ->
                currentUserName = snapshot.value?.toString() ?: "User"
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
                if (messages.isNotEmpty()) {
                    messagesRecyclerView.scrollToPosition(messages.size - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatActivity, "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendTextMessage(text: String) {
        val messageId = messagesRef.push().key ?: return
        val chatId = getChatId(currentUserId, otherUserId)

        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            receiverId = otherUserId,
            senderName = currentUserName,
            messageText = text,
            messageType = "text",
            timestamp = System.currentTimeMillis()
        )

        messagesRef.child(chatId).child(messageId).setValue(message)
            .addOnSuccessListener {
                Log.d("ChatActivity", "Message sent")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
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

        val base64Image = bitmapToBase64(bitmap)

        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            receiverId = otherUserId,
            senderName = currentUserName,
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

    private fun showEditDeleteDialog(message: Message) {
        if (message.senderId != currentUserId) {
            return
        }

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

    // ============ SCREENSHOT DETECTION ============

    private fun startScreenshotDetection() {
        screenshotObserver = object : ContentObserver(screenshotHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)

                if (uri != null && uri.toString().contains("screenshots", ignoreCase = true)) {
                    Log.d("Screenshot", "Screenshot detected!")
                    handleScreenshotDetected()
                }
            }
        }

        // Observe external storage for new images (screenshots)
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver!!
        )
    }

    private fun handleScreenshotDetected() {
        // Send notification to the other user
        sendScreenshotNotification()

        // Optional: Show toast to current user
        Toast.makeText(this, "$otherUserName will be notified", Toast.LENGTH_SHORT).show()
    }

    private fun sendScreenshotNotification() {
        val notificationId = notificationsRef.push().key ?: return

        val notification = mapOf(
            "notificationId" to notificationId,
            "toUserId" to otherUserId,
            "fromUserId" to currentUserId,
            "fromUsername" to currentUserName,
            "type" to "screenshot",
            "message" to "$currentUserName took a screenshot of the chat",
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )

        notificationsRef.child(otherUserId).child(notificationId).setValue(notification)
            .addOnSuccessListener {
                Log.d("Screenshot", "Screenshot notification sent to $otherUserName")
            }
            .addOnFailureListener {
                Log.e("Screenshot", "Failed to send screenshot notification")
            }
    }

    private fun stopScreenshotDetection() {
        screenshotObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
    }

    private fun startVideoCall() {
        // Create unique channel name using both user IDs
        val channelName = getChatId(currentUserId, otherUserId)

        // Send notification to other user about incoming call
        sendVideoCallNotification(channelName)

        // Start video call activity
        val intent = Intent(this, VideoCallActivity::class.java)
        intent.putExtra("CHANNEL_NAME", channelName)
        intent.putExtra("OTHER_USER_NAME", otherUserName)
        startActivity(intent)
    }

    private fun sendVideoCallNotification(channelName: String) {
        val notificationId = notificationsRef.push().key ?: return

        val notification = mapOf(
            "notificationId" to notificationId,
            "toUserId" to otherUserId,
            "fromUserId" to currentUserId,
            "fromUsername" to currentUserName,
            "type" to "video_call",
            "message" to "$currentUserName is calling you...",
            "channelName" to channelName,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )

        notificationsRef.child(otherUserId).child(notificationId).setValue(notification)
            .addOnSuccessListener {
                Log.d("VideoCall", "Call notification sent")
            }
    }


    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScreenshotDetection()
    }
}