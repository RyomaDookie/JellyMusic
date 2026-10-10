package com.jellymusic.player.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class JellyfinClient(
    private var baseUrl: String = "",
    private var token: String = "",
    private var userId: String = ""
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun updateCredentials(url: String, userToken: String, currentUserId: String) {
        this.baseUrl = normalizeUrl(url)
        this.token = userToken
        this.userId = currentUserId
    }

    private fun normalizeUrl(url: String): String {
        var clean = url.trim().removeSuffix("/")
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        return clean
    }

    private fun authHeader(): String {
        return "MediaBrowser Client=\"JellyMusic\", Device=\"Android\", DeviceId=\"jellymusic-device\", Version=\"1.0.0\"" +
                if (token.isNotEmpty()) ", Token=\"$token\"" else ""
    }

    suspend fun authenticate(serverUrl: String, user: String, pass: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            try {
                val cleanUrl = normalizeUrl(serverUrl)
                val authPayload = json.encodeToString(AuthRequest.serializer(), AuthRequest(Username = user, Pw = pass))
                val body = authPayload.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$cleanUrl/Users/AuthenticateByName")
                    .addHeader("X-Emby-Authorization", "MediaBrowser Client=\"JellyMusic\", Device=\"Android\", DeviceId=\"jellymusic-device\", Version=\"1.0.0\"")
                    .post(body)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Server error HTTP ${response.code}: $responseBody"))
                }

                val authResp = json.decodeFromString(AuthResponse.serializer(), responseBody)
                updateCredentials(cleanUrl, authResp.accessToken, authResp.user.id)
                Result.success(authResp)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getAudioTracks(
        searchTerm: String = "",
        startIndex: Int = 0,
        limit: Int = 1000,
        sortBy: String = "DateCreated",
        sortOrder: String = "Descending"
    ): Result<List<JellyfinItem>> = withContext(Dispatchers.IO) {
        try {
            var url = "$baseUrl/Users/$userId/Items?IncludeItemTypes=Audio&Recursive=true&SortBy=$sortBy&SortOrder=$sortOrder&StartIndex=$startIndex&Limit=$limit&Fields=DateCreated,UserData,MediaSources"
            if (searchTerm.isNotBlank()) {
                val encoded = URLEncoder.encode(searchTerm.trim(), "UTF-8")
                url += "&SearchTerm=$encoded"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("X-Emby-Authorization", authHeader())
                .cacheControl(CacheControl.FORCE_NETWORK)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to fetch tracks: ${response.code}"))
            }

            val itemsResp = json.decodeFromString(ItemsResponse.serializer(), responseBody)
            Result.success(itemsResp.items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshLibraryScan(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val refreshUrl = if (baseUrl.isNotBlank() && !baseUrl.contains("emby.xantu-bull.ts.net")) {
                "$baseUrl/Library/Refresh?api_key=a58a15c785944afc9bc00dc7437053ee"
            } else {
                "https://emby.xantu-bull.ts.net/Library/Refresh?api_key=a58a15c785944afc9bc00dc7437053ee"
            }
            val request = Request.Builder()
                .url(refreshUrl)
                .addHeader("X-Emby-Authorization", authHeader())
                .post("".toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful || response.code in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Server error HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAlbums(): Result<List<JellyfinItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/Users/$userId/Items?IncludeItemTypes=MusicAlbum&Recursive=true&SortBy=SortName&SortOrder=Ascending"
            val request = Request.Builder()
                .url(url)
                .addHeader("X-Emby-Authorization", authHeader())
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to fetch albums: ${response.code}"))
            }

            val itemsResp = json.decodeFromString(ItemsResponse.serializer(), responseBody)
            Result.success(itemsResp.items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAlbumTracks(albumId: String): Result<List<JellyfinItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/Users/$userId/Items?ParentId=$albumId&IncludeItemTypes=Audio&SortBy=IndexNumber,SortName&SortOrder=Ascending"
            val request = Request.Builder()
                .url(url)
                .addHeader("X-Emby-Authorization", authHeader())
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to fetch album tracks: ${response.code}"))
            }

            val itemsResp = json.decodeFromString(ItemsResponse.serializer(), responseBody)
            Result.success(itemsResp.items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaylists(): Result<List<JellyfinItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/Users/$userId/Items?IncludeItemTypes=Playlist&Recursive=true&SortBy=SortName&SortOrder=Ascending"
            val request = Request.Builder()
                .url(url)
                .addHeader("X-Emby-Authorization", authHeader())
                .cacheControl(CacheControl.FORCE_NETWORK)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to fetch playlists: ${response.code}"))
            }

            val itemsResp = json.decodeFromString(ItemsResponse.serializer(), responseBody)
            Result.success(itemsResp.items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaylistTracks(playlistId: String): Result<List<JellyfinItem>> = withContext(Dispatchers.IO) {
        try {
            val queryUserId = if (userId.isNotBlank()) "?userId=$userId" else ""
            var url = "$baseUrl/Playlists/$playlistId/Items$queryUserId"
            var request = Request.Builder()
                .url(url)
                .addHeader("X-Emby-Authorization", authHeader())
                .cacheControl(CacheControl.FORCE_NETWORK)
                .get()
                .build()

            var response = httpClient.newCall(request).execute()
            var responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val itemsResp = json.decodeFromString(ItemsResponse.serializer(), responseBody)
                if (itemsResp.items.isNotEmpty()) {
                    return@withContext Result.success(itemsResp.items)
                }
            }

            // Fallback: /Items?ParentId=$playlistId&UserId=$userId
            url = if (userId.isNotBlank()) {
                "$baseUrl/Items?ParentId=$playlistId&UserId=$userId"
            } else {
                "$baseUrl/Items?ParentId=$playlistId"
            }
            request = Request.Builder()
                .url(url)
                .addHeader("X-Emby-Authorization", authHeader())
                .cacheControl(CacheControl.FORCE_NETWORK)
                .get()
                .build()

            response = httpClient.newCall(request).execute()
            responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val itemsResp = json.decodeFromString(ItemsResponse.serializer(), responseBody)
                return@withContext Result.success(itemsResp.items)
            }
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPlaylist(name: String, itemIds: List<String> = emptyList()): Result<String> = withContext(Dispatchers.IO) {
        try {
            val server = if (baseUrl.isNotBlank() && !baseUrl.contains("localhost")) baseUrl else "https://emby.xantu-bull.ts.net"
            val encodedName = URLEncoder.encode(name.trim(), "UTF-8")
            val idsParam = if (itemIds.isNotEmpty()) "&ids=${itemIds.joinToString(",")}" else ""
            val userParam = if (userId.isNotBlank()) "&userId=$userId" else ""
            val url = "$server/Playlists?name=$encodedName&mediaType=Audio$idsParam$userParam"

            val body = "{}".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("X-Emby-Authorization", authHeader())
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful || response.code in 200..299) {
                if (baseUrl.isNotBlank() && baseUrl != server) {
                    try {
                        val secondaryUrl = "$baseUrl/Playlists?name=$encodedName&mediaType=Audio$idsParam$userParam"
                        val secReq = Request.Builder()
                            .url(secondaryUrl)
                            .addHeader("X-Emby-Authorization", authHeader())
                            .post(body)
                            .build()
                        httpClient.newCall(secReq).execute()
                    } catch (_: Exception) {}
                }
                Result.success(responseBody)
            } else {
                Result.failure(IOException("Server error HTTP ${response.code}: $responseBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addItemToPlaylist(playlistId: String, itemIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val server = if (baseUrl.isNotBlank() && !baseUrl.contains("localhost")) baseUrl else "https://emby.xantu-bull.ts.net"
            val idsParam = itemIds.joinToString(",")
            val userParam = if (userId.isNotBlank()) "&userId=$userId" else ""
            val url = "$server/Playlists/$playlistId/Items?ids=$idsParam$userParam"

            val body = "".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("X-Emby-Authorization", authHeader())
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful || response.code in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Server error HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFavorites(): Result<List<JellyfinItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/Users/$userId/Items?IncludeItemTypes=Audio&Recursive=true&Filters=IsFavorite&SortBy=DateCreated&SortOrder=Descending&Fields=DateCreated,UserData,MediaSources"
            val request = Request.Builder()
                .url(url)
                .addHeader("X-Emby-Authorization", authHeader())
                .cacheControl(CacheControl.FORCE_NETWORK)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to fetch favorites: ${response.code}"))
            }

            val itemsResp = json.decodeFromString(ItemsResponse.serializer(), responseBody)
            Result.success(itemsResp.items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setFavorite(itemId: String, isFavorite: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (isFavorite) "POST" else "DELETE"
            val url = "$baseUrl/Users/$userId/FavoriteItems/$itemId"
            val body = "".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("X-Emby-Authorization", authHeader())
                .method(endpoint, if (isFavorite) body else null)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful || response.code in 200..299) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Server error HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLyrics(itemId: String): Result<List<LyricLineDto>> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/Audio/$itemId/Lyrics"
            val request = Request.Builder()
                .url(url)
                .addHeader("X-Emby-Authorization", authHeader())
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful || body.isBlank()) {
                return@withContext Result.failure(IOException("Lyrics not found: ${response.code}"))
            }

            if (body.trimStart().startsWith("{")) {
                val lyricsResp = json.decodeFromString(LyricsResponse.serializer(), body)
                val lines = lyricsResp.lyrics.orEmpty().filter { it.text.isNotBlank() }
                return@withContext Result.success(lines)
            }

            // Fallback for LRC or plain-text lines
            val lrcRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\](.*)""")
            val parsedLines = mutableListOf<LyricLineDto>()
            for (raw in body.lines()) {
                val trimmed = raw.trim()
                if (trimmed.isEmpty()) continue
                val match = lrcRegex.find(trimmed)
                if (match != null) {
                    val min = match.groupValues[1].toLongOrNull() ?: 0L
                    val sec = match.groupValues[2].toLongOrNull() ?: 0L
                    val msPart = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                    val totalMs = (min * 60 + sec) * 1000 + msPart
                    val text = match.groupValues[4].trim()
                    if (text.isNotEmpty()) {
                        parsedLines.add(LyricLineDto(text = text, startTicks = totalMs * 10_000L))
                    }
                } else if (!trimmed.startsWith("[")) {
                    parsedLines.add(LyricLineDto(text = trimmed, startTicks = null))
                }
            }
            Result.success(parsedLines)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getStreamUrl(itemId: String, maxBitrateKbps: Int = 0): String {
        var url = "$baseUrl/Audio/$itemId/stream?static=true"
        if (maxBitrateKbps > 0) {
            // Request transcoding if limited bitrate
            url = "$baseUrl/Audio/$itemId/stream?audioCodec=mp3&maxStreamingBitrate=${maxBitrateKbps * 1000}"
        }
        if (token.isNotEmpty()) {
            url += "&api_key=$token"
        }
        return url
    }

    fun getArtworkUrl(itemId: String, maxWidth: Int = 500): String {
        return "$baseUrl/Items/$itemId/Images/Primary?maxWidth=$maxWidth&api_key=$token"
    }

    fun getAlbumArtworkUrl(albumId: String?, maxWidth: Int = 500): String {
        return if (!albumId.isNullOrEmpty()) {
            "$baseUrl/Items/$albumId/Images/Primary?maxWidth=$maxWidth&api_key=$token"
        } else ""
    }

    fun downloadArtworkToFileSync(itemId: String, destinationFile: java.io.File): Boolean {
        val url = getArtworkUrl(itemId, maxWidth = 500)
        if (url.isBlank()) return false
        return try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
