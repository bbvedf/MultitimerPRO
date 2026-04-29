package com.android.multitimerpro.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoryDto(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("timer_name") val timerName: String,
    @SerialName("duration_millis") val durationMillis: Long,
    @SerialName("start_time") val startTime: Long? = null,
    @SerialName("end_time") val endTime: Long,
    @SerialName("category") val category: String? = null,
    @SerialName("color") val color: Int? = null,
    @SerialName("is_snoozed") val isSnoozed: Boolean = false,
    @SerialName("notes") val notes: String? = null,
    @SerialName("intervals_json") val intervalsJson: String? = null
)
