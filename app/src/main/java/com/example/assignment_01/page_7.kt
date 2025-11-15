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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class page_7 : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference
    private lateinit var followsRef: DatabaseReference
    private lateinit var followRequestsRef: DatabaseReference

    private lateinit var searchInput: EditText
    private lateinit var searchResultsContainer: LinearLayout
    private lateinit var filterTop: TextView
    private lateinit var filterAccount: TextView
    private lateinit var filterTags: TextView
    private lateinit var filterPlaces: TextView
    private lateinit var clearSearch: TextView

    private var currentFilter = "Account" // Default filter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page7)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        usersRef = database.getReference("Users")
        followsRef = database.getReference("follows")
        followRequestsRef = database.getReference("followRequests")

        // Initialize views
        searchInput = findViewById(R.id.searchInput)
        searchResultsContainer = findViewById(R.id.searchResultsContainer)
        filterTop = findViewById(R.id.filterTop)
        filterAccount = findViewById(R.id.filterAccount)
        filterTags = findViewById(R.id.filterTags)
        filterPlaces = findViewById(R.id.filterPlaces)
        clearSearch = findViewById(R.id.clearSearch)

        setupSearch()
        setupFilters()
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.length >= 2) {
                    searchUsers(query)
                } else {
                    clearSearchResults()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        clearSearch.setOnClickListener {
            searchInput.text.clear()
            clearSearchResults()
        }
    }

    private fun setupFilters() {
        filterTop.setOnClickListener {
            currentFilter = "Top"
            updateFilterUI()
        }

        filterAccount.setOnClickListener {
            currentFilter = "Account"
            updateFilterUI()
            if (searchInput.text.toString().trim().length >= 2) {
                searchUsers(searchInput.text.toString())
            }
        }

        filterTags.setOnClickListener {
            currentFilter = "Tags"
            updateFilterUI()
            Toast.makeText(this, "Tags search not implemented yet", Toast.LENGTH_SHORT).show()
        }

        filterPlaces.setOnClickListener {
            currentFilter = "Places"
            updateFilterUI()
            Toast.makeText(this, "Places search not implemented yet", Toast.LENGTH_SHORT).show()
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

        // Search by username
        usersRef.orderByChild("username")
            .startAt(query.lowercase())
            .endAt(query.lowercase() + "\uf8ff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    clearSearchResults()
                    var foundResults = false

                    for (userSnapshot in snapshot.children) {
                        val userId = userSnapshot.key ?: continue
                        if (userId == currentUserId) continue // Skip current user

                        val username = userSnapshot.child("username").value?.toString() ?: ""
                        val firstName = userSnapshot.child("firstName").value?.toString() ?: ""
                        val lastName = userSnapshot.child("lastName").value?.toString() ?: ""
                        val profileImage = userSnapshot.child("profileImage").value?.toString() ?: ""
                        val isOnline = userSnapshot.child("isOnline").value as? Boolean ?: false

                        // Check if username contains the query (case-insensitive)
                        if (username.lowercase().contains(query.lowercase())) {
                            addSearchResult(userId, username, "$firstName $lastName", profileImage, isOnline)
                            foundResults = true
                        }
                    }

                    if (!foundResults) {
                        showNoResults()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Search", "Error searching users: ${error.message}")
                    Toast.makeText(this@page_7, "Search failed: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun clearSearchResults() {
        searchResultsContainer.removeAllViews()
    }

    private fun showNoResults() {
        clearSearchResults()
        val noResultsView = TextView(this)
        noResultsView.text = "No users found"
        noResultsView.textSize = 16f
        noResultsView.setPadding(16, 32, 16, 32)
        noResultsView.gravity = android.view.Gravity.CENTER
        searchResultsContainer.addView(noResultsView)
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

        searchResultsContainer.addView(resultView)
    }

    private fun openUserProfile(userId: String) {
        val intent = Intent(this, UserProfileActivity::class.java)
        intent.putExtra("USER_ID", userId)
        startActivity(intent)
    }
}