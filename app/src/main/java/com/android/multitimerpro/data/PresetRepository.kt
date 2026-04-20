package com.android.multitimerpro.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetRepository @Inject constructor(
    private val presetDao: PresetDao
) {
    val allPresets: Flow<List<PresetEntity>> = presetDao.getAllPresets()

    fun getPresetsByUid(uid: String): Flow<List<PresetEntity>> = presetDao.getPresetsByUid(uid)

    suspend fun insert(preset: PresetEntity) {
        presetDao.insertPreset(preset)
    }

    suspend fun update(preset: PresetEntity) {
        presetDao.updatePreset(preset)
    }

    suspend fun delete(preset: PresetEntity) {
        presetDao.deletePreset(preset)
    }

    suspend fun clearAll() {
        presetDao.clearAll()
    }
}
