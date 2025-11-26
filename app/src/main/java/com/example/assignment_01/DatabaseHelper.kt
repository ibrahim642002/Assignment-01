package com.example.assignment_01.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log


class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "socially_offline.db"
        private const val DATABASE_VERSION = 1

        // Tables
        private const val TABLE_CACHED_POSTS = "cached_posts"
        private const val TABLE_CACHED_STORIES = "cached_stories"
        private const val TABLE_CACHED_MESSAGES = "cached_messages"
        private const val TABLE_CACHED_USERS = "cached_users"
        private const val TABLE_PENDING_REQUESTS = "pending_requests"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Cached Posts Table
        db.execSQL("""
            CREATE TABLE $TABLE_CACHED_POSTS (
                post_id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                username TEXT NOT NULL,
                profile_image_url TEXT,
                image_url TEXT NOT NULL,
                caption TEXT,
                location TEXT,
                likes_count INTEGER DEFAULT 0,
                comments_count INTEGER DEFAULT 0,
                is_liked INTEGER DEFAULT 0,
                created_at TEXT NOT NULL,
                cached_at INTEGER NOT NULL
            )
        """)

        // Cached Stories Table
        db.execSQL("""
            CREATE TABLE $TABLE_CACHED_STORIES (
                story_id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                username TEXT NOT NULL,
                image_url TEXT NOT NULL,
                created_at TEXT NOT NULL,
                expires_at TEXT NOT NULL,
                cached_at INTEGER NOT NULL
            )
        """)

        // Cached Messages Table
        db.execSQL("""
            CREATE TABLE $TABLE_CACHED_MESSAGES (
                message_id TEXT PRIMARY KEY,
                sender_id TEXT NOT NULL,
                receiver_id TEXT NOT NULL,
                sender_name TEXT NOT NULL,
                message_text TEXT,
                image_url TEXT,
                message_type TEXT DEFAULT 'text',
                is_edited INTEGER DEFAULT 0,
                is_deleted INTEGER DEFAULT 0,
                created_at INTEGER NOT NULL,
                cached_at INTEGER NOT NULL
            )
        """)

        // Cached Users Table
        db.execSQL("""
            CREATE TABLE $TABLE_CACHED_USERS (
                user_id TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                first_name TEXT,
                last_name TEXT,
                profile_image_url TEXT,
                bio TEXT,
                is_online INTEGER DEFAULT 0,
                cached_at INTEGER NOT NULL
            )
        """)

        // Pending Requests Queue (for offline operations)
        db.execSQL("""
            CREATE TABLE $TABLE_PENDING_REQUESTS (
                request_id INTEGER PRIMARY KEY AUTOINCREMENT,
                request_type TEXT NOT NULL,
                endpoint TEXT NOT NULL,
                method TEXT NOT NULL,
                payload TEXT,
                file_path TEXT,
                priority INTEGER DEFAULT 0,
                retry_count INTEGER DEFAULT 0,
                max_retries INTEGER DEFAULT 3,
                created_at INTEGER NOT NULL,
                status TEXT DEFAULT 'pending'
            )
        """)

        // Create indexes
        db.execSQL("CREATE INDEX idx_posts_created ON $TABLE_CACHED_POSTS(created_at DESC)")
        db.execSQL("CREATE INDEX idx_messages_chat ON $TABLE_CACHED_MESSAGES(sender_id, receiver_id, created_at)")
        db.execSQL("CREATE INDEX idx_pending_status ON $TABLE_PENDING_REQUESTS(status, priority, created_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older tables if existed
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHED_POSTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHED_STORIES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHED_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHED_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING_REQUESTS")
        onCreate(db)
    }

    // ==================== CACHED POSTS ====================

    fun cachePosts(posts: List<CachedPost>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (post in posts) {
                val values = ContentValues().apply {
                    put("post_id", post.postId)
                    put("user_id", post.userId)
                    put("username", post.username)
                    put("profile_image_url", post.profileImageUrl)
                    put("image_url", post.imageUrl)
                    put("caption", post.caption)
                    put("location", post.location)
                    put("likes_count", post.likesCount)
                    put("comments_count", post.commentsCount)
                    put("is_liked", if (post.isLiked) 1 else 0)
                    put("created_at", post.createdAt)
                    put("cached_at", System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_CACHED_POSTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getCachedPosts(limit: Int = 20): List<CachedPost> {
        val posts = mutableListOf<CachedPost>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_CACHED_POSTS,
            null,
            null,
            null,
            null,
            null,
            "created_at DESC",
            limit.toString()
        )

        cursor.use {
            while (it.moveToNext()) {
                posts.add(CachedPost(
                    postId = it.getString(it.getColumnIndexOrThrow("post_id")),
                    userId = it.getString(it.getColumnIndexOrThrow("user_id")),
                    username = it.getString(it.getColumnIndexOrThrow("username")),
                    profileImageUrl = it.getString(it.getColumnIndexOrThrow("profile_image_url")),
                    imageUrl = it.getString(it.getColumnIndexOrThrow("image_url")),
                    caption = it.getString(it.getColumnIndexOrThrow("caption")),
                    location = it.getString(it.getColumnIndexOrThrow("location")),
                    likesCount = it.getInt(it.getColumnIndexOrThrow("likes_count")),
                    commentsCount = it.getInt(it.getColumnIndexOrThrow("comments_count")),
                    isLiked = it.getInt(it.getColumnIndexOrThrow("is_liked")) == 1,
                    createdAt = it.getString(it.getColumnIndexOrThrow("created_at"))
                ))
            }
        }
        return posts
    }

    fun clearOldPosts(daysOld: Int = 7) {
        val db = writableDatabase
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        db.delete(TABLE_CACHED_POSTS, "cached_at < ?", arrayOf(cutoffTime.toString()))
    }

    // ==================== CACHED MESSAGES ====================

    fun cacheMessages(messages: List<CachedMessage>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (message in messages) {
                val values = ContentValues().apply {
                    put("message_id", message.messageId)
                    put("sender_id", message.senderId)
                    put("receiver_id", message.receiverId)
                    put("sender_name", message.senderName)
                    put("message_text", message.messageText)
                    put("image_url", message.imageUrl)
                    put("message_type", message.messageType)
                    put("is_edited", if (message.isEdited) 1 else 0)
                    put("is_deleted", if (message.isDeleted) 1 else 0)
                    put("created_at", message.createdAt)
                    put("cached_at", System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_CACHED_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getCachedMessages(userId1: String, userId2: String): List<CachedMessage> {
        val messages = mutableListOf<CachedMessage>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_CACHED_MESSAGES,
            null,
            "(sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)",
            arrayOf(userId1, userId2, userId2, userId1),
            null,
            null,
            "created_at ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                messages.add(CachedMessage(
                    messageId = it.getString(it.getColumnIndexOrThrow("message_id")),
                    senderId = it.getString(it.getColumnIndexOrThrow("sender_id")),
                    receiverId = it.getString(it.getColumnIndexOrThrow("receiver_id")),
                    senderName = it.getString(it.getColumnIndexOrThrow("sender_name")),
                    messageText = it.getString(it.getColumnIndexOrThrow("message_text")),
                    imageUrl = it.getString(it.getColumnIndexOrThrow("image_url")),
                    messageType = it.getString(it.getColumnIndexOrThrow("message_type")),
                    isEdited = it.getInt(it.getColumnIndexOrThrow("is_edited")) == 1,
                    isDeleted = it.getInt(it.getColumnIndexOrThrow("is_deleted")) == 1,
                    createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))
                ))
            }
        }
        return messages
    }

    // ==================== PENDING REQUESTS QUEUE ====================

    fun queueRequest(request: PendingRequest): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("request_type", request.requestType)
            put("endpoint", request.endpoint)
            put("method", request.method)
            put("payload", request.payload)
            put("file_path", request.filePath)
            put("priority", request.priority)
            put("created_at", System.currentTimeMillis())
            put("status", "pending")
        }
        return db.insert(TABLE_PENDING_REQUESTS, null, values)
    }

    fun getPendingRequests(): List<PendingRequest> {
        val requests = mutableListOf<PendingRequest>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_PENDING_REQUESTS,
            null,
            "status = ? AND retry_count < max_retries",
            arrayOf("pending"),
            null,
            null,
            "priority DESC, created_at ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                requests.add(PendingRequest(
                    requestId = it.getLong(it.getColumnIndexOrThrow("request_id")),
                    requestType = it.getString(it.getColumnIndexOrThrow("request_type")),
                    endpoint = it.getString(it.getColumnIndexOrThrow("endpoint")),
                    method = it.getString(it.getColumnIndexOrThrow("method")),
                    payload = it.getString(it.getColumnIndexOrThrow("payload")),
                    filePath = it.getString(it.getColumnIndexOrThrow("file_path")),
                    priority = it.getInt(it.getColumnIndexOrThrow("priority")),
                    retryCount = it.getInt(it.getColumnIndexOrThrow("retry_count")),
                    maxRetries = it.getInt(it.getColumnIndexOrThrow("max_retries"))
                ))
            }
        }
        return requests
    }

    fun markRequestCompleted(requestId: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("status", "completed")
        }
        db.update(TABLE_PENDING_REQUESTS, values, "request_id = ?", arrayOf(requestId.toString()))
    }

    fun markRequestFailed(requestId: Long) {
        val db = writableDatabase
        db.execSQL("UPDATE $TABLE_PENDING_REQUESTS SET retry_count = retry_count + 1 WHERE request_id = ?", arrayOf(requestId))
    }

    fun deletePendingRequest(requestId: Long) {
        val db = writableDatabase
        db.delete(TABLE_PENDING_REQUESTS, "request_id = ?", arrayOf(requestId.toString()))
    }

    fun clearCompletedRequests() {
        val db = writableDatabase
        db.delete(TABLE_PENDING_REQUESTS, "status = ?", arrayOf("completed"))
    }
}

// Data Classes for SQLite
data class CachedPost(
    val postId: String,
    val userId: String,
    val username: String,
    val profileImageUrl: String?,
    val imageUrl: String,
    val caption: String?,
    val location: String?,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean,
    val createdAt: String
)

data class CachedMessage(
    val messageId: String,
    val senderId: String,
    val receiverId: String,
    val senderName: String,
    val messageText: String?,
    val imageUrl: String?,
    val messageType: String,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val createdAt: Long
)

data class PendingRequest(
    val requestId: Long = 0,
    val requestType: String,
    val endpoint: String,
    val method: String,
    val payload: String? = null,
    val filePath: String? = null,
    val priority: Int = 0,
    val retryCount: Int = 0,
    val maxRetries: Int = 3
)