package com.jellymusic.player.playback

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import androidx.media3.common.Player
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.jellymusic.player.JellyMusicApp
import com.jellymusic.player.R
import com.jellymusic.player.data.JellyfinItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

private const val ACTION_SPEED_DOWN = "com.jellymusic.player.SPEED_DOWN"
private const val ACTION_TOGGLE_KNOB_MODE = "com.jellymusic.player.TOGGLE_KNOB_MODE"
private const val ACTION_SPEED_UP = "com.jellymusic.player.SPEED_UP"
private const val ACTION_TOGGLE_SHUFFLE = "com.jellymusic.player.TOGGLE_SHUFFLE"
private const val ACTION_CYCLE_REPEAT = "com.jellymusic.player.CYCLE_REPEAT"

private const val MEDIA_ID_ROOT = "[root]"
private const val MEDIA_ID_TRACKS = "category_tracks"
private const val MEDIA_ID_PLAYLISTS = "category_playlists"
private const val PREFIX_PLAYLIST = "PLAYLIST_"

@UnstableApi
class PlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    lateinit var player: ExoPlayer

    private var isKnobPitchMode = false
    private var lastSeekTargetMs: Long? = null
    private var lastSeekTime: Long = 0L
    private var lastDirection: Int = 0
    private var resetJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val forwardingListeners = CopyOnWriteArraySet<Player.Listener>()
    private var forwardingPlayerInstance: ForwardingPlayer? = null
    private var lastSearchResults = listOf<JellyfinItem>()
    private var cachedPlaylists = listOf<JellyfinItem>()
    private val cachedPlaylistTracks = java.util.concurrent.ConcurrentHashMap<String, List<JellyfinItem>>()

    private suspend fun ensureCredentials() {
        val app = JellyMusicApp.instance
        val server = app.preferences.serverUrl.first()
        val token = app.preferences.accessToken.first()
        val uid = app.preferences.userId.first()
        if (server.isNotBlank() && token.isNotBlank()) {
            app.jellyfinClient.updateCredentials(server, token, uid)
        }
    }

    private fun notifyPositionReset() {
        if (!::player.isInitialized) return
        val current = player.currentPosition
        val mediaItem = player.currentMediaItem
        val index = player.currentMediaItemIndex
        val posInfo = Player.PositionInfo(
            null, index, mediaItem, null, index, current, current, -1, -1
        )
        forwardingListeners.forEach {
            it.onPositionDiscontinuity(posInfo, posInfo, Player.DISCONTINUITY_REASON_SEEK)
        }
        lastSeekTargetMs = current
        lastDirection = 0
    }

    private fun notifyMetadataChanged() {
        val meta = forwardingPlayerInstance?.mediaMetadata ?: return
        forwardingListeners.forEach {
            it.onMediaMetadataChanged(meta)
        }
    }

    private fun handleKnobSeek(targetMs: Long) {
        val now = System.currentTimeMillis()
        val current = player.currentPosition
        val isGestureActive = (now - lastSeekTime) < 800L
        val lastTarget = lastSeekTargetMs

        val direction = if (isGestureActive && lastTarget != null) {
            when {
                targetMs > lastTarget -> +1
                targetMs < lastTarget -> -1
                else -> {
                    // targetMs == lastTarget (user continuing turn at physical/virtual boundary)
                    if (lastDirection != 0) lastDirection
                    else if (targetMs >= current) +1 else -1
                }
            }
        } else {
            // Start of a new seek gesture
            when {
                targetMs > current -> +1
                targetMs < current -> -1
                else -> 0
            }
        }

        if (direction > 0) {
            lastDirection = +1
            JellyMusicApp.instance.playerController.adjustSpeedAndPitchLinked(+0.01f)
        } else if (direction < 0) {
            lastDirection = -1
            JellyMusicApp.instance.playerController.adjustSpeedAndPitchLinked(-0.01f)
        }

        lastSeekTargetMs = targetMs
        lastSeekTime = now
        mediaLibrarySession?.let { updateAllControllersLayout(it) }
        notifyMetadataChanged()

        resetJob?.cancel()
        resetJob = serviceScope.launch {
            delay(400L)
            notifyPositionReset()
        }
    }

    private fun buildCustomLayout(): List<CommandButton> {
        val state = JellyMusicApp.instance.playerController.state.value
        val isShuffle = state.isShuffleEnabled
        val repeatMode = state.repeatMode

        val shuffleBtn = CommandButton.Builder()
            .setDisplayName(if (isShuffle) "Casuale: Attivo" else "Casuale: Disattivo")
            .setIconResId(if (isShuffle) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off)
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY))
            .build()

        val (repeatName, repeatIcon) = when (repeatMode) {
            Player.REPEAT_MODE_ONE -> "Ripeti: Brano" to R.drawable.ic_repeat_one
            Player.REPEAT_MODE_ALL -> "Ripeti: Tutti" to R.drawable.ic_repeat_all
            else -> "Ripeti: Disattivo" to R.drawable.ic_repeat_off
        }
        val repeatBtn = CommandButton.Builder()
            .setDisplayName(repeatName)
            .setIconResId(repeatIcon)
            .setSessionCommand(SessionCommand(ACTION_CYCLE_REPEAT, Bundle.EMPTY))
            .build()

        val speedDownBtn = CommandButton.Builder()
            .setDisplayName("-0.01")
            .setIconResId(R.drawable.ic_speed_down)
            .setSessionCommand(SessionCommand(ACTION_SPEED_DOWN, Bundle.EMPTY))
            .build()

        val knobModeBtn = CommandButton.Builder()
            .setDisplayName(if (isKnobPitchMode) "Rotella: Pitch" else "Rotella: Brano")
            .setIconResId(if (isKnobPitchMode) R.drawable.ic_speed_dial else R.drawable.ic_seek_mode)
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_KNOB_MODE, Bundle.EMPTY))
            .build()

        val speedUpBtn = CommandButton.Builder()
            .setDisplayName("+0.01")
            .setIconResId(R.drawable.ic_speed_up)
            .setSessionCommand(SessionCommand(ACTION_SPEED_UP, Bundle.EMPTY))
            .build()

        return listOf(shuffleBtn, repeatBtn, speedDownBtn, knobModeBtn, speedUpBtn)
    }

    private fun updateAllControllersLayout(session: MediaSession) {
        val layout = buildCustomLayout()
        session.connectedControllers.forEach { controller ->
            session.setCustomLayout(controller, layout)
        }
    }

    private val librarySessionCallback = object : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(SessionCommand(ACTION_SPEED_DOWN, Bundle.EMPTY))
                .add(SessionCommand(ACTION_TOGGLE_KNOB_MODE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_SPEED_UP, Bundle.EMPTY))
                .add(SessionCommand(ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_CYCLE_REPEAT, Bundle.EMPTY))
                .build()

            val availablePlayerCommands = connectionResult.availablePlayerCommands.buildUpon()
                .add(Player.COMMAND_SET_SHUFFLE_MODE)
                .add(Player.COMMAND_SET_REPEAT_MODE)
                .build()

            return MediaSession.ConnectionResult.accept(
                availableSessionCommands,
                availablePlayerCommands
            )
        }

        override fun onPostConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ) {
            super.onPostConnect(session, controller)
            session.setCustomLayout(controller, buildCustomLayout())
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            val playerController = JellyMusicApp.instance.playerController
            when (customCommand.customAction) {
                ACTION_TOGGLE_SHUFFLE -> {
                    playerController.toggleShuffle()
                    updateAllControllersLayout(session)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_CYCLE_REPEAT -> {
                    playerController.cycleRepeatMode()
                    updateAllControllersLayout(session)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_SPEED_DOWN -> {
                    playerController.adjustSpeedAndPitchLinked(-0.01f)
                    updateAllControllersLayout(session)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_TOGGLE_KNOB_MODE -> {
                    isKnobPitchMode = !isKnobPitchMode
                    lastSeekTargetMs = null
                    lastSeekTime = 0L
                    lastDirection = 0
                    updateAllControllersLayout(session)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_SPEED_UP -> {
                    playerController.adjustSpeedAndPitchLinked(+0.01f)
                    updateAllControllersLayout(session)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootExtras = Bundle().apply {
                putBoolean("androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_SUPPORTED", true)
                putBoolean("android.media.browse.CONTENT_STYLE_SUPPORTED", true)
                putInt("androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE", 1)
                putInt("androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE", 1)
                putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)
                putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1)
            }

            val rootItem = MediaItem.Builder()
                .setMediaId(MEDIA_ID_ROOT)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .setTitle("JellyMusic")
                        .setExtras(rootExtras)
                        .build()
                )
                .build()

            val libraryParams = LibraryParams.Builder()
                .setExtras(rootExtras)
                .build()

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, libraryParams))
        }

        private fun buildTrackMediaItems(tracks: List<JellyfinItem>): List<MediaItem> {
            val itemExtras = Bundle().apply {
                putInt("androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE", 1)
                putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1)
            }
            return tracks.map { item ->
                val streamUri = Uri.parse(JellyMusicApp.instance.jellyfinClient.getStreamUrl(item.id))
                val artUri = ArtworkContentProvider.getArtworkUri(item.id)
                MediaItem.Builder()
                    .setMediaId(item.id)
                    .setUri(streamUri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(item.name)
                            .setArtist(item.artistName)
                            .setAlbumTitle(item.album ?: "")
                            .setArtworkUri(artUri)
                            .setExtras(itemExtras)
                            .setIsPlayable(true)
                            .setIsBrowsable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                            .build()
                    )
                    .build()
            }
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val folderExtras = Bundle().apply {
                putInt("androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE", 1)
                putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)
            }

            when {
                parentId == MEDIA_ID_ROOT -> {
                    val tracksCat = MediaItem.Builder()
                        .setMediaId(MEDIA_ID_TRACKS)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("Brani")
                                .setIsPlayable(false)
                                .setIsBrowsable(true)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                .setExtras(folderExtras)
                                .build()
                        )
                        .build()

                    val playlistsCat = MediaItem.Builder()
                        .setMediaId(MEDIA_ID_PLAYLISTS)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("Playlist")
                                .setIsPlayable(false)
                                .setIsBrowsable(true)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                                .setExtras(folderExtras)
                                .build()
                        )
                        .build()

                    return Futures.immediateFuture(
                        LibraryResult.ofItemList(ImmutableList.of(tracksCat, playlistsCat), params)
                    )
                }

                parentId == MEDIA_ID_TRACKS -> {
                    val controller = JellyMusicApp.instance.playerController
                    val cached = if (controller.cachedTracks.isNotEmpty()) {
                        controller.cachedTracks
                    } else {
                        controller.state.value.queue
                    }

                    if (cached.isNotEmpty()) {
                        return Futures.immediateFuture(
                            LibraryResult.ofItemList(ImmutableList.copyOf(buildTrackMediaItems(cached)), params)
                        )
                    }

                    val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                    serviceScope.launch {
                        ensureCredentials()
                        val tracks = JellyMusicApp.instance.jellyfinClient.getAudioTracks().getOrElse { emptyList() }
                        if (tracks.isNotEmpty()) {
                            controller.updateCachedTracks(tracks)
                        }
                        val items = buildTrackMediaItems(tracks)
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                    }
                    return future
                }

                parentId == MEDIA_ID_PLAYLISTS -> {
                    val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                    serviceScope.launch {
                        ensureCredentials()
                        val playlists = JellyMusicApp.instance.jellyfinClient.getPlaylists().getOrElse { emptyList() }
                        cachedPlaylists = playlists
                        val items = playlists.map { p ->
                            val artUri = ArtworkContentProvider.getArtworkUri(p.id)
                            val pExtras = Bundle().apply {
                                putInt("androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE", 1)
                                putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)
                            }
                            MediaItem.Builder()
                                .setMediaId("$PREFIX_PLAYLIST${p.id}")
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(p.name)
                                        .setIsPlayable(false)
                                        .setIsBrowsable(true)
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                        .setArtworkUri(artUri)
                                        .setExtras(pExtras)
                                        .build()
                                )
                                .build()
                        }
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                    }
                    return future
                }

                parentId.startsWith(PREFIX_PLAYLIST) -> {
                    val playlistId = parentId.removePrefix(PREFIX_PLAYLIST)
                    val cached = cachedPlaylistTracks[playlistId]
                    if (!cached.isNullOrEmpty()) {
                        return Futures.immediateFuture(
                            LibraryResult.ofItemList(ImmutableList.copyOf(buildTrackMediaItems(cached)), params)
                        )
                    }

                    val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                    serviceScope.launch {
                        ensureCredentials()
                        val tracks = JellyMusicApp.instance.jellyfinClient.getPlaylistTracks(playlistId).getOrElse { emptyList() }
                        cachedPlaylistTracks[playlistId] = tracks
                        val items = buildTrackMediaItems(tracks)
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                    }
                    return future
                }

                else -> {
                    return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))
                }
            }
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            val future = SettableFuture.create<LibraryResult<Void>>()
            serviceScope.launch {
                ensureCredentials()
                val app = JellyMusicApp.instance

                // 1. Server search query
                val serverResults = app.jellyfinClient.getAudioTracks(searchTerm = query).getOrElse { emptyList() }

                // 2. Ensure local cache populated
                var cached = app.playerController.cachedTracks
                if (cached.isEmpty()) {
                    cached = app.jellyfinClient.getAudioTracks().getOrElse { emptyList() }
                    if (cached.isNotEmpty()) {
                        app.playerController.updateCachedTracks(cached)
                    }
                }

                // 3. Match across title, artist name, artists list, artist items, and album
                val q = query.trim().lowercase()
                val localResults = cached.filter { track ->
                    track.name.lowercase().contains(q) ||
                    track.artistName.lowercase().contains(q) ||
                    (track.artists?.any { it.lowercase().contains(q) } == true) ||
                    (track.artistItems?.any { it.name.lowercase().contains(q) } == true) ||
                    (track.album?.lowercase()?.contains(q) == true)
                }

                val merged = (serverResults + localResults).distinctBy { it.id }
                lastSearchResults = merged
                session.notifySearchResultChanged(browser, query, merged.size, params)
                future.set(LibraryResult.ofVoid())
            }
            return future
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = buildTrackMediaItems(lastSearchResults)
            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
            )
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val folderExtras = Bundle().apply {
                putInt("androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE", 1)
                putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)
            }

            when (mediaId) {
                MEDIA_ID_ROOT -> return onGetLibraryRoot(session, browser, null)
                MEDIA_ID_TRACKS -> {
                    val item = MediaItem.Builder()
                        .setMediaId(MEDIA_ID_TRACKS)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("Brani")
                                .setIsPlayable(false)
                                .setIsBrowsable(true)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                .setExtras(folderExtras)
                                .build()
                        ).build()
                    return Futures.immediateFuture(LibraryResult.ofItem(item, null))
                }
                MEDIA_ID_PLAYLISTS -> {
                    val item = MediaItem.Builder()
                        .setMediaId(MEDIA_ID_PLAYLISTS)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle("Playlist")
                                .setIsPlayable(false)
                                .setIsBrowsable(true)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                                .setExtras(folderExtras)
                                .build()
                        ).build()
                    return Futures.immediateFuture(LibraryResult.ofItem(item, null))
                }
            }

            if (mediaId.startsWith(PREFIX_PLAYLIST)) {
                val pId = mediaId.removePrefix(PREFIX_PLAYLIST)
                val p = cachedPlaylists.firstOrNull { it.id == pId }
                if (p != null) {
                    val item = MediaItem.Builder()
                        .setMediaId(mediaId)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(p.name)
                                .setIsPlayable(false)
                                .setIsBrowsable(true)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                                .setArtworkUri(ArtworkContentProvider.getArtworkUri(p.id))
                                .setExtras(folderExtras)
                                .build()
                        ).build()
                    return Futures.immediateFuture(LibraryResult.ofItem(item, null))
                }
            }

            val controller = JellyMusicApp.instance.playerController
            val allTracks = (controller.cachedTracks + controller.state.value.queue + lastSearchResults + cachedPlaylistTracks.values.flatten()).distinctBy { it.id }
            val found = allTracks.firstOrNull { it.id == mediaId }
            if (found != null) {
                val items = buildTrackMediaItems(listOf(found))
                return Futures.immediateFuture(LibraryResult.ofItem(items.first(), null))
            }
            return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val firstId = mediaItems.firstOrNull()?.mediaId
            if (!firstId.isNullOrEmpty()) {
                val playerController = JellyMusicApp.instance.playerController
                val playlistEntry = cachedPlaylistTracks.entries.firstOrNull { entry ->
                    entry.value.any { it.id == firstId }
                }
                if (playlistEntry != null) {
                    val playlistTracks = playlistEntry.value
                    val idx = playlistTracks.indexOfFirst { it.id == firstId }
                    playerController.playQueue(playlistTracks, if (idx >= 0) idx else 0)
                } else {
                    val allKnown = (playerController.cachedTracks + lastSearchResults).distinctBy { it.id }
                    if (allKnown.isNotEmpty() && playerController.cachedTracks.size < allKnown.size) {
                        playerController.updateCachedTracks(allKnown)
                    }
                    playerController.playTrackById(firstId)
                }
            }
            return Futures.immediateFuture(mediaItems)
        }
    }

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        val forwardingPlayer = object : ForwardingPlayer(player) {
            override fun addListener(listener: Player.Listener) {
                super.addListener(listener)
                forwardingListeners.add(listener)
            }

            override fun removeListener(listener: Player.Listener) {
                super.removeListener(listener)
                forwardingListeners.remove(listener)
            }

            override fun getMediaMetadata(): MediaMetadata {
                val base = super.getMediaMetadata()
                val currentTrack = JellyMusicApp.instance.playerController.state.value.currentTrack ?: return base
                val speed = JellyMusicApp.instance.playerController.state.value.playbackSpeed
                val speedBadge = "%.2fx".format(java.util.Locale.US, speed)
                val artistText = if (currentTrack.artistName.isNotBlank()) {
                    "${currentTrack.artistName} • $speedBadge"
                } else speedBadge
                return base.buildUpon()
                    .setTitle(currentTrack.name)
                    .setArtist(artistText)
                    .build()
            }

            override fun seekTo(positionMs: Long) {
                if (isKnobPitchMode) {
                    handleKnobSeek(positionMs)
                } else {
                    super.seekTo(positionMs)
                }
            }

            override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
                if (mediaItemIndex == currentMediaItemIndex && isKnobPitchMode) {
                    handleKnobSeek(positionMs)
                } else {
                    super.seekTo(mediaItemIndex, positionMs)
                }
            }

            override fun seekToNext() {
                JellyMusicApp.instance.playerController.skipNext()
            }

            override fun seekToNextMediaItem() {
                JellyMusicApp.instance.playerController.skipNext()
            }

            override fun seekToPrevious() {
                JellyMusicApp.instance.playerController.skipPrevious()
            }

            override fun seekToPreviousMediaItem() {
                JellyMusicApp.instance.playerController.skipPrevious()
            }
        }
        forwardingPlayerInstance = forwardingPlayer

        val launchIntent = packageManager?.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = launchIntent?.let {
            android.app.PendingIntent.getActivity(
                this,
                0,
                it,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val sessionBuilder = MediaLibrarySession.Builder(this, forwardingPlayer, librarySessionCallback)
        if (sessionActivityPendingIntent != null) {
            sessionBuilder.setSessionActivity(sessionActivityPendingIntent)
        }
        mediaLibrarySession = sessionBuilder.build()

        // Register player to singleton controller
        JellyMusicApp.instance.playerController.attachPlayer(player)

        serviceScope.launch {
            JellyMusicApp.instance.playerController.state
                .map { Triple(it.playbackSpeed, it.isShuffleEnabled, it.repeatMode) }
                .distinctUntilChanged()
                .collect {
                    mediaLibrarySession?.let { session -> updateAllControllersLayout(session) }
                    notifyMetadataChanged()
                }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaLibrarySession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        super.onDestroy()
    }
}
