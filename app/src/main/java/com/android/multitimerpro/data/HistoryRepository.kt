package com.android.multitimerpro.data

import android.util.Log
import com.android.multitimerpro.data.remote.HistoryDto
import com.android.multitimerpro.data.remote.SupabaseService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val supabaseService: SupabaseService
) {
    private val TAG = "MT_DEBUG"

    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    fun getHistoryByUid(uid: String): Flow<List<HistoryEntity>> = historyDao.getHistoryByUid(uid)

    suspend fun insert(history: HistoryEntity) {
        try {
            Log.d(TAG, "[HISTORIAL] Insertando local: ${history.timerName} (ID: ${history.id})")
            historyDao.insert(history)
            if (history.uid.isNotEmpty() && history.uid != "ANONYMOUS") {
                Log.d(TAG, "[SUPABASE] Sincronizando historial: ${history.timerName}")
                supabaseService.upsertHistory(history.toDto())
            }
        } catch (e: Exception) {
            Log.e(TAG, "[HISTORIAL] Error en insert", e)
        }
    }

    suspend fun reclaimLocalHistory(userId: String) {
        try {
            Log.d(TAG, "[HISTORIAL] Reclamando historial local para: $userId")
            historyDao.reclaimHistory(userId)
            
            // Subir a la nube lo que acabamos de reclamar
            val localItems = historyDao.getHistoryByUid(userId).first()
            localItems.forEach { item ->
                try {
                    supabaseService.upsertHistory(item.toDto())
                } catch (e: Exception) {
                    Log.e(TAG, "Error subiendo item reclamado: ${item.timerName}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[HISTORIAL] Error reclamando historial", e)
        }
    }

    suspend fun insertAll(historyList: List<HistoryEntity>) {
        try {
            historyDao.insertAll(historyList)
            historyList.filter { it.uid.isNotEmpty() && it.uid != "ANONYMOUS" }.forEach { history ->
                Log.d(TAG, "[SUPABASE] Sincronizando bloque de historial: ${history.timerName}")
                supabaseService.upsertHistory(history.toDto())
            }
        } catch (e: Exception) {
            Log.e(TAG, "[HISTORIAL] Error en insertAll", e)
        }
    }

    suspend fun refreshHistoryFromCloud(userId: String) {
        try {
            Log.d(TAG, "[SUPABASE] Descargando historial para: $userId")
            val remoteHistory = supabaseService.getHistory(userId)
            if (remoteHistory.isNotEmpty()) {
                val entities = remoteHistory.map { it.toEntity() }
                historyDao.insertAll(entities)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[SUPABASE] Error refrescando historial", e)
        }
    }

    private fun HistoryDto.toEntity() = HistoryEntity(
        id = this.id ?: UUID.randomUUID().toString(),
        timerName = this.timerName,
        durationMillis = this.durationMillis,
        completedAt = this.endTime,
        uid = this.userId,
        category = this.category ?: "GENERAL",
        color = this.color ?: 0,
        isSnoozed = this.isSnoozed,
        notes = this.notes ?: "",
        intervalsJson = this.intervalsJson ?: "[]"
    )

    private fun HistoryEntity.toDto() = HistoryDto(
        id = this.id,
        userId = this.uid,
        timerName = this.timerName,
        durationMillis = this.durationMillis,
        startTime = this.completedAt - this.durationMillis,
        endTime = this.completedAt,
        category = this.category,
        color = this.color,
        isSnoozed = this.isSnoozed,
        notes = this.notes.ifBlank { null },
        intervalsJson = this.intervalsJson.ifBlank { null }
    )

    suspend fun update(history: HistoryEntity) {
        try {
            Log.d(TAG, "[HISTORIAL] Actualizando local: ${history.timerName}")
            historyDao.insert(history) // Usamos insert (que es REPLACE) para evitar problemas si no existe
            if (history.uid.isNotEmpty() && history.uid != "ANONYMOUS") {
                supabaseService.upsertHistory(history.toDto())
            }
        } catch (e: Exception) {
            Log.e(TAG, "[HISTORIAL] Error en update", e)
        }
    }

    suspend fun delete(history: HistoryEntity) {
        historyDao.delete(history)
        if (history.uid.isNotEmpty() && history.uid != "ANONYMOUS") {
            try {
                supabaseService.deleteHistory(history.id)
            } catch (e: Exception) {
                Log.e(TAG, "[HISTORIAL] Error en delete remoto", e)
            }
        }
    }

    suspend fun clearAll(userId: String? = null) {
        historyDao.clearAll()
        if (userId != null && userId.isNotEmpty() && userId != "ANONYMOUS") {
            try {
                supabaseService.clearRemoteHistory(userId)
            } catch (e: Exception) {
                Log.e(TAG, "[HISTORIAL] Error clearing remote history for $userId", e)
            }
        }
    }
}
