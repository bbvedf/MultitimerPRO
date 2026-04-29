package com.android.multitimerpro.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TimerDto(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("duration") val duration: Long,
    @SerialName("base_duration") val baseDuration: Long? = null,
    @SerialName("remaining_time") val remainingTime: Long? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("color") val color: Int? = null,
    @SerialName("status") val status: String = "READY",
    @SerialName("description") val description: String? = null,
    @SerialName("is_snoozed") val isSnoozed: Boolean = false,
    @SerialName("intervals_json") val intervalsJson: String? = null,
    @SerialName("last_history_id") val lastHistoryId: String? = null,
    @SerialName("created_at") val createdAt: Long? = null
)
