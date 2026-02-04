package com.balungpisah.data.models

import com.google.gson.annotations.SerializedName

// Attachment models
data class ThreadAttachment(
    val id: String,
    @SerializedName("thread_id") val threadId: String,
    @SerializedName("file_id") val fileId: String,
    @SerializedName("original_filename") val originalFilename: String,
    @SerializedName("content_type") val contentType: String,
    @SerializedName("file_size") val fileSize: Long,
    val url: String,
    @SerializedName("created_at") val createdAt: String
)

data class AttachmentCountResponse(
    val count: Long,
    @SerializedName("max_allowed") val maxAllowed: Long,
    @SerializedName("can_upload") val canUpload: Boolean
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?,
    val errors: List<String>?
)

// Rate limit models
data class UserRateLimitStatus(
    @SerializedName("tickets_used") val ticketsUsed: Long,
    @SerializedName("tickets_remaining") val ticketsRemaining: Long,
    @SerializedName("max_tickets") val maxTickets: Long,
    @SerializedName("can_chat") val canChat: Boolean,
    @SerializedName("resets_at") val resetsAt: String
)

// Thread models for API
data class ThreadResponse(
    val id: String,
    val title: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class ThreadDetailResponse(
    val id: String,
    val title: String?,
    @SerializedName("message_count") val messageCount: Long,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class MessageResponse(
    val id: String,
    @SerializedName("thread_id") val threadId: String,
    val role: String,
    val content: Any, // Can be String or List of content blocks
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("episode_id") val episodeId: String?
)

data class ChatThread(
    val id: String,
    val title: String,
    val lastMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)