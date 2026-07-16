package com.iiros.scanner.ui.appdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iiros.scanner.core.scoring.AppScanResult
import com.iiros.scanner.data.AppScannerRepository
import com.iiros.scanner.data.HistoryRepository
import com.iiros.scanner.data.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppDetailUiState(
    val isLoading: Boolean = true,
    val app: InstalledAppInfo? = null,
    val result: AppScanResult? = null,
    val isDeepScanning: Boolean = false,
    val hasRunDeepScan: Boolean = false,
)

class AppDetailViewModel(
    private val packageName: String,
    private val repository: AppScannerRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val app = withContext(Dispatchers.Default) { repository.getInstalledApp(packageName) }
            if (app == null) {
                _uiState.value = AppDetailUiState(isLoading = false)
                return@launch
            }
            val result = withContext(Dispatchers.Default) { repository.quickScan(app) }
            historyRepository.record(result)
            _uiState.value = AppDetailUiState(isLoading = false, app = app, result = result)
        }
    }

    fun runDeepScan() {
        val app = _uiState.value.app ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeepScanning = true)
            val result = withContext(Dispatchers.Default) { repository.deepScan(app) }
            historyRepository.record(result)
            _uiState.value = _uiState.value.copy(
                isDeepScanning = false,
                hasRunDeepScan = true,
                result = result,
            )
        }
    }
}
