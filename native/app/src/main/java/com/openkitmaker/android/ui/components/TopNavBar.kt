// Implements: SCR-HOME / SCR-CATALOG / SCR-EDITOR / SCR-SAVED / SCR-EXPORT / FUN-OFFLINE-MODE
package com.openkitmaker.android.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openkitmaker.android.R

@Composable
fun TopNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = menuExpanded) {
        menuExpanded = false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xFF1A2230))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand logo e Titolo "OpenKitMaker" con dot di stato
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "Logo OpenKitMaker",
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(Color(0xFF00C49F), CircleShape)
                )
                Text(
                    text = "OpenKitMaker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Navigazione: Icone rapide + Menu a tendina Dropdown
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(onClick = { onNavigate("SCR-HOME") }) {
                Text(
                    text = "🏠",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (currentRoute.startsWith("SCR-HOME")) Color(0xFF00C49F) else Color(0xFF94A3B8)
                )
            }
            IconButton(onClick = { onNavigate("SCR-CATALOG") }) {
                Text(
                    text = "📋",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (currentRoute.startsWith("SCR-CATALOG")) Color(0xFF00C49F) else Color(0xFF94A3B8)
                )
            }
            IconButton(onClick = { onNavigate("SCR-SAVED") }) {
                Text(
                    text = "📁",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (currentRoute.startsWith("SCR-SAVED")) Color(0xFF00C49F) else Color(0xFF94A3B8)
                )
            }
            IconButton(onClick = { onNavigate("SCR-EXPORT") }) {
                Text(
                    text = "📤",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (currentRoute.startsWith("SCR-EXPORT")) Color(0xFF00C49F) else Color(0xFF94A3B8)
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Text(
                        text = "⋮",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    DropdownMenuItem(
                        text = { Text("🏠 Home", color = Color.White) },
                        onClick = {
                            menuExpanded = false
                            onNavigate("SCR-HOME")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📋 Catalogo Template", color = Color.White) },
                        onClick = {
                            menuExpanded = false
                            onNavigate("SCR-CATALOG")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("👕 Editor Divisa", color = Color.White) },
                        onClick = {
                            menuExpanded = false
                            onNavigate("SCR-EDITOR")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📁 I Miei Progetti", color = Color.White) },
                        onClick = {
                            menuExpanded = false
                            onNavigate("SCR-SAVED")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📤 Esporta Kit", color = Color.White) },
                        onClick = {
                            menuExpanded = false
                            onNavigate("SCR-EXPORT")
                        }
                    )
                }
            }
        }
    }
}
