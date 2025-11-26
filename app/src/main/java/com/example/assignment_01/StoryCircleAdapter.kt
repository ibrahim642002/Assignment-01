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

class StoryCircleAdapter(
    private val storyCircles: MutableList<StoryCircle>,
    private val onStoryClick: (StoryCircle) -> Unit
) : RecyclerView.Adapter<StoryCircleAdapter.StoryCircleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryCircleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story_circle, parent, false)
        return StoryCircleViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryCircleViewHolder, position: Int) {
        val storyCircle = storyCircles[position]
        holder.bind(storyCircle)
    }

    override fun getItemCount() = storyCircles.size

    inner class StoryCircleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val circleImage: ImageView = view.findViewById(R.id.storyCircleImage)
        private val username: TextView = view.findViewById(R.id.storyCircleUsername)

        fun bind(storyCircle: StoryCircle) {
            // Set username
            username.text = storyCircle.username

            // Load profile image or story thumbnail
            val imageToLoad = if (storyCircle.userProfileImage.isNotEmpty()) {
                storyCircle.userProfileImage
            } else if (storyCircle.stories.isNotEmpty()) {
                storyCircle.stories.first().imageUrl
            } else {
                ""
            }

            if (imageToLoad.isNotEmpty()) {
                try {
                    val imageBytes = Base64.decode(imageToLoad, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    circleImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Log.e("StoryCircleAdapter", "Error loading image: ${e.message}")
                }
            }

            // Click listener
            itemView.setOnClickListener {
                onStoryClick(storyCircle)
            }
        }
    }

    fun updateStories(newStoryCircles: List<StoryCircle>) {
        storyCircles.clear()
        storyCircles.addAll(newStoryCircles)
        notifyDataSetChanged()
    }
}