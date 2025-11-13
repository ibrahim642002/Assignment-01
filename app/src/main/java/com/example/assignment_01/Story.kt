package com.example.assignment_01

data class Story(
    val storyId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val imageUrl: String = "", // Base64 encoded image
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L
)