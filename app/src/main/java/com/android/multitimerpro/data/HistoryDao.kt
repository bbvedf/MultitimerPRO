package com.android.multitimerpro.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM timer_history ORDER BY completedAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM timer_history WHERE uid = :uid ORDER BY completedAt DESC")
    fun getHistoryByUid(uid: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM timer_history WHERE id = :id")
    suspend fun getHistoryById(id: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)

    @Update
    suspend fun update(history: HistoryEntity)

    @Delete
    suspend fun delete(history: HistoryEntity)

    @Query("DELETE FROM timer_history WHERE uid = :uid")
    suspend fun clearHistory(uid: String)
}
