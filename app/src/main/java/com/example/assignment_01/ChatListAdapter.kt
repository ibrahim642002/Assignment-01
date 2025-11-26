package com.example.assignment_01

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatListAdapter(
    private val chats: MutableList<ChatPreview>,
    private val onChatClick: (ChatPreview) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_preview, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        holder.bind(chat)
    }

    override fun getItemCount() = chats.size

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val profileImage: ImageView = view.findViewById(R.id.profileImage)
        private val usernameText: TextView = view.findViewById(R.id.usernameText)
        private val lastMessageText: TextView = view.findViewById(R.id.lastMessageText)
        private val timestampText: TextView = view.findViewById(R.id.timestampText)
        private val onlineIndicator: View = view.findViewById(R.id.onlineIndicator)

        fun bind(chat: ChatPreview) {
            // Set username
            usernameText.text = chat.otherUsername

            // Set last message
            if (chat.lastMessage.isNotEmpty()) {
                lastMessageText.text = chat.lastMessage
            } else {
                lastMessageText.text = "No messages yet"
            }

            // Set timestamp
            timestampText.text = getTimeAgo(chat.lastMessageTimestamp)

            // Load profile image
            if (chat.otherUserProfileImage.isNotEmpty()) {
                try {
                    val imageBytes = Base64.decode(chat.otherUserProfileImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    profileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Log.e("ChatListAdapter", "Error loading profile image: ${e.message}")
                }
            }

            // Show/hide online indicator
            onlineIndicator.visibility = if (chat.isOnline) View.VISIBLE else View.GONE

            // Click listener
            itemView.setOnClickListener {
                onChatClick(chat)
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            if (timestamp == 0L) return ""

            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "now"
                diff < 3600000 -> "${diff / 60000}m"
                diff < 86400000 -> "${diff / 3600000}h"
                diff < 604800000 -> "${diff / 86400000}d"
                else -> "${diff / 604800000}w"
            }
        }
    }

    // ✅ IMPORTANT: This method must be present
    fun updateChats(newChats: List<ChatPreview>) {
        chats.clear()
        chats.addAll(newChats)
        notifyDataSetChanged()
    }
}