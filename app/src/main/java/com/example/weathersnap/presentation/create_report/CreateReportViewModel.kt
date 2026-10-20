package com.example.weathersnap.presentation.create_report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathersnap.data.local.entity.ReportEntity
import com.example.weathersnap.data.local.prefs.WeatherPreferences
import com.example.weathersnap.domain.model.CommunityReport
import com.example.weathersnap.domain.repository.CommunityReportRepository
import com.example.weathersnap.domain.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class CreateReportUiState(
    val notes: String = "",
    val imagePath: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CreateReportViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val communityReportRepository: CommunityReportRepository,
    private val preferences: WeatherPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateReportUiState())
    val uiState: StateFlow<CreateReportUiState> = _uiState.asStateFlow()

    fun onNotesChange(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun onImageCaptured(path: String) {
        _uiState.value = _uiState.value.copy(imagePath = path)
    }

    fun saveReport(
        cityName: String,
        temperature: Double,
        condition: String,
        humidity: Int,
        windSpeed: Double,
        pressure: Double,
        cityId: Int = 0,
        cityAdmin1: String? = null,
        cityCountry: String? = null,
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ) {
        val currentState = _uiState.value
        // Prevent duplicate creation from repeated taps or missing image
        if (currentState.imagePath == null || currentState.isSaving) return

        _uiState.value = currentState.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(currentState.imagePath)
                val compressedSize = if (file.exists()) file.length() else 0L
                val originalSize = compressedSize * 2 // Estimate uncompressed size
                val now = System.currentTimeMillis()

                // Resolve structured city metadata from preferences if available
                val savedCity = preferences.getLastCity()
                val resolvedCityId = if (cityId != 0) cityId else (savedCity?.id ?: 0)
                val resolvedCityName = if (savedCity != null && savedCity.displayName.equals(cityName, true)) {
                    savedCity.name
                } else {
                    cityName.split(",").firstOrNull()?.trim() ?: cityName
                }
                val resolvedAdmin1 = cityAdmin1 ?: savedCity?.admin1
                val resolvedCountry = cityCountry ?: (savedCity?.country ?: (cityName.split(",").lastOrNull()?.trim() ?: ""))
                val resolvedDisplayName = savedCity?.displayName ?: cityName
                val resolvedLat = if (latitude != 0.0) latitude else (savedCity?.latitude ?: 0.0)
                val resolvedLon = if (longitude != 0.0) longitude else (savedCity?.longitude ?: 0.0)

                // 1. Save to Room database for local device history (always preserved)
                val report = ReportEntity(
                    cityName = resolvedDisplayName,
                    temperature = temperature,
                    condition = condition,
                    humidity = humidity,
                    windSpeed = windSpeed,
                    pressure = pressure,
                    notes = currentState.notes,
                    imagePath = currentState.imagePath,
                    originalImageSize = originalSize,
                    compressedImageSize = compressedSize,
                    timestamp = now
                )
                repository.saveReport(report)

                // 2. Publish to Supabase Community backend
                val communityReport = CommunityReport(
                    id = UUID.randomUUID().toString(),
                    cityId = resolvedCityId,
                    cityName = resolvedCityName,
                    cityAdmin1 = resolvedAdmin1,
                    cityCountry = resolvedCountry,
                    displayName = resolvedDisplayName,
                    latitude = resolvedLat,
                    longitude = resolvedLon,
                    temperature = temperature,
                    condition = condition,
                    humidity = humidity,
                    windSpeed = windSpeed,
                    pressure = pressure,
                    notes = currentState.notes,
                    imagePath = currentState.imagePath,
                    drawableResId = null,
                    timestamp = now,
                    isLocalUser = true,
                    authorName = "You"
                )
                val publishResult = communityReportRepository.publishReport(communityReport)

                if (publishResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                } else {
                    val error = publishResult.exceptionOrNull()
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = error?.message ?: "Failed to publish report to community."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Failed to save and publish report."
                )
            }
        }
    }
}
