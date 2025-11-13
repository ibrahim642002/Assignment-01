package com.example.assignment_01

data class FollowRequest(
    val requestId: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val toUserId: String = "",
    val timestamp: Long = 0L,
    val status: String = "pending" // "pending", "accepted", "rejected"
)