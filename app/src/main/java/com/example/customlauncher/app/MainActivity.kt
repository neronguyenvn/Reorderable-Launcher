package com.example.customlauncher.app

import android.app.AppOpsManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.customlauncher.core.designsystem.theme.CustomLauncherTheme
import com.example.customlauncher.feature.home.HomeScreen
import dagger.hilt.android.AndroidEntryPoint

@Suppress("DEPRECATION")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!checkUsageStatsPermission()) {
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                startActivity(this)
            }
        }
        setContent {
            CustomLauncherTheme {
                HomeScreen()
            }
        }
    }

    private fun checkUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(AppCompatActivity.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(), packageName
            )
        } else {
            appOpsManager.checkOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(), packageName
            )
        }

        return mode == AppOpsManager.MODE_ALLOWED
    }
}

