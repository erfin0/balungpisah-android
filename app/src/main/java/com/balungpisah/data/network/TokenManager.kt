package com.balungpisah.data.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_tokens")

class TokenManager(private val context: Context) {
    
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val ID_TOKEN_KEY = stringPreferencesKey("id_token")
        
        @Volatile
        private var instance: TokenManager? = null
        
        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    suspend fun saveTokens(
        accessToken: String?,
        refreshToken: String? = null,
        idToken: String? = null
    ) {
        context.dataStore.edit { preferences ->
            accessToken?.let { preferences[ACCESS_TOKEN_KEY] = it }
            refreshToken?.let { preferences[REFRESH_TOKEN_KEY] = it }
            idToken?.let { preferences[ID_TOKEN_KEY] = it }
        }
    }
    
    fun getAccessToken(): String? {
        return runBlocking {
            context.dataStore.data.map { preferences ->
                preferences[ACCESS_TOKEN_KEY]
            }.first()
        }
    }
    
    suspend fun getAccessTokenAsync(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }.first()
    }
    
    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[REFRESH_TOKEN_KEY]
        }.first()
    }
    
    suspend fun getIdToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[ID_TOKEN_KEY]
        }.first()
    }
    
    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    suspend fun isLoggedIn(): Boolean {
        val token = getAccessTokenAsync()
        return !token.isNullOrEmpty()
    }
}
