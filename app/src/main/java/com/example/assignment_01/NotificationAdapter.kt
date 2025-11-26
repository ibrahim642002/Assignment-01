package com.example.assignment_01

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(
    private val notifications: MutableList<Notification>,
    private val onFollowBackClick: (Notification) -> Unit,
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.bind(notification)
    }

    override fun getItemCount() = notifications.size

    inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val profileImage: ImageView = view.findViewById(R.id.profileImage)
        private val notificationText: TextView = view.findViewById(R.id.notificationText)
        private val notificationTime: TextView = view.findViewById(R.id.notificationTime)
        private val followBackButton: Button = view.findViewById(R.id.followBackButton)
        private val postThumbnail: ImageView = view.findViewById(R.id.postThumbnail)

        fun bind(notification: Notification) {
            // Set notification message
            notificationText.text = notification.message

            // Set timestamp
            notificationTime.text = getTimeAgo(notification.timestamp)

            // Load profile image
            if (notification.fromUserProfileImage.isNotEmpty()) {
                try {
                    val imageBytes = Base64.decode(notification.fromUserProfileImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    profileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Log.e("NotificationAdapter", "Error loading profile image: ${e.message}")
                }
            }

            // Handle different notification types
            when (notification.type) {
                "follow" -> {
                    followBackButton.visibility = View.VISIBLE
                    postThumbnail.visibility = View.GONE

                    followBackButton.setOnClickListener {
                        onFollowBackClick(notification)
                    }
                }
                "like", "comment" -> {
                    followBackButton.visibility = View.GONE
                    postThumbnail.visibility = View.VISIBLE

                    // Load post thumbnail if available
                    if (notification.postImageUrl.isNotEmpty()) {
                        try {
                            val imageBytes = Base64.decode(notification.postImageUrl, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            postThumbnail.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.e("NotificationAdapter", "Error loading post thumbnail: ${e.message}")
                        }
                    }
                }
                else -> {
                    followBackButton.visibility = View.GONE
                    postThumbnail.visibility = View.GONE
                }
            }

            // Click listener for entire notification
            itemView.setOnClickListener {
                onNotificationClick(notification)
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000}m"
                diff < 86400000 -> "${diff / 3600000}h"
                diff < 604800000 -> "${diff / 86400000}d"
                else -> "${diff / 604800000}w"
            }
        }
    }

    fun updateNotifications(newNotifications: List<Notification>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }
}