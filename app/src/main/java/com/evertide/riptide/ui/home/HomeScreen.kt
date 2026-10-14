package com.evertide.riptide.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.evertide.riptide.R
import com.evertide.riptide.navigation.Routes
import com.evertide.riptide.ui.components.MusicList

@Composable
fun HomeScreen(
        navController: NavHostController,
        viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val activeTrackId by viewModel.activeTrackId.collectAsStateWithLifecycle()
    var showAddModal by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.nav_home), style = MaterialTheme.typography.titleLarge)

            IconButton(onClick = { viewModel.loadMusics() }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh songs")
            }

            if (isAdmin) {
                Button(onClick = { showAddModal = true }) {
                    Text(stringResource(R.string.add_music))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text(stringResource(R.string.search_songs)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(4.dp)) {
            if (state.refreshing || state.searching) LinearProgressIndicator(Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(8.dp))

        when {
            state.loading ->
                    Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                        CircularProgressIndicator()
                    }
            state.error && state.allMusics.isEmpty() ->
                    Column(
                            Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.load_error))
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = viewModel::loadMusics) {
                            Text(stringResource(R.string.retry))
                        }
                    }
            else ->
                    MusicList(
                            musics = state.musics,
                            queueMusics = state.allMusics,
                            sourceId = "home",
                            activeTrackId = activeTrackId,
                            modifier = Modifier.weight(1f),
                            playerRepository = viewModel.playerRepository,
                            onArtistClick = {
                                navController.navigate(Routes.artistDetail(it.artist.id))
                            },
                    )
        }
    }

    if (showAddModal) {
        AddMusicModal(onClose = { showAddModal = false })
    }
}
