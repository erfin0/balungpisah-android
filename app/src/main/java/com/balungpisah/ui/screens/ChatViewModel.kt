package com.balungpisah.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balungpisah.data.models.*
import com.balungpisah.data.network.SSEEvent
import com.balungpisah.data.repository.ChatRepository
import com.balungpisah.util.AppConfig
import com.google.gson.JsonElement
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import java.util.UUID

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    val currentThreadId = repository.currentThreadId
    val attachments = repository.attachments
    val rateLimitStatus = repository.rateLimitStatus
    
    private val _showRateLimitDialog = MutableStateFlow(false)
    val showRateLimitDialog: StateFlow<Boolean> = _showRateLimitDialog
    
    private val _isUploadingAttachment = MutableStateFlow(false)
    val isUploadingAttachment: StateFlow<Boolean> = _isUploadingAttachment

    private val _isThreadLocked = MutableStateFlow(false)
    val isThreadLocked: StateFlow<Boolean> = _isThreadLocked

    private val _threadLockMessage = MutableStateFlow<String?>(null)
    val threadLockMessage: StateFlow<String?> = _threadLockMessage
    
    // Flag to prevent streaming text after terminal tool actions
    private var suppressStreamingText = false

    init {
        checkRateLimit()
    }
    
    fun checkRateLimit() {
        viewModelScope.launch {
            repository.checkRateLimit()
        }
    }
    
    fun loadAttachments(threadId: String) {
        viewModelScope.launch {
            repository.loadAttachments(threadId)
        }
    }
    
    fun uploadAttachment(threadId: String, uri: Uri, mimeType: String?) {
        viewModelScope.launch {
            // Check if attachment limit reached
            val currentCount = repository.getAttachmentsForThread(threadId).size
            if (currentCount >= AppConfig.MAX_ATTACHMENT) {
                _error.value = "Maksimal ${AppConfig.MAX_ATTACHMENT} file per percakapan"
                return@launch
            }
            
            // Validate file type
            if (mimeType != null && !isValidFileType(mimeType)) {
                _error.value = "Tipe file tidak didukung. Hanya gambar, video, dan PDF yang diperbolehkan"
                return@launch
            }
            
            _isUploadingAttachment.value = true
            val result = repository.uploadAttachment(threadId, uri)
            _isUploadingAttachment.value = false
            
            result.onSuccess {
                // Success - attachment added to repository
            }.onFailure { e ->
                _error.value = e.message ?: "Gagal mengunggah file"
            }
        }
    }
    
    private fun isValidFileType(mimeType: String): Boolean {
        return AppConfig.SUPPORTED_IMAGE_TYPES.contains(mimeType) ||
               AppConfig.SUPPORTED_VIDEO_TYPES.contains(mimeType) ||
               AppConfig.SUPPORTED_DOCUMENT_TYPES.contains(mimeType)
    }
    
    fun deleteAttachment(threadId: String, attachmentId: String) {
        viewModelScope.launch {
            val result = repository.deleteAttachment(threadId, attachmentId)
            result.onFailure { e ->
                _error.value = e.message ?: "Gagal menghapus file"
            }
        }
    }
    
    fun sendMessage(content: String) {
        if (_isThreadLocked.value) {
            _error.value = "Thread ini sudah ditutup. Silakan mulai percakapan baru."
            return
        }
        val currentStatus = rateLimitStatus.value
        if (currentStatus != null && !currentStatus.canChat) {
            _showRateLimitDialog.value = true
            return
        }
        
        // Reset suppression flag for new message
        suppressStreamingText = false
        
        val threadId = currentThreadId.value
        val userMessageId = UUID.randomUUID().toString()
        
        // Add user message optimistically
        val userMessage = ChatMessage(
            id = userMessageId,
            role = MessageRole.USER,
            blocks = listOf(MessageBlock(type = BlockType.TEXT, content = content)),
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + userMessage
        
        // Add assistant message placeholder
        val assistantMessageId = UUID.randomUUID().toString()
        val assistantMessage = ChatMessage(
            id = assistantMessageId,
            role = MessageRole.ASSISTANT,
            blocks = emptyList(),
            timestamp = System.currentTimeMillis(),
            isStreaming = true
        )
        _messages.value = _messages.value + assistantMessage
        
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                repository.sendMessage(content, threadId, userMessageId)
                    .catch { e ->
                        _error.value = e.message ?: "Terjadi kesalahan"
                        _isLoading.value = false
                        _messages.value = _messages.value.filter { it.id != assistantMessageId }
                    }
                    .collect { event ->
                        when (event) {
                            is SSEEvent.ThreadCreated -> {
                                val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                                val json = gson.toJson(event)
                                Log.d("ChatViewModel", "=== Thread Created Event ===")
                                Log.d("ChatViewModel", json)
                                Log.d("ChatViewModel", "===========================")

                                repository.setCurrentThread(event.threadId)
                                Log.d("ChatViewModel", "Current thread after setting: ${currentThreadId.value}")
                            }
                            is SSEEvent.MessageStarted -> {
                                // Extract and save thread ID from message.started!
                                Log.d("ChatViewModel", "Message started - Thread ID: ${event.threadId}")
                                if (currentThreadId.value == null) {
                                    Log.d("ChatViewModel", "Setting thread ID from MessageStarted: ${event.threadId}")
                                    repository.setCurrentThread(event.threadId)
                                }
                            }
                            is SSEEvent.TextDelta -> {
                                // Ignore text deltas after terminal tool actions
                                if (!suppressStreamingText) {
                                    updateAssistantMessage(assistantMessageId, event.text)
                                } else {
                                    Log.d("ChatViewModel", "Suppressing text delta after terminal tool action")
                                }
                            }
                            is SSEEvent.ToolCall -> {
                                Log.d("ChatViewModel", "Tool call started: ${event.toolName}, ID: ${event.toolCallId}")
                                // Don't add tool call block to UI - we don't need to show it
                            }
                            is SSEEvent.Thinking -> {
                                event.thought?.let { addThinkingBlock(assistantMessageId, it) }
                            }
                            is SSEEvent.MessageComplete -> {
                                finalizeMessage(assistantMessageId)
                                _isLoading.value = false
                                
                                currentThreadId.value?.let { tid ->
                                    repository.updateThreadTitle(tid, extractTitle(content))
                                    repository.updateThreadLastMessage(tid, content)
                                }
                                
                                checkRateLimit()
                            }
                            is SSEEvent.ToolExecutionCompleted -> {
                                Log.d("ChatViewModel", "=== Tool Execution Completed ===")
                                Log.d("ChatViewModel", "Tool name: ${event.toolName}")
                                Log.d("ChatViewModel", "Tool call ID: ${event.toolCallId}")
                                Log.d("ChatViewModel", "Tool use ID: ${event.toolUseId}")
                                Log.d("ChatViewModel", "Result action: ${event.result.action}")
                                Log.d("ChatViewModel", "Result message: ${event.result.message}")
                                Log.d("ChatViewModel", "Will be processed: ${event.result.willBeProcessed}")
                                
                                // Add the result message to chat
                                event.result.message?.let { message ->
                                    if (message.isNotBlank()) {
                                        // Don't add extra newlines - the message is already formatted
                                        updateAssistantMessage(assistantMessageId, message)
                                    }
                                }
                                
                                // Handle different action types and lock thread accordingly
                                val action = event.result.action?.lowercase()
                                Log.d("ChatViewModel", "Processing action: $action")
                                
                                when {
                                    // Thread closed without submission
                                    action == "closed" -> {
                                        Log.d("ChatViewModel", "Thread closed - action: closed")
                                        suppressStreamingText = true  // Ignore subsequent text deltas
                                        _isThreadLocked.value = true
                                        _threadLockMessage.value = "Percakapan telah ditutup."
                                        
                                        currentThreadId.value?.let { threadId ->
                                            repository.lockThread(threadId)
                                        }
                                    }
                                    
                                    // Report submitted and will be processed
                                    action == "submitted" && event.result.willBeProcessed == true -> {
                                        Log.d("ChatViewModel", "Thread closed - report submitted and will be processed")
                                        suppressStreamingText = false 
                                        _isThreadLocked.value = true
                                        _threadLockMessage.value = event.result.referenceNumber?.let {
                                            "Laporan berhasil dikirim dengan nomor referensi $it."
                                        } ?: "Laporan berhasil dikirim."
                                        
                                        currentThreadId.value?.let { threadId ->
                                            repository.lockThread(threadId)
                                        }
                                    }
                                    
                                    // Report submitted but rejected (won't be processed)
                                    action == "submitted" && event.result.willBeProcessed == false -> {
                                        Log.d("ChatViewModel", "Thread closed - report rejected")
                                        suppressStreamingText = true  // Ignore subsequent text deltas
                                        _isThreadLocked.value = true
                                        _threadLockMessage.value = event.result.referenceNumber?.let {
                                            "Laporan dengan nomor referensi $it memerlukan informasi tambahan."
                                        } ?: "Laporan memerlukan informasi tambahan."
                                        
                                        currentThreadId.value?.let { threadId ->
                                            repository.lockThread(threadId)
                                        }
                                    }
                                    
                                    else -> {
                                        Log.d("ChatViewModel", "Unknown or no action - not locking thread")
                                    }
                                }
                                
                                Log.d("ChatViewModel", "Thread locked status: ${_isThreadLocked.value}")
                                Log.d("ChatViewModel", "================================")
                            }

                            is SSEEvent.Error -> {
                                _error.value = event.message
                                _isLoading.value = false
                                _messages.value = _messages.value.filter { it.id != assistantMessageId }
                            }
                            is SSEEvent.ToolExecutionFailed -> {
                                _error.value = "Tool execution failed: ${event.error}"
                            }
                            else -> { /* No action needed */ }
                        }
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "Terjadi kesalahan"
                _isLoading.value = false
                _messages.value = _messages.value.filter { it.id != assistantMessageId }
            }
        }
    }

    fun startNewChatFromLocked() {
        _isThreadLocked.value = false
        _threadLockMessage.value = null
        startNewChat()
    }
    
    private fun updateAssistantMessage(messageId: String, text: String) {
        _messages.value = _messages.value.map { message ->
            if (message.id == messageId) {
                val existingTextBlock = message.blocks.find { it.type == BlockType.TEXT }
                val newBlocks = if (existingTextBlock != null) {
                    message.blocks.map { block ->
                        if (block.type == BlockType.TEXT) {
                            block.copy(content = block.content + text)
                        } else {
                            block
                        }
                    }
                } else {
                    message.blocks + MessageBlock(type = BlockType.TEXT, content = text)
                }
                message.copy(blocks = newBlocks)
            } else {
                message
            }
        }
    }
    
    private fun addToolCallBlock(messageId: String, toolName: String, toolInput: String, toolCallId: String? = null) {
        Log.d("ChatViewModel", "=== Adding Tool Call Block ===")
        Log.d("ChatViewModel", "messageId: $messageId")
        Log.d("ChatViewModel", "toolName: $toolName")
        Log.d("ChatViewModel", "toolInput: $toolInput")
        Log.d("ChatViewModel", "toolCallId: $toolCallId")
        
        _messages.value = _messages.value.map { message ->
            if (message.id == messageId) {
                val newBlock = MessageBlock(
                    type = BlockType.TOOL_CALL,
                    content = toolInput,
                    toolName = toolName,
                    toolCallId = toolCallId,  // Store the tool call ID
                    isExecuting = true
                )
                Log.d("ChatViewModel", "Created block with toolCallId: ${newBlock.toolCallId}")
                message.copy(blocks = message.blocks + newBlock)
            } else {
                message
            }
        }
        
        Log.d("ChatViewModel", "================================")
    }
    
    private fun addThinkingBlock(messageId: String, thought: String?) {
        _messages.value = _messages.value.map { message ->
            if (message.id == messageId) {
                val newBlock = MessageBlock(
                    type = BlockType.THOUGHT,
                    content = thought.toString()
                )
                message.copy(blocks = message.blocks + newBlock)
            } else {
                message
            }
        }
    }
    
    private fun finalizeMessage(messageId: String) {
        _messages.value = _messages.value.map { message ->
            if (message.id == messageId) {
                message.copy(isStreaming = false)
            } else {
                message
            }
        }
    }
    
    private fun extractTitle(content: String): String {
        return content.take(50).trim() + if (content.length > 50) "..." else ""
    }

    private fun extractToolResultMessage(result: SSEEvent.ToolExecutionResult): String {
        return buildString {
            result.message?.let { append(it) }

            // Optionally include reference number if present
            result.referenceNumber?.let { refNum ->
                if (isNotEmpty()) append("\n")
                append("Nomor referensi: $refNum")
            }
        }.ifBlank { "Tool executed successfully" }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun dismissRateLimitDialog() {
        _showRateLimitDialog.value = false
    }

    fun createNewThread(): String {
        return repository.createNewThread()
    }

    fun ensureThreadExists(): String {
        val threadId = currentThreadId.value
        return if (threadId != null) {
            threadId
        } else {
            createNewThread()
        }
    }

    private fun markToolExecutionComplete(messageId: String, toolCallId: String) {
        Log.d("ChatViewModel", "=== Marking Tool Execution Complete ===")
        Log.d("ChatViewModel", "Message ID: $messageId")
        Log.d("ChatViewModel", "Tool Call ID to mark complete: $toolCallId")
        
        val currentMessages = _messages.value
        Log.d("ChatViewModel", "Total messages: ${currentMessages.size}")
        
        _messages.value = _messages.value.map { message ->
            if (message.id == messageId) {
                Log.d("ChatViewModel", "Found matching message, blocks: ${message.blocks.size}")
                
                val updatedBlocks = message.blocks.mapIndexed { index, block ->
                    Log.d("ChatViewModel", "Block $index: type=${block.type}, toolName=${block.toolName}, toolCallId=${block.toolCallId}, isExecuting=${block.isExecuting}")
                    
                    if (block.type == BlockType.TOOL_CALL && block.toolCallId == toolCallId) {
                        Log.d("ChatViewModel", "MATCH FOUND! Stopping spinner for block $index")
                        block.copy(isExecuting = false)
                    } else if (block.type == BlockType.TOOL_CALL) {
                        Log.d("ChatViewModel", "TOOL_CALL block but ID mismatch: ${block.toolCallId} != $toolCallId")
                        block
                    } else {
                        block
                    }
                }
                
                val updatedMessage = message.copy(blocks = updatedBlocks)
                Log.d("ChatViewModel", "Updated message blocks count: ${updatedMessage.blocks.size}")
                updatedMessage
            } else {
                message
            }
        }
        
        Log.d("ChatViewModel", "======================================")
    }

    fun startNewChat() {
        Log.d("ChatViewModel", "Starting new chat")        
        _messages.value = emptyList()
        
        repository.createNewThread()
        
        _isLoading.value = false
        _error.value = null
        suppressStreamingText = false
        
        Log.d("ChatViewModel", "New chat started")
    }

    fun loadThread(threadId: String) {
        Log.d("ChatViewModel", "Loading thread: $threadId")
        
        _messages.value = emptyList()
        
        repository.setCurrentThread(threadId)
        
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = repository.loadThreadDetail(threadId)
            result.onSuccess { threadDetail ->
                _messages.value = emptyList()
                _isLoading.value = false
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to load thread"
                _isLoading.value = false
            }
        }
    }
}