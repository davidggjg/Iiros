package com.iiros.scanner.ui.filescanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iiros.scanner.data.FileScanRepository
import com.iiros.scanner.data.ScannedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FileScannerUiState(
    val hasAccess: Boolean = false,
    val isScanning: Boolean = false,
    val hasScannedOnce: Boolean = false,
    val scannedCount: Int = 0,
    val flagged: List<ScannedFile> = emptyList(),
)

class FileScannerViewModel(private val repository: FileScanRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FileScannerUiState())
    val uiState: StateFlow<FileScannerUiState> = _uiState.asStateFlow()

    init {
        refreshAccessState()
    }

    fun refreshAccessState() {
        _uiState.value = _uiState.value.copy(hasAccess = repository.hasFullAccess())
    }

    fun startScan() {
        if (_uiState.value.isScanning) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, scannedCount = 0, flagged = emptyList())
            withContext(Dispatchers.IO) {
                repository.scanAll { progress ->
                    _uiState.value = _uiState.value.copy(
                        scannedCount = progress.scannedCount,
                        flagged = progress.newlyFlagged?.let { _uiState.value.flagged + it } ?: _uiState.value.flagged,
                    )
                }
            }
            _uiState.value = _uiState.value.copy(isScanning = false, hasScannedOnce = true)
        }
    }
}
