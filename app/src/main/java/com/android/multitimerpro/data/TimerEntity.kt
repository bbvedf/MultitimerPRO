package com.android.multitimerpro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val duration: Long,
    val remainingTime: Long,
    val status: String, // "LIVE", "READY", "PAUSED", "FINISHED"
    val color: Int,
    val category: String,
    val description: String = "",
    val intervalsJson: String = "[]",
    val isSnoozed: Boolean = false,
    val baseDuration: Long = 0,
    val lastSnoozeDuration: Long = 0,
    val lastHistoryId: String? = null,
    val uid: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
