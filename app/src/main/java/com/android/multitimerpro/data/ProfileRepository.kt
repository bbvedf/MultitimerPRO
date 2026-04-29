package com.android.multitimerpro.data

import android.util.Log
import com.android.multitimerpro.data.remote.ProfileDto
import com.android.multitimerpro.data.remote.SupabaseService
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val supabaseService: SupabaseService
) {
    private val TAG = "MT_DEBUG"

    /**
     * Resuelve el ID del usuario. Si ya es un UUID válido (Supabase), lo usa tal cual.
     * Si no (Firebase legacy), genera un UUID determinista.
     */
    private fun resolveUserId(id: String): String {
        return try {
            UUID.fromString(id)
            id // Ya es un UUID válido
        } catch (e: IllegalArgumentException) {
            // No es un UUID, asumimos ID de Firebase y convertimos
            UUID.nameUUIDFromBytes(id.toByteArray()).toString()
        }
    }

    suspend fun getProfile(userId: String): ProfileDto? {
        val mappedId = resolveUserId(userId)
        Log.d(TAG, "[PROFILE] Cargando perfil de Supabase para: $mappedId")
        return supabaseService.getProfile(mappedId)
    }

    suspend fun saveProfile(profile: ProfileDto) {
        val mappedId = resolveUserId(profile.id)
        val mappedProfile = profile.copy(id = mappedId)
        Log.d(TAG, "[PROFILE] Guardando perfil en Supabase como: $mappedId")
        supabaseService.upsertProfile(mappedProfile)
    }

    suspend fun updateXpData(
        userId: String,
        extraXp: Long,
        streak: Int,
        totalCheckIns: Int,
        lastCheckIn: Long
    ) {
        val mappedId = resolveUserId(userId)
        // Recuperamos el perfil actual primero para NO machacar el nombre/avatar
        val current = getProfile(mappedId)
        val updatedProfile = (current ?: ProfileDto(id = mappedId)).copy(
            extraXpCheckIn = extraXp,
            checkInStreak = streak,
            totalCheckIns = totalCheckIns,
            lastCheckIn = lastCheckIn
        )
        supabaseService.upsertProfile(updatedProfile)
    }
}
