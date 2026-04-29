package com.android.multitimerpro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity(tableName = "timers")
@Serializable
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
) {
    fun getIntervals(): List<TimerInterval> {
        return try {
            com.google.gson.Gson().fromJson(intervalsJson, object : com.google.gson.reflect.TypeToken<List<TimerInterval>>() {}.type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    fun setIntervals(list: List<TimerInterval>): String {
        return com.google.gson.Gson().toJson(list)
    }
}

@Serializable
data class TimerInterval(val label: String, val timestamp: Long)
