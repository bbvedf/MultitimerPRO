package com.android.multitimerpro.data.remote

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseService @Inject constructor(
    val client: SupabaseClient
) {
    private val storage = client.storage
    private val TAG = "MT_SUPABASE"

    // Flujo del usuario actual
    val currentUser: Flow<UserInfo?> = client.auth.sessionStatus.map { status ->
        when(status) {
            is SessionStatus.Authenticated -> status.session.user
            else -> null
        }
    }

    /**
     * Resuelve el ID del usuario. Si ya es un UUID válido (Supabase), lo usa tal cual.
     * Si no (Firebase legacy), genera un UUID determinista.
     */
    fun resolveUserId(id: String): String {
        if (id.isEmpty() || id == "ANONYMOUS") return id
        return try {
            java.util.UUID.fromString(id)
            id // Ya es un UUID válido
        } catch (e: IllegalArgumentException) {
            // No es un UUID, asumimos ID de Firebase y convertimos
            java.util.UUID.nameUUIDFromBytes(id.toByteArray()).toString()
        }
    }

    // AUTH METHODS
    suspend fun login(email: String, pass: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = pass
        }
    }

    suspend fun signUp(email: String, pass: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "MT_SUPABASE [IO] Starting signUpWith for $email")
                // Forzamos un timeout de 15 segundos para no quedar colgados
                kotlinx.coroutines.withTimeout(15000) {
                    client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = pass
                    }
                }
                Log.d(TAG, "MT_SUPABASE [IO] signUpWith completed successfully")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(TAG, "MT_SUPABASE [IO] signUpWith TIMEOUT (15s) - Check connection or Supabase URL")
                throw Exception("Timeout en la conexión. Revisa tu internet.")
            } catch (t: Throwable) {
                Log.e(TAG, "MT_SUPABASE [IO] signUpWith FATAL ERROR: ${t.message}", t)
                throw t
            }
        }
    }

    suspend fun logout() {
        client.auth.signOut()
    }

    suspend fun resetPassword(email: String) {
        client.auth.resetPasswordForEmail(email)
    }

    suspend fun importSessionManual(accessToken: String, refreshToken: String): Boolean {
        return try {
            // Creamos una sesión manual con los tokens recibidos
            val session = UserSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = 3600, // Valor por defecto si no lo tenemos
                tokenType = "bearer",
                user = null // La SDK lo recuperará automáticamente al validar
            )
            client.auth.importSession(session)
            
            // Esperar a que el estado cambie a Authenticated
            withTimeoutOrNull(2000) {
                client.auth.sessionStatus.first { it is SessionStatus.Authenticated }
            } != null
        } catch (e: Exception) {
            Log.e(TAG, "Error in importSessionManual: ${e.message}")
            false
        }
    }

    suspend fun updatePassword(newPassword: String) {
        try {
            Log.d(TAG, "updatePassword: Comprobando sesión para actualización...")
            
            // 1. Intentar pillar la sesión actual (debería estar tras la inyección manual)
            var session = client.auth.currentSessionOrNull()
            
            if (session == null) {
                Log.d(TAG, "Sesión no detectada al inicio, esperando sincronización (2s)...")
                withTimeoutOrNull(2000) {
                    client.auth.sessionStatus.first { it is io.github.jan.supabase.auth.status.SessionStatus.Authenticated }
                }
                session = client.auth.currentSessionOrNull()
            }

            if (session == null) {
                Log.e(TAG, "ERROR: No hay sesión activa tras inyección y espera.")
                throw Exception("Sesión no válida o expirada. Solicita un nuevo enlace.")
            }

            Log.d(TAG, "Sesión válida para usuario: ${session.user?.id}. Ejecutando updateUser...")
            client.auth.updateUser {
                password = newPassword
            }
            
            // Opcional: Cerrar sesión para limpiar el estado después del éxito
            client.auth.signOut()
            Log.d(TAG, "¡CONTRASEÑA ACTUALIZADA CON ÉXITO!")
        } catch (e: Exception) {
            Log.e(TAG, "Error crítico en updatePassword: ${e.message}")
            throw e
        }
    }

    // PROFILES
    suspend fun getProfile(userId: String): ProfileDto? {
        val mappedId = resolveUserId(userId)
        return try {
            client.from("profiles").select {
                filter { eq("id", mappedId) }
            }.decodeSingleOrNull<ProfileDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching profile", e)
            null
        }
    }

    suspend fun upsertProfile(profile: ProfileDto) {
        val mappedId = resolveUserId(profile.id)
        val mappedProfile = profile.copy(id = mappedId)
        try {
            // Usamos select() para que Supabase nos devuelva cómo ha quedado la fila realmente
            val result = client.from("profiles").upsert(mappedProfile) {
                select()
            }.decodeSingle<ProfileDto>()
            
            Log.d(TAG, "CONFIRMACIÓN NUBE: dark_mode en Supabase ahora es: ${result.darkMode}")
        } catch (e: Exception) {
            Log.e(TAG, "Error upserting profile: ${e.message}", e)
            throw e
        }
    }

    // TIMERS
    suspend fun getTimers(userId: String): List<TimerDto> {
        val mappedId = resolveUserId(userId)
        return try {
            client.from("timers").select {
                filter { eq("user_id", mappedId) }
            }.decodeList<TimerDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching timers", e)
            emptyList()
        }
    }

    suspend fun upsertTimer(timer: TimerDto) {
        val mappedId = resolveUserId(timer.userId)
        val mappedTimer = timer.copy(userId = mappedId)
        client.from("timers").upsert(mappedTimer)
    }

    // HISTORY
    suspend fun getHistory(userId: String): List<HistoryDto> {
        val mappedId = resolveUserId(userId)
        return try {
            client.from("history").select {
                filter { eq("user_id", mappedId) }
                order(column = "end_time", order = Order.DESCENDING)
            }.decodeList<HistoryDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching history", e)
            emptyList()
        }
    }

    suspend fun upsertHistory(history: HistoryDto) {
        val mappedId = resolveUserId(history.userId)
        val mappedHistory = history.copy(userId = mappedId)
        try {
            client.from("history").upsert(mappedHistory)
            Log.d(TAG, "History UPSERT OK: ${mappedHistory.timerName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error upserting history: ${e.message}")
            throw e
        }
    }

    // PRESETS
    suspend fun getPresets(userId: String): List<PresetDto> {
        val mappedId = resolveUserId(userId)
        return try {
            client.from("presets").select {
                filter { eq("user_id", mappedId) }
            }.decodeList<PresetDto>()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching presets", e)
            emptyList()
        }
    }

    suspend fun upsertPreset(preset: PresetDto) {
        val mappedId = resolveUserId(preset.userId ?: "")
        val mappedPreset = preset.copy(userId = mappedId)
        client.from("presets").upsert(mappedPreset)
    }

    suspend fun deleteTimer(timerId: String) {
        client.from("timers").delete {
            filter { eq("id", timerId) }
        }
    }

    suspend fun deleteHistory(id: String) {
        client.from("history").delete {
            filter { eq("id", id) }
        }
    }

    suspend fun clearRemoteHistory(userId: String) {
        val mappedId = resolveUserId(userId)
        client.from("history").delete {
            filter { eq("user_id", mappedId) }
        }
    }

    suspend fun deletePreset(presetId: String) {
        client.from("presets").delete {
            filter { eq("id", presetId) }
        }
    }

    suspend fun clearUserData(userId: String) {
        val mappedId = resolveUserId(userId)
        withContext(Dispatchers.IO) {
            try {
                // Borrar Timers
                client.from("timers").delete {
                    filter { eq("user_id", mappedId) }
                }
                // Borrar Presets
                client.from("presets").delete {
                    filter { eq("user_id", mappedId) }
                }
                // Borrar Historial
                client.from("history").delete {
                    filter { eq("user_id", mappedId) }
                }
                // Borrar Avatar
                try {
                    storage.from("avatars").delete(listOf("$mappedId/avatar.jpeg"))
                    Log.d(TAG, "Avatar cleared for user: $mappedId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing avatar for user $mappedId", e)
                }
                Log.d(TAG, "Cloud data cleared for user: $mappedId")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cloud data", e)
                throw e
            }
        }
    }

    // AVATARS
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): String? {
        val mappedId = resolveUserId(userId)
        return try {
            // Primero, intentar borrar cualquier avatar existente para este usuario
            try {
                storage.from("avatars").delete(listOf("$mappedId/avatar.jpeg"))
            } catch (e: Exception) {
                Log.w(TAG, "No existing avatar to delete for user $mappedId or error during delete: ${e.message}")
            }

            // Subir el nuevo avatar con upsert activo
            val path = "$mappedId/avatar.jpeg"
            storage.from("avatars").upload(path, imageBytes) {
                upsert = true
            }
            
            // Devuelve la URL pública con un parámetro de versión para evitar caché en la app
            val baseUrl = storage.from("avatars").publicUrl(path)
            "$baseUrl?t=${System.currentTimeMillis()}"
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading avatar for user: $mappedId", e)
            null
        }
    }

    suspend fun deleteAvatar(userId: String) {
        val mappedId = resolveUserId(userId)
        try {
            storage.from("avatars").delete(listOf("$mappedId/avatar.jpeg"))
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting avatar for user: $mappedId", e)
        }
    }
}
