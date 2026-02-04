package com.balungpisah.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.balungpisah.data.repository.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationScreen(
    email: String,
    authRepository: AuthRepository,
    onVerified: () -> Unit
) {
    var verificationCode by remember { mutableStateOf("") }
    val isLoading by authRepository.isLoading.collectAsState()
    val error by authRepository.error.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Auto-verify when code is 6 digits
    LaunchedEffect(verificationCode) {
        if (verificationCode.length == 6) {
            scope.launch {
                val result = authRepository.verifyEmail(verificationCode, email)
                if (result.isSuccess) {
                    onVerified()
                } else {
                    verificationCode = ""
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Spacer(modifier = Modifier.weight(0.5f))
        
        // Header section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Verifikasi Email",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Masukkan 6 digit kode yang dikirim ke",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Verification code input
        VerificationCodeInput(
            code = verificationCode,
            onCodeChange = { newCode ->
                // Only allow digits and max 6 characters
                val filtered = newCode.filter { it.isDigit() }.take(6)
                verificationCode = filtered
            }
        )
        
        if (isLoading) {
            CircularProgressIndicator()
        }
        
        // Resend section
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tidak menerima kode?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(
                onClick = {
                    scope.launch {
                        authRepository.sendVerificationCode(email)
                    }
                },
                enabled = !isLoading
            ) {
                Text(
                    text = "Kirim Ulang",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
    
    // Error snackbar
    if (error != null) {
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(2000)
            authRepository.clearError()
        }
    }
}

@Composable
fun VerificationCodeInput(
    code: String,
    onCodeChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hidden text field for actual input
        Box {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    VerificationDigitBox(
                        digit = code.getOrNull(index)?.toString() ?: "",
                        isFilled = index < code.length
                    )
                }
            }
            
            // Invisible text field overlay
            BasicTextField(
                value = code,
                onValueChange = onCodeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        innerTextField()
                    }
                },
                textStyle = TextStyle(color = Color.Transparent)
            )
        }
    }
}

@Composable
fun VerificationDigitBox(
    digit: String,
    isFilled: Boolean
) {
    Box(
        modifier = Modifier
            .width(45.dp)
            .height(55.dp)
            .border(
                width = 2.dp,
                color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.3f
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}