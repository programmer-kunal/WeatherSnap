package com.example.weathersnap.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.weathersnap.R
import com.example.weathersnap.domain.model.CommunityReport
import com.example.weathersnap.domain.repository.CommunityReportRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalCommunityReportRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : CommunityReportRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("community_reports_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Demonstration sample reports for Phase 3
    private val demoReports: List<CommunityReport> by lazy {
        val now = System.currentTimeMillis()
        val oneHour = 3600 * 1000L

        listOf(
            CommunityReport(
                id = "demo_gzb_1",
                cityName = "Ghaziabad, Uttar Pradesh, India",
                temperature = 28.5,
                condition = "Clear sky",
                humidity = 55,
                windSpeed = 6.2,
                pressure = 990.0,
                notes = "Warm evening with calm winds. Good visibility across Hindon area.",
                imagePath = null,
                drawableResId = R.drawable.ic_demo_weather_clear,
                timestamp = now - (oneHour * 1),
                isLocalUser = false,
                authorName = "Ghaziabad Weather Club"
            ),
            CommunityReport(
                id = "demo_gzb_2",
                cityName = "Ghaziabad, Uttar Pradesh, India",
                temperature = 26.0,
                condition = "Haze",
                humidity = 60,
                windSpeed = 4.8,
                pressure = 991.2,
                notes = "Slight evening haze settling in near Raj Nagar Extension.",
                imagePath = null,
                drawableResId = R.drawable.ic_demo_weather_haze,
                timestamp = now - (oneHour * 3),
                isLocalUser = false,
                authorName = "Station Hindon #02"
            ),
            CommunityReport(
                id = "demo_jpr_1",
                cityName = "Jaipur, Rajasthan, India",
                temperature = 34.2,
                condition = "Sunny",
                humidity = 32,
                windSpeed = 8.5,
                pressure = 1004.5,
                notes = "Dry afternoon heat around Hawa Mahal. Clear skies throughout.",
                imagePath = null,
                drawableResId = R.drawable.ic_demo_weather_clear,
                timestamp = now - (oneHour * 2),
                isLocalUser = false,
                authorName = "PinkCity Metro Demo"
            ),
            CommunityReport(
                id = "demo_del_1",
                cityName = "Delhi, India",
                temperature = 29.0,
                condition = "Partly cloudy",
                humidity = 58,
                windSpeed = 7.0,
                pressure = 992.0,
                notes = "Pleasant breeze near India Gate with scattered cumulus clouds.",
                imagePath = null,
                drawableResId = R.drawable.ic_demo_weather_clouds,
                timestamp = now - (oneHour * 4),
                isLocalUser = false,
                authorName = "Capital Climate Watch"
            ),
            CommunityReport(
                id = "demo_mum_1",
                cityName = "Mumbai, Maharashtra, India",
                temperature = 31.0,
                condition = "Humid & Breezy",
                humidity = 78,
                windSpeed = 14.0,
                pressure = 1008.0,
                notes = "High humidity along Marine Drive with gusty sea winds.",
                imagePath = null,
                drawableResId = R.drawable.ic_demo_weather_rain,
                timestamp = now - (oneHour * 5),
                isLocalUser = false,
                authorName = "CoastLine Observer"
            ),
            CommunityReport(
                id = "demo_blr_1",
                cityName = "Bengaluru, Karnataka, India",
                temperature = 23.5,
                condition = "Overcast",
                humidity = 68,
                windSpeed = 11.2,
                pressure = 1012.0,
                notes = "Cool afternoon in Indiranagar, overcast skies but no rain.",
                imagePath = null,
                drawableResId = R.drawable.ic_demo_weather_clouds,
                timestamp = now - (oneHour * 6),
                isLocalUser = false,
                authorName = "Garden City Tracker"
            ),
            CommunityReport(
                id = "demo_ldn_1",
                cityName = "London, United Kingdom",
                temperature = 14.0,
                condition = "Light rain",
                humidity = 85,
                windSpeed = 12.0,
                pressure = 1015.0,
                notes = "Typical overcast drizzle over Westminster and River Thames.",
                imagePath = null,
                drawableResId = R.drawable.ic_demo_weather_rain,
                timestamp = now - (oneHour * 8),
                isLocalUser = false,
                authorName = "Thames Observer Demo"
            )
        )
    }

    // In-memory state flow holding all active reports (user-published + demo)
    private val _allReports = MutableStateFlow<List<CommunityReport>>(emptyList())

    init {
        loadPersistedReports()
    }

    private fun loadPersistedReports() {
        val json = prefs.getString("user_community_reports", null)
        val userReports: List<CommunityReport> = if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<CommunityReport>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        // Combine user reports and demo reports, sorted newest first
        _allReports.value = (userReports + demoReports).sortedByDescending { it.timestamp }
    }

    private fun persistUserReports() {
        val userOnly = _allReports.value.filter { it.isLocalUser }
        val json = gson.toJson(userOnly)
        prefs.edit().putString("user_community_reports", json).apply()
    }

    override fun getCommunityReports(cityQuery: String?, cityId: Int?): Flow<List<CommunityReport>> {
        return _allReports.map { list ->
            if (cityId != null && cityId != 0) {
                list.filter { it.cityId == cityId }.sortedByDescending { it.timestamp }
            } else if (cityQuery.isNullOrBlank() || cityQuery.trim().equals("All", ignoreCase = true)) {
                list.sortedByDescending { it.timestamp }
            } else {
                list.filter { report -> matchesCity(report.cityName, cityQuery) }
                    .sortedByDescending { it.timestamp }
            }
        }
    }

    override fun getMyReports(): Flow<List<CommunityReport>> {
        return _allReports.map { list ->
            list.filter { it.isLocalUser }.sortedByDescending { it.timestamp }
        }
    }

    override suspend fun publishReport(report: CommunityReport): Result<Unit> {
        return try {
            val updated = listOf(report) + _allReports.value.filter { it.id != report.id }
            _allReports.value = updated.sortedByDescending { it.timestamp }
            persistUserReports()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCommunityReport(reportId: String): Result<Unit> {
        return try {
            val target = _allReports.value.find { it.id == reportId }
            if (target == null) {
                return Result.failure(IllegalArgumentException("Report not found"))
            }
            if (!target.isLocalUser) {
                return Result.failure(IllegalStateException("Cannot delete demonstration reports"))
            }

            _allReports.value = _allReports.value.filter { it.id != reportId }
            persistUserReports()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun matchesCity(reportCity: String, query: String): Boolean {
        val cleanQuery = query.trim().lowercase()
        val cleanCity = reportCity.trim().lowercase()
        if (cleanQuery.isEmpty() || cleanQuery == "all") return true
        if (cleanCity.contains(cleanQuery)) return true

        // Extract first city token (e.g. "Ghaziabad" from "Ghaziabad, Uttar Pradesh, India")
        val queryFirst = cleanQuery.split(",").firstOrNull()?.trim() ?: ""
        val cityFirst = cleanCity.split(",").firstOrNull()?.trim() ?: ""
        if (queryFirst.isNotEmpty() && cleanCity.contains(queryFirst)) return true
        if (cityFirst.isNotEmpty() && cleanQuery.contains(cityFirst)) return true

        return false
    }
}
