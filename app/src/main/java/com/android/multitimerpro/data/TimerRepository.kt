package com.android.multitimerpro.data

import android.util.Log
import com.android.multitimerpro.data.remote.SupabaseService
import com.android.multitimerpro.data.remote.TimerDto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepository @Inject constructor(
    private val timerDao: TimerDao,
    private val supabaseService: SupabaseService
) {
    private val TAG = "MT_DEBUG"

    val allTimers: Flow<List<TimerEntity>> = timerDao.getAllTimers()

    suspend fun insert(timer: TimerEntity, isPro: Boolean = false) {
        try {
            timerDao.insertTimer(timer)
            if (timer.uid.isNotEmpty()) {
                supabaseService.upsertTimer(timer.toDto())
            }
        } catch (e: Exception) {
            Log.e(TAG, "[TIMER] Error en insert", e)
        }
    }

    suspend fun update(timer: TimerEntity, isPro: Boolean = false) {
        try {
            timerDao.updateTimer(timer)
            if (timer.uid.isNotEmpty()) {
                supabaseService.upsertTimer(timer.toDto())
            }
        } catch (e: Exception) {
            Log.e(TAG, "[TIMER] Error actualizando local", e)
        }
    }

    suspend fun delete(timer: TimerEntity) {
        try {
            timerDao.deleteTimer(timer)
            if (timer.uid.isNotEmpty()) {
                supabaseService.deleteTimer(timer.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "[TIMER] Error en delete", e)
        }
    }

    private fun TimerEntity.toDto() = TimerDto(
        id = this.id,
        userId = this.uid,
        name = this.name,
        duration = this.duration,
        baseDuration = this.baseDuration,
        remainingTime = this.remainingTime,
        category = this.category,
        color = this.color,
        status = this.status,
        description = this.description.ifBlank { null },
        isSnoozed = this.isSnoozed,
        intervalsJson = this.intervalsJson.ifBlank { null },
        lastHistoryId = this.lastHistoryId,
        startTime = this.startTime,
        createdAt = this.createdAt
    )

    suspend fun refreshTimersFromCloud(userId: String) {
        try {
            Log.d(TAG, "[SUPABASE] Descargando timers para: $userId")
            val remoteTimers = supabaseService.getTimers(userId)
            if (remoteTimers.isNotEmpty()) {
                val entities = remoteTimers.map { it.toEntity() }
                entities.forEach { timerDao.insertTimer(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[SUPABASE] Error refrescando timers", e)
        }
    }

    private fun TimerDto.toEntity() = TimerEntity(
        id = this.id ?: UUID.randomUUID().toString(),
        uid = this.userId,
        name = this.name,
        duration = this.duration,
        baseDuration = this.baseDuration ?: this.duration,
        remainingTime = this.remainingTime ?: this.duration,
        category = this.category ?: "GENERAL",
        color = this.color ?: 0,
        status = this.status ?: "READY",
        description = this.description ?: "",
        isSnoozed = this.isSnoozed ?: false,
        intervalsJson = this.intervalsJson ?: "[]",
        lastHistoryId = this.lastHistoryId,
        startTime = this.startTime,
        createdAt = this.createdAt ?: System.currentTimeMillis()
    )
    
    suspend fun getTimerById(id: String): TimerEntity? = timerDao.getTimerById(id)
    suspend fun clearAll() = timerDao.clearAll()
}
