package com.balungpisah.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.balungpisah.R
import com.balungpisah.data.repository.AuthRepository
import com.balungpisah.ui.theme.BalungPisahGold
import com.balungpisah.ui.theme.BalungPisahTheme
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    onLoginSuccess: () -> Unit
) {
    val isLoading by authRepository.isLoading.collectAsState()
    val error by authRepository.error.collectAsState()
    val scope = rememberCoroutineScope()

    BalungPisahTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2D2A22), 
                            Color(0xFF080C0B)  
                        ),
                        center = Offset(300f, 300f),
                        radius = 1800f
                    )
                )
        ) {
            AuthScreenContent(
                isLoading = isLoading,
                error = error,
                onClearError = { authRepository.clearError() },
                onLogin = { email, password ->
                    scope.launch {
                        val result = authRepository.login(email, password)
                        if (result.isSuccess) onLoginSuccess()
                    }
                },
                onRegister = { email, password, onSuccess ->
                    scope.launch {
                        val result = authRepository.register(email, password)
                        if (result.isSuccess) onSuccess()
                    }
                },
                authRepository = authRepository
            )
        }
    }
}

@Composable
fun AuthScreenContent(
    isLoading: Boolean,
    error: Throwable?,
    onClearError: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, () -> Unit) -> Unit,
    authRepository: AuthRepository? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var showVerification by remember { mutableStateOf(false) }
    var registeredEmail by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (showVerification && authRepository != null) {
        EmailVerificationScreen(
            email = registeredEmail,
            authRepository = authRepository,
            onVerified = {
                showVerification = false
                isSignUp = false
                password = ""
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 32.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Image(
                painter = painterResource(id = R.drawable.balung_pisah),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isSignUp) "Buat Akun" else "Selamat Datang",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Untuk melanjutkan ke BalungPisah",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BalungPisahGold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = BalungPisahGold,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BalungPisahGold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = BalungPisahGold,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val trimmedEmail = email.trim().lowercase()
                    if (isSignUp) {
                        onRegister(trimmedEmail, password) {
                            registeredEmail = email
                            showVerification = true
                        }
                    } else {
                        onLogin(trimmedEmail, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BalungPisahGold,
                    contentColor = Color(0xFF0D0D0D)
                ),
                enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
            ) {
                if (isLoading) {
                    DotsPulsing()
                } else {
                    Text(
                        text = if (isSignUp) "Daftar" else "Masuk",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { isSignUp = !isSignUp }) {
                Text(
                    text = if (isSignUp) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar",
                    style = MaterialTheme.typography.bodySmall,
                    color = BalungPisahGold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (error != null) {
            AlertDialog(
                onDismissRequest = onClearError,
                title = { Text("Error") },
                text = { Text(error.message ?: "Terjadi kesalahan") },
                confirmButton = {
                    TextButton(onClick = onClearError) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun DotsPulsing() {
    val dotSize = 10.dp
    val delayUnit = 300

    @Composable
    fun Dot(delay: Int) {
        val infiniteTransition = rememberInfiniteTransition(label = "dots")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = delay, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(
            Modifier
                .size(dotSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .background(color = Color(0xFF0D0D0D), shape = CircleShape)
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Dot(delay = 0)
        Dot(delay = delayUnit)
        Dot(delay = delayUnit * 2)
    }
}