package com.android.multitimerpro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "timer_presets")
data class PresetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val durationMillis: Long,
    val color: Int,
    val category: String,
    val description: String = "",
    val uid: String = ""
)
