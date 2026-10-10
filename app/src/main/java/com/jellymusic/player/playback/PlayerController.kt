package com.jellymusic.player.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jellymusic.player.data.JellyfinClient
import com.jellymusic.player.data.JellyfinItem
import com.jellymusic.player.data.UserItemDataDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackState(
    val currentTrack: JellyfinItem? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val isPitchLinked: Boolean = false,
    val queue: List<JellyfinItem> = emptyList(),
    val currentIndex: Int = -1
)

class PlayerController(
    private val client: JellyfinClient
) {
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var pendingSpeedJob: Job? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    var cachedTracks: List<JellyfinItem> = emptyList()
        private set

    fun updateCachedTracks(tracks: List<JellyfinItem>) {
        cachedTracks = tracks
    }

    fun updateTrackFavorite(trackId: String, isFavorite: Boolean) {
        val updated = cachedTracks.map {
            if (it.id == trackId) {
                it.copy(userData = (it.userData ?: UserItemDataDto()).copy(isFavorite = isFavorite))
            } else it
        }
        cachedTracks = updated
        val cur = _state.value.currentTrack
        if (cur?.id == trackId) {
            _state.value = _state.value.copy(
                currentTrack = cur.copy(userData = (cur.userData ?: UserItemDataDto()).copy(isFavorite = isFavorite))
            )
        }
    }

    fun playTrackById(trackId: String) {
        val tracks = if (cachedTracks.isNotEmpty()) cachedTracks else _state.value.queue
        val index = tracks.indexOfFirst { it.id == trackId }
        if (index != -1) {
            playQueue(tracks, index)
        }
    }

    private var pendingPlay: Triple<List<JellyfinItem>, Int, Int>? = null

    fun attachPlayer(player: ExoPlayer) {
        this.exoPlayer = player
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                if (isPlaying) startProgressTracker() else stopProgressTracker()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.value = _state.value.copy(
                        durationMs = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                    )
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _state.value = _state.value.copy(isShuffleEnabled = shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _state.value = _state.value.copy(repeatMode = repeatMode)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = exoPlayer?.currentMediaItemIndex ?: -1
                val queue = _state.value.queue
                if (index in queue.indices) {
                    val currentSpeed = _state.value.playbackSpeed
                    val currentPitch = _state.value.playbackPitch
                    val tr = queue[index]
                    _state.value = _state.value.copy(
                        currentTrack = tr,
                        currentIndex = index,
                        durationMs = tr.durationMs
                    )
                    exoPlayer?.playbackParameters = PlaybackParameters(currentSpeed, currentPitch)
                    scope.launch {
                        try {
                            com.jellymusic.player.JellyMusicApp.instance.preferences.addRecentTrackId(tr.id)
                        } catch (_: Exception) {}
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                try {
                    exoPlayer?.prepare()
                    exoPlayer?.play()
                } catch (e: Exception) {
                    // Safe guard
                }
            }
        })

        pendingPlay?.let { (items, index, bitrate) ->
            pendingPlay = null
            playQueue(items, index, bitrate)
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _state.value = _state.value.copy(
                        currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playQueue(items: List<JellyfinItem>, startIndex: Int = 0, bitrateKbps: Int = 0) {
        if (items.isEmpty()) return

        val player = exoPlayer
        if (player == null) {
            pendingPlay = Triple(items, startIndex, bitrateKbps)
            _state.value = _state.value.copy(
                queue = items,
                currentIndex = startIndex,
                currentTrack = items[startIndex]
            )
            return
        }

        val startTrack = items[startIndex]
        _state.value = _state.value.copy(
            queue = items,
            currentIndex = startIndex,
            currentTrack = startTrack
        )
        scope.launch {
            try {
                com.jellymusic.player.JellyMusicApp.instance.preferences.addRecentTrackId(startTrack.id)
            } catch (_: Exception) {}
        }

        val speed = _state.value.playbackSpeed
        val speedBadge = "%.2fx".format(java.util.Locale.US, speed)
        val mediaItems = items.map { item ->
            val streamUri = Uri.parse(client.getStreamUrl(item.id, bitrateKbps))
            val artUri = ArtworkContentProvider.getArtworkUri(item.id)

            val displayArtist = if (item.artistName.isNotBlank()) "${item.artistName} • $speedBadge" else speedBadge

            val metadata = MediaMetadata.Builder()
                .setTitle(item.name)
                .setArtist(displayArtist)
                .setAlbumTitle(item.album ?: "")
                .setArtworkUri(artUri)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build()

            MediaItem.Builder()
                .setMediaId(item.id)
                .setUri(streamUri)
                .setMediaMetadata(metadata)
                .build()
        }

        val pitch = _state.value.playbackPitch
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.playbackParameters = PlaybackParameters(speed, pitch)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _state.value = _state.value.copy(currentPositionMs = positionMs)
    }

    fun skipNext() {
        exoPlayer?.let {
            if (it.hasNextMediaItem()) it.seekToNextMediaItem()
        }
    }

    fun skipPrevious() {
        exoPlayer?.let {
            if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem()
        }
    }

    fun toggleShuffle() {
        val player = exoPlayer ?: return
        val newShuffle = !_state.value.isShuffleEnabled
        player.shuffleModeEnabled = newShuffle
        _state.value = _state.value.copy(isShuffleEnabled = newShuffle)
    }

    fun cycleRepeatMode() {
        val player = exoPlayer ?: return
        val newMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = newMode
        _state.value = _state.value.copy(repeatMode = newMode)
    }

    fun setSpeed(speed: Float) {
        val clampedSpeed = (kotlin.math.round(speed * 100f) / 100f).coerceIn(0.25f, 3.0f)
        val pitch = if (_state.value.isPitchLinked) clampedSpeed else 1.0f
        _state.value = _state.value.copy(
            playbackSpeed = clampedSpeed,
            playbackPitch = pitch
        )

        // Throttle updates to ExoPlayer to prevent Sonic buffer underflows / audio glitches
        pendingSpeedJob?.cancel()
        pendingSpeedJob = scope.launch {
            delay(40)
            val currentParams = exoPlayer?.playbackParameters
            if (currentParams == null ||
                kotlin.math.abs(currentParams.speed - clampedSpeed) > 0.005f ||
                kotlin.math.abs(currentParams.pitch - pitch) > 0.005f) {
                try {
                    exoPlayer?.playbackParameters = PlaybackParameters(clampedSpeed, pitch)
                } catch (e: Exception) {
                    // Safe guard
                }
            }
        }
    }

    fun setPitchLinked(linked: Boolean) {
        pendingSpeedJob?.cancel()
        val currentSpeed = _state.value.playbackSpeed
        val pitch = if (linked) currentSpeed else 1.0f
        try {
            exoPlayer?.playbackParameters = PlaybackParameters(currentSpeed, pitch)
        } catch (e: Exception) {
            // Safe guard
        }
        _state.value = _state.value.copy(
            isPitchLinked = linked,
            playbackPitch = pitch
        )
    }

    fun setSpeedAndPitchLinked(speed: Float) {
        pendingSpeedJob?.cancel()
        val clamped = (kotlin.math.round(speed * 100f) / 100f).coerceIn(0.50f, 2.00f)
        try {
            exoPlayer?.playbackParameters = PlaybackParameters(clamped, clamped)
        } catch (e: Exception) {
            // Safe guard
        }
        _state.value = _state.value.copy(
            playbackSpeed = clamped,
            playbackPitch = clamped,
            isPitchLinked = true
        )
    }

    fun adjustSpeedAndPitchLinked(delta: Float) {
        setSpeedAndPitchLinked(_state.value.playbackSpeed + delta)
    }

    fun cycleSpeedAndPitchLinked() {
        val speeds = listOf(0.75f, 0.90f, 1.00f, 1.10f, 1.25f, 1.50f)
        val current = _state.value.playbackSpeed
        val next = speeds.firstOrNull { it > current + 0.02f } ?: speeds.first()
        setSpeedAndPitchLinked(next)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentQueue = _state.value.queue.toMutableList()
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices || fromIndex == toIndex) {
            return
        }
        val item = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, item)

        val currentIdx = _state.value.currentIndex
        val newCurrentIdx = when {
            currentIdx == fromIndex -> toIndex
            fromIndex < currentIdx && toIndex >= currentIdx -> currentIdx - 1
            fromIndex > currentIdx && toIndex <= currentIdx -> currentIdx + 1
            else -> currentIdx
        }

        try {
            exoPlayer?.moveMediaItem(fromIndex, toIndex)
        } catch (e: Exception) {
            // Safe guard
        }

        _state.value = _state.value.copy(
            queue = currentQueue,
            currentIndex = newCurrentIdx,
            currentTrack = if (newCurrentIdx in currentQueue.indices) currentQueue[newCurrentIdx] else _state.value.currentTrack
        )
    }

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }

    fun getVolume(): Float {
        return exoPlayer?.volume ?: 1f
    }
}
