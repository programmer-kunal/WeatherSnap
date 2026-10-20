package com.example.weathersnap.presentation.community

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathersnap.data.local.prefs.WeatherPreferences
import com.example.weathersnap.domain.model.CommunityReport
import com.example.weathersnap.domain.repository.CommunityReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommunityReportsUiState(
    val reports: List<CommunityReport> = emptyList(),
    val selectedCityFilter: String = "",
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val myReportsCount: Int = 0
)

data class FeedFilter(
    val query: String? = null,
    val cityId: Int? = null,
    val displayLabel: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CommunityReportsViewModel @Inject constructor(
    private val repository: CommunityReportRepository,
    private val preferences: WeatherPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // City and city ID passed from Home Screen / Create Report screen
    private val initialCity: String = savedStateHandle.get<String>("city") ?: ""
    private val initialCityId: Int = savedStateHandle.get<Int>("cityId")
        ?: savedStateHandle.get<String>("cityId")?.toIntOrNull()
        ?: 0

    private val savedCity = preferences.getLastCity()

    private val initialFilter: FeedFilter = run {
        if (initialCityId > 0) {
            val label = if (initialCity.isNotBlank()) {
                initialCity.split(",").firstOrNull()?.trim() ?: initialCity
            } else {
                savedCity?.name ?: ""
            }
            FeedFilter(cityId = initialCityId, displayLabel = label)
        } else if (savedCity != null && savedCity.id > 0 && initialCity.isNotBlank() &&
            (savedCity.name.equals(initialCity, true) ||
             savedCity.displayName.equals(initialCity, true) ||
             savedCity.displayName.contains(initialCity, true) ||
             initialCity.contains(savedCity.name, true))
        ) {
            FeedFilter(cityId = savedCity.id, displayLabel = savedCity.name)
        } else if (initialCity.isNotBlank() && !initialCity.equals("All", true)) {
            val numeric = initialCity.toIntOrNull()
            if (numeric != null && numeric > 0) {
                FeedFilter(cityId = numeric, displayLabel = initialCity)
            } else {
                // If it's a full address string like "Ghaziabad, Uttar Pradesh, India",
                // extract the base city name for the display label
                val baseLabel = initialCity.split(",").firstOrNull()?.trim() ?: initialCity
                FeedFilter(query = initialCity, displayLabel = baseLabel)
            }
        } else {
            FeedFilter(query = null, cityId = null, displayLabel = "")
        }
    }

    private val _feedFilter = MutableStateFlow(initialFilter)

    private val _uiState = MutableStateFlow(
        CommunityReportsUiState(
            selectedCityFilter = initialFilter.displayLabel,
            searchQuery = "",
            isLoading = true
        )
    )
    val uiState: StateFlow<CommunityReportsUiState> = _uiState.asStateFlow()

    init {
        observeCommunityFeed()
        observeMyReportsCount()
    }

    private fun observeCommunityFeed() {
        viewModelScope.launch {
            _feedFilter.flatMapLatest { filter ->
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                repository.getCommunityReports(cityQuery = filter.query, cityId = filter.cityId)
                    .catch { e ->
                        logSanitizedError("CommunityReportsVM", "Failed to load community feed", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Unable to load community reports. Please check your network connection and try again."
                            )
                        }
                    }
            }.collect { reports ->
                _uiState.update {
                    it.copy(
                        reports = reports,
                        selectedCityFilter = _feedFilter.value.displayLabel,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun observeMyReportsCount() {
        viewModelScope.launch {
            repository.getMyReports()
                .catch { /* ignore count errors */ }
                .collect { myReports ->
                    _uiState.update { it.copy(myReportsCount = myReports.size) }
                }
        }
    }

    fun onCityFilterSelected(city: String) {
        if (city.equals("All", ignoreCase = true) || city.equals(_feedFilter.value.displayLabel, ignoreCase = true)) {
            clearFilter()
            return
        }
        val saved = preferences.getLastCity()
        val newFilter = if (saved != null && saved.name.equals(city, ignoreCase = true)) {
            FeedFilter(cityId = saved.id, displayLabel = saved.name)
        } else {
            FeedFilter(query = city, displayLabel = city)
        }
        _feedFilter.value = newFilter
        _uiState.update {
            it.copy(
                selectedCityFilter = newFilter.displayLabel,
                searchQuery = ""
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        val trimmed = query.trim()
        _uiState.update { it.copy(searchQuery = query) }
        if (trimmed.isBlank()) {
            clearFilter()
        } else {
            val saved = preferences.getLastCity()
            val newFilter = if (saved != null && (saved.name.equals(trimmed, ignoreCase = true) || saved.displayName.equals(trimmed, ignoreCase = true))) {
                FeedFilter(cityId = saved.id, displayLabel = trimmed)
            } else {
                FeedFilter(query = trimmed, displayLabel = trimmed)
            }
            _feedFilter.value = newFilter
            _uiState.update { it.copy(selectedCityFilter = trimmed) }
        }
    }

    fun clearFilter() {
        _feedFilter.value = FeedFilter(query = null, cityId = null, displayLabel = "")
        _uiState.update {
            it.copy(
                selectedCityFilter = "",
                searchQuery = ""
            )
        }
    }

    fun retry() {
        val current = _feedFilter.value
        _feedFilter.value = FeedFilter(query = null, cityId = null, displayLabel = "")
        _feedFilter.value = current
    }

    private fun logSanitizedError(tag: String, message: String, e: Throwable) {
        val raw = e.message ?: e.toString()
        val sanitized = raw
            .replace(Regex("Bearer\\s+[A-Za-z0-9-_=.]+", RegexOption.IGNORE_CASE), "Bearer [REDACTED]")
            .replace(Regex("apikey=[A-Za-z0-9-_=.]+", RegexOption.IGNORE_CASE), "apikey=[REDACTED]")
            .replace(Regex("apikey:[\\s]*[A-Za-z0-9-_=.]+", RegexOption.IGNORE_CASE), "apikey: [REDACTED]")
            .replace(Regex("https?://[^\\s]+"), "[URL_REDACTED]")
        Log.e(tag, "$message: $sanitized")
    }
}
