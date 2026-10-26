// Implements: SCR-HOME / SCR-CATALOG / SCR-EDITOR / SCR-SAVED / SCR-EXPORT / FUN-OFFLINE-MODE
package com.openkitmaker.android

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.work.HiltWorkerFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.Configuration
import com.openkitmaker.android.ui.catalog.CatalogScreen
import com.openkitmaker.android.ui.components.TopNavBar
import com.openkitmaker.android.ui.editor.EditorScreen
import com.openkitmaker.android.ui.export.ExportScreen
import com.openkitmaker.android.ui.home.HomeScreen
import com.openkitmaker.android.ui.saved.SavedKitsScreen
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OpenKitMakerApp : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

private val OpenKitDarkColors = darkColorScheme(
    primary = Color(0xFF0F52BA),
    secondary = Color(0xFF00C49F),
    background = Color(0xFF0F141C),
    surface = Color(0xFF1A2230),
    surfaceVariant = Color(0xFF253146),
    onPrimary = Color.White,
    onSecondary = Color(0xFF0F141C),
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFEF4444)
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = OpenKitDarkColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OpenKitMakerNavGraph()
                }
            }
        }
    }
}

@Composable
fun OpenKitMakerNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "SCR-HOME"

    Scaffold(
        topBar = {
            TopNavBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "SCR-HOME",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("SCR-HOME") {
                HomeScreen(
                    onOpenCatalog = { navController.navigate("SCR-CATALOG") },
                    onOpenSaved = { navController.navigate("SCR-SAVED") },
                    onOpenKitInEditor = { kitId -> navController.navigate("SCR-EDITOR/$kitId") }
                )
            }
            composable("SCR-CATALOG") {
                CatalogScreen(
                    onTemplateCloned = { kitId -> navController.navigate("SCR-EDITOR/$kitId") }
                )
            }
            composable(
                route = "SCR-EDITOR/{kitId}",
                arguments = listOf(navArgument("kitId") { type = NavType.StringType })
            ) {
                EditorScreen(
                    onNavigateToExport = { kitId -> navController.navigate("SCR-EXPORT/$kitId") }
                )
            }
            composable("SCR-SAVED") {
                SavedKitsScreen(
                    onOpenKit = { kitId -> navController.navigate("SCR-EDITOR/$kitId") },
                    onExportKit = { kitId -> navController.navigate("SCR-EXPORT/$kitId") }
                )
            }
            composable(
                route = "SCR-EXPORT/{kitId}",
                arguments = listOf(navArgument("kitId") { type = NavType.StringType })
            ) {
                ExportScreen(
                    onBackToEditor = { navController.popBackStack() }
                )
            }
        }
    }
}
