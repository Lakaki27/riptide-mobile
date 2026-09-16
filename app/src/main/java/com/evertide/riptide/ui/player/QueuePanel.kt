package com.evertide.riptide.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.evertide.riptide.R
import com.evertide.riptide.navigation.Routes
import com.evertide.riptide.ui.components.MusicList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueuePanel(
        onDismiss: () -> Unit,
        navController: NavHostController,
        playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by playerViewModel.playerRepository.state.collectAsState()
    var query by remember { mutableStateOf("") }

    val indexed = state.queue.mapIndexed { index, music -> index to music }
    val filtered =
            if (query.isBlank()) {
                indexed
            } else {
                indexed.filter {
                    it.second.title.contains(query, ignoreCase = true) ||
                            it.second.artist.name.contains(query, ignoreCase = true)
                }
            }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).height(500.dp)) {
            Text(
                    stringResource(R.string.playing_next),
                    style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_queue)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            MusicList(
                    musics = filtered.map { it.second },
                    showMenu = false,
                    showFilters = false,
                    activeTrackId = state.currentTrack?.id,
                    onPlayOverride = { music ->
                        filtered.firstOrNull { it.second.id == music.id }?.first?.let {
                            playerViewModel.playerRepository.jumpTo(it)
                        }
                    },
                    onArtistClick = { music ->
                        onDismiss()
                        navController.navigate(Routes.artistDetail(music.artist.id))
                    },
                    modifier = Modifier.weight(1f),
            )
        }
    }
}
