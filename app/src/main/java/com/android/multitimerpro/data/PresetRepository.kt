package com.android.multitimerpro.data

import android.util.Log
import com.android.multitimerpro.data.remote.PresetDto
import com.android.multitimerpro.data.remote.SupabaseService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetRepository @Inject constructor(
    private val presetDao: PresetDao,
    private val supabaseService: SupabaseService
) {
    private val TAG = "MT_DEBUG"

    val allPresets: Flow<List<PresetEntity>> = presetDao.getAllPresets()

    /**
     * Reclama los presets locales que no tienen dueño y los asocia al usuario actual.
     */
    suspend fun reclaimLocalPresets(userId: String) {
        try {
            val mappedId = resolveUserId(userId)
            val localPresets = presetDao.getAllPresets().first().filter {
                it.uid.isEmpty() || it.uid == "ANONYMOUS"
            }
            
            Log.d(TAG, "[PRESET] Reclamando ${localPresets.size} presets para $mappedId")
            
            localPresets.forEach { preset ->
                val updatedPreset = preset.copy(uid = mappedId)
                presetDao.insertPreset(updatedPreset) // insert con OnConflictStrategy.REPLACE
                supabaseService.upsertPreset(updatedPreset.toDto())
            }
        } catch (e: Exception) {
            Log.e(TAG, "[PRESET] Error reclamando presets locales", e)
        }
    }

    /**
     * Resuelve el ID del usuario. Si ya es un UUID válido (Supabase), lo usa tal cual.
     * Si no (Firebase legacy), genera un UUID determinista.
     */
    private fun resolveUserId(id: String): String {
        if (id.isEmpty() || id == "ANONYMOUS") return id
        return try {
            UUID.fromString(id)
            id // Ya es un UUID válido
        } catch (e: IllegalArgumentException) {
            // No es un UUID, asumimos ID de Firebase y convertimos
            UUID.nameUUIDFromBytes(id.toByteArray()).toString()
        }
    }

    suspend fun insert(preset: PresetEntity) {
        try {
            val mappedUid = resolveUserId(preset.uid)
            val mappedPreset = preset.copy(uid = mappedUid)
            presetDao.insertPreset(mappedPreset)
            if (mappedUid.isNotEmpty()) {
                supabaseService.upsertPreset(mappedPreset.toDto())
            }
        } catch (e: Exception) {
            Log.e(TAG, "[PRESET] Error en insert", e)
        }
    }

    suspend fun refreshPresetsFromCloud(userId: String) {
        try {
            val mappedId = resolveUserId(userId)
            Log.d(TAG, "[SUPABASE] Descargando presets para: $mappedId")
            val remotePresets = supabaseService.getPresets(mappedId)
            if (remotePresets.isNotEmpty()) {
                val entities = remotePresets.map { it.toEntity() }
                entities.forEach { presetDao.insertPreset(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[SUPABASE] Error refrescando presets", e)
        }
    }

    fun getPresetsByUid(uid: String): Flow<List<PresetEntity>> = presetDao.getPresetsByUid(resolveUserId(uid))
    
    suspend fun getPresetById(id: String): PresetEntity? = presetDao.getPresetById(id)
    
    suspend fun update(preset: PresetEntity) {
        val mappedUid = resolveUserId(preset.uid)
        val mappedPreset = preset.copy(uid = mappedUid)
        presetDao.updatePreset(mappedPreset)
        if (mappedUid.isNotEmpty()) supabaseService.upsertPreset(mappedPreset.toDto())
    }
    
    suspend fun delete(preset: PresetEntity) {
        presetDao.deletePreset(preset)
        if (preset.uid.isNotEmpty()) supabaseService.deletePreset(preset.id)
    }

    suspend fun clearAll() {
        presetDao.clearAll()
    }

    private fun PresetEntity.toDto() = PresetDto(
        id = this.id,
        name = this.name,
        durationMillis = this.durationMillis,
        color = this.color,
        category = this.category,
        description = this.description.ifBlank { null },
        userId = this.uid
    )

    private fun PresetDto.toEntity() = PresetEntity(
        id = this.id ?: UUID.randomUUID().toString(),
        name = this.name,
        durationMillis = this.durationMillis,
        color = this.color,
        category = this.category,
        description = this.description ?: "",
        uid = this.userId ?: ""
    )
}
