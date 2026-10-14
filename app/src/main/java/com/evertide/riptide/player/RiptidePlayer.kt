package com.evertide.riptide.player

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.evertide.riptide.data.repository.PlayerRepository

@UnstableApi
class RiptidePlayer(
        exoPlayer: ExoPlayer,
        private val playerRepository: PlayerRepository,
) : ForwardingPlayer(exoPlayer) {

    override fun seekToNext() {
        playerRepository.skipNext()
    }

    override fun seekToPrevious() {
        playerRepository.previous()
    }

    override fun seekToNextMediaItem() = playerRepository.skipNext()
    override fun seekToPreviousMediaItem() = playerRepository.previous()

    override fun getAvailableCommands(): Player.Commands {
        return super.getAvailableCommands()
                .buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM, -> true
            else -> super.isCommandAvailable(command)
        }
    }
}
