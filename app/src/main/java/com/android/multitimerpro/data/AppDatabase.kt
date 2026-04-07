package com.android.multitimerpro.data

import androidx.room.*

@Database(entities = [TimerEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timerDao(): TimerDao
}
