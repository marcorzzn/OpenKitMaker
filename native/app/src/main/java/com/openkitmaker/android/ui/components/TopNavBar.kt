// Implements: SCR-HOME / SCR-CATALOG / SCR-EDITOR / SCR-SAVED / SCR-EXPORT / FUN-OFFLINE-MODE
package com.openkitmaker.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openkitmaker.android.i18n.StringsIt

@Composable
fun TopNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A2230))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = StringsIt.APP_NAME,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NavTabButton(
                label = StringsIt.NAV_HOME,
                selected = currentRoute.startsWith("SCR-HOME"),
                onClick = { onNavigate("SCR-HOME") }
            )
            NavTabButton(
                label = StringsIt.NAV_CATALOG,
                selected = currentRoute.startsWith("SCR-CATALOG"),
                onClick = { onNavigate("SCR-CATALOG") }
            )
            NavTabButton(
                label = StringsIt.NAV_SAVED,
                selected = currentRoute.startsWith("SCR-SAVED"),
                onClick = { onNavigate("SCR-SAVED") }
            )
        }
    }
}

@Composable
private fun NavTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (selected) Color(0xFF00C49F) else Color(0xFF94A3B8)
        )
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
