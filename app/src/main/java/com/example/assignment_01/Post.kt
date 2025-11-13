package com.example.assignment_01

data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val imageUrl: String = "", // Base64 encoded image
    val caption: String = "",
    val location: String = "",
    val timestamp: Long = 0L,
    val likes: Int = 0,
    val comments: Int = 0
)