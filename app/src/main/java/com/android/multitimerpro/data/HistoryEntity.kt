package com.android.multitimerpro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "timer_history")
data class HistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timerName: String,
    val category: String,
    val durationMillis: Long,
    val completedAt: Long = System.currentTimeMillis(),
    val uid: String = "",
    val color: Int,
    val notes: String = ""
)
