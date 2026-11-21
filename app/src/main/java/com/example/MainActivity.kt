package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.screens.CoinDetailScreen
import com.example.ui.screens.RadarHomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyberCyanBright
import com.example.ui.theme.CyberCyanTint
import com.example.ui.theme.LunoIntelTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.ScannerViewModel
import kotlinx.serialization.Serializable

@Serializable
data object RadarRoute

@Serializable
data object SettingsRoute

@Serializable
data class CoinDetailRoute(val pair: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(ObsidianBg.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(ObsidianSurface.toArgb())
        )
        val initialPair = intent?.getStringExtra("extra_pair")

        setContent {
            LunoIntelTheme {
                LunoIntelRootApp(initialPair = initialPair)
            }
        }
    }
}

@Composable
fun LunoIntelRootApp(
    initialPair: String? = null,
    viewModel: ScannerViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val showTopLevelChrome = currentDest?.hasRoute<CoinDetailRoute>() != true

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsFlow.collectAsStateWithLifecycle()
    val errorLogs by viewModel.errorLogs.collectAsStateWithLifecycle()
    val isTestingApi by viewModel.isTestingApi.collectAsStateWithLifecycle()
    val apiTestResult by viewModel.apiTestResult.collectAsStateWithLifecycle()

    val isRadarSelected = currentDest?.hasRoute<RadarRoute>() == true
    val isSettingsSelected = currentDest?.hasRoute<SettingsRoute>() == true

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showTopLevelChrome && !useRail) {
                    NavigationBar(containerColor = ObsidianSurface) {
                        NavigationBarItem(
                            selected = isRadarSelected,
                            onClick = {
                                navController.navigate(RadarRoute) {
                                    popUpTo(RadarRoute) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isRadarSelected) Icons.Filled.ShowChart else Icons.Outlined.ShowChart,
                                    contentDescription = "Pasaran"
                                )
                            },
                            label = { Text("Pasaran") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberCyanBright,
                                selectedTextColor = CyberCyanBright,
                                indicatorColor = CyberCyanTint,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_radar")
                        )
                        NavigationBarItem(
                            selected = isSettingsSelected,
                            onClick = {
                                navController.navigate(SettingsRoute) {
                                    popUpTo(RadarRoute) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Tetapan & Keselamatan"
                                )
                            },
                            label = { Text("Tetapan") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberCyanBright,
                                selectedTextColor = CyberCyanBright,
                                indicatorColor = CyberCyanTint,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelChrome && useRail) {
                    NavigationRail(containerColor = ObsidianSurface) {
                        NavigationRailItem(
                            selected = isRadarSelected,
                            onClick = {
                                navController.navigate(RadarRoute) {
                                    popUpTo(RadarRoute) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isRadarSelected) Icons.Filled.ShowChart else Icons.Outlined.ShowChart,
                                    contentDescription = "Pasaran"
                                )
                            },
                            label = { Text("Pasaran") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = CyberCyanBright,
                                selectedTextColor = CyberCyanBright,
                                indicatorColor = CyberCyanTint,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("rail_radar")
                        )
                        NavigationRailItem(
                            selected = isSettingsSelected,
                            onClick = {
                                navController.navigate(SettingsRoute) {
                                    popUpTo(RadarRoute) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Tetapan"
                                )
                            },
                            label = { Text("Tetapan") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = CyberCyanBright,
                                selectedTextColor = CyberCyanBright,
                                indicatorColor = CyberCyanTint,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("rail_settings")
                        )
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (initialPair != null) CoinDetailRoute(initialPair) else RadarRoute,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable<RadarRoute> {
                        RadarHomeScreen(
                            uiState = uiState,
                            onSelectFilter = viewModel::setFilter,
                            onManualRefresh = viewModel::triggerManualScan,
                            onCoinClick = { pair ->
                                navController.navigate(CoinDetailRoute(pair))
                            }
                        )
                    }

                    composable<SettingsRoute> {
                        SettingsScreen(
                            settings = settings,
                            errorLogs = errorLogs,
                            isTestingApi = isTestingApi,
                            apiTestResult = apiTestResult,
                            onSaveSettings = viewModel::saveSettings,
                            onTestApi = viewModel::testCustomApi,
                            onClearLogs = viewModel::clearLogs
                        )
                    }

                    composable<CoinDetailRoute> { entry ->
                        val route = entry.toRoute<CoinDetailRoute>()
                        val coinDetail by viewModel.observeCoinDetail(route.pair)
                            .collectAsStateWithLifecycle(initialValue = null)

                        CoinDetailScreen(
                            coin = coinDetail,
                            onBack = {
                                if (!navController.popBackStack()) {
                                    navController.navigate(RadarRoute) {
                                        popUpTo(RadarRoute) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
