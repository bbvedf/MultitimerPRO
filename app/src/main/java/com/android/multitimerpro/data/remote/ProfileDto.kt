package com.android.multitimerpro.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: Long? = null,
    @SerialName("last_check_in") val lastCheckIn: Long = 0,
    @SerialName("check_in_streak") val checkInStreak: Int = 0,
    @SerialName("total_check_ins") val totalCheckIns: Int = 0,
    @SerialName("extra_xp_checkin") val extraXpCheckIn: Long = 0,
    @SerialName("color_tokens") val colorTokens: Int = 0,
    @SerialName("is_pro") val isPro: Boolean = false,
    @SerialName("dark_mode") val darkMode: Boolean = false,
    @SerialName("snooze_1") val snooze1: Int = 5,
    @SerialName("snooze_2") val snooze2: Int = 10,
    @SerialName("silver_reward_redeemed") val silverRewardRedeemed: Boolean = false,
    @SerialName("silver_reward_consumed") val silverRewardConsumed: Boolean = false,
    @SerialName("unlocked_pro_colors") val unlockedProColors: List<String> = emptyList(),
    @SerialName("permanent_gold_medals") val permanentGoldMedals: List<String> = emptyList()
)
