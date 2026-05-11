package com.example.weathersnap.presentation.create_report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathersnap.data.local.entity.ReportEntity
import com.example.weathersnap.domain.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CreateReportUiState(
    val notes: String = "",
    val imagePath: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class CreateReportViewModel @Inject constructor(
    private val repository: WeatherRepository
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
        pressure: Double
    ) {
        val currentState = _uiState.value
        if (currentState.imagePath == null) return

        _uiState.value = currentState.copy(isSaving = true)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(currentState.imagePath)
                val compressedSize = if (file.exists()) file.length() else 0L
                val originalSize = compressedSize * 2 // Simulate uncompressed size for now

                val report = ReportEntity(
                    cityName = cityName,
                    temperature = temperature,
                    condition = condition,
                    humidity = humidity,
                    windSpeed = windSpeed,
                    pressure = pressure,
                    notes = currentState.notes,
                    imagePath = currentState.imagePath,
                    originalImageSize = originalSize,
                    compressedImageSize = compressedSize,
                    timestamp = System.currentTimeMillis()
                )

                repository.saveReport(report)
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }
}
