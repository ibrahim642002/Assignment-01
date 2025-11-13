package com.example.assignment_01

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class page_7 : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference
    private lateinit var followsRef: DatabaseReference

    private lateinit var searchInput: EditText
    private lateinit var searchResultsContainer: LinearLayout
    private lateinit var filterTop: TextView
    private lateinit var filterAccount: TextView
    private lateinit var filterTags: TextView
    private lateinit var filterPlaces: TextView

    private var currentFilter = "Account" // Default filter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page7)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        usersRef = database.getReference("Users")
        followsRef = database.getReference("follows")

        // Initialize views
        searchInput = findViewById(R.id.searchInput)
        filterTop = findViewById(R.id.filterTop)
        filterAccount = findViewById(R.id.filterAccount)
        filterTags = findViewById(R.id.filterTags)
        filterPlaces = findViewById(R.id.filterPlaces)

        // Convert search display to EditText (or use existing)
        setupSearch()

        // Setup filters
        setupFilters()
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.length >= 2) {
                    searchUsers(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilters() {
        filterTop.setOnClickListener {
            currentFilter = "Top"
            updateFilterUI()
        }

        filterAccount.setOnClickListener {
            currentFilter = "Account"
            updateFilterUI()
            searchUsers(searchInput.text.toString())
        }

        filterTags.setOnClickListener {
            currentFilter = "Tags"
            updateFilterUI()
        }

        filterPlaces.setOnClickListener {
            currentFilter = "Places"
            updateFilterUI()
        }
    }

    private fun updateFilterUI() {
        // Reset all
        filterTop.setTextColor(resources.getColor(android.R.color.black))
        filterAccount.setTextColor(resources.getColor(android.R.color.black))
        filterTags.setTextColor(resources.getColor(android.R.color.black))
        filterPlaces.setTextColor(resources.getColor(android.R.color.black))

        // Highlight selected
        when (currentFilter) {
            "Top" -> filterTop.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            "Account" -> filterAccount.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            "Tags" -> filterTags.setTextColor(resources.getColor(android.R.color.holo_red_dark))
            "Places" -> filterPlaces.setTextColor(resources.getColor(android.R.color.holo_red_dark))
        }
    }

    private fun searchUsers(query: String) {
        if (currentFilter != "Account") return

        val currentUserId = mAuth.currentUser?.uid ?: return

        usersRef.orderByChild("username").startAt(query).endAt(query + "\uf8ff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    clearSearchResults()

                    for (userSnapshot in snapshot.children) {
                        val userId = userSnapshot.key ?: continue
                        if (userId == currentUserId) continue // Skip current user

                        val username = userSnapshot.child("username").value?.toString() ?: ""
                        val firstName = userSnapshot.child("firstName").value?.toString() ?: ""
                        val lastName = userSnapshot.child("lastName").value?.toString() ?: ""
                        val profileImage = userSnapshot.child("profileImage").value?.toString() ?: ""
                        val isOnline = userSnapshot.child("isOnline").value as? Boolean ?: false

                        addSearchResult(userId, username, "$firstName $lastName", profileImage, isOnline)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Search", "Error searching users: ${error.message}")
                }
            })
    }

    private fun clearSearchResults() {
        // Clear the container where search results are displayed
        val container = findViewById<LinearLayout>(R.id.searchResultsContainer)
        container?.removeAllViews()
    }

    private fun addSearchResult(userId: String, username: String, fullName: String, profileImage: String, isOnline: Boolean) {
        val resultView = LayoutInflater.from(this).inflate(R.layout.item_search_result, null)

        val profileImageView = resultView.findViewById<ImageView>(R.id.profileImage)
        val usernameText = resultView.findViewById<TextView>(R.id.usernameText)
        val fullNameText = resultView.findViewById<TextView>(R.id.fullNameText)
        val onlineIndicator = resultView.findViewById<View>(R.id.onlineIndicator)

        usernameText.text = username
        fullNameText.text = fullName

        // Load profile image
        if (profileImage.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(profileImage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                profileImageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e("Search", "Error loading profile image: ${e.message}")
            }
        }

        // Show online status
        onlineIndicator.visibility = if (isOnline) View.VISIBLE else View.GONE

        // Click to view profile
        resultView.setOnClickListener {
            openUserProfile(userId)
        }

        val container = findViewById<LinearLayout>(R.id.searchResultsContainer)
        container?.addView(resultView)
    }

    private fun openUserProfile(userId: String) {
        val intent = Intent(this, UserProfile::class.java)
        intent.putExtra("USER_ID", userId)
        startActivity(intent)
    }
}