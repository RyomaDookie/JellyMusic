package com.jellymusic.player.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val Username: String,
    val Pw: String? = null
)

@Serializable
data class AuthResponse(
    @SerialName("AccessToken") val accessToken: String,
    @SerialName("User") val user: UserDto,
    @SerialName("ServerId") val serverId: String? = null
)

@Serializable
data class UserDto(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String
)

@Serializable
data class ItemsResponse(
    @SerialName("Items") val items: List<JellyfinItem> = emptyList(),
    @SerialName("TotalRecordCount") val totalRecordCount: Int = 0
)

@Serializable
data class JellyfinItem(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("Type") val type: String = "",
    @SerialName("Artists") val artists: List<String>? = null,
    @SerialName("ArtistItems") val artistItems: List<NameIdPair>? = null,
    @SerialName("Album") val album: String? = null,
    @SerialName("AlbumId") val albumId: String? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("IndexNumber") val indexNumber: Int? = null, // Track number
    @SerialName("ParentIndexNumber") val discNumber: Int? = null,
    @SerialName("Container") val container: String? = null,
    @SerialName("MediaSources") val mediaSources: List<MediaSource>? = null,
    @SerialName("ImageTags") val imageTags: Map<String, String>? = null,
    @SerialName("AlbumPrimaryImageTag") val albumPrimaryImageTag: String? = null,
    @SerialName("UserData") val userData: UserItemDataDto? = null,
    @SerialName("DateCreated") val dateCreated: String? = null
) {
    val artistName: String
        get() = artists?.firstOrNull() ?: artistItems?.firstOrNull()?.name ?: "Unknown Artist"

    val durationMs: Long
        get() = (runTimeTicks ?: 0L) / 10_000L

    val isFavorite: Boolean
        get() = userData?.isFavorite == true

    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

@Serializable
data class UserItemDataDto(
    @SerialName("IsFavorite") val isFavorite: Boolean = false,
    @SerialName("Played") val played: Boolean = false,
    @SerialName("PlayCount") val playCount: Int = 0
)

@Serializable
data class NameIdPair(
    @SerialName("Name") val name: String,
    @SerialName("Id") val id: String
)

@Serializable
data class MediaSource(
    @SerialName("Id") val id: String,
    @SerialName("Container") val container: String? = null,
    @SerialName("Bitrate") val bitrate: Int? = null,
    @SerialName("Size") val size: Long? = null
)

@Serializable
data class LyricsResponse(
    @SerialName("Lyrics") val lyrics: List<LyricLineDto>? = null
)

@Serializable
data class LyricLineDto(
    @SerialName("Text") val text: String = "",
    @SerialName("Start") val startTicks: Long? = null
) {
    val startMs: Long
        get() = (startTicks ?: 0L) / 10_000L
}
