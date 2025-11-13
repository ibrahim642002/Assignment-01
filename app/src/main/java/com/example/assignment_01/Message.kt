package com.example.assignment_01

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val senderName: String = "",
    val messageText: String = "",
    val imageUrl: String = "", // Base64 for images
    val messageType: String = "text", // "text", "image", "post"
    val timestamp: Long = 0L,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false
) {
    fun canEdit(): Boolean {
        val currentTime = System.currentTimeMillis()
        val fiveMinutes = 5 * 60 * 1000
        return (currentTime - timestamp) < fiveMinutes && !isDeleted
    }
}