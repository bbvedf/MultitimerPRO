package com.android.multitimerpro.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {
    @Query("SELECT * FROM timers ORDER BY isCompleted ASC, id DESC")
    fun getAllTimers(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun getTimerById(id: Int): TimerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimer(timer: TimerEntity): Long

    @Update
    suspend fun updateTimer(timer: TimerEntity)

    @Delete
    suspend fun deleteTimer(timer: TimerEntity)

    @Query("UPDATE timers SET isRunning = :isRunning, lastUpdatedMs = :currentTime WHERE id = :id")
    suspend fun updateTimerState(id: Int, isRunning: Boolean, currentTime: Long)
}
