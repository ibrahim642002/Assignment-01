package com.example.assignment_01.api

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.assignment_01.database.DatabaseHelper
import com.example.assignment_01.database.PendingRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class ApiClient(private val context: Context) {

    companion object {

        private const val BASE_URL = "http://192.168.59.1/socially/api/"

        private const val PREFS_NAME = "socially_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"

        @Volatile
        private var instance: ApiClient? = null

        fun getInstance(context: Context): ApiClient {
            return instance ?: synchronized(this) {
                instance ?: ApiClient(context.applicationContext).also { instance = it }
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .apply {
                    getAuthToken()?.let { token ->
                        addHeader("Authorization", "Bearer $token")
                    }
                }
                .build()
            chain.proceed(request)
        }
        .build()

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dbHelper = DatabaseHelper.getInstance(context)

    // AUTH TOKEN MANAGEMENT
    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun clearAuthToken() {
        prefs.edit().remove(KEY_AUTH_TOKEN).remove(KEY_USER_ID).apply()
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    // NETWORK CHECK
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    // POST REQUEST
    suspend fun post(endpoint: String, body: JSONObject): ApiResponse = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            queueRequest("POST", endpoint, body.toString())
            return@withContext ApiResponse(false, "No internet connection. Request queued.", null)
        }

        try {
            val requestBody = body.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(BASE_URL + endpoint)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            val json = JSONObject(responseBody)

            ApiResponse(
                success = json.optBoolean("success", false),
                message = json.optString("message", ""),
                data = json.optJSONObject("data")
            )
        } catch (e: Exception) {
            Log.e("ApiClient", "POST error: ${e.message}")
            queueRequest("POST", endpoint, body.toString())
            ApiResponse(false, "Request queued: ${e.message}", null)
        }
    }

    // GET REQUEST - FIXED (No deprecated HttpUrl.parse)
    suspend fun get(endpoint: String, params: Map<String, String>? = null): ApiResponse = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext ApiResponse(false, "No internet connection", null)
        }

        try {
            // Build URL with query parameters
            var url = BASE_URL + endpoint
            if (!params.isNullOrEmpty()) {
                val queryString = params.entries.joinToString("&") { "${it.key}=${it.value}" }
                url += "?$queryString"
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            val json = JSONObject(responseBody)

            ApiResponse(
                success = json.optBoolean("success", false),
                message = json.optString("message", ""),
                data = json.optJSONObject("data")
            )
        } catch (e: Exception) {
            Log.e("ApiClient", "GET error: ${e.message}")
            ApiResponse(false, e.message ?: "Network error", null)
        }
    }

    // FILE UPLOAD
    suspend fun uploadFile(
        endpoint: String,
        file: File,
        additionalData: Map<String, String>? = null
    ): ApiResponse = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            queueFileUpload(endpoint, file.absolutePath, additionalData)
            return@withContext ApiResponse(false, "No internet. Upload queued.", null)
        }

        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    file.name,
                    file.asRequestBody("image/*".toMediaType())
                )
                .apply {
                    additionalData?.forEach { (key, value) ->
                        addFormDataPart(key, value)
                    }
                }
                .build()

            val request = Request.Builder()
                .url(BASE_URL + endpoint)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            val json = JSONObject(responseBody)

            ApiResponse(
                success = json.optBoolean("success", false),
                message = json.optString("message", ""),
                data = json.optJSONObject("data")
            )
        } catch (e: Exception) {
            Log.e("ApiClient", "Upload error: ${e.message}")
            queueFileUpload(endpoint, file.absolutePath, additionalData)
            ApiResponse(false, "Upload queued: ${e.message}", null)
        }
    }

    // DELETE REQUEST
    suspend fun delete(endpoint: String): ApiResponse = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            queueRequest("DELETE", endpoint, null)
            return@withContext ApiResponse(false, "No internet. Request queued.", null)
        }

        try {
            val request = Request.Builder()
                .url(BASE_URL + endpoint)
                .delete()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            val json = JSONObject(responseBody)

            ApiResponse(
                success = json.optBoolean("success", false),
                message = json.optString("message", ""),
                data = json.optJSONObject("data")
            )
        } catch (e: Exception) {
            Log.e("ApiClient", "DELETE error: ${e.message}")
            queueRequest("DELETE", endpoint, null)
            ApiResponse(false, "Request queued: ${e.message}", null)
        }
    }

    // OFFLINE QUEUE
    private fun queueRequest(method: String, endpoint: String, payload: String?) {
        val request = PendingRequest(
            requestType = "api_call",
            endpoint = endpoint,
            method = method,
            payload = payload,
            priority = if (endpoint.contains("message")) 1 else 0
        )
        dbHelper.queueRequest(request)
        Log.d("ApiClient", "Request queued: $method $endpoint")
    }

    private fun queueFileUpload(endpoint: String, filePath: String, additionalData: Map<String, String>?) {
        val request = PendingRequest(
            requestType = "file_upload",
            endpoint = endpoint,
            method = "POST",
            filePath = filePath,
            payload = additionalData?.let { JSONObject(it).toString() },
            priority = 0
        )
        dbHelper.queueRequest(request)
        Log.d("ApiClient", "File upload queued: $filePath")
    }

    suspend fun processPendingRequests() = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            Log.d("ApiClient", "No network, skipping pending requests")
            return@withContext
        }

        val pendingRequests = dbHelper.getPendingRequests()
        Log.d("ApiClient", "Processing ${pendingRequests.size} pending requests")

        for (request in pendingRequests) {
            try {
                val success = when (request.requestType) {
                    "api_call" -> processApiCall(request)
                    "file_upload" -> processFileUpload(request)
                    else -> false
                }

                if (success) {
                    dbHelper.markRequestCompleted(request.requestId)
                } else {
                    dbHelper.markRequestFailed(request.requestId)
                }
            } catch (e: Exception) {
                Log.e("ApiClient", "Error processing request ${request.requestId}: ${e.message}")
                dbHelper.markRequestFailed(request.requestId)
            }
        }

        dbHelper.clearCompletedRequests()
    }

    private suspend fun processApiCall(request: PendingRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBody = request.payload?.toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url(BASE_URL + request.endpoint)
                .apply {
                    when (request.method) {
                        "POST" -> post(requestBody ?: "{}".toRequestBody("application/json".toMediaType()))
                        "DELETE" -> delete()
                        "PUT" -> put(requestBody ?: "{}".toRequestBody("application/json".toMediaType()))
                    }
                }
                .build()

            val response = client.newCall(httpRequest).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("ApiClient", "processApiCall error: ${e.message}")
            false
        }
    }

    private suspend fun processFileUpload(request: PendingRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(request.filePath ?: return@withContext false)
            if (!file.exists()) {
                Log.e("ApiClient", "File not found: ${request.filePath}")
                return@withContext false
            }

            val additionalData = request.payload?.let { JSONObject(it) }
            val response = uploadFile(
                request.endpoint,
                file,
                additionalData?.let { json ->
                    json.keys().asSequence().associateWith { json.getString(it) }
                }
            )
            response.success
        } catch (e: Exception) {
            Log.e("ApiClient", "processFileUpload error: ${e.message}")
            false
        }
    }
}

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val data: JSONObject?
)