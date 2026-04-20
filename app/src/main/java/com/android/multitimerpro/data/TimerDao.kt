package com.android.multitimerpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {
    @Query("SELECT * FROM timers ORDER BY createdAt DESC")
    fun getAllTimers(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers WHERE uid = :uid ORDER BY createdAt DESC")
    fun getTimersByUid(uid: String): Flow<List<TimerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimer(timer: TimerEntity)

    @Update
    suspend fun updateTimer(timer: TimerEntity)

    @Delete
    suspend fun deleteTimer(timer: TimerEntity)

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun getTimerById(id: String): TimerEntity?

    @Query("DELETE FROM timers")
    suspend fun clearAll()
}
