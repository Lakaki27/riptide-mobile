package com.evertide.riptide.data.repository

import com.evertide.riptide.data.model.Music
import com.evertide.riptide.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository
@Inject
constructor(
        private val api: ApiService,
) {
    suspend fun getAllMusics(): List<Music> = api.getMusics()

    suspend fun search(query: String): List<Music> = api.search(type = "music", query = query)

    suspend fun deleteMusic(id: String) = api.deleteMusic(id)

    suspend fun getStreamUrl(musicId: String): String = api.getStreamUrl(musicId).url
}
