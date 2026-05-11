package com.example.weathersnap.domain.model

data class Weather(
    val temperature: Double,
    val conditionCode: Int,
    val humidity: Int,
    val windSpeed: Double,
    val pressure: Double
) {
    val conditionText: String
        get() = when (conditionCode) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            95, 96, 99 -> "Thunderstorm"
            else -> "Unknown"
        }
}
