package com.balungpisah.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.balungpisah.data.api.AttachmentApiService
import com.balungpisah.data.models.*
import com.balungpisah.data.network.NetworkClient
import com.balungpisah.data.network.SSEClient
import com.balungpisah.data.network.SSEEvent
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.util.*

class ChatRepository(private val context: Context) {
    
    private val sseClient = SSEClient.getInstance(context)
    private val attachmentApi: AttachmentApiService
    
    private val _threads = MutableStateFlow<List<ChatThread>>(emptyList())
    val threads: StateFlow<List<ChatThread>> = _threads
    
    private val _currentThreadId = MutableStateFlow<String?>(null)
    val currentThreadId: StateFlow<String?> = _currentThreadId
    
    private val _attachments = MutableStateFlow<Map<String, List<ThreadAttachment>>>(emptyMap())
    val attachments: StateFlow<Map<String, List<ThreadAttachment>>> = _attachments
    
    private val _rateLimitStatus = MutableStateFlow<UserRateLimitStatus?>(null)
    val rateLimitStatus: StateFlow<UserRateLimitStatus?> = _rateLimitStatus
    
    private val _lockedThreads = MutableStateFlow<Set<String>>(emptySet())

    init {
        // Initialize Retrofit for attachment API using NetworkClient
        val retrofit = NetworkClient.getInstance(context).getRetrofit()
        attachmentApi = retrofit.create(AttachmentApiService::class.java)
    }
    
    companion object {
        @Volatile
        private var instance: ChatRepository? = null
        
        fun getInstance(context: Context): ChatRepository {
            return instance ?: synchronized(this) {
                instance ?: ChatRepository(context.applicationContext).also { instance = it }
            }
        }
    }
    
    suspend fun checkRateLimit(): Result<UserRateLimitStatus> {
        return try {
            val response = attachmentApi.getRateLimit()
            if (response.isSuccessful && response.body()?.success == true) {
                val status = response.body()!!.data!!
                _rateLimitStatus.value = status
                Result.success(status)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to check rate limit"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun loadThreads(): Result<List<ChatThread>> {
        return try {
            val response = attachmentApi.getThreads()
            if (response.isSuccessful && response.body()?.success == true) {
                val threadResponses = response.body()!!.data!!
                val threads = threadResponses.map { threadResponse ->
                    ChatThread(
                        id = threadResponse.id,
                        title = threadResponse.title ?: "Chat Baru",
                        lastMessage = null,
                        createdAt = parseTimestamp(threadResponse.createdAt),
                        updatedAt = parseTimestamp(threadResponse.updatedAt)
                    )
                }
                _threads.value = threads
                Result.success(threads)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load threads"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun loadThreadDetail(threadId: String): Result<ThreadDetailResponse> {
        return try {
            val response = attachmentApi.getThreadDetail(threadId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load thread detail"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun loadAttachments(threadId: String): Result<List<ThreadAttachment>> {
        return try {
            val response = attachmentApi.getAttachments(threadId)
            if (response.isSuccessful && response.body()?.success == true) {
                val attachmentList = response.body()!!.data!!
                _attachments.value = _attachments.value.toMutableMap().apply {
                    put(threadId, attachmentList)
                }
                Result.success(attachmentList)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to load attachments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAttachmentCount(threadId: String): Result<AttachmentCountResponse> {
        return try {
            val response = attachmentApi.getAttachmentCount(threadId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to get attachment count"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadAttachment(threadId: String, uri: Uri): Result<ThreadAttachment> {
        return try {
            val file = uriToFile(uri)
            val requestFile = file.asRequestBody(getMimeType(uri)?.toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            
            val response = attachmentApi.uploadAttachment(threadId, body)
            
            // Clean up temp file
            file.delete()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val attachment = response.body()!!.data!!
                // Update local attachments
                _attachments.value = _attachments.value.toMutableMap().apply {
                    val current = get(threadId) ?: emptyList()
                    put(threadId, current + attachment)
                }
                Result.success(attachment)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to upload attachment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteAttachment(threadId: String, attachmentId: String): Result<Boolean> {
        return try {
            val response = attachmentApi.deleteAttachment(threadId, attachmentId)
            if (response.isSuccessful && response.body()?.success == true) {
                // Update local attachments
                _attachments.value = _attachments.value.toMutableMap().apply {
                    val current = get(threadId) ?: emptyList()
                    put(threadId, current.filter { it.id != attachmentId })
                }
                Result.success(true)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to delete attachment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun uriToFile(uri: Uri): File {
        val contentResolver = context.contentResolver
        val tempFile = File.createTempFile("upload", ".tmp", context.cacheDir)
        
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        
        return tempFile
    }
    
    private fun getMimeType(uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }
    
    private fun parseTimestamp(timestamp: String): Long {
        // Parse ISO 8601 timestamp to millis
        return try {
            java.time.Instant.parse(timestamp).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    fun getAttachmentsForThread(threadId: String): List<ThreadAttachment> {
        return _attachments.value[threadId] ?: emptyList()
    }
    
    fun createNewThread(): String {
        val threadId = UUID.randomUUID().toString()
        val newThread = ChatThread(
            id = threadId,
            title = "Chat Baru",
            lastMessage = null
        )
        _threads.value = listOf(newThread) + _threads.value
        _currentThreadId.value = threadId
        return threadId
    }
    
    fun setCurrentThread(threadId: String?) {
        _currentThreadId.value = threadId
    }
    
    fun sendMessage(
        content: String,
        threadId: String? = null,
        userMessageId: String? = null
    ): Flow<SSEEvent> {
        val chatRequest = ChatRequest(
            threadId = threadId,
            userMessageId = userMessageId,
            content = content
        )
        
        return sseClient.sendMessage(chatRequest)
    }
    
    fun updateThreadTitle(threadId: String, title: String) {
        _threads.value = _threads.value.map { thread ->
            if (thread.id == threadId) {
                thread.copy(title = title)
            } else {
                thread
            }
        }
    }
    
    fun updateThreadLastMessage(threadId: String, message: String) {
        _threads.value = _threads.value.map { thread ->
            if (thread.id == threadId) {
                thread.copy(lastMessage = message, updatedAt = System.currentTimeMillis())
            } else {
                thread
            }
        }.sortedByDescending { it.updatedAt }
    }
    
    fun deleteThread(threadId: String) {
        _threads.value = _threads.value.filter { it.id != threadId }
        if (_currentThreadId.value == threadId) {
            _currentThreadId.value = null
        }
        // Clean up attachments
        _attachments.value = _attachments.value.toMutableMap().apply {
            remove(threadId)
        }
    }
    
    fun clearAllThreads() {
        _threads.value = emptyList()
        _currentThreadId.value = null
        _attachments.value = emptyMap()
    }

    fun lockThread(threadId: String) {
        _lockedThreads.value = _lockedThreads.value + threadId
        Log.d("ChatRepository", "Thread locked: $threadId")
    }

    fun isThreadLocked(threadId: String): Boolean {
        return _lockedThreads.value.contains(threadId)
    }

    fun unlockThread(threadId: String) {
        _lockedThreads.value = _lockedThreads.value - threadId
    }
}