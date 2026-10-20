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

    fun saveDefaultCity(city: City) {
        prefs.edit()
            .putInt("city_id", city.id)
            .putString("city_name", city.name)
            .putString("city_country", city.country)
            .putString("city_admin1", city.admin1)
            .putFloat("city_lat", city.latitude.toFloat())
            .putFloat("city_lon", city.longitude.toFloat())
            .putBoolean("is_default_set", true)
            .apply()
    }

    fun saveLastCity(city: City) {
        prefs.edit()
            .putInt("last_city_id", city.id)
            .putString("last_city_name", city.name)
            .putString("last_city_country", city.country)
            .putString("last_city_admin1", city.admin1)
            .putFloat("last_city_lat", city.latitude.toFloat())
            .putFloat("last_city_lon", city.longitude.toFloat())
            .apply()
    }

    fun clearDefaultCity() {
        prefs.edit()
            .remove("city_id")
            .remove("city_name")
            .remove("city_country")
            .remove("city_admin1")
            .remove("city_lat")
            .remove("city_lon")
            .remove("is_default_set")
            .apply()
    }

    fun getDefaultCity(): City? {
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

    fun getLastCity(): City? {
        val name = prefs.getString("last_city_name", null)
            ?: prefs.getString("city_name", null)
            ?: return null
        val id = prefs.getInt("last_city_id", 0).let { if (it != 0) it else prefs.getInt("city_id", 0) }
        val country = prefs.getString("last_city_country", null) ?: prefs.getString("city_country", "") ?: ""
        val admin1 = prefs.getString("last_city_admin1", null) ?: prefs.getString("city_admin1", null)
        val lat = prefs.getFloat("last_city_lat", prefs.getFloat("city_lat", 0f)).toDouble()
        val lon = prefs.getFloat("last_city_lon", prefs.getFloat("city_lon", 0f)).toDouble()

        return City(
            id = id,
            name = name,
            country = country,
            admin1 = admin1,
            latitude = lat,
            longitude = lon
        )
    }

    fun isDefaultCity(city: City?): Boolean {
        if (city == null) return false
        val saved = getDefaultCity() ?: return false
        if (saved.id != 0 && city.id != 0 && saved.id == city.id) return true
        if (saved.displayName.equals(city.displayName, ignoreCase = true)) return true
        val latDiff = kotlin.math.abs(saved.latitude - city.latitude)
        val lonDiff = kotlin.math.abs(saved.longitude - city.longitude)
        return latDiff < 0.001 && lonDiff < 0.001
    }
}
