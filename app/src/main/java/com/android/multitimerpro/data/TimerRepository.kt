package com.android.multitimerpro.data

import com.android.multitimerpro.data.local.TimerDao
import com.android.multitimerpro.data.local.TimerEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepository @Inject constructor(
    private val timerDao: TimerDao
) {
    fun getAllTimers(): Flow<List<TimerEntity>> = timerDao.getAllTimers()

    suspend fun getTimerById(id: Int): TimerEntity? = timerDao.getTimerById(id)

    suspend fun addTimer(name: String, timeMs: Long) {
        val timer = TimerEntity(
            name = name,
            initialTimeMs = timeMs,
            remainingTimeMs = timeMs,
            isRunning = false
        )
        timerDao.insertTimer(timer)
    }

    suspend fun updateTimer(timer: TimerEntity) {
        timerDao.updateTimer(timer)
    }

    suspend fun deleteTimer(timer: TimerEntity) {
        timerDao.deleteTimer(timer)
    }

    suspend fun updateTimerRunningState(id: Int, isRunning: Boolean) {
        timerDao.updateTimerState(id, isRunning, System.currentTimeMillis())
    }
}
