package com.example.weathersnap.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import com.example.weathersnap.domain.model.City
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)

    fun saveLastCity(city: City) {
        prefs.edit()
            .putInt("city_id", city.id)
            .putString("city_name", city.name)
            .putString("city_country", city.country)
            .putString("city_admin1", city.admin1)
            .putFloat("city_lat", city.latitude.toFloat())
            .putFloat("city_lon", city.longitude.toFloat())
            .apply()
    }

    fun getLastCity(): City? {
        val name = prefs.getString("city_name", null) ?: return null
        val id = prefs.getInt("city_id", 0)
        val country = prefs.getString("city_country", "") ?: ""
        val admin1 = prefs.getString("city_admin1", null)
        val lat = prefs.getFloat("city_lat", 0f).toDouble()
        val lon = prefs.getFloat("city_lon", 0f).toDouble()
        
        return City(
            id = id,
            name = name,
            country = country,
            admin1 = admin1,
            latitude = lat,
            longitude = lon
        )
    }
}
