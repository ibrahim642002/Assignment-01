package com.example.assignment_01

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class page_8 : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var messagesRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference

    private lateinit var chatListRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatListAdapter  // Changed variable name to avoid confusion
    private val chatsList = mutableListOf<ChatPreview>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page8)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        messagesRef = database.getReference("messages")
        usersRef = database.getReference("Users")

        // Initialize RecyclerView
        chatListRecyclerView = findViewById(R.id.chatListRecyclerView)
        chatListRecyclerView.layoutManager = LinearLayoutManager(this)

        chatAdapter = ChatListAdapter(chatsList) { chat ->
            openChat(chat)
        }
        chatListRecyclerView.adapter = chatAdapter

        // Load chats
        loadChats()
    }

    private fun loadChats() {
        val currentUserId = mAuth.currentUser?.uid ?: return

        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatsMap = mutableMapOf<String, ChatPreview>()

                for (chatSnapshot in snapshot.children) {
                    val chatId = chatSnapshot.key ?: continue

                    // Check if current user is part of this chat
                    if (!chatId.contains(currentUserId)) continue

                    // Get other user ID
                    val otherUserId = if (chatId.startsWith(currentUserId)) {
                        chatId.substringAfter("_")
                    } else {
                        chatId.substringBefore("_")
                    }

                    // Get last message
                    var lastMessage = ""
                    var lastMessageTimestamp = 0L
                    var lastMessageSenderId = ""

                    val messagesList = mutableListOf<Message>()
                    for (messageSnapshot in chatSnapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        if (message != null) {
                            messagesList.add(message)
                        }
                    }

                    if (messagesList.isNotEmpty()) {
                        messagesList.sortBy { it.timestamp }
                        val lastMsg = messagesList.last()
                        lastMessage = if (lastMsg.messageType == "image") {
                            "📷 Photo"
                        } else {
                            lastMsg.messageText
                        }
                        lastMessageTimestamp = lastMsg.timestamp
                        lastMessageSenderId = lastMsg.senderId
                    }

                    // Create ChatPreview
                    val chatPreview = ChatPreview(
                        chatId = chatId,
                        otherUserId = otherUserId,
                        otherUsername = "", // Will be loaded separately
                        otherUserProfileImage = "",
                        lastMessage = lastMessage,
                        lastMessageTimestamp = lastMessageTimestamp,
                        lastMessageSenderId = lastMessageSenderId,
                        isOnline = false
                    )

                    chatsMap[chatId] = chatPreview
                }

                // Load user details for each chat
                loadUserDetailsForChats(chatsMap)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatInbox", "Error: ${error.message}")
                Toast.makeText(this@page_8, "Failed to load chats", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserDetailsForChats(chatsMap: Map<String, ChatPreview>) {
        val loadedChats = mutableListOf<ChatPreview>()
        var loadedCount = 0

        for ((chatId, chat) in chatsMap) {
            usersRef.child(chat.otherUserId).get()
                .addOnSuccessListener { snapshot ->
                    val username = snapshot.child("username").value?.toString() ?: "User"
                    val profileImage = snapshot.child("profileImage").value?.toString() ?: ""
                    val isOnline = snapshot.child("isOnline").value as? Boolean ?: false

                    val updatedChat = chat.copy(
                        otherUsername = username,
                        otherUserProfileImage = profileImage,
                        isOnline = isOnline
                    )

                    loadedChats.add(updatedChat)
                    loadedCount++

                    // When all loaded, update adapter
                    if (loadedCount == chatsMap.size) {
                        // Sort by most recent message
                        loadedChats.sortByDescending { it.lastMessageTimestamp }
                        chatAdapter.updateChats(loadedChats)
                        Log.d("ChatInbox", "Loaded ${loadedChats.size} chats")
                    }
                }
                .addOnFailureListener {
                    loadedCount++
                    if (loadedCount == chatsMap.size) {
                        loadedChats.sortByDescending { it.lastMessageTimestamp }
                        chatAdapter.updateChats(loadedChats)
                    }
                }
        }

        if (chatsMap.isEmpty()) {
            chatAdapter.updateChats(emptyList())
            Log.d("ChatInbox", "No chats found")
        }
    }

    private fun openChat(chat: ChatPreview) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("USER_ID", chat.otherUserId)
        intent.putExtra("USER_NAME", chat.otherUsername)
        startActivity(intent)
    }
}