package com.jellymusic.player.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jellymusic_settings")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK, AMOLED
}

enum class AccentPalette {
    DYNAMIC, JELLYFIN_PURPLE, CYBERPUNK_CYAN, SUNSET_AMBER, EMERALD_FOREST, SAKURA_PINK
}

enum class PlayerLayoutStyle {
    MODERN_HERO, COMPACT_CARD, MINIMALIST
}

enum class StreamingQuality(val label: String, val kbps: Int) {
    DIRECT("Direct Stream (Original/Lossless)", 0),
    HIGH_320("High (320 kbps MP3)", 320),
    MEDIUM_192("Medium (192 kbps MP3)", 192),
    LOW_128("Data Saver (128 kbps MP3)", 128)
}

data class UserCustomizations(
    val themeMode: ThemeMode = ThemeMode.AMOLED,
    val accentPalette: AccentPalette = AccentPalette.JELLYFIN_PURPLE,
    val playerLayout: PlayerLayoutStyle = PlayerLayoutStyle.MODERN_HERO,
    val streamingQuality: StreamingQuality = StreamingQuality.DIRECT,
    val playbackSpeed: Float = 1.0f,
    val cornerRadiusDp: Int = 16,
    val showArtworkOnLockscreen: Boolean = true,
    val debugLogging: Boolean = false
)

class AppPreferences(private val context: Context) {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val PLAYER_LAYOUT = stringPreferencesKey("player_layout")
        val STREAMING_QUALITY = stringPreferencesKey("streaming_quality")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val CORNER_RADIUS = intPreferencesKey("corner_radius")
        val LOCKSCREEN_ART = booleanPreferencesKey("lockscreen_art")
        val DEBUG_LOGGING = booleanPreferencesKey("debug_logging")
        val RECENT_TRACK_IDS = stringPreferencesKey("recent_track_ids")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { it[Keys.SERVER_URL] ?: "" }
    val accessToken: Flow<String> = context.dataStore.data.map { it[Keys.ACCESS_TOKEN] ?: "" }
    val userId: Flow<String> = context.dataStore.data.map { it[Keys.USER_ID] ?: "" }
    val username: Flow<String> = context.dataStore.data.map { it[Keys.USERNAME] ?: "" }
    val recentTrackIds: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.RECENT_TRACK_IDS].orEmpty()
        if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
    }

    val customizations: Flow<UserCustomizations> = context.dataStore.data.map { prefs ->
        UserCustomizations(
            themeMode = ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.AMOLED.name),
            accentPalette = AccentPalette.valueOf(prefs[Keys.ACCENT_PALETTE] ?: AccentPalette.JELLYFIN_PURPLE.name),
            playerLayout = PlayerLayoutStyle.valueOf(prefs[Keys.PLAYER_LAYOUT] ?: PlayerLayoutStyle.MODERN_HERO.name),
            streamingQuality = StreamingQuality.valueOf(prefs[Keys.STREAMING_QUALITY] ?: StreamingQuality.DIRECT.name),
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: 16,
            showArtworkOnLockscreen = prefs[Keys.LOCKSCREEN_ART] ?: true,
            debugLogging = prefs[Keys.DEBUG_LOGGING] ?: false
        )
    }

    suspend fun saveAuth(serverUrl: String, token: String, userId: String, username: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = serverUrl
            prefs[Keys.ACCESS_TOKEN] = token
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USERNAME] = username
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.USER_ID)
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateAccentPalette(palette: AccentPalette) {
        context.dataStore.edit { it[Keys.ACCENT_PALETTE] = palette.name }
    }

    suspend fun updatePlayerLayout(layout: PlayerLayoutStyle) {
        context.dataStore.edit { it[Keys.PLAYER_LAYOUT] = layout.name }
    }

    suspend fun updateStreamingQuality(quality: StreamingQuality) {
        context.dataStore.edit { it[Keys.STREAMING_QUALITY] = quality.name }
    }

    suspend fun updatePlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed }
    }

    suspend fun updateCornerRadius(radiusDp: Int) {
        context.dataStore.edit { it[Keys.CORNER_RADIUS] = radiusDp }
    }

    suspend fun updateDebugLogging(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEBUG_LOGGING] = enabled }
    }

    suspend fun addRecentTrackId(trackId: String) {
        if (trackId.isBlank()) return
        context.dataStore.edit { prefs ->
            val raw = prefs[Keys.RECENT_TRACK_IDS].orEmpty()
            val list = if (raw.isBlank()) mutableListOf() else raw.split(",").filter { it.isNotBlank() }.toMutableList()
            list.remove(trackId)
            list.add(0, trackId)
            val trimmed = list.take(50)
            prefs[Keys.RECENT_TRACK_IDS] = trimmed.joinToString(",")
        }
    }
}
