package com.evertide.riptide.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.evertide.riptide.navigation.Routes
import com.evertide.riptide.ui.components.MusicList

@Composable
fun HomeScreen(
        navController: NavHostController,
        viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val playerState by viewModel.playerRepository.state.collectAsState()
    var showAddModal by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text("Home", style = MaterialTheme.typography.titleLarge)

            IconButton(onClick = { viewModel.loadMusics() }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh songs")
            }

            if (isAdmin) {
                Button(onClick = { showAddModal = true }) { Text("Add music") }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Search songs...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        if (state.loading) {
            Box(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = androidx.compose.ui.Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            MusicList(
                    musics = state.musics,
                    queueMusics = state.allMusics,
                    sourceId = "home",
                    activeTrackId = playerState.currentTrack?.id,
                    modifier = Modifier.weight(1f),
                    playerRepository = viewModel.playerRepository,
                    onArtistClick = { music ->
                        navController.navigate(Routes.artistDetail(music.artist.id))
                    },
            )
        }
    }

    if (showAddModal) {
        AddMusicModal(onClose = { showAddModal = false })
    }
}
