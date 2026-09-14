package com.evertide.riptide.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

private data class BottomNavItem(
        val route: String,
        val labelRes: String, // resolved via stringResource at call site
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val items =
        listOf(
                BottomNavItem(Routes.HOME, "nav_home", Icons.Filled.Home),
                BottomNavItem(Routes.ARTISTS, "nav_artists", Icons.Filled.Mic),
                BottomNavItem(Routes.PLAYLISTS, "nav_playlists", Icons.Filled.PlaylistPlay),
                BottomNavItem(Routes.STATISTICS, "nav_stats", Icons.Filled.BarChart),
                BottomNavItem(Routes.SETTINGS, "nav_settings", Icons.Filled.Settings),
        )

@Composable
fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                    selected = selected,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(labelFor(item.labelRes)) },
            )
        }
    }
}

// Temporary literal labels until Phase 11 wires real string resources everywhere.
private fun labelFor(key: String): String =
        when (key) {
            "nav_home" -> "Home"
            "nav_artists" -> "Artists"
            "nav_playlists" -> "Playlists"
            "nav_stats" -> "Stats"
            "nav_settings" -> "Settings"
            else -> key
        }
