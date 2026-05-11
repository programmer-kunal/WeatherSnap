package com.example.weathersnap.data.repository

import com.example.weathersnap.data.remote.OpenMeteoApi
import com.example.weathersnap.domain.model.City
import com.example.weathersnap.domain.model.Weather
import com.example.weathersnap.domain.repository.WeatherRepository
import com.example.weathersnap.domain.util.NetworkResult
import com.example.weathersnap.data.local.dao.ReportDao
import com.example.weathersnap.data.local.entity.ReportEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val api: OpenMeteoApi,
    private val dao: ReportDao
) : WeatherRepository {

    // Simple in-memory cache for city queries
    private val cityCache = mutableMapOf<String, List<City>>()

    override suspend fun searchCity(query: String): NetworkResult<List<City>> {
        val lowercaseQuery = query.lowercase()
        if (cityCache.containsKey(lowercaseQuery)) {
            return NetworkResult.Success(cityCache[lowercaseQuery]!!)
        }

        return try {
            val response = api.searchCity(query = query)
            val cities = response.results?.map { dto ->
                City(
                    id = dto.id,
                    name = dto.name,
                    country = dto.country ?: "",
                    admin1 = dto.admin1,
                    latitude = dto.latitude,
                    longitude = dto.longitude
                )
            } ?: emptyList()
            
            cityCache[lowercaseQuery] = cities
            NetworkResult.Success(cities)
        } catch (e: Exception) {
            NetworkResult.Error("Failed to fetch cities: ${e.message}")
        }
    }

    override suspend fun getWeather(latitude: Double, longitude: Double): NetworkResult<Weather> {
        return try {
            val response = api.getWeather(latitude = latitude, longitude = longitude)
            val current = response.current
            val weather = Weather(
                temperature = current.temperature,
                conditionCode = current.weatherCode,
                humidity = current.humidity,
                windSpeed = current.windSpeed,
                pressure = current.pressure
            )
            NetworkResult.Success(weather)
        } catch (e: Exception) {
            NetworkResult.Error("Failed to fetch weather: ${e.message}")
        }
    }

    override suspend fun saveReport(report: ReportEntity) {
        dao.insertReport(report)
    }

    override fun getReports(): Flow<List<ReportEntity>> {
        return dao.getAllReports()
    }
}
