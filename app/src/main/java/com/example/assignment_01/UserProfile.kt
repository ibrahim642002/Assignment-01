package com.example.assignment_01

data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val profileImage: String = "", // Base64
    val bio: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)