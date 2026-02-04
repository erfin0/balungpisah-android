package com.balungpisah.data.network

import com.google.gson.annotations.SerializedName

/**
 * Server-Sent Events for streaming chat responses
 */
sealed class SSEEvent {
    
    /**
     * Keepalive ping from server
     */
    object KeepAlive : SSEEvent()
    
    /**
     * Thread created (for new conversations)
     */
    data class ThreadCreated(
        @SerializedName("thread_id") 
        val threadId: String
    ) : SSEEvent()
    
    /**
     * Message streaming started
     */
    data class MessageStarted(
        @SerializedName("message_id") 
        val messageId: String,
        @SerializedName("thread_id") 
        val threadId: String
    ) : SSEEvent()
    
    /**
     * New content block created
     */
    data class BlockCreated(
        @SerializedName("block_id") 
        val blockId: String,
        @SerializedName("block_type") 
        val blockType: String, // "text", "tool_use", "thinking"
        @SerializedName("index") 
        val index: Int
    ) : SSEEvent()
    
    /**
     * Text delta (streaming text)
     */
    data class BlockDelta(
        @SerializedName("block_id") 
        val blockId: String,
        @SerializedName("block_type") 
        val blockType: String,
        @SerializedName("delta") 
        val delta: Delta,
        @SerializedName("message_id") 
        val messageId: String
    ) : SSEEvent() {
        data class Delta(
            @SerializedName("text")
            val text: String?
        )
    }
    
    /**
     * Block completed
     */
    data class BlockCompleted(
        @SerializedName("block_id") 
        val blockId: String,
        @SerializedName("block_type") 
        val blockType: String
    ) : SSEEvent()
    
    /**
     * Tool execution started
     */
    data class ToolExecutionStarted(
        @SerializedName("tool_use_id") 
        val toolUseId: String,
        @SerializedName("tool_name") 
        val toolName: String
    ) : SSEEvent()
    
    
    /**
     * Tool execution failed
     */
    data class ToolExecutionFailed(
        @SerializedName("tool_use_id") 
        val toolUseId: String,
        @SerializedName("tool_name") 
        val toolName: String,
        @SerializedName("error") 
        val error: String
    ) : SSEEvent()
    
    /**
     * Message usage statistics
     */
    data class MessageUsage(
        @SerializedName("input_tokens") 
        val inputTokens: Int,
        @SerializedName("output_tokens") 
        val outputTokens: Int,
        @SerializedName("total_tokens") 
        val totalTokens: Int
    ) : SSEEvent()
    
    /**
     * Message completed
     */
    data class MessageCompleted(
        @SerializedName("message_id") 
        val messageId: String,
        @SerializedName("thread_id") 
        val threadId: String,
        @SerializedName("stop_reason") 
        val stopReason: String
    ) : SSEEvent()
    
    /**
     * Error occurred
     */
    data class Error(
        @SerializedName("error_code") 
        val errorCode: String,
        val message: String
    ) : SSEEvent()
    
    // ========== Simplified Event Types for ViewModel ==========
    
    /**
     * Text delta - simplified for ViewModel consumption
     */
    data class TextDelta(
        val text: String  
    ) : SSEEvent()
    
    /**
     * Tool call - simplified for ViewModel consumption
     */
    data class ToolCall(
        val toolName: String,
        val toolInput: String,
        val toolCallId: String? = null
    ) : SSEEvent()
    
    /**
     * Tool result - simplified for ViewModel consumption
     */
    data class ToolResult(
        val result: String
    ) : SSEEvent()
    
    /**
     * Thinking/reasoning block
     */
    data class Thinking(
        val thought: String? 
    ) : SSEEvent()
    
    /**
     * Message complete - simplified for ViewModel consumption
     */
    data class MessageComplete(
        val threadId: String
    ) : SSEEvent()

    data class ToolExecutionCompleted(
        @SerializedName("tool_use_id") 
        val toolUseId: String? = null,
        @SerializedName("tool_call_id")
        val toolCallId: String? = null,
        @SerializedName("tool_name") 
        val toolName: String,
        @SerializedName("result") 
        val result: ToolExecutionResult,
        @SerializedName("success")
        val success: Boolean = false
    ) : SSEEvent()

    data class ToolExecutionResult(
        @SerializedName("action")
        val action: String? = null,
        @SerializedName("message")
        val message: String? = null,
        @SerializedName("reference_number")
        val referenceNumber: String? = null,
        @SerializedName("report_id")
        val reportId: String? = null,
        @SerializedName("success")
        val success: Boolean? = null,
        @SerializedName("will_be_processed")
        val willBeProcessed: Boolean? = null
    )
}