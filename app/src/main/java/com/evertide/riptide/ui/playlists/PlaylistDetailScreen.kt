package com.evertide.riptide.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.evertide.riptide.navigation.Routes
import com.evertide.riptide.ui.components.MusicList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
        navController: NavHostController,
        viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerRepository.state.collectAsState()
    val playlist = state.playlist

    if (playlist == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

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
                Text(playlist.name, style = MaterialTheme.typography.titleLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.setAddModalVisible(true) }) {
                    Text("Add song")
                }
                Button(onClick = viewModel::playAll) { Text("Play all") }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
                value = state.localQuery,
                onValueChange = viewModel::onLocalQueryChange,
                placeholder = { Text("Search in this playlist") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        MusicList(
                musics = viewModel.filteredMusics(),
                queueMusics = playlist.musics,
                playlistId = playlist.id,
                onRemoved = viewModel::loadPlaylist,
                sourceId = "playlist_${playlist.id}",
                activeTrackId = playerState.currentTrack?.id,
                playerRepository = viewModel.playerRepository,
                onArtistClick = { music ->
                    navController.navigate(Routes.artistDetail(music.artist.id))
                },
                modifier = Modifier.weight(1f),
        )
    }

    if (state.showAddModal) {
        ModalBottomSheet(onDismissRequest = { viewModel.setAddModalVisible(false) }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).height(500.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Add song", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { viewModel.setAddModalVisible(false) }) { Text("Close") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                        value = state.addQuery,
                        onValueChange = viewModel::onAddQueryChange,
                        placeholder = { Text("Search songs...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(viewModel.visibleAddResults(), key = { it.id }) { music ->
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { viewModel.addSong(music.id) }
                                                .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AsyncImage(
                                    model = music.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                            )
                            Column {
                                Text(music.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                        music.artist.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
