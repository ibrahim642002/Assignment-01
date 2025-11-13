package com.example.assignment_01

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: MutableList<Message>,
    private val onMessageLongClick: (Message) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount() = messages.size

    inner class SentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.messageText)
        private val messageTime: TextView = view.findViewById(R.id.messageTime)
        private val messageImage: ImageView = view.findViewById(R.id.messageImage)
        private val editedLabel: TextView = view.findViewById(R.id.editedLabel)

        fun bind(message: Message) {
            if (message.isDeleted) {
                messageText.text = "This message was deleted"
                messageText.alpha = 0.5f
                messageImage.visibility = View.GONE
            } else {
                if (message.messageType == "image" && message.imageUrl.isNotEmpty()) {
                    messageImage.visibility = View.VISIBLE
                    messageText.visibility = View.GONE
                    try {
                        val imageBytes = Base64.decode(message.imageUrl, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        messageImage.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    messageImage.visibility = View.GONE
                    messageText.visibility = View.VISIBLE
                    messageText.text = message.messageText
                    messageText.alpha = 1f
                }

                if (message.isEdited) {
                    editedLabel.visibility = View.VISIBLE
                } else {
                    editedLabel.visibility = View.GONE
                }
            }

            messageTime.text = formatTime(message.timestamp)

            // Long click for edit/delete (only if can edit)
            itemView.setOnLongClickListener {
                if (message.canEdit() && !message.isDeleted) {
                    onMessageLongClick(message)
                }
                true
            }
        }
    }

    inner class ReceivedMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.messageText)
        private val messageTime: TextView = view.findViewById(R.id.messageTime)
        private val messageImage: ImageView = view.findViewById(R.id.messageImage)

        fun bind(message: Message) {
            if (message.messageType == "image" && message.imageUrl.isNotEmpty()) {
                messageImage.visibility = View.VISIBLE
                messageText.visibility = View.GONE
                try {
                    val imageBytes = Base64.decode(message.imageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    messageImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                messageImage.visibility = View.GONE
                messageText.visibility = View.VISIBLE
                messageText.text = message.messageText
            }

            messageTime.text = formatTime(message.timestamp)
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun updateMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}