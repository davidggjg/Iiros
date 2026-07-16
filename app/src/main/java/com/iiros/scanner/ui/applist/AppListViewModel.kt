package com.iiros.scanner.ui.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iiros.scanner.core.scoring.AppScanResult
import com.iiros.scanner.data.AppScannerRepository
import com.iiros.scanner.data.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScannedApp(val info: InstalledAppInfo, val result: AppScanResult)

data class AppListUiState(
    val isLoading: Boolean = true,
    val apps: List<ScannedApp> = emptyList(),
)

class AppListViewModel(private val repository: AppScannerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val scanned = withContext(Dispatchers.Default) {
                repository.listInstalledApps()
                    .map { app -> ScannedApp(app, repository.quickScan(app)) }
                    .sortedByDescending { it.result.score }
            }
            _uiState.value = AppListUiState(isLoading = false, apps = scanned)
        }
    }
}
