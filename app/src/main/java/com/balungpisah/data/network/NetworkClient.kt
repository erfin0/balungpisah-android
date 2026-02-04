package com.balungpisah.data.network

import android.content.Context
import com.balungpisah.util.AppConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class NetworkClient(private val context: Context) {
    
    private val client by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (AppConfig.environment == com.balungpisah.util.AppEnvironment.DEVELOPMENT) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                // Add auth token to all requests
                val token = TokenManager.getInstance(context).getAccessToken()
                val newRequest = if (token != null) {
                    originalRequest.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    originalRequest
                }
                chain.proceed(newRequest)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    private val retrofitInstance by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val gson = Gson()
    
    companion object {
        @Volatile
        private var instance: NetworkClient? = null
        
        fun getInstance(context: Context): NetworkClient {
            return instance ?: synchronized(this) {
                instance ?: NetworkClient(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Get OkHttpClient for SSE connections
     * Used by: SSEClient
     */
    fun getOkHttpClient(): OkHttpClient = client
    
    /**
     * Get Retrofit instance for API calls
     * Used by: ChatRepository (for AttachmentApiService)
     */
    fun getRetrofit(): Retrofit = retrofitInstance
    
    /**
     * Make an authenticated request to the backend API
     */
    suspend fun <T> request(
        endpoint: String,
        method: String = "GET",
        body: Map<String, Any>? = null,
        responseClass: Class<T>
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val url = "${AppConfig.API_BASE_URL}$endpoint"
            
            val requestBuilder = Request.Builder()
                .url(url)
                .method(method, body?.let {
                    gson.toJson(it).toRequestBody("application/json".toMediaType())
                })
                .addHeader("Content-Type", "application/json")
            
            // Get access token from TokenManager
            val token = TokenManager.getInstance(context).getAccessToken()
            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            
            val request = requestBuilder.build()
            val response = client.newCall(request).execute()
            
            when (response.code) {
                in 200..299 -> {
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        Result.failure(NetworkError.EmptyResponse)
                    } else {
                        val data = gson.fromJson(responseBody, responseClass)
                        Result.success(data)
                    }
                }
                401 -> Result.failure(NetworkError.Unauthorized)
                else -> Result.failure(NetworkError.HttpError(response.code, response.message))
            }
        } catch (e: IOException) {
            Result.failure(NetworkError.NetworkException(e))
        } catch (e: Exception) {
            Result.failure(NetworkError.UnknownError(e))
        }
    }
    
    /**
     * Make an authenticated request without expecting a response body
     */
    suspend fun requestNoResponse(
        endpoint: String,
        method: String = "POST",
        body: Map<String, Any>? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${AppConfig.API_BASE_URL}$endpoint"
            
            val requestBuilder = Request.Builder()
                .url(url)
                .method(method, body?.let {
                    gson.toJson(it).toRequestBody("application/json".toMediaType())
                })
                .addHeader("Content-Type", "application/json")
            
            // Get access token from TokenManager
            val token = TokenManager.getInstance(context).getAccessToken()
            if (token != null) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            
            val request = requestBuilder.build()
            val response = client.newCall(request).execute()
            
            when (response.code) {
                in 200..299 -> Result.success(Unit)
                401 -> Result.failure(NetworkError.Unauthorized)
                else -> Result.failure(NetworkError.HttpError(response.code, response.message))
            }
        } catch (e: IOException) {
            Result.failure(NetworkError.NetworkException(e))
        } catch (e: Exception) {
            Result.failure(NetworkError.UnknownError(e))
        }
    }
}

sealed class NetworkError : Exception() {
    object InvalidURL : NetworkError() {
        override val message: String = "URL tidak valid"
    }
    
    object InvalidResponse : NetworkError() {
        override val message: String = "Respons tidak valid"
    }
    
    object Unauthorized : NetworkError() {
        override val message: String = "Sesi berakhir. Silakan login kembali"
    }
    
    data class HttpError(val statusCode: Int, val statusMessage: String) : NetworkError() {
        override val message: String = "HTTP Error: $statusCode - $statusMessage"
    }
    
    object EmptyResponse : NetworkError() {
        override val message: String = "Respons kosong dari server"
    }
    
    data class NetworkException(override val cause: IOException) : NetworkError() {
        override val message: String = "Kesalahan jaringan: ${cause.message}"
    }
    
    data class UnknownError(override val cause: Exception) : NetworkError() {
        override val message: String = "Kesalahan tidak diketahui: ${cause.message}"
    }
}