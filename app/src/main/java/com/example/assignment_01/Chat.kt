package com.example.assignment_01

data class Chat(
    val chatId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0
)