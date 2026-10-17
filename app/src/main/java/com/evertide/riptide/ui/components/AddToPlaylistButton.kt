package com.evertide.riptide.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.evertide.riptide.R
import com.evertide.riptide.data.model.Music

@Composable
fun AddToPlaylistButton(
        music: Music,
        viewModel: SongMenuViewModel = hiltViewModel(),
) {
    val isAdmin by viewModel.isAdmin.collectAsState()

    // Keep the header layout balanced (title stays centered) for non-admins
    if (!isAdmin) {
        Spacer(Modifier.width(48.dp))
        return
    }

    var expanded by remember { mutableStateOf(false) }
    val playlists by viewModel.playlists.collectAsState()

    Box {
        IconButton(
                onClick = {
                    expanded = true
                    viewModel.ensurePlaylistsLoaded()
                }
        ) {
            Icon(
                    Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = stringResource(R.string.song_menu_add_to_playlist),
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (playlists.isEmpty()) {
                DropdownMenuItem(
                        text = { Text(stringResource(R.string.song_menu_no_playlists)) },
                        onClick = {},
                        enabled = false,
                )
            } else {
                playlists.forEach { playlist ->
                    DropdownMenuItem(
                            text = { Text(playlist.name, modifier = Modifier.width(180.dp)) },
                            onClick = {
                                viewModel.addToPlaylist(playlist.id, playlist.name, music.id)
                                expanded = false
                            },
                    )
                }
            }
        }
    }
}
