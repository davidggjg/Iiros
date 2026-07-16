package com.iiros.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.iiros.scanner.ui.navigation.IirosNavHost
import com.iiros.scanner.ui.theme.IirosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as IirosApplication

        setContent {
            IirosTheme {
                IirosNavHost(
                    appScannerRepository = app.appScannerRepository,
                    historyRepository = app.historyRepository,
                )
            }
        }
    }
}
