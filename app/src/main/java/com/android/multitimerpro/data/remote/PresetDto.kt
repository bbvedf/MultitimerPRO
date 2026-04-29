package com.android.multitimerpro.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PresetDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String,
    @SerialName("duration_millis") val durationMillis: Long,
    @SerialName("color") val color: Int,
    @SerialName("category") val category: String,
    @SerialName("description") val description: String? = null,
    @SerialName("user_id") val userId: String? = null
)
