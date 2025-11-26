package com.example.assignment_01

data class Notification(
    val notificationId: String = "",
    val toUserId: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromUserProfileImage: String = "", // Base64
    val type: String = "", // "follow", "like", "comment"
    val message: String = "",
    val postImageUrl: String = "", // Base64 for post thumbnail (if applicable)
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)