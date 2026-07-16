package com.iiros.scanner.ui.urlscanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iiros.scanner.core.url.UrlScanResult
import com.iiros.scanner.core.url.UrlThreatScanner
import com.iiros.scanner.data.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UrlScannerUiState(
    val input: String = "",
    val isScanning: Boolean = false,
    val result: UrlScanResult? = null,
)

class UrlScannerViewModel(private val historyRepository: HistoryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(UrlScannerUiState())
    val uiState: StateFlow<UrlScannerUiState> = _uiState.asStateFlow()

    fun onInputChanged(value: String) {
        _uiState.value = _uiState.value.copy(input = value)
    }

    fun scan() {
        val input = _uiState.value.input
        if (input.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            val result = withContext(Dispatchers.Default) { UrlThreatScanner.scan(input) }
            historyRepository.record(result)
            _uiState.value = _uiState.value.copy(isScanning = false, result = result)
        }
    }
}
