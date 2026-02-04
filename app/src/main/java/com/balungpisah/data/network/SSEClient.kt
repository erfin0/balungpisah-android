package com.balungpisah.data.network

import android.content.Context
import android.util.Log
import com.balungpisah.data.models.ChatRequest
import com.balungpisah.util.AppConfig
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

class SSEClient(private val context: Context) {
    
    private val networkClient = NetworkClient.getInstance(context)
    private val gson = Gson()
    
    companion object {
        private const val TAG = "SSEClient"
        
        @Volatile
        private var instance: SSEClient? = null
        
        fun getInstance(context: Context): SSEClient {
            return instance ?: synchronized(this) {
                instance ?: SSEClient(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Send a chat message and receive SSE events
     */
    fun sendMessage(chatRequest: ChatRequest): Flow<SSEEvent> = callbackFlow {
        val url = "${AppConfig.API_BASE_URL}/api/citizen-report-agent/chat"
        
        val requestBody = gson.toJson(chatRequest).toRequestBody("application/json".toMediaType())
        
        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
        
        // Get access token
        val token = TokenManager.getInstance(context).getAccessToken()
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        
        val request = requestBuilder.build()
        
        val eventSourceListener = object : EventSourceListener() {
            private var buffer = StringBuilder()
            
            override fun onOpen(eventSource: EventSource, response: okhttp3.Response) {
                Log.d(TAG, "SSE connection opened")
            }
            
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                try {
                    Log.d(TAG, "=== SSE Event ===")
                    Log.d(TAG, "Type: $type")
                    try {
                        val jsonElement = gson.fromJson(data, com.google.gson.JsonElement::class.java)
                        val prettyJson = gson.newBuilder().setPrettyPrinting().create().toJson(jsonElement)
                        Log.d(TAG, "Data (formatted):\n$prettyJson")
                    } catch (e: Exception) {
                        // Not valid JSON, log as-is
                        Log.d(TAG, "Data (raw): $data")
                    }
                    Log.d(TAG, "================")

                    // Skip keepalive pings
                    if (data.isBlank() || data.trim().startsWith(":")) {
                        trySend(SSEEvent.KeepAlive)
                        return
                    }
                    
                    val event = parseSSEEvent(type ?: "unknown", data)
                    trySend(event)
                    
                    // Close channel on terminal events
                    if (event is SSEEvent.MessageCompleted || event is SSEEvent.Error) {
                        eventSource.cancel()
                        close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing SSE event", e)
                    trySend(SSEEvent.Error("parse_error", e.message ?: "Unknown error"))
                }
            }
            
            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "SSE connection closed")
                close()
            }
            
            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: okhttp3.Response?
            ) {
                Log.e(TAG, "SSE connection failed", t)
                trySend(
                    SSEEvent.Error(
                        "connection_error",
                        t?.message ?: "Connection failed"
                    )
                )
                close(t)
            }
        }
        
        val eventSource = EventSources.createFactory(networkClient.getOkHttpClient())
            .newEventSource(request, eventSourceListener)
        
        awaitClose {
            eventSource.cancel()
        }
    }
    
    private fun parseSSEEvent(type: String, data: String): SSEEvent {
        return try {
            when (type) {
                "thread.created" -> gson.fromJson(data, SSEEvent.ThreadCreated::class.java)
                "message.started" -> gson.fromJson(data, SSEEvent.MessageStarted::class.java)
                
                // Block events
                "block.created" -> gson.fromJson(data, SSEEvent.BlockCreated::class.java)
                "block.delta" -> {
                    val blockDelta = gson.fromJson(data, SSEEvent.BlockDelta::class.java)
                    // Extract text from nested delta object
                    SSEEvent.TextDelta(blockDelta.delta.text ?: "")
                }
                "block.completed" -> gson.fromJson(data, SSEEvent.BlockCompleted::class.java)
                
                // Tool events
                "tool.execution_started" -> {
                    val toolEvent = gson.fromJson(data, SSEEvent.ToolExecutionStarted::class.java)
                    Log.d(TAG, "ToolExecutionStarted parsed - toolName: ${toolEvent.toolName}, toolUseId: ${toolEvent.toolUseId}")
                    val toolCall = SSEEvent.ToolCall(toolEvent.toolName, "", toolEvent.toolUseId)
                    Log.d(TAG, "Created ToolCall - toolName: ${toolCall.toolName}, toolCallId: ${toolCall.toolCallId}")
                    toolCall
                }
                "tool.execution_completed" -> {
                    // Parse the full event first
                    val toolEvent = gson.fromJson(data, SSEEvent.ToolExecutionCompleted::class.java)
                    
                    // The tool_call_id field should be extracted properly
                    // Log it for debugging
                    Log.d(TAG, "ToolExecutionCompleted - tool_call_id: ${toolEvent.toolCallId}, tool_use_id: ${toolEvent.toolUseId}")
                    
                    // Return the parsed event with the correct toolCallId
                    toolEvent
                }
                "tool.execution_failed" -> gson.fromJson(data, SSEEvent.ToolExecutionFailed::class.java)
                
                // Thinking/reasoning
                "thinking" -> gson.fromJson(data, SSEEvent.Thinking::class.java)
                
                // Message events
                "message.usage" -> gson.fromJson(data, SSEEvent.MessageUsage::class.java)
                "message.completed" -> {
                    val messageComplete = gson.fromJson(data, SSEEvent.MessageCompleted::class.java)
                    SSEEvent.MessageComplete(messageComplete.threadId)
                }
                
                // Error
                "error" -> gson.fromJson(data, SSEEvent.Error::class.java)
                
                else -> {
                    Log.w(TAG, "Unknown event type: $type, data: $data")
                    SSEEvent.Error("unknown_event", "Unknown event type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing event type '$type' with data: $data", e)
            SSEEvent.Error("parse_error", "Failed to parse $type: ${e.message}")
        }
    }
}