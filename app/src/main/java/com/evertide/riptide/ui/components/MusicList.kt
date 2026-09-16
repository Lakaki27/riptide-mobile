package com.evertide.riptide.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.evertide.riptide.R
import com.evertide.riptide.data.model.Music
import com.evertide.riptide.data.repository.PlayerRepository

enum class SortField(@StringRes val labelRes: Int) {
    CREATED_AT(R.string.filter_created_at),
    NAME(R.string.filter_name),
    ARTIST(R.string.filter_artist),
    DURATION(R.string.filter_duration),
}

@Composable
fun MusicList(
        musics: List<Music>,
        modifier: Modifier = Modifier,
        queueMusics: List<Music> = musics,
        playlistId: String? = null,
        onRemoved: (() -> Unit)? = null,
        activeTrackId: String? = null,
        compact: Boolean = false,
        showMenu: Boolean = true,
        showFilters: Boolean = true,
        sourceId: String = "default",
        onArtistClick: ((Music) -> Unit)? = null,
        onPlayOverride: ((Music) -> Unit)? = null,
        playerRepository: PlayerRepository? = null,
) {
    var sortField by remember { mutableStateOf(SortField.CREATED_AT) }
    var ascending by remember { mutableStateOf(false) }

    val sorted =
            remember(musics, sortField, ascending) {
                val comparator =
                        when (sortField) {
                            SortField.CREATED_AT -> compareBy<Music> { it.createdAt }
                            SortField.NAME -> compareBy { it.title.lowercase() }
                            SortField.ARTIST -> compareBy { it.artist.name.lowercase() }
                            SortField.DURATION -> compareBy { it.durationSeconds }
                        }
                val result = musics.sortedWith(comparator)
                if (ascending) result else result.reversed()
            }

    LaunchedEffect(sorted) { playerRepository?.sortQueue(sorted) }

    Column(modifier = modifier) {
        if (showFilters) {
            Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { menuExpanded = true }) {
                        Text(stringResource(sortField.labelRes))
                    }
                    DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                    ) {
                        SortField.entries.forEach { field ->
                            DropdownMenuItem(
                                    text = { Text(stringResource(field.labelRes)) },
                                    onClick = {
                                        sortField = field
                                        menuExpanded = false
                                    },
                            )
                        }
                    }
                }
                IconButton(onClick = { ascending = !ascending }) {
                    Icon(
                            if (ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                            contentDescription = "Toggle sort direction",
                    )
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(sorted, key = { it.id }) { music ->
                MusicRow(
                        music = music,
                        onPlay = {
                            if (onPlayOverride != null) {
                                onPlayOverride(music)
                            } else {
                                playerRepository?.playFromList(queueMusics, music, sourceId)
                            }
                        },
                        active = music.id == activeTrackId,
                        compact = compact,
                        showMenu = showMenu,
                        onArtistClick = onArtistClick?.let { { it(music) } },
                        playlistId = playlistId,
                        onRemoved = onRemoved,
                )
            }
        }
    }
}
