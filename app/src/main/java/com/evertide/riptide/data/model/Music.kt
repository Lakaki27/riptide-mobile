package com.evertide.riptide.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val createdAt: String,
)

@Serializable
data class Music(
    val id: String,
    val title: String,
    val artist: Artist,
    val durationSeconds: Int,
    val createdAt: String,
    val thumbnailUrl: String? = null,
    val codec: String? = null,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val createdAt: String,
    val musics: List<Music> = emptyList(),
)
