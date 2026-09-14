package com.evertide.riptide.data.repository

import com.evertide.riptide.data.model.AddSongRequest
import com.evertide.riptide.data.model.CreatePlaylistRequest
import com.evertide.riptide.data.model.Playlist
import com.evertide.riptide.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository
@Inject
constructor(
        private val api: ApiService,
) {
    suspend fun getPlaylists(): List<Playlist> = api.getPlaylists()

    suspend fun getPlaylist(id: String): Playlist = api.getPlaylist(id)

    suspend fun createPlaylist(name: String) = api.createPlaylist(CreatePlaylistRequest(name))

    suspend fun addSong(playlistId: String, songId: String) =
            api.addSongToPlaylist(playlistId, AddSongRequest(songId))

    suspend fun removeSong(playlistId: String, musicId: String) =
            api.removeSongFromPlaylist(playlistId, musicId)
}
