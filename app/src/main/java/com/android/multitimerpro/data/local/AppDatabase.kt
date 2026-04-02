package com.android.multitimerpro.data.local

import androidx.room.*

@Database(entities = [TimerEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timerDao(): TimerDao
}
