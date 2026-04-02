package com.android.multitimerpro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val initialTimeMs: Long,
    val remainingTimeMs: Long,
    val isRunning: Boolean = false,
    val lastUpdatedMs: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)
