package com.evertide.riptide.data.repository

import com.evertide.riptide.data.model.EarliestPlayResponse
import com.evertide.riptide.data.model.HeatmapResponse
import com.evertide.riptide.data.model.RecordPlayRequest
import com.evertide.riptide.data.model.TopArtistsResponse
import com.evertide.riptide.data.model.TopSongsResponse
import com.evertide.riptide.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository
@Inject
constructor(
        private val api: ApiService,
) {
    suspend fun recordPlay(musicId: String) = api.recordPlay(RecordPlayRequest(musicId))

    suspend fun getHeatmap(startDate: String, endDate: String, scope: String): HeatmapResponse =
            api.getHeatmap(startDate, endDate, scope)

    suspend fun getTopSongs(limit: Int, scope: String): TopSongsResponse =
            api.getTopSongs(limit, scope)

    suspend fun getTopArtists(limit: Int, scope: String): TopArtistsResponse =
            api.getTopArtists(limit, scope)

    suspend fun getEarliestPlay(): EarliestPlayResponse = api.getEarliestPlay()
}
