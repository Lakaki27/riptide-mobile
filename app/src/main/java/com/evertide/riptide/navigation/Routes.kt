package com.evertide.riptide.navigation

object Routes {
    const val SERVER_SETUP = "server_setup"
    const val AUTH = "auth"

    const val HOME = "home"
    const val ARTISTS = "artists"
    const val ARTIST_DETAIL = "artists/{artistId}"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_DETAIL = "playlists/{playlistId}"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"

    fun artistDetail(id: String) = "artists/$id"
    fun playlistDetail(id: String) = "playlists/$id"

    /** Routes that show the bottom nav bar. */
    val bottomNavRoutes = setOf(HOME, ARTISTS, PLAYLISTS, STATISTICS, SETTINGS)
}
