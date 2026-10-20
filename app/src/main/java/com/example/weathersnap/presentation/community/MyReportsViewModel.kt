package com.example.weathersnap.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weathersnap.domain.model.CommunityReport
import com.example.weathersnap.domain.repository.CommunityReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyReportsUiState(
    val reports: List<CommunityReport> = emptyList(),
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val userMessage: String? = null
)

@HiltViewModel
class MyReportsViewModel @Inject constructor(
    private val repository: CommunityReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyReportsUiState())
    val uiState: StateFlow<MyReportsUiState> = _uiState.asStateFlow()

    init {
        loadMyReports()
    }

    fun loadMyReports() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            repository.getMyReports()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userMessage = e.message ?: "Failed to load your community reports."
                    )
                }
                .collect { list ->
                    _uiState.value = _uiState.value.copy(
                        reports = list,
                        isLoading = false
                    )
                }
        }
    }

    fun deleteReport(reportId: String) {
        if (_uiState.value.isDeleting) return

        _uiState.value = _uiState.value.copy(isDeleting = true)
        viewModelScope.launch {
            val result = repository.deleteCommunityReport(reportId)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    userMessage = "Report removed from community feed"
                )
                // Reload list to ensure UI is in sync with database
                loadMyReports()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    userMessage = error.message ?: "Failed to delete report"
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
