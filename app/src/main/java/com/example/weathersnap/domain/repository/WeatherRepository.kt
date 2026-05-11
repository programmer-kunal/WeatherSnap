package com.example.weathersnap.domain.repository

import com.example.weathersnap.domain.model.City
import com.example.weathersnap.domain.model.Weather
import com.example.weathersnap.domain.util.NetworkResult
import com.example.weathersnap.data.local.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    suspend fun searchCity(query: String): NetworkResult<List<City>>
    suspend fun getWeather(latitude: Double, longitude: Double): NetworkResult<Weather>
    suspend fun saveReport(report: ReportEntity)
    fun getReports(): Flow<List<ReportEntity>>
}
