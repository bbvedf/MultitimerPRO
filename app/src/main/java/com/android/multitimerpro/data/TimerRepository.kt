package com.android.multitimerpro.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TimerRepository @Inject constructor(private val timerDao: TimerDao) {
    val allTimers: Flow<List<TimerEntity>> = timerDao.getAllTimers()

    suspend fun insert(timer: TimerEntity) {
        timerDao.insertTimer(timer)
    }

    suspend fun update(timer: TimerEntity) {
        timerDao.updateTimer(timer)
    }

    suspend fun delete(timer: TimerEntity) {
        timerDao.deleteTimer(timer)
    }

    suspend fun getTimerById(id: Int): TimerEntity? {
        return timerDao.getTimerById(id)
    }
}
