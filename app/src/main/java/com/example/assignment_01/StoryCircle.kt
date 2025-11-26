package com.example.assignment_01

data class StoryCircle(
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "", // Base64
    val stories: List<Story> = emptyList(),
    val latestStoryTimestamp: Long = 0L
)