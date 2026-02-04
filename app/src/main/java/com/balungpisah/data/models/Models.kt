package com.balungpisah.data.models

import com.google.gson.annotations.SerializedName

// Chat request/response models
data class ChatRequest(
    @SerializedName("thread_id") val threadId: String? = null,
    @SerializedName("user_message_id") val userMessageId: String? = null,
    val content: String
)

sealed class ContentBlock {
    data class Text(val type: String = "text", val text: String) : ContentBlock()
    data class File(val type: String = "file", val url: String) : ContentBlock()
    data class FileData(
        val type: String = "file_data",
        @SerializedName("mime_type") val mimeType: String,
        val data: String // base64 encoded
    ) : ContentBlock()
}

// Auth models
data class RegisterRequest(
    val username: String,
    @SerializedName("primaryEmail") val primaryEmail: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class VerificationRequest(
    val email: String
)

data class RootResponse(
    val success: Boolean,
    val message: String?,
    val data: LoginResponse?,
    val errors: List<String>?
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("expires_in") val expiresIn: Long,
    @SerializedName("token_type") val tokenType: String,
    val user: LogtoUser
)

data class LogtoUser(
    val id: String,
    val name: String?,
    val username: String?,
    val email: String?,
    @SerializedName("email_verified") val emailVerified: Boolean?,
    val avatar: String?
)

data class M2MTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("token_type") val tokenType: String
)

/**
 * Chat message
 */
data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val blocks: List<MessageBlock>,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

/**
 * Message role enum
 */
enum class MessageRole {
    @SerializedName("user")
    USER,
    
    @SerializedName("assistant")
    ASSISTANT
}

/**
 * Message block (part of a message)
 */
data class MessageBlock(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: BlockType,
    val content: String,
    val toolName: String? = null,
    val toolCallId: String? = null, 
    val isExecuting: Boolean = false
)

/**
 * Block type enum
 */
enum class BlockType {
    @SerializedName("text")
    TEXT,
    
    @SerializedName("tool_call")
    TOOL_CALL,
    
    @SerializedName("tool_result")
    TOOL_RESULT,
    
    @SerializedName("thought")
    THOUGHT
}