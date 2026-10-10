package com.jellymusic.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.jellymusic.player.data.JellyfinClient
import com.jellymusic.player.debug.DebugLogManager
import com.jellymusic.player.playback.PlaybackService
import com.jellymusic.player.playback.PlayerController
import com.jellymusic.player.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class JellyMusicApp : Application() {

    lateinit var preferences: AppPreferences
        private set
    lateinit var jellyfinClient: JellyfinClient
        private set
    lateinit var playerController: PlayerController
        private set

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        lateinit var instance: JellyMusicApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferences = AppPreferences(this)
        jellyfinClient = JellyfinClient()
        playerController = PlayerController(jellyfinClient)

        // Initialize credentials and prefetch tracks for Android Auto / background startup
        applicationScope.launch {
            val server = preferences.serverUrl.first()
            val token = preferences.accessToken.first()
            val uid = preferences.userId.first()
            if (server.isNotBlank() && token.isNotBlank()) {
                jellyfinClient.updateCredentials(server, token, uid)
                jellyfinClient.getAudioTracks().onSuccess { tracks ->
                    playerController.updateCachedTracks(tracks)
                }
            }
        }

        // Observe debug logging preference and stream logcat to /LOGS
        applicationScope.launch {
            preferences.customizations.collectLatest { custom ->
                if (custom.debugLogging) {
                    DebugLogManager.startLogging(this@JellyMusicApp, applicationScope)
                } else {
                    DebugLogManager.stopLogging()
                }
            }
        }

        // Start playback service
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({
            // Bound to media session service
        }, MoreExecutors.directExecutor())
    }
}
