package com.android.multitimerpro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val duration: Long, // Total duration in milliseconds
    val remainingTime: Long, // Remaining time in milliseconds
    val status: String, // "LIVE", "READY", "PAUSED"
    val color: Int, // Color as an Int
    val category: String,
    val description: String = "",
    val intervalsJson: String = "[]", // Store intervals as JSON string
    val createdAt: Long = System.currentTimeMillis()
)
