package com.example.assignment_01

data class ChatPreview(
    val chatId: String = "",
    val otherUserId: String = "",
    val otherUsername: String = "",
    val otherUserProfileImage: String = "", // Base64
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val lastMessageSenderId: String = "",
    val isOnline: Boolean = false,
    val unreadCount: Int = 0 // For future use
)