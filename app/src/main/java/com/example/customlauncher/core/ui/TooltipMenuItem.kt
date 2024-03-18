package com.example.customlauncher.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.customlauncher.core.model.TooltipMenu

@Composable
fun TooltipMenuItem(tooltipMenu: TooltipMenu, action: () -> Unit) {
    Row(
        Modifier
            .clickable { action() }
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Icon(imageVector = tooltipMenu.icon, contentDescription = "", tint = Color.DarkGray)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = tooltipMenu.name,
        )
    }
}