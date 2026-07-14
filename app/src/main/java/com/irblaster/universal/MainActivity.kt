package com.irblaster.universal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irblaster.universal.ui.screens.HomeScreen
import com.irblaster.universal.ui.screens.RemoteScreen
import com.irblaster.universal.ui.screens.ScanScreen
import com.irblaster.universal.ui.theme.DarkBg
import com.irblaster.universal.ui.theme.IRBlasterTheme
import com.irblaster.universal.viewmodel.MainViewModel

sealed class Screen {
    object Home : Screen()
    data class Remote(val categoryId: String) : Screen()
    object Scan : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IRBlasterTheme(darkTheme = true) {
                val viewModel: MainViewModel = viewModel()
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBg)
                ) {
                    when (val screen = currentScreen) {
                        is Screen.Home -> HomeScreen(
                            viewModel = viewModel,
                            onCategoryClick = { id ->
                                viewModel.selectCategory(viewModel.categories.first { it.id == id })
                                currentScreen = Screen.Remote(id)
                            },
                            onScanClick = { currentScreen = Screen.Scan },
                            onSettingsClick = { }
                        )
                        is Screen.Remote -> RemoteScreen(
                            viewModel = viewModel,
                            categoryId = screen.categoryId,
                            onBack = { currentScreen = Screen.Home }
                        )
                        is Screen.Scan -> ScanScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                }
            }
        }
    }
}
