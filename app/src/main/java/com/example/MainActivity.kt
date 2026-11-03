package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.FreeScannerNavGraph
import com.example.ui.theme.FreeScannerTheme
import com.example.ui.viewmodel.ScannerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val scannerViewModel: ScannerViewModel = viewModel()
            val themeSetting by scannerViewModel.appTheme.collectAsState()
            val isDark = when (themeSetting) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            FreeScannerTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                FreeScannerNavGraph(
                    navController = navController,
                    viewModel = scannerViewModel
                )
            }
        }
    }
}
