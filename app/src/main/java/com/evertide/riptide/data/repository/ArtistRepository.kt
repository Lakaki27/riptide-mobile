package com.evertide.riptide.data.repository

import com.evertide.riptide.data.model.Artist
import com.evertide.riptide.data.model.ArtistDetail
import com.evertide.riptide.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtistRepository
@Inject
constructor(
        private val api: ApiService,
) {
    suspend fun getAllArtists(): List<Artist> = api.getArtists()

    suspend fun search(query: String): List<Artist> = api.searchArtists(query = query)

    suspend fun getArtistDetail(id: String): ArtistDetail = api.getArtistDetail(id)
}
