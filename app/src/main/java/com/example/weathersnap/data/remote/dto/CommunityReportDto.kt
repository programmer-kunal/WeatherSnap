package com.example.weathersnap.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunityReportDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("city_id") val cityId: Int,
    @SerialName("city_name") val cityName: String,
    @SerialName("city_admin1") val cityAdmin1: String? = null,
    @SerialName("city_country") val cityCountry: String,
    @SerialName("display_name") val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val condition: String,
    val humidity: Int,
    @SerialName("wind_speed") val windSpeed: Double,
    val pressure: Double,
    val notes: String = "",
    @SerialName("image_path") val imagePath: String,
    @SerialName("created_at") val createdAt: String? = null
)
