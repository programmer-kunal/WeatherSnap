package com.example.weathersnap.presentation.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathersnap.domain.model.City
import com.example.weathersnap.domain.model.Weather
import com.example.weathersnap.domain.repository.WeatherRepository
import com.example.weathersnap.domain.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeatherUiState(
    val searchQuery: String = "",
    val citySuggestions: List<City> = emptyList(),
    val isSearching: Boolean = false,
    val selectedCity: City? = null,
    val weather: Weather? = null,
    val isLoadingWeather: Boolean = false,
    val weatherError: String? = null,
    val searchError: String? = null
)

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val preferences: com.example.weathersnap.data.local.prefs.WeatherPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        restoreLastCity()
    }

    private fun restoreLastCity() {
        preferences.getLastCity()?.let { city ->
            onCitySelected(city)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        searchJob?.cancel()
        
        if (query.length > 2) {
            searchJob = viewModelScope.launch {
                delay(500) // debounce
                _uiState.update { it.copy(isSearching = true, searchError = null) }
                when (val result = repository.searchCity(query)) {
                    is NetworkResult.Success -> {
                        _uiState.update { 
                            it.copy(
                                citySuggestions = result.data ?: emptyList(),
                                isSearching = false
                            ) 
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { 
                            it.copy(
                                searchError = result.message,
                                isSearching = false,
                                citySuggestions = emptyList()
                            ) 
                        }
                    }
                    else -> {}
                }
            }
        } else {
            _uiState.update { it.copy(citySuggestions = emptyList(), searchError = null) }
        }
    }

    fun onCitySelected(city: City) {
        _uiState.update { 
            it.copy(
                selectedCity = city,
                searchQuery = city.displayName,
                citySuggestions = emptyList(),
                weather = null,
                isLoadingWeather = true,
                weatherError = null
            ) 
        }
        
        viewModelScope.launch {
            when (val result = repository.getWeather(city.latitude, city.longitude)) {
                is NetworkResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            weather = result.data,
                            isLoadingWeather = false
                        ) 
                    }
                    // Save city only after successful weather fetch
                    preferences.saveLastCity(city)
                }
                is NetworkResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            weatherError = result.message,
                            isLoadingWeather = false
                        ) 
                    }
                }
                else -> {}
            }
        }
    }
}
