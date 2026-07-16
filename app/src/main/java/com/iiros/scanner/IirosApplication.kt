package com.iiros.scanner

import android.app.Application
import com.iiros.scanner.data.AppScannerRepository
import com.iiros.scanner.data.HistoryRepository
import com.iiros.scanner.data.db.ScanHistoryDatabase

class IirosApplication : Application() {

    lateinit var appScannerRepository: AppScannerRepository
        private set

    lateinit var historyRepository: HistoryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        appScannerRepository = AppScannerRepository(this)
        historyRepository = HistoryRepository(ScanHistoryDatabase.getInstance(this).scanHistoryDao())
    }
}
