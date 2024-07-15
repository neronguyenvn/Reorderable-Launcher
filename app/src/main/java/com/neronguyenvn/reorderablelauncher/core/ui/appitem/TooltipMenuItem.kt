package com.neronguyenvn.reorderablelauncher.core.ui.appitem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neronguyenvn.reorderablelauncher.core.model.TooltipMenu

@Composable
fun TooltipMenuItem(tooltipMenu: TooltipMenu, action: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { action() }
            .padding(16.dp)
    ) {
        Icon(
            imageVector = tooltipMenu.icon,
            tint = MaterialTheme.colorScheme.onSecondary,
            contentDescription = "",
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = tooltipMenu.name,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}