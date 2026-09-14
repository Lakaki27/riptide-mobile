package com.evertide.riptide.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.evertide.riptide.navigation.Routes

@Composable
fun PlaylistsScreen(
        navController: NavHostController,
        viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Playlists", style = MaterialTheme.typography.titleLarge)
            if (isAdmin) {
                Button(onClick = { showCreateDialog = true }) { Text("New playlist") }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.playlists, key = { it.id }) { playlist ->
                    Column(
                            modifier =
                                    Modifier.clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clickable {
                                                navController.navigate(
                                                        Routes.playlistDetail(playlist.id)
                                                )
                                            }
                                            .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .aspectRatio(1.2f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                    playlist.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                                playlist.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("New playlist") },
                text = {
                    OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("Playlist name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                viewModel.createPlaylist(newName)
                                newName = ""
                                showCreateDialog = false
                            }
                    ) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                },
        )
    }
}
