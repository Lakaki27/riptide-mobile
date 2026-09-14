package com.evertide.riptide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.evertide.riptide.data.model.Music

@Composable
fun MusicRow(
        music: Music,
        onPlay: () -> Unit,
        active: Boolean = false,
        compact: Boolean = false,
        showMenu: Boolean = true,
        onArtistClick: (() -> Unit)? = null,
        playlistId: String? = null,
        onRemoved: (() -> Unit)? = null,
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                    if (active) MaterialTheme.colorScheme.surfaceVariant
                                    else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .clickable(onClick = onPlay)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
                model = music.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                        Modifier.size(if (compact) 32.dp else 40.dp).clip(RoundedCornerShape(8.dp)),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                    music.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                    music.artist.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                            if (onArtistClick != null) {
                                Modifier.clickable(onClick = onArtistClick)
                            } else {
                                Modifier
                            },
            )
        }

        if (showMenu) {
            SongMenu(music = music, playlistId = playlistId, onRemoved = onRemoved)
        }
    }
}
