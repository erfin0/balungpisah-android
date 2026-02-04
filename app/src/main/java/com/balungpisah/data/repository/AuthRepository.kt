package com.balungpisah.data.repository

import android.content.Context
import com.balungpisah.data.models.LoginRequest
import com.balungpisah.data.models.LoginResponse
import com.balungpisah.data.models.LogtoUser
import com.balungpisah.data.models.M2MTokenResponse
import com.balungpisah.data.models.RegisterRequest
import com.balungpisah.data.models.RootResponse
import com.balungpisah.data.models.VerificationRequest
import com.balungpisah.data.network.TokenManager
import com.balungpisah.util.AppConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Date
import java.util.concurrent.TimeUnit

class AuthRepository(private val context: Context) {
    
    private val tokenManager = TokenManager.getInstance(context)
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<AuthError?>(null)
    val error: StateFlow<AuthError?> = _error
    
    private val _currentUser = MutableStateFlow<LogtoUser?>(null)
    val currentUser: StateFlow<LogtoUser?> = _currentUser
    
    private var m2mToken: String? = null
    private var m2mTokenExpiry: Date? = null
    private var pendingVerificationEmail: String? = null
    
    companion object {
        @Volatile
        private var instance: AuthRepository? = null
        
        fun getInstance(context: Context): AuthRepository {
            return instance ?: synchronized(this) {
                instance ?: AuthRepository(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private suspend fun getM2MToken(): String = withContext(Dispatchers.IO) {
        // Check cached token
        if (m2mToken != null && m2mTokenExpiry != null && m2mTokenExpiry!!.after(Date())) {
            return@withContext m2mToken!!
        }
        
        val url = "${AppConfig.LOGTO_ENDPOINT}/oidc/token"
        val formData = "grant_type=client_credentials" +
                "&client_id=${AppConfig.LOGTO_APP_ID}" +
                "&client_secret=${AppConfig.LOGTO_APP_SECRET}" +
                "&resource=https://default.logto.app/api" +
                "&scope=all"
        
        val request = Request.Builder()
            .url(url)
            .post(formData.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("M2M token request failed: ${response.code}")
        }
        
        val tokenResponse = gson.fromJson(response.body?.string(), M2MTokenResponse::class.java)
        m2mToken = tokenResponse.accessToken
        m2mTokenExpiry = Date(System.currentTimeMillis() + (tokenResponse.expiresIn - 300) * 1000)
        
        return@withContext tokenResponse.accessToken
    }
    
    suspend fun register(email: String, password: String, username: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            _error.value = null
            
            val m2mToken = getM2MToken()
            val url = "${AppConfig.LOGTO_ENDPOINT}/api/users"
            
            val registerData = RegisterRequest(
                username = username ?: email.substringBefore("@"),
                primaryEmail = email,
                password = password
            )
            
            val request = Request.Builder()
                .url(url)
                .post(gson.toJson(registerData).toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $m2mToken")
                .build()
            
            val response = client.newCall(request).execute()
            
            when (response.code) {
                200, 201 -> {
                    pendingVerificationEmail = email
                    sendVerificationCode(email)
                    Result.success(Unit)
                }
                409, 422 -> {
                    _error.value = AuthError.UserAlreadyExists
                    Result.failure(Exception("User already exists"))
                }
                else -> {
                    _error.value = AuthError.Unknown("Server error: ${response.code}")
                    Result.failure(Exception("Registration failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            _error.value = AuthError.NetworkError
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun sendVerificationCode(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            _error.value = null
            
            val m2mToken = getM2MToken()
            val url = "${AppConfig.LOGTO_ENDPOINT}/api/verification-codes"
            
            val verificationData = VerificationRequest(email = email)
            
            val request = Request.Builder()
                .url(url)
                .post(gson.toJson(verificationData).toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $m2mToken")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.code in 200..204) {
                pendingVerificationEmail = email
                Result.success(Unit)
            } else {
                _error.value = AuthError.Unknown("Failed to send code: ${response.code}")
                Result.failure(Exception("Verification code send failed"))
            }
        } catch (e: Exception) {
            _error.value = AuthError.NetworkError
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun verifyEmail(code: String, email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            _error.value = null
            
            val m2mToken = getM2MToken()
            val url = "${AppConfig.LOGTO_ENDPOINT}/api/verification-codes/verify"
            
            val verifyData = mapOf(
                "email" to email,
                "verificationCode" to code
            )
            
            val request = Request.Builder()
                .url(url)
                .post(gson.toJson(verifyData).toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $m2mToken")
                .build()
            
            val response = client.newCall(request).execute()
            
            when (response.code) {
                200, 204 -> {
                    pendingVerificationEmail = null
                    Result.success(Unit)
                }
                400, 401, 404 -> {
                    _error.value = AuthError.InvalidVerificationCode
                    Result.failure(Exception("Invalid verification code"))
                }
                else -> {
                    _error.value = AuthError.Unknown("Verification failed: ${response.code}")
                    Result.failure(Exception("Verification failed"))
                }
            }
        } catch (e: Exception) {
            _error.value = AuthError.InvalidVerificationCode
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun login(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            _error.value = null
            
            val url = "${AppConfig.API_BASE_URL}/api/auth/login"
            val loginData = LoginRequest(email = email, password = password)
            
            val request = Request.Builder()
                .url(url)
                .post(gson.toJson(loginData).toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            when (response.code) {
                200 -> {
                    val rootResponse = gson.fromJson(response.body?.string(), RootResponse::class.java)
                    rootResponse.data?.let { loginResponse ->
                        tokenManager.saveTokens(
                            accessToken = loginResponse.accessToken,
                            refreshToken = loginResponse.refreshToken
                        )
                        _currentUser.value = loginResponse.user
                    }
                    Result.success(Unit)
                }
                400, 401 -> {
                    _error.value = AuthError.InvalidCredentials
                    Result.failure(Exception("Invalid credentials"))
                }
                else -> {
                    _error.value = AuthError.Unknown("Server error: ${response.code}")
                    Result.failure(Exception("Login failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            _error.value = AuthError.InvalidCredentials
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            tokenManager.clearTokens()
            _currentUser.value = null
            m2mToken = null
            m2mTokenExpiry = null
            pendingVerificationEmail = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    suspend fun isAuthenticated(): Boolean {
        return tokenManager.isLoggedIn() && _currentUser.value != null
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun getPendingVerificationEmail(): String? = pendingVerificationEmail
}

sealed class AuthError : Exception() {
    object InvalidCredentials : AuthError() {
        override val message: String = "Email atau password salah"
    }
    
    object EmailNotVerified : AuthError() {
        override val message: String = "Email belum diverifikasi. Silakan cek inbox Anda"
    }
    
    object NetworkError : AuthError() {
        override val message: String = "Koneksi bermasalah. Silakan coba lagi"
    }
    
    object UserAlreadyExists : AuthError() {
        override val message: String = "Email sudah terdaftar"
    }
    
    object InvalidToken : AuthError() {
        override val message: String = "Sesi tidak valid. Silakan login kembali"
    }
    
    object SignInCancelled : AuthError() {
        override val message: String = "Login dibatalkan"
    }
    
    object InvalidVerificationCode : AuthError() {
        override val message: String = "Kode verifikasi salah atau sudah kadaluarsa"
    }
    
    data class Unknown(override val message: String) : AuthError()
}