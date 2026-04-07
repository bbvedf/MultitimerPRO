package com.android.multitimerpro.data

import androidx.room.*

@Database(
    entities = [TimerEntity::class, HistoryEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timerDao(): TimerDao
    abstract fun historyDao(): HistoryDao
}
