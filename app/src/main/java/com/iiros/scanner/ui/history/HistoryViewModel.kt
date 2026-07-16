package com.iiros.scanner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iiros.scanner.data.HistoryRepository
import com.iiros.scanner.data.db.ScanHistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {

    val history: StateFlow<List<ScanHistoryEntity>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clear() {
        viewModelScope.launch { repository.clear() }
    }
}
