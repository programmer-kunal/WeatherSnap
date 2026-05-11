package com.example.weathersnap.domain.model

data class City(
    val id: Int,
    val name: String,
    val country: String,
    val admin1: String?,
    val latitude: Double,
    val longitude: Double
) {
    val displayName: String
        get() = if (admin1 != null) "$name, $admin1, $country" else "$name, $country"
}
