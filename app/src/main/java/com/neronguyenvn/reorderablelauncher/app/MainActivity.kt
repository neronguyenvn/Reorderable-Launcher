package com.neronguyenvn.reorderablelauncher.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.neronguyenvn.reorderablelauncher.core.designsystem.theme.CustomLauncherTheme
import com.neronguyenvn.reorderablelauncher.feature.home.HomeScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CustomLauncherTheme {
                HomeScreen()
            }
        }
    }
}
