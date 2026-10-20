package com.example.weathersnap.data.remote.supabase

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesSessionManager(
    context: Context
) : SessionManager {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("weathersnap_supabase_auth", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun saveSession(session: UserSession) {
        try {
            val serialized = json.encodeToString(UserSession.serializer(), session)
            prefs.edit().putString("auth_session", serialized).apply()
        } catch (_: Exception) {}
    }

    override suspend fun loadSession(): UserSession? {
        val serialized = prefs.getString("auth_session", null) ?: return null
        return try {
            json.decodeFromString(UserSession.serializer(), serialized)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun deleteSession() {
        prefs.edit().remove("auth_session").apply()
    }
}

@Singleton
class SupabaseAuthManager @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val mutex = Mutex()

    val sessionStatus: StateFlow<SessionStatus>
        get() = supabase.auth.sessionStatus

    suspend fun getOrCreateAuthenticatedUserId(): String = mutex.withLock {
        // 1. Check existing session / user in memory or loaded by session manager
        var user = supabase.auth.currentUserOrNull()
        if (user != null) {
            return@withLock user.id
        }

        // 2. Wait for auto-restore from SessionManager if in progress
        try {
            supabase.auth.awaitInitialization()
            user = supabase.auth.currentUserOrNull()
            if (user != null) {
                return@withLock user.id
            }
        } catch (_: Exception) {}

        // 3. Create fresh anonymous session if missing or expired
        supabase.auth.signInAnonymously()
        user = supabase.auth.currentUserOrNull()
            ?: throw IllegalStateException("Failed to establish anonymous Supabase session.")

        return@withLock user.id
    }

    fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }
}
