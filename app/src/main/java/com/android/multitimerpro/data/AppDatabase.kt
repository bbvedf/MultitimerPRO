package com.android.multitimerpro.data

import androidx.room.*

@Database(
    entities = [TimerEntity::class, HistoryEntity::class, PresetEntity::class],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timerDao(): TimerDao
    abstract fun historyDao(): HistoryDao
    abstract fun presetDao(): PresetDao
}
