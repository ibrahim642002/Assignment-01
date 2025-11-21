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

    private lateinit var searchInput: EditText
    private lateinit var searchResultsContainer: LinearLayout
    private lateinit var filterAccount: TextView
    private lateinit var clearSearch: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page7)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://assignment01-7a5e4-default-rtdb.firebaseio.com/")
        usersRef = database.getReference("Users")

        // Initialize views
        searchInput = findViewById(R.id.searchInput)
        searchResultsContainer = findViewById(R.id.searchResultsContainer)
        filterAccount = findViewById(R.id.filterAccount)
        clearSearch = findViewById(R.id.clearSearch)

        setupSearch()

        // DEBUG: Show all users on load
        loadAllUsers()
    }

    private fun setupSearch() {
        // Load all users immediately when page opens
        loadAllUsers()

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                Log.d("SEARCH_DEBUG", "Searching for: '$query'")

                if (query.length >= 2) {
                    searchUsers(query)
                } else if (query.isEmpty()) {
                    loadAllUsers() // Show all users when search is empty
                } else {
                    clearSearchResults()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        clearSearch.setOnClickListener {
            searchInput.text.clear()
            loadAllUsers()
        }
    }

    private fun loadAllUsers() {
        Log.d("SEARCH_DEBUG", "Loading all users...")
        val currentUserId = mAuth.currentUser?.uid ?: return

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                clearSearchResults()
                var count = 0

                Log.d("SEARCH_DEBUG", "Total users in database: ${snapshot.childrenCount}")

                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    if (userId == currentUserId) continue

                    val username = userSnapshot.child("username").value?.toString() ?: ""
                    val firstName = userSnapshot.child("firstName").value?.toString() ?: ""
                    val lastName = userSnapshot.child("lastName").value?.toString() ?: ""
                    val profileImage = userSnapshot.child("profileImage").value?.toString() ?: ""
                    val isOnline = userSnapshot.child("isOnline").value as? Boolean ?: false

                    Log.d("SEARCH_DEBUG", "Found user: $username (ID: $userId)")

                    addSearchResult(userId, username, "$firstName $lastName", profileImage, isOnline)
                    count++
                }

                Log.d("SEARCH_DEBUG", "Displayed $count users")

                if (count == 0) {
                    showMessage("No other users found. Create another account to test!")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SEARCH_DEBUG", "Error: ${error.message}")
                Toast.makeText(this@page_7, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchUsers(query: String) {
        val currentUserId = mAuth.currentUser?.uid ?: return
        val lowerQuery = query.lowercase()

        Log.d("SEARCH_DEBUG", "Searching for: '$lowerQuery'")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                clearSearchResults()
                var foundResults = false

                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue
                    if (userId == currentUserId) continue

                    val username = userSnapshot.child("username").value?.toString() ?: ""
                    val firstName = userSnapshot.child("firstName").value?.toString() ?: ""
                    val lastName = userSnapshot.child("lastName").value?.toString() ?: ""
                    val profileImage = userSnapshot.child("profileImage").value?.toString() ?: ""
                    val isOnline = userSnapshot.child("isOnline").value as? Boolean ?: false

                    // Search in username (case-insensitive)
                    if (username.lowercase().contains(lowerQuery)) {
                        Log.d("SEARCH_DEBUG", "Match found: $username")
                        addSearchResult(userId, username, "$firstName $lastName", profileImage, isOnline)
                        foundResults = true
                    }
                }

                if (!foundResults) {
                    Log.d("SEARCH_DEBUG", "No results found for: $query")
                    showMessage("No users found matching '$query'")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SEARCH_DEBUG", "Search error: ${error.message}")
                Toast.makeText(this@page_7, "Search failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun clearSearchResults() {
        searchResultsContainer.removeAllViews()
    }

    private fun showMessage(message: String) {
        clearSearchResults()
        val textView = TextView(this)
        textView.text = message
        textView.textSize = 16f
        textView.setPadding(16, 32, 16, 32)
        textView.gravity = android.view.Gravity.CENTER
        searchResultsContainer.addView(textView)
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
                Log.e("SEARCH_DEBUG", "Error loading image: ${e.message}")
            }
        }

        // Show online status
        onlineIndicator.visibility = if (isOnline) View.VISIBLE else View.GONE

        // Click to view profile
        resultView.setOnClickListener {
            Log.d("SEARCH_DEBUG", "Clicked on user: $username")
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