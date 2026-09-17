package com.evertide.riptide.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.evertide.riptide.ui.artists.ArtistDetailScreen
import com.evertide.riptide.ui.artists.ArtistsScreen
import com.evertide.riptide.ui.auth.AuthScreen
import com.evertide.riptide.ui.common.ToastBus
import com.evertide.riptide.ui.common.ToastHost
import com.evertide.riptide.ui.downloads.DownloadsOverlay
import com.evertide.riptide.ui.home.HomeScreen
import com.evertide.riptide.ui.player.ExpandedPlayer
import com.evertide.riptide.ui.player.MiniBar
import com.evertide.riptide.ui.player.PlayerViewModel
import com.evertide.riptide.ui.player.QueuePanel
import com.evertide.riptide.ui.playlists.PlaylistDetailScreen
import com.evertide.riptide.ui.playlists.PlaylistsScreen
import com.evertide.riptide.ui.serversetup.ServerSetupScreen
import com.evertide.riptide.ui.settings.SettingsScreen
import com.evertide.riptide.ui.statistics.StatisticsScreen

@Composable
fun RiptideNavHost(startDestination: String, toastBus: ToastBus) {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route
    val showBottomNav = currentRoute in Routes.bottomNavRoutes

    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.playerRepository.state.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
                bottomBar = {
                    Column {
                        if (playerState.currentTrack != null) {
                            MiniBar(onExpand = { expanded = true })
                        }
                        if (showBottomNav) {
                            BottomNavBar(navController)
                        }
                    }
                },
        ) { innerPadding ->
            NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.padding(innerPadding),
            ) {
                composable(Routes.SERVER_SETUP) { ServerSetupScreen(navController) }
                composable(Routes.AUTH) { AuthScreen(navController) }
                composable(Routes.HOME) { HomeScreen(navController) }
                composable(Routes.ARTISTS) { ArtistsScreen(navController) }
                composable(
                        route = Routes.ARTIST_DETAIL,
                        arguments = listOf(navArgument("artistId") { type = NavType.StringType }),
                ) { ArtistDetailScreen(navController) }
                composable(Routes.PLAYLISTS) { PlaylistsScreen(navController) }
                composable(
                        route = Routes.PLAYLIST_DETAIL,
                        arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
                ) { PlaylistDetailScreen(navController) }
                composable(Routes.STATISTICS) { StatisticsScreen() }
                composable(Routes.SETTINGS) { SettingsScreen(navController) }
            }
        }

        if (expanded && playerState.currentTrack != null) {
            ExpandedPlayer(
                    onCollapse = { expanded = false },
                    onOpenQueue = {
                        expanded = false
                        showQueue = true
                    },
            )
        }

        if (showQueue) {
            QueuePanel(onDismiss = { showQueue = false }, navController = navController)
        }

        ToastHost(bus = toastBus)
        DownloadsOverlay()
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) { Text(label) }
}
