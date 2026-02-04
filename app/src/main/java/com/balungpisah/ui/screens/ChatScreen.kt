package com.balungpisah.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.balungpisah.R
import com.balungpisah.data.models.*
import com.balungpisah.data.repository.ChatRepository
import com.balungpisah.util.AppConfig
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onInputFocusChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentThreadId by viewModel.currentThreadId.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val showRateLimitDialog by viewModel.showRateLimitDialog.collectAsState()
    val rateLimitStatus by viewModel.rateLimitStatus.collectAsState()
    val isUploadingAttachment by viewModel.isUploadingAttachment.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var isInputFocused by remember { mutableStateOf(false) }

    val isThreadLocked by viewModel.isThreadLocked.collectAsState()
    val threadLockMessage by viewModel.threadLockMessage.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d("ChatScreen", "=== FILE PICKER RESULT ===")
        Log.d("ChatScreen", "URI: $uri")
        Log.d("ChatScreen", "Current thread: $currentThreadId")
        Log.d("ChatScreen", "Messages count: ${messages.size}")
        
        uri?.let {
            val mimeType = context.contentResolver.getType(uri)
            Log.d("ChatScreen", "MIME type: $mimeType")
            
            // Ensure thread exists (create if needed)
            val threadId = currentThreadId ?: viewModel.createNewThread().also {
                Log.d("ChatScreen", "Created new thread: $it")
            }
            
            Log.d("ChatScreen", "Uploading to thread: $threadId")
            viewModel.uploadAttachment(threadId, uri, mimeType)
        }
    }
    
    // Load attachments when thread changes
    LaunchedEffect(currentThreadId) {
        currentThreadId?.let { threadId ->
            viewModel.loadAttachments(threadId)
        }
        Log.d("ChatScreen", "Thread ID changed: $currentThreadId")
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Rate limit dialog
    if (showRateLimitDialog && rateLimitStatus != null) {
        RateLimitDialog(
            status = rateLimitStatus!!,
            onDismiss = { viewModel.dismissRateLimitDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.balung_pisah),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildAnnotatedString {
                                append("Balung")
                                withStyle(style = SpanStyle(color = Color(0xFFD4A574))) {
                                    append("Pisah")
                                }
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.Menu, contentDescription = "Riwayat")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (messages.isEmpty() && currentThreadId == null) {
                LandingView(
                    messageText = messageText,
                    onMessageChange = { messageText = it },
                    onSend = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    },
                    onDismissKeyboard = { focusManager.clearFocus() },
                    onFocusChange = { focused ->
                        isInputFocused = focused
                    }
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                focusManager.clearFocus()
                            },
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages) { message ->
                            MessageItem(message = message)
                        }

                        if (isLoading) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }

                    if (isThreadLocked && threadLockMessage != null) {
                        ThreadLockedBanner(
                            message = threadLockMessage!!,
                            onStartNewChat = {
                                viewModel.startNewChatFromLocked()
                            }
                        )
                    }

                    // Attachments preview
                    currentThreadId?.let { threadId ->
                        val threadAttachments = attachments[threadId] ?: emptyList()
                        if (threadAttachments.isNotEmpty()) {
                            AttachmentsPreview(
                                attachments = threadAttachments,
                                onDeleteAttachment = { attachmentId ->
                                    viewModel.deleteAttachment(threadId, attachmentId)
                                }
                            )
                        }
                    }

                    ChatInputBar(
                        messageText = messageText,
                        onMessageChange = { messageText = it },
                        onSend = {
                            if (messageText.isNotBlank() && !isLoading) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        onAttachImage = {
                            filePickerLauncher.launch(AppConfig.FILE_PICKER_MIME_TYPES)
                        },
                        isLoading = isLoading,
                        isUploadingAttachment = isUploadingAttachment,
                        isDisabled = isThreadLocked, 
                        onFocusChange = { focused ->
                            isInputFocused = focused
                        }
                    )
                }
            }

            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("TUTUP", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    }
                ) {
                    Text(text = errorMessage)
                }
            }
        }
    }
}

@Composable
fun AttachmentsPreview(
    attachments: List<ThreadAttachment>,
    onDeleteAttachment: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Lampiran (${attachments.size}/${AppConfig.MAX_ATTACHMENT})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(attachments) { attachment ->
                    AttachmentThumbnail(
                        attachment = attachment,
                        onDelete = { onDeleteAttachment(attachment.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AttachmentThumbnail(
    attachment: ThreadAttachment,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.size(80.dp)
    ) {
        when {
            attachment.contentType.startsWith("image/") -> {
                // Image preview
                AsyncImage(
                    model = attachment.url,
                    contentDescription = attachment.originalFilename,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentScale = ContentScale.Crop
                )
            }
            attachment.contentType.startsWith("video/") -> {
                // Video thumbnail with play icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            attachment.contentType == "application/pdf" -> {
                // PDF icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Info, // You can use a PDF icon if available
                            contentDescription = "PDF",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "PDF",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            else -> {
                // Generic file icon
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "File",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Delete button
        IconButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Hapus",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        
        // File info overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                )
                .padding(4.dp)
        ) {
            Text(
                text = formatFileSize(attachment.fileSize),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = getFileExtension(attachment.originalFilename),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Lampiran") },
            text = {
                Column {
                    Text("Apakah Anda yakin ingin menghapus lampiran ini?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = attachment.originalFilename,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

private fun getFileExtension(filename: String): String {
    return filename.substringAfterLast('.', "").uppercase()
}

@Composable
fun RateLimitDialog(
    status: UserRateLimitStatus,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Batas Pengaduan Tercapai")
        },
        text = {
            Column {
                Text(
                    "Anda telah mengirim ${status.ticketsUsed} dari ${status.maxTickets} pengaduan hari ini."
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Silakan coba lagi besok. Kuota akan direset pada pukul 00:00 WIB.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Mengerti")
            }
        }
    )
}

@Composable
fun LandingView(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismissKeyboard: () -> Unit,
    onFocusChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onDismissKeyboard()
                }
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.balung_pisah),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = buildAnnotatedString {
                        append("Balung")
                        withStyle(style = SpanStyle(color = Color(0xFFD4A574))) {
                            append("Pisah")
                        }
                    },
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Rakyat urun data, AI menjernihkan,\nPejabat menuntaskan.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Mulai percakapan",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        ChatInputBar(
            messageText = messageText,
            onMessageChange = onMessageChange,
            onSend = onSend,
            onAttachImage = null, // No attachments on landing view
            isLoading = false,
            isUploadingAttachment = false,
            onFocusChange = onFocusChange
        )
    }
}

@Composable
fun ChatInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachImage: (() -> Unit)?,
    isLoading: Boolean,
    isUploadingAttachment: Boolean,
    isDisabled: Boolean = false, 
    onFocusChange: (Boolean) -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = messageText,
                onValueChange = onMessageChange,
                enabled = !isDisabled, 
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { state ->
                        onFocusChange(state.isFocused)
                    },
                placeholder = { Text(if (isDisabled) "Thread sudah ditutup" else "Ketik pesan...", color = Color.Gray) },
                leadingIcon = onAttachImage?.let { attachAction ->
                    {
                        IconButton(
                            onClick = attachAction,
                            enabled = !isUploadingAttachment && !isDisabled
                        ) {
                            if (isUploadingAttachment) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Attachment,
                                    contentDescription = "Lampirkan file (gambar, video, PDF)",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(28.dp),
                maxLines = 4
            )

            IconButton(
                onClick = {
                    onSend()
                    focusManager.clearFocus()
                },
                enabled = messageText.isNotBlank() && !isLoading && !isDisabled,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (messageText.isNotBlank() && !isDisabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Kirim",
                    tint = if (messageText.isNotBlank() && !isDisabled) Color.White else Color.Gray
                )
            }
        }
    }
}

@Composable
fun MessageItem(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.brain),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            message.blocks.forEach { block ->
                BlockItem(block = block, isUser = isUser)
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (message.isStreaming) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.user),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ThreadLockedBanner(
    message: String,
    onStartNewChat: () -> Unit
) {
    // Determine icon and color based on message content
    val (icon, containerColor, contentColor) = when {
        message.contains("berhasil dikirim", ignoreCase = true) -> 
            Triple(Icons.Default.CheckCircle, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        message.contains("informasi tambahan", ignoreCase = true) -> 
            Triple(Icons.Default.Warning, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        else -> 
            Triple(Icons.Default.Lock, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }
            
            FilledTonalButton(
                onClick = onStartNewChat,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Chat Baru")
            }
        }
    }
}

@Composable
fun BlockItem(block: MessageBlock, isUser: Boolean) {
    when (block.type) {
        BlockType.TEXT -> {
            Surface(
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                )
            ) {
                Text(
                    text = block.content,
                    modifier = Modifier.padding(12.dp),
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        BlockType.TOOL_CALL -> {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (block.isExecuting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "🔧 ${block.toolName ?: "Tool"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        BlockType.THOUGHT -> {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "💭 ${block.content}",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        else -> {}
    }
}

private fun formatFileSize(bytes: Long): String {
    val df = DecimalFormat("#.##")
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${df.format(bytes / 1024.0)} KB"
        else -> "${df.format(bytes / (1024.0 * 1024.0))} MB"
    }
}