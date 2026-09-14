package com.evertide.riptide.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evertide.riptide.data.model.Music

@Composable
fun SongMenu(
        music: Music,
        playlistId: String? = null,
        onRemoved: (() -> Unit)? = null,
        viewModel: SongMenuViewModel = hiltViewModel(),
) {
    var expanded by remember { mutableStateOf(false) }
    var showPlaylistSubmenu by remember { mutableStateOf(false) }
    val playlists by viewModel.playlists.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    Box {
        IconButton(
                onClick = {
                    expanded = true
                    showPlaylistSubmenu = false
                    viewModel.ensurePlaylistsLoaded()
                }
        ) { Icon(Icons.Filled.MoreVert, contentDescription = "Song options") }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                    text = { Text("Add to queue") },
                    onClick = {
                        viewModel.addToQueue(music)
                        expanded = false
                    },
            )
            DropdownMenuItem(
                    text = { Text("Play next") },
                    onClick = {
                        viewModel.playNext(music)
                        expanded = false
                    },
            )

            if (isAdmin) {
                DropdownMenuItem(
                        text = { Text("Add to playlist ›") },
                        onClick = { showPlaylistSubmenu = true },
                )
            }

            if (playlistId != null) {
                DropdownMenuItem(
                        text = { Text("Remove from playlist") },
                        onClick = {
                            viewModel.removeFromPlaylist(playlistId, music.id)
                            onRemoved?.invoke()
                            expanded = false
                        },
                )
            }
        }

        DropdownMenu(
                expanded = showPlaylistSubmenu,
                onDismissRequest = { showPlaylistSubmenu = false }
        ) {
            if (playlists.isEmpty()) {
                DropdownMenuItem(text = { Text("No playlists") }, onClick = {}, enabled = false)
            } else {
                playlists.forEach { playlist ->
                    DropdownMenuItem(
                            text = {
                                Text(
                                        playlist.name,
                                        modifier = androidx.compose.ui.Modifier.width(180.dp)
                                )
                            },
                            onClick = {
                                viewModel.addToPlaylist(playlist.id, playlist.name, music.id)
                                showPlaylistSubmenu = false
                                expanded = false
                            },
                    )
                }
            }
        }
    }
}
