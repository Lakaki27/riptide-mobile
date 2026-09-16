package com.evertide.riptide.ui.player

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.evertide.riptide.R
import com.evertide.riptide.data.repository.PlayMode
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ExpandedPlayer(
        onCollapse: () -> Unit,
        onOpenQueue: () -> Unit,
        playerViewModel: PlayerViewModel = hiltViewModel(),
        ambientViewModel: ExpandedPlayerViewModel = hiltViewModel(),
) {
    val state by playerViewModel.playerRepository.state.collectAsState()
    val positionMs by playerViewModel.playerRepository.currentPositionMs.collectAsState()
    val track =
            state.currentTrack
                    ?: run {
                        onCollapse()
                        return
                    }

    LaunchedEffect(track.id) { ambientViewModel.loadAmbientColor(track.thumbnailUrl) }

    var dragOffsetY by remember { mutableStateOf(0f) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var axisLocked by remember { mutableStateOf("none") }

    Surface(
            modifier =
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        detectVerticalDragGestures(
                                onDragEnd = {
                                    if (axisLocked == "vertical" && dragOffsetY > 300f) onCollapse()
                                    dragOffsetY = 0f
                                    axisLocked = "none"
                                },
                        ) { change, dragAmount ->
                            if (axisLocked == "none" && abs(dragAmount) > 5) axisLocked = "vertical"
                            if (axisLocked == "vertical" && dragAmount > 0) {
                                dragOffsetY += dragAmount
                                change.consume()
                            }
                        }
                    },
    ) {
        Column(
                modifier = Modifier.fillMaxSize().offset { IntOffset(0, dragOffsetY.roundToInt()) },
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse")
                }
                Text(
                        stringResource(R.string.now_playing),
                        style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(48.dp))
            }

            Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
            ) {
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .aspectRatio(1f)
                                        .pointerInput(track.id) {
                                            detectHorizontalDragGestures(
                                                    onDragEnd = {
                                                        if (abs(dragOffsetX) > 150f) {
                                                            if (dragOffsetX < 0)
                                                                    playerViewModel.playerRepository
                                                                            .skipNext()
                                                            else
                                                                    playerViewModel.playerRepository
                                                                            .previous()
                                                        }
                                                        dragOffsetX = 0f
                                                    },
                                            ) { change, dragAmount ->
                                                dragOffsetX += dragAmount
                                                change.consume()
                                            }
                                        }
                                        .offset { IntOffset(dragOffsetX.roundToInt(), 0) },
                ) {
                    AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                    )
                }

                Spacer(Modifier.height(32.dp))

                Text(
                        track.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().basicMarquee(),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                )
                Text(
                        track.artist.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(24.dp))

                val durationMs = (track.durationSeconds * 1000f).coerceAtLeast(1f)
                Slider(
                        value = positionMs.toFloat().coerceIn(0f, durationMs),
                        onValueChange = { playerViewModel.playerRepository.seekTo(it.toLong()) },
                        valueRange = 0f..durationMs,
                        modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(positionMs / 1000), style = MaterialTheme.typography.bodyMedium)
                    Text(
                            formatTime(track.durationSeconds.toLong()),
                            style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { cycleMode(playerViewModel, state.mode) }) {
                        Icon(modeIcon(state.mode), contentDescription = "Playback mode")
                    }
                    IconButton(onClick = { playerViewModel.playerRepository.previous() }) {
                        Icon(
                                Icons.Filled.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(32.dp)
                        )
                    }
                    FilledIconButton(
                            onClick = { playerViewModel.playerRepository.togglePlay() },
                            modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                                if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(32.dp),
                        )
                    }
                    IconButton(onClick = { playerViewModel.playerRepository.skipNext() }) {
                        Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = onOpenQueue) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue")
                    }
                }
            }
        }
    }
}

private fun cycleMode(viewModel: PlayerViewModel, current: PlayMode) {
    val order =
            listOf(PlayMode.NORMAL, PlayMode.LOOP_SONG, PlayMode.LOOP_PLAYLIST, PlayMode.RANDOMIZE)
    val next = order[(order.indexOf(current) + 1) % order.size]
    viewModel.playerRepository.setMode(next)
}

private fun modeIcon(mode: PlayMode) =
        when (mode) {
            PlayMode.NORMAL -> Icons.Filled.MusicNote
            PlayMode.LOOP_SONG -> Icons.Filled.RepeatOne
            PlayMode.LOOP_PLAYLIST -> Icons.Filled.Repeat
            PlayMode.RANDOMIZE -> Icons.Filled.Shuffle
        }

private fun formatTime(totalSeconds: Long): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
