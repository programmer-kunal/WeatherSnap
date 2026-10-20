package com.example.weathersnap.domain.model

data class CommunityReport(
    val id: String,
    val userId: String? = null,
    val cityId: Int = 0,
    val cityName: String,
    val cityAdmin1: String? = null,
    val cityCountry: String = "",
    val displayName: String = cityName,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val temperature: Double,
    val condition: String,
    val humidity: Int,
    val windSpeed: Double,
    val pressure: Double,
    val notes: String,
    val imagePath: String? = null,
    val imageUrl: String? = null,
    val drawableResId: Int? = null,
    val timestamp: Long,
    val isLocalUser: Boolean,
    val authorName: String = if (isLocalUser) "You" else "Community Member"
)
