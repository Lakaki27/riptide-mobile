package com.evertide.riptide.ui.artists

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.evertide.riptide.ui.components.MusicList

@Composable
fun ArtistDetailScreen(
        navController: NavHostController,
        viewModel: ArtistDetailViewModel = hiltViewModel(),
) {
    val artist by viewModel.artist.collectAsState()
    val playerState by viewModel.playerRepository.state.collectAsState()

    if (artist == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val current = artist!!

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(current.name, style = MaterialTheme.typography.titleLarge)
            }
            Button(onClick = viewModel::playAll) { Text("Play all") }
        }

        Spacer(Modifier.height(12.dp))

        MusicList(
                musics = current.musics,
                queueMusics = current.musics,
                sourceId = "artist_${current.id}",
                activeTrackId = playerState.currentTrack?.id,
                playerRepository = viewModel.playerRepository,
                modifier = Modifier.weight(1f),
        )
    }
}
