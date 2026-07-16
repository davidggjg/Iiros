package com.iiros.scanner.ui.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iiros.scanner.core.RiskLevel
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

data class ScannedApp(val info: InstalledAppInfo, val result: AppScanResult, val deepScanned: Boolean = false)

data class AppListUiState(
    val isLoading: Boolean = true,
    val apps: List<ScannedApp> = emptyList(),
    val isDeepScanningAll: Boolean = false,
    val deepScanCompleted: Int = 0,
    val deepScanTotal: Int = 0,
)

class AppListViewModel(
    private val repository: AppScannerRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

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

    /**
     * Runs the code-level (APK dex string) scan across every installed app,
     * one by one, instead of requiring the user to open each app's detail
     * screen and tap "deep scan" individually. This reads every APK on the
     * device from disk, so it can take a while on a phone with hundreds of
     * apps — progress is reported via [AppListUiState.deepScanCompleted].
     */
    fun runDeepScanAll() {
        if (_uiState.value.isDeepScanningAll) return
        viewModelScope.launch {
            val startingApps = _uiState.value.apps
            _uiState.value = _uiState.value.copy(
                isDeepScanningAll = true,
                deepScanCompleted = 0,
                deepScanTotal = startingApps.size,
            )
            val updated = startingApps.toMutableList()
            for ((index, scanned) in startingApps.withIndex()) {
                val result = withContext(Dispatchers.Default) { repository.deepScan(scanned.info) }
                if (result.riskLevel != RiskLevel.SAFE) {
                    historyRepository.record(result)
                }
                updated[index] = scanned.copy(result = result, deepScanned = true)
                _uiState.value = _uiState.value.copy(apps = updated.toList(), deepScanCompleted = index + 1)
            }
            _uiState.value = _uiState.value.copy(
                isDeepScanningAll = false,
                apps = updated.sortedByDescending { it.result.score },
            )
        }
    }
}
