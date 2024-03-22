package com.example.customlauncher.core.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TooltipMenu {

    abstract val icon: ImageVector
    abstract val name: String

    data object Edit : TooltipMenu() {
        override val icon: ImageVector = Icons.Default.Edit
        override val name: String = "Edit"
    }

    data object AppInfo : TooltipMenu() {
        override val icon: ImageVector = Icons.Default.Info
        override val name: String = "App Info"
    }

    data object Move : TooltipMenu() {
        override val icon: ImageVector = Icons.AutoMirrored.Filled.ExitToApp
        override val name: String = "Move"
    }

    data object Uninstall : TooltipMenu() {
        override val icon: ImageVector = Icons.Default.Delete
        override val name: String = "Uninstall"
    }
}