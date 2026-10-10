package com.jellymusic.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import kotlinx.coroutines.CoroutineScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.jellymusic.player.data.LyricLineDto
import com.jellymusic.player.data.LyricPhrase
import com.jellymusic.player.data.LyricSpan
import com.jellymusic.player.data.LyricWord
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.drawscope.clipRect
import com.jellymusic.player.preferences.AccentPalette
import com.jellymusic.player.ui.theme.CyberpunkPrimary
import com.jellymusic.player.ui.theme.CyberpunkSecondary
import com.jellymusic.player.ui.theme.CyberpunkTertiary
import com.jellymusic.player.ui.theme.EmeraldPrimary
import com.jellymusic.player.ui.theme.EmeraldSecondary
import com.jellymusic.player.ui.theme.EmeraldTertiary
import com.jellymusic.player.ui.theme.SakuraPrimary
import com.jellymusic.player.ui.theme.SakuraSecondary
import com.jellymusic.player.ui.theme.SakuraTertiary
import com.jellymusic.player.ui.theme.SunsetPrimary
import com.jellymusic.player.ui.theme.SunsetSecondary
import com.jellymusic.player.ui.theme.SunsetTertiary
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.jellymusic.player.JellyMusicApp
import com.jellymusic.player.playback.PlaybackState
import com.jellymusic.player.preferences.UserCustomizations
import com.jellymusic.player.ui.theme.AmbientAmber
import com.jellymusic.player.ui.theme.AmbientDeepBlue
import com.jellymusic.player.ui.theme.AmbientIndigo
import com.jellymusic.player.ui.theme.ArtworkBackground
import com.jellymusic.player.ui.theme.ArtworkBorder
import com.jellymusic.player.ui.theme.ArtworkControlFill
import com.jellymusic.player.ui.theme.ArtworkSurface
import com.jellymusic.player.ui.theme.ArtworkTextMuted
import com.jellymusic.player.ui.theme.ArtworkTextPrimary
import com.jellymusic.player.ui.theme.ArtworkTextSecondary
import com.jellymusic.player.ui.theme.ArtworkTrackBackground

enum class SheetMode {
    ART, LYRICS, QUEUE
}

private fun sm(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

private fun q(p: Float, a: Float, b: Float): Float {
    if (b <= a) return if (p >= a) 1f else 0f
    return sm((p - a) / (b - a))
}

private fun lerpFloat(a: Float, b: Float, t: Float): Float = a + (b - a) * t
private fun lerpDp(a: Dp, b: Dp, t: Float): Dp = a + (b - a) * t

private class DragSample(val timeMs: Long, val y: Float)

@Composable
fun PlayerScreen(
    playbackState: PlaybackState,
    customizations: UserCustomizations,
    animProgress: Animatable<Float, AnimationVector1D>,
    screenWidth: Dp,
    screenHeight: Dp,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val client = JellyMusicApp.instance.jellyfinClient
    val controller = JellyMusicApp.instance.playerController
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val p = animProgress.value.coerceIn(0f, 1f)

    var mode by remember { mutableStateOf(SheetMode.ART) }
    var lastContentMode by remember { mutableStateOf(SheetMode.LYRICS) }
    var savedModeBeforeCollapse by remember { mutableStateOf<SheetMode?>(null) }
    var savedTrackIdBeforeCollapse by remember { mutableStateOf<String?>(null) }

    val handleCollapse: () -> Unit = {
        coroutineScope.launch {
            if (mode != SheetMode.ART) {
                savedModeBeforeCollapse = mode
                savedTrackIdBeforeCollapse = track.id
                mode = SheetMode.ART
                delay(120)
            }
            onCollapse()
        }
    }

    LaunchedEffect(mode) {
        if (mode != SheetMode.ART) {
            lastContentMode = mode
        }
    }

    // Heart like state with bounce/pop animation synced with Jellyfin
    var isLiked by remember(track.id) { mutableStateOf(track.isFavorite) }
    val heartScale = remember { Animatable(1f) }

    // Scrubber drag state with smooth height expansion
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val scrubBarHeight by animateDpAsState(
        targetValue = if (isDraggingSlider) 8.dp else 4.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "scrubBarHeight"
    )

    // Volume state (0.0f - 1.0f) with smooth height expansion
    var volume by remember { mutableFloatStateOf(controller.getVolume()) }
    var isVolDragging by remember { mutableStateOf(false) }
    val volBarHeight by animateDpAsState(
        targetValue = if (isVolDragging) 9.dp else 4.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "volBarHeight"
    )

    var showSpeedDialog by remember { mutableStateOf(false) }
    val speedDialogAnim = remember { Animatable(0f) }

    // Real lyrics fetching from Jellyfin for the current track
    var lyrics by remember(track.id) { mutableStateOf<List<LyricLineDto>>(emptyList()) }
    var isLoadingLyrics by remember(track.id) { mutableStateOf(false) }
    var lyricsError by remember(track.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(track.id) {
        isLoadingLyrics = true
        lyricsError = null
        val result = JellyMusicApp.instance.lyricsService.getLyrics(track)
        result.onSuccess {
            lyrics = it
            isLoadingLyrics = false
        }.onFailure { err ->
            lyrics = emptyList()
            lyricsError = err.message
            isLoadingLyrics = false
        }
    }

    // Dynamic cover palette extraction
    val context = LocalContext.current
    var extractedPalette by remember(track.id) {
        mutableStateOf<Triple<Color, Color, Color>?>(null)
    }

    LaunchedEffect(track.id) {
        try {
            val request = ImageRequest.Builder(context)
                .data(client.getArtworkUrl(track.id, maxWidth = 300))
                .allowHardware(false)
                .size(48, 48)
                .build()
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    ?: result.drawable.toBitmap(48, 48)
                val palette = withContext(Dispatchers.Default) {
                    extractArtworkPalette(bitmap)
                }
                extractedPalette = palette
            }
        } catch (_: Exception) {
            // Keep default/theme fallback on decode or network error
        }
    }

    val currentMs = if (isDraggingSlider) sliderPosition.toLong() else playbackState.currentPositionMs
    val durationMs = playbackState.durationMs.coerceAtLeast(1L)
    val remainMs = (durationMs - currentMs).coerceAtLeast(0L)

    // Artwork breathing scale: 1.0f playing, 0.88f paused
    val artScale by animateFloatAsState(
        targetValue = if (playbackState.isPlaying || mode != SheetMode.ART) 1.0f else 0.88f,
        animationSpec = tween(durationMillis = 600),
        label = "artScale"
    )

    // Atmospheric Drift Animations matching Transition.dc.html & ARTWORK
    val infiniteTransition = rememberInfiniteTransition(label = "ambientDrift")
    val drift1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(19000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift1"
    )
    val drift2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift2"
    )
    val drift3Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift3"
    )

    val fallbackPalette = when (customizations.accentPalette) {
        AccentPalette.CYBERPUNK_CYAN -> Triple(CyberpunkPrimary, CyberpunkSecondary, CyberpunkTertiary)
        AccentPalette.SUNSET_AMBER -> Triple(SunsetPrimary, SunsetSecondary, SunsetTertiary)
        AccentPalette.EMERALD_FOREST -> Triple(EmeraldPrimary, EmeraldSecondary, EmeraldTertiary)
        AccentPalette.SAKURA_PINK -> Triple(SakuraPrimary, SakuraSecondary, SakuraTertiary)
        else -> Triple(AmbientAmber, AmbientIndigo, AmbientDeepBlue)
    }

    val targetPalette = extractedPalette ?: fallbackPalette

    val colorEase = remember { CubicBezierEasing(0.22f, 0.8f, 0.2f, 1f) }
    val orb1Color by animateColorAsState(
        targetValue = targetPalette.first,
        animationSpec = tween(durationMillis = 650, easing = colorEase),
        label = "orb1Color"
    )
    val orb2Color by animateColorAsState(
        targetValue = targetPalette.second,
        animationSpec = tween(durationMillis = 650, easing = colorEase),
        label = "orb2Color"
    )
    val orb3Color by animateColorAsState(
        targetValue = targetPalette.third,
        animationSpec = tween(durationMillis = 650, easing = colorEase),
        label = "orb3Color"
    )

    // Track Change Transition Animations
    val bezierE = remember { CubicBezierEasing(0.22f, 0.8f, 0.2f, 1f) }
    var skipDirection by remember { mutableIntStateOf(1) }
    val artEnterAlpha = remember { Animatable(1f) }
    val artEnterX = remember { Animatable(0f) }
    val artEnterScale = remember { Animatable(1f) }
    val titleEnterAlpha = remember { Animatable(1f) }
    val titleEnterY = remember { Animatable(0f) }
    val artistEnterAlpha = remember { Animatable(1f) }
    val artistEnterY = remember { Animatable(0f) }
    var isInitialTrackComposition by remember { mutableStateOf(true) }

    LaunchedEffect(track.id) {
        if (savedTrackIdBeforeCollapse != null && savedTrackIdBeforeCollapse != track.id) {
            savedModeBeforeCollapse = null
            savedTrackIdBeforeCollapse = null
        }
        if (isInitialTrackComposition) {
            isInitialTrackComposition = false
            return@LaunchedEffect
        }
        artEnterAlpha.snapTo(0f)
        artEnterX.snapTo(28f * skipDirection)
        artEnterScale.snapTo(0.97f)
        titleEnterAlpha.snapTo(0f)
        titleEnterY.snapTo(8f)
        artistEnterAlpha.snapTo(0f)
        artistEnterY.snapTo(8f)

        launch { artEnterAlpha.animateTo(1f, tween(700, easing = bezierE)) }
        launch { artEnterX.animateTo(0f, tween(700, easing = bezierE)) }
        launch { artEnterScale.animateTo(1f, tween(700, easing = bezierE)) }
        launch { titleEnterAlpha.animateTo(1f, tween(600, easing = bezierE)) }
        launch { titleEnterY.animateTo(0f, tween(600, easing = bezierE)) }
        launch {
            delay(50)
            artistEnterAlpha.animateTo(1f, tween(600, easing = bezierE))
        }
        launch {
            delay(50)
            artistEnterY.animateTo(0f, tween(600, easing = bezierE))
        }
    }

    // Auto-revert to ART mode if current track has no lyrics or player collapses
    LaunchedEffect(mode, track.id, isLoadingLyrics, lyrics.size) {
        if (!isLoadingLyrics && lyrics.isEmpty() && mode == SheetMode.LYRICS) {
            mode = SheetMode.ART
            savedModeBeforeCollapse = null
            savedTrackIdBeforeCollapse = null
        }
    }
    LaunchedEffect(p < 0.2f) {
        if (p < 0.2f && mode != SheetMode.ART) {
            if (savedModeBeforeCollapse == null) {
                savedModeBeforeCollapse = mode
                savedTrackIdBeforeCollapse = track.id
            }
            mode = SheetMode.ART
        }
    }

    // Reopen inverse animation: when player completes expansion, smoothly restore lyrics/queue
    LaunchedEffect(p >= 0.98f) {
        if (p >= 0.98f && savedModeBeforeCollapse != null) {
            val restoreMode = savedModeBeforeCollapse
            val restoreTrackId = savedTrackIdBeforeCollapse
            savedModeBeforeCollapse = null
            savedTrackIdBeforeCollapse = null
            if (restoreMode == SheetMode.LYRICS) {
                // Restore lyrics ONLY if still on the same track AND lyrics are present or still loading
                if (track.id == restoreTrackId && (isLoadingLyrics || lyrics.isNotEmpty())) {
                    delay(60)
                    mode = SheetMode.LYRICS
                }
            } else if (restoreMode != null) {
                delay(60)
                mode = restoreMode
            }
        }
    }

    BackHandler(enabled = p > 0.01f && !showSpeedDialog) {
        handleCollapse()
    }

    // Exact geometry calculations from Transition.dc.html
    val miniH = 62.dp
    val navH = 72.dp
    val miniMarginBottom = 10.dp
    val miniTop = screenHeight - navH - miniH - miniMarginBottom
    val travelDistPx = with(density) { miniTop.toPx().coerceAtLeast(350.dp.toPx()) }

    // Morphing container dimensions
    val cl = lerpDp(10.dp, 0.dp, p)
    val ct = lerpDp(miniTop, 0.dp, p)
    val cw = lerpDp(screenWidth - 20.dp, screenWidth, p)
    val ch = lerpDp(miniH, screenHeight, p)
    val cr = (lerpDp(14.dp, 0.dp, p) + (14.dp * kotlin.math.sin(Math.PI.toFloat() * p).toFloat())).coerceAtLeast(0.dp)

    // Container border
    val borderAlpha = lerpFloat(0.14f, 0f, p)

    // Cover Art layout
    // Bottom-anchored vertical layout ensuring no empty space on tall screens (e.g. S25 Ultra 915dp)
    val bottomMargin = (screenHeight * 0.035f).coerceIn(24.dp, 36.dp)
    val bottomTop = screenHeight - 44.dp - bottomMargin
    val volTop = bottomTop - 56.dp
    val controlsCenterYFull = volTop - 64.dp
    val scrubTop = controlsCenterYFull - 72.dp
    val fullArtistY = scrubTop - 42.dp
    val fullTitleY = fullArtistY - 32.dp

    val availableArtHeight = (fullTitleY - 24.dp - 76.dp).coerceAtLeast(200.dp)
    val artFullSize = (screenWidth - 56.dp).coerceAtMost(availableArtHeight).coerceAtLeast(260.dp)
    val artFullX = (screenWidth - artFullSize) / 2
    val artFullY = 76.dp + ((fullTitleY - 24.dp - 76.dp) - artFullSize) / 2

    val isCompactTarget = mode != SheetMode.ART && p > 0.85f
    val compactProgress by animateFloatAsState(
        targetValue = if (isCompactTarget) 1f else 0f,
        animationSpec = tween(durationMillis = 750, easing = CubicBezierEasing(0.20f, 0.95f, 0.22f, 1f)),
        label = "compactProgress"
    )

    // Cover Art coordinates & dimensions
    val fullArtTargetSize = lerpDp(artFullSize, 52.dp, compactProgress)
    val fullArtTargetX = lerpDp(artFullX, 20.dp, compactProgress)
    val fullArtTargetY = lerpDp(artFullY, 46.dp, compactProgress)
    val fullArtTargetRadius = lerpDp(16.dp, 8.dp, compactProgress)

    val artSize = lerpDp(44.dp, fullArtTargetSize, p)
    val artX = lerpDp(9.dp, fullArtTargetX, p)
    val artY = lerpDp(9.dp, fullArtTargetY, p)
    val artRadius = lerpDp(8.dp, fullArtTargetRadius, p).coerceAtLeast(0.dp)

    // Title & Artist coordinates
    val fullTitleTargetX = lerpDp(24.dp, 84.dp, compactProgress)
    val fullTitleTargetY = lerpDp(fullTitleY, 48.dp, compactProgress)
    val fullTitleTargetScale = lerpFloat(1.0f, 16f / 24f, compactProgress)

    val fullArtistTargetX = lerpDp(24.dp, 84.dp, compactProgress)
    val fullArtistTargetY = lerpDp(fullArtistY, 72.dp, compactProgress)
    val fullArtistTargetScale = lerpFloat(1.0f, 13f / 17f, compactProgress)
    val fullArtistTargetAlpha = lerpFloat(0.72f, 0.65f, compactProgress)

    val titleX = lerpDp(65.dp, fullTitleTargetX, p)
    val titleY = lerpDp(11.dp, fullTitleTargetY, p)
    val artistX = lerpDp(65.dp, fullArtistTargetX, p)
    val artistY = lerpDp(31.dp, fullArtistTargetY, p)

    val fullTextWidth = lerpDp(
        screenWidth - 100.dp,
        (screenWidth - 84.dp - 60.dp).coerceAtLeast(80.dp),
        compactProgress
    )
    val textWidth = lerpDp((cw - 156.dp).coerceAtLeast(60.dp), fullTextWidth, p)
    val titleScale = lerpFloat(14f / 24f, fullTitleTargetScale, p)
    val artistScale = lerpFloat(12f / 17f, fullArtistTargetScale, p)
    val artistAlpha = lerpFloat(0.60f, fullArtistTargetAlpha, p)

    // Heart Button position (smoothly glides up beside the title in compact mode)
    val heartAlpha = q(p, 0.55f, 0.9f)
    val heartShiftY = 20.dp * (1f - heartAlpha)
    val heartX = screenWidth - 60.dp
    val fullHeartTargetY = lerpDp(fullTitleY + 2.dp, 52.dp, compactProgress)
    val heartY = lerpDp(11.dp, fullHeartTargetY, p)

    // Scrubber & Times fade
    val scrubAlpha = q(p, 0.55f, 0.9f)
    val scrubShiftY = 30.dp * (1f - scrubAlpha)
    val scrubX = 24.dp
    val scrubW = screenWidth - 48.dp

    // Media Controls layout
    val controlsCenterY = lerpDp(miniTop + 31.dp, controlsCenterYFull, p) - ct

    val playCx = lerpDp(cw - 68.dp, screenWidth / 2, p)
    val playBtnSize = lerpDp(44.dp, 76.dp, p)
    val playIconScale = lerpFloat(24f / 52f, 1.0f, p)

    val nextCx = lerpDp(cw - 24.dp, screenWidth / 2 + 73.dp, p)
    val nextBtnSize = lerpDp(44.dp, 64.dp, p)
    val nextIconScale = lerpFloat(24f / 36f, 1.0f, p)

    val prevCx = lerpDp(playCx, screenWidth / 2 - 77.dp, p)
    val prevAlpha = q(p, 0.35f, 0.8f)

    val shuffleCx = lerpDp(cw - 44.dp, 44.dp, p)
    val shuffleAlpha = q(p, 0.50f, 0.9f)

    val repeatCx = lerpDp(cw - 22.dp, screenWidth - 44.dp, p)
    val repeatAlpha = q(p, 0.50f, 0.9f)

    // Volume Slider fade
    val volAlpha = q(p, 0.65f, 0.95f)
    val volShiftY = 36.dp * (1f - volAlpha)

    // Bottom Tools fade
    val bottomAlpha = q(p, 0.70f, 1.0f)
    val bottomShiftY = 40.dp * (1f - bottomAlpha)

    // Top Pill handle fade
    val pillAlpha = q(p, 0.50f, 0.9f)

    // Mini line fade
    val miniLineAlpha = 1f - q(p, 0f, 0.12f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Unified Morphing Container
        val containerElevation = 14.dp * (1f - p)
        Box(
            modifier = modifier
                .offset(x = cl, y = ct)
                .size(width = cw, height = ch)
                .shadow(
                    elevation = containerElevation,
                    shape = RoundedCornerShape(cr),
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clip(RoundedCornerShape(cr))
                .background(Color(0xFF0B0B0F))
                .then(
                    if (borderAlpha > 0.001f) {
                        Modifier.border(1.dp, Color(1f, 1f, 1f, borderAlpha), RoundedCornerShape(cr))
                    } else Modifier
                )
        ) {
            // Atmospheric Ambient Halos (continuous fixed full-screen origin across mini & full player)
            Box(
                modifier = Modifier
                    // unbounded: otherwise size() is clamped to the (small) mini container and the halos get squashed
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .offset(x = -cl, y = -ct)
                    .size(width = screenWidth, height = screenHeight)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    if (w <= 0f || h <= 0f) return@Canvas

                    val orb1Center = Offset(
                        x = w * 0.22f + (70.dp.toPx() * drift1Progress),
                        y = h * 0.15f + (100.dp.toPx() * drift1Progress)
                    )
                    val orb1Radius = (w * 0.75f) * (1f + 0.18f * drift1Progress)
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.00f to orb1Color.copy(alpha = 0.75f),
                            0.25f to orb1Color.copy(alpha = 0.50f),
                            0.55f to orb1Color.copy(alpha = 0.22f),
                            0.80f to orb1Color.copy(alpha = 0.06f),
                            1.00f to Color.Transparent,
                            center = orb1Center,
                            radius = orb1Radius
                        ),
                        radius = orb1Radius,
                        center = orb1Center
                    )

                    val orb2Center = Offset(
                        x = w * 0.88f - (90.dp.toPx() * drift2Progress),
                        y = h * 0.52f - (70.dp.toPx() * drift2Progress)
                    )
                    val orb2Radius = (w * 0.70f) * (1.10f - 0.15f * drift2Progress)
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.00f to orb2Color.copy(alpha = 0.70f),
                            0.25f to orb2Color.copy(alpha = 0.45f),
                            0.55f to orb2Color.copy(alpha = 0.20f),
                            0.80f to orb2Color.copy(alpha = 0.05f),
                            1.00f to Color.Transparent,
                            center = orb2Center,
                            radius = orb2Radius
                        ),
                        radius = orb2Radius,
                        center = orb2Center
                    )

                    val orb3Center = Offset(
                        x = w * 0.18f - (60.dp.toPx() * drift3Progress),
                        y = h * 0.82f + (80.dp.toPx() * drift3Progress)
                    )
                    val orb3Radius = (w * 0.72f) * (0.95f + 0.20f * drift3Progress)
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.00f to orb3Color.copy(alpha = 0.70f),
                            0.25f to orb3Color.copy(alpha = 0.45f),
                            0.55f to orb3Color.copy(alpha = 0.18f),
                            0.80f to orb3Color.copy(alpha = 0.05f),
                            1.00f to Color.Transparent,
                            center = orb3Center,
                            radius = orb3Radius
                        ),
                        radius = orb3Radius,
                        center = orb3Center
                    )

                    drawRect(Color(0x660B0B0F))
                }
            }

        // Top Grab Handle (pill)
        if (pillAlpha > 0.001f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 10.dp)
                    .size(width = 36.dp, height = 5.dp)
                    .graphicsLayer { alpha = pillAlpha }
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }

        // Mini Player Progress Line: anchored to the bottom edge of the container while fading out
        if (miniLineAlpha > 0.001f) {
            val progressFrac = (currentMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = ch - 2.dp)
                    .size(width = cw * progressFrac, height = 2.dp)
                    .graphicsLayer { alpha = miniLineAlpha }
                    .background(Color(0xFFF2F0EC))
            )
        }

        // Album Artwork (Smoothly morphs size, position, radius and shadow)
        val shadowElevationPx = with(density) { lerpDp(0.dp, lerpDp(30.dp, 4.dp, compactProgress), p).toPx() }
        // One fixed request per track: a URL/size that changes mid-animation made Coil reload and the cover blink
        val artRequest = remember(track.id) {
            ImageRequest.Builder(context)
                .data(client.getArtworkUrl(track.id, maxWidth = 900))
                .size(900)
                .build()
        }
        AsyncImage(
            model = artRequest,
            contentDescription = "Cover",
            modifier = Modifier
                .offset(x = artX + (artEnterX.value * p).dp, y = artY)
                .size(artSize)
                .scale(lerpFloat(lerpFloat(1.0f, artScale, q(p, 0.75f, 0.95f)), 1.0f, compactProgress) * artEnterScale.value)
                .graphicsLayer {
                    alpha = if (p > 0.8f) artEnterAlpha.value else 1f
                    shadowElevation = shadowElevationPx
                    shape = RoundedCornerShape(artRadius)
                    clip = true
                }
                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(artRadius))
                .background(ArtworkSurface),
            contentScale = ContentScale.Crop
        )

        // Title and Artist Texts
        // Track Title Text
        Box(
            modifier = Modifier
                .offset(x = titleX, y = titleY)
                .width(textWidth),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = track.name,
                color = ArtworkTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 30.sp,
                letterSpacing = (-0.015).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .offset(y = (titleEnterY.value * p).dp)
                    .graphicsLayer {
                        scaleX = titleScale
                        scaleY = titleScale
                        transformOrigin = TransformOrigin(0f, 0f)
                        alpha = if (p > 0.8f) titleEnterAlpha.value else 1f
                    }
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1200,
                        initialDelayMillis = 1500,
                        spacing = MarqueeSpacing(32.dp),
                        velocity = 30.dp
                    )
            )
        }

        // Artist Name Text
        Box(
            modifier = Modifier
                .offset(x = artistX, y = artistY)
                .width(textWidth),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = track.artistName,
                color = ArtworkTextSecondary.copy(alpha = artistAlpha),
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .offset(y = (artistEnterY.value * p).dp)
                    .graphicsLayer {
                        scaleX = artistScale
                        scaleY = artistScale
                        transformOrigin = TransformOrigin(0f, 0f)
                        alpha = if (p > 0.8f) artistEnterAlpha.value else 1f
                    }
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1200,
                        initialDelayMillis = 1500,
                        spacing = MarqueeSpacing(32.dp),
                        velocity = 28.dp
                    )
            )
        }

        // Heart Button (Fade + TranslateY)
        if (heartAlpha > 0.001f) {
            IconButton(
                onClick = {
                    val targetLiked = !isLiked
                    isLiked = targetLiked
                    coroutineScope.launch {
                        heartScale.animateTo(1.22f, tween(110, easing = FastOutSlowInEasing))
                        heartScale.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                    val curId = track.id
                    coroutineScope.launch {
                        client.setFavorite(curId, targetLiked)
                        controller.updateTrackFavorite(curId, targetLiked)
                    }
                },
                enabled = p > 0.6f,
                modifier = Modifier
                    .offset(x = heartX, y = heartY - ct)
                    .size(44.dp)
                    .graphicsLayer {
                        alpha = heartAlpha
                        translationY = with(density) { heartShiftY.toPx() }
                    }
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Preferiti",
                    tint = if (isLiked) ArtworkTextPrimary else Color(0xD9FFFFFF),
                    modifier = Modifier
                        .size(26.dp)
                        .scale(heartScale.value)
                )
            }
        }

        // Middle Content Panel (Lyrics / Queue) in Compact Mode with fluid fade & slide
        if (compactProgress > 0.001f) {
            val panelShiftY = 20.dp * (1f - compactProgress)
            Box(
                modifier = Modifier
                    .offset(x = 20.dp, y = 110.dp - ct)
                    .size(
                        width = screenWidth - 40.dp,
                        height = (scrubTop - 122.dp).coerceAtLeast(100.dp)
                    )
                    .graphicsLayer {
                        alpha = compactProgress
                        translationY = with(density) { panelShiftY.toPx() }
                    }
            ) {
                Crossfade(
                    targetState = lastContentMode,
                    animationSpec = tween(250),
                    label = "contentModeCrossfade"
                ) { currentMode ->
                    if (currentMode == SheetMode.LYRICS) {
                        LyricsPanel(
                            lyrics = lyrics,
                            isLoading = isLoadingLyrics,
                            isPlaying = playbackState.isPlaying,
                            currentPositionMs = currentMs,
                            durationMs = durationMs,
                            onSeek = { controller.seekTo(it) },
                            onCollapse = handleCollapse
                        )
                    } else if (currentMode == SheetMode.QUEUE) {
                        QueuePanel(
                            playbackState = playbackState,
                            onPlayIndex = { index ->
                                skipDirection = 1
                                controller.playQueue(
                                    items = playbackState.queue,
                                    startIndex = index,
                                    bitrateKbps = customizations.streamingQuality.kbps
                                )
                            },
                            onMoveItem = { from, to ->
                                controller.moveQueueItem(from, to)
                            },
                            onCollapse = handleCollapse
                        )
                    }
                }
            }
        }

        // Scrubber Bar & Timestamps (Transition.dc.html: scrubStyle & timesStyle)
        if (scrubAlpha > 0.001f) {
            val progressFrac = (currentMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .offset(x = scrubX, y = scrubTop - ct)
                    .width(scrubW)
                    .graphicsLayer {
                        alpha = scrubAlpha
                        translationY = with(density) { scrubShiftY.toPx() }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .pointerInput(durationMs, p > 0.6f) {
                            if (p < 0.6f) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                isDraggingSlider = true
                                val frac = (down.position.x / size.width).coerceIn(0f, 1f)
                                sliderPosition = frac * durationMs

                                var pointerId = down.id
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                    if (!change.pressed) break
                                    val newFrac = (change.position.x / size.width).coerceIn(0f, 1f)
                                    sliderPosition = newFrac * durationMs
                                    change.consume()
                                }
                                controller.seekTo(sliderPosition.toLong())
                                isDraggingSlider = false
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(scrubBarHeight)
                            .clip(RoundedCornerShape(scrubBarHeight / 2))
                            .background(ArtworkTrackBackground)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFrac)
                            .height(scrubBarHeight)
                            .clip(RoundedCornerShape(scrubBarHeight / 2))
                            .background(ArtworkControlFill)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMs(currentMs),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xA6FFFFFF)
                    )
                    Text(
                        text = "-${formatMs(remainMs)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xA6FFFFFF)
                    )
                }
            }
        }

        val speedControlsAlpha = (1f - speedDialogAnim.value).coerceIn(0f, 1f)

        // Shuffle Button
        // Shuffle Button
        if (shuffleAlpha > 0.001f) {
            val shuffleInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = { controller.toggleShuffle() },
                interactionSource = shuffleInteraction,
                enabled = p > 0.6f && !showSpeedDialog,
                modifier = Modifier
                    .offset(x = shuffleCx - 22.dp, y = controlsCenterY - 22.dp)
                    .size(44.dp)
                    .graphicsLayer { alpha = shuffleAlpha * speedControlsAlpha }
                    .pressScale(shuffleInteraction)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (playbackState.isShuffleEnabled) ArtworkTextPrimary else Color(0x66FFFFFF),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Previous Button
        if (prevAlpha > 0.001f) {
            val prevInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = {
                    skipDirection = -1
                    controller.skipPrevious()
                },
                interactionSource = prevInteraction,
                enabled = p > 0.6f && !showSpeedDialog,
                modifier = Modifier
                    .offset(x = prevCx - 32.dp, y = controlsCenterY - 32.dp)
                    .size(64.dp)
                    .graphicsLayer { alpha = prevAlpha * speedControlsAlpha }
                    .pressScale(prevInteraction)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Precedente",
                    tint = ArtworkTextPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Play / Pause Button
        val playInteraction = remember { MutableInteractionSource() }
        IconButton(
            onClick = { controller.togglePlayPause() },
            interactionSource = playInteraction,
            enabled = !showSpeedDialog,
            modifier = Modifier
                .offset(x = playCx - (playBtnSize / 2), y = controlsCenterY - (playBtnSize / 2))
                .size(playBtnSize)
                .graphicsLayer { alpha = speedControlsAlpha }
                .pressScale(playInteraction)
        ) {
            Icon(
                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playbackState.isPlaying) "Pausa" else "Riproduci",
                tint = ArtworkTextPrimary,
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = playIconScale
                        scaleY = playIconScale
                    }
            )
        }

        // Next Button
        val nextInteraction = remember { MutableInteractionSource() }
        IconButton(
            onClick = {
                skipDirection = 1
                controller.skipNext()
            },
            interactionSource = nextInteraction,
            enabled = !showSpeedDialog,
            modifier = Modifier
                .offset(x = nextCx - (nextBtnSize / 2), y = controlsCenterY - (nextBtnSize / 2))
                .size(nextBtnSize)
                .graphicsLayer { alpha = speedControlsAlpha }
                .pressScale(nextInteraction)
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Successivo",
                tint = ArtworkTextPrimary,
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer {
                        scaleX = nextIconScale
                        scaleY = nextIconScale
                    }
            )
        }

        // Repeat Button
        if (repeatAlpha > 0.001f) {
            val repeatInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = { controller.cycleRepeatMode() },
                interactionSource = repeatInteraction,
                enabled = p > 0.6f && !showSpeedDialog,
                modifier = Modifier
                    .offset(x = repeatCx - 22.dp, y = controlsCenterY - 22.dp)
                    .size(44.dp)
                    .graphicsLayer { alpha = repeatAlpha * speedControlsAlpha }
                    .pressScale(repeatInteraction)
            ) {
                val icon = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                val isRepeatActive = playbackState.repeatMode != Player.REPEAT_MODE_OFF
                Icon(
                    imageVector = icon,
                    contentDescription = "Ripeti",
                    tint = if (isRepeatActive) ArtworkTextPrimary else Color(0x66FFFFFF),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Volume Slider (Transition.dc.html: volStyle)
        if (volAlpha > 0.001f) {
            Row(
                modifier = Modifier
                    .offset(x = scrubX, y = volTop - ct)
                    .width(scrubW)
                    .graphicsLayer {
                        alpha = volAlpha * speedControlsAlpha
                        translationY = with(density) { volShiftY.toPx() }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.VolumeDown,
                    contentDescription = "Volume giù",
                    tint = Color(0xA6FFFFFF),
                    modifier = Modifier.size(18.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .pointerInput(p > 0.6f) {
                            if (p < 0.6f) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                isVolDragging = true
                                val newVol = (down.position.x / size.width).coerceIn(0f, 1f)
                                volume = newVol
                                controller.setVolume(newVol)

                                var pointerId = down.id
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                    if (!change.pressed) break
                                    val draggedVol = (change.position.x / size.width).coerceIn(0f, 1f)
                                    volume = draggedVol
                                    controller.setVolume(draggedVol)
                                    change.consume()
                                }
                                isVolDragging = false
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(volBarHeight)
                            .clip(RoundedCornerShape(volBarHeight / 2))
                            .background(ArtworkTrackBackground)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(volume)
                            .height(volBarHeight)
                            .clip(RoundedCornerShape(volBarHeight / 2))
                            .background(ArtworkControlFill)
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.VolumeUp,
                    contentDescription = "Volume su",
                    tint = Color(0xA6FFFFFF),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Bottom Tools Row (Lyrics, Speed, Queue) (Transition.dc.html: bottomStyle)
        if (bottomAlpha > 0.001f) {
            Row(
                modifier = Modifier
                    .offset(x = scrubX, y = bottomTop - ct)
                    .width(scrubW)
                    .graphicsLayer {
                        alpha = bottomAlpha * speedControlsAlpha
                        translationY = with(density) { bottomShiftY.toPx() }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lyrics Pill Button (Greyed out if lyrics are not present unless currently in lyrics mode)
                val hasLyrics = !isLoadingLyrics && lyrics.isNotEmpty()
                PillButton(
                    icon = Icons.Rounded.ChatBubbleOutline,
                    isActive = mode == SheetMode.LYRICS,
                    enabled = (hasLyrics || mode == SheetMode.LYRICS) && p > 0.6f,
                    onClick = {
                        savedModeBeforeCollapse = null
                        savedTrackIdBeforeCollapse = null
                        mode = if (mode == SheetMode.LYRICS) SheetMode.ART else SheetMode.LYRICS
                    }
                )

                // Playback Speed Button (Clean flat translucent pill, NO frosted blur when closed)
                val minPillWidth = if (playbackState.isPitchLinked) 80.dp else 66.dp
                Box(
                    modifier = Modifier
                        .size(width = minPillWidth, height = 36.dp)
                        .graphicsLayer {
                            // Hide static box when dialog is open so the morphing box takes over seamlessly
                            alpha = if (showSpeedDialog) 0f else 1f
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(12.dp))
                        .pressScale(remember { MutableInteractionSource() })
                        .clickable(enabled = p > 0.6f && !showSpeedDialog) { showSpeedDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (playbackState.isPitchLinked) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = ArtworkTextPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "%.2fx".format(java.util.Locale.US, playbackState.playbackSpeed),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ArtworkTextPrimary
                        )
                    }
                }

                // Queue Pill Button
                PillButton(
                    icon = Icons.Rounded.FormatListBulleted,
                    isActive = mode == SheetMode.QUEUE,
                    enabled = p > 0.6f,
                    onClick = {
                        savedModeBeforeCollapse = null
                        savedTrackIdBeforeCollapse = null
                        mode = if (mode == SheetMode.QUEUE) SheetMode.ART else SheetMode.QUEUE
                    }
                )
            }
        }

        // Playback Speed & Pitch Adjustment Pop-up Modal
        if (showSpeedDialog && p > 0.95f) {
            PlaybackSpeedDialog(
                currentSpeed = playbackState.playbackSpeed,
                isPitchLinked = playbackState.isPitchLinked,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                buttonY = bottomTop - ct + 4.dp,
                dialogAnim = speedDialogAnim,
                onSpeedChange = { controller.setSpeed(it) },
                onPitchLinkedChange = { controller.setPitchLinked(it) },
                onDismiss = { showSpeedDialog = false }
            )
        }
    }

    // Full player drag zone: covers header, pill, and down to the height of the cover art
    if (p > 0.01f && !showSpeedDialog) {
        val dragHeight = lerpDp(fullTitleY - 8.dp, 110.dp, compactProgress)
        Box(
            modifier = Modifier
                .offset(x = 0.dp, y = 0.dp)
                .size(width = screenWidth, height = dragHeight)
                .pointerInput(travelDistPx) {
                    val springSpec = spring<Float>(
                        dampingRatio = 0.9431f,
                        stiffness = 120f
                    )
                    val touchSlop = viewConfiguration.touchSlop

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startY = down.position.y
                        val startP = animProgress.value
                        var moved = false
                        val samples = mutableListOf(DragSample(System.currentTimeMillis(), down.position.y))

                        var pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                            val currentY = change.position.y
                            val dy = currentY - startY
                            if (!moved && kotlin.math.abs(dy) > touchSlop) {
                                moved = true
                                if (dy > 0 && mode != SheetMode.ART) {
                                    savedModeBeforeCollapse = mode
                                    savedTrackIdBeforeCollapse = track.id
                                    mode = SheetMode.ART
                                }
                            }
                            if (moved) {
                                change.consume()
                                val newP = (startP - dy / travelDistPx).coerceIn(0f, 1f)
                                coroutineScope.launch {
                                    animProgress.snapTo(newP)
                                }
                                val now = System.currentTimeMillis()
                                samples.add(DragSample(now, currentY))
                                if (samples.size > 6) {
                                    samples.removeAt(0)
                                }
                            }
                        }

                        if (!moved) {
                            if (startY <= with(density) { 70.dp.toPx() }) {
                                handleCollapse()
                            } else if (mode != SheetMode.ART) {
                                handleCollapse()
                            }
                        } else {
                            val first = samples.first()
                            val last = samples.last()
                            val spanSec = (last.timeMs - first.timeMs) / 1000f
                            val vy = if (spanSec > 0.001f) (last.y - first.y) / spanSec else 0f
                            val vp = (-vy / travelDistPx).coerceIn(-6f, 6f)
                            val currentP = animProgress.value
                            val proj = currentP + vp * 0.18f
                            val target = if (proj > 0.5f) 1f else 0f
                            if (target == 0f && mode != SheetMode.ART) {
                                savedModeBeforeCollapse = mode
                                savedTrackIdBeforeCollapse = track.id
                                mode = SheetMode.ART
                            }
                            coroutineScope.launch {
                                animProgress.animateTo(
                                    targetValue = target,
                                    animationSpec = springSpec,
                                    initialVelocity = vp
                                )
                            }
                        }
                    }
                }
        )
    }

    // Mini player hit zone: catches taps & drag-ups when in mini mode
    if (p < 0.99f) {
        Box(
            modifier = Modifier
                .offset(x = 10.dp, y = miniTop)
                .size(width = (screenWidth - 20.dp - 88.dp).coerceAtLeast(0.dp), height = miniH)
                .pointerInput(travelDistPx) {
                    val springSpec = spring<Float>(
                        dampingRatio = 0.9431f,
                        stiffness = 120f
                    )
                    val touchSlop = viewConfiguration.touchSlop

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startY = down.position.y
                        val startP = animProgress.value
                        var moved = false
                        val samples = mutableListOf(DragSample(System.currentTimeMillis(), down.position.y))

                        var pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                            val currentY = change.position.y
                            val dy = currentY - startY
                            if (!moved && kotlin.math.abs(dy) > touchSlop) {
                                moved = true
                            }
                            if (moved) {
                                change.consume()
                                val newP = (startP - dy / travelDistPx).coerceIn(0f, 1f)
                                coroutineScope.launch {
                                    animProgress.snapTo(newP)
                                }
                                val now = System.currentTimeMillis()
                                samples.add(DragSample(now, currentY))
                                if (samples.size > 6) {
                                    samples.removeAt(0)
                                }
                            }
                        }

                        if (!moved) {
                            onExpand()
                        } else {
                            val first = samples.first()
                            val last = samples.last()
                            val spanSec = (last.timeMs - first.timeMs) / 1000f
                            val vy = if (spanSec > 0.001f) (last.y - first.y) / spanSec else 0f
                            val vp = (-vy / travelDistPx).coerceIn(-6f, 6f)
                            val currentP = animProgress.value
                            val proj = currentP + vp * 0.18f
                            val target = if (proj > 0.5f) 1f else 0f
                            coroutineScope.launch {
                                animProgress.animateTo(
                                    targetValue = target,
                                    animationSpec = springSpec,
                                    initialVelocity = vp
                                )
                            }
                        }
                    }
                }
        )
    }
}
}

@Composable
private fun PlaybackSpeedDialog(
    currentSpeed: Float,
    isPitchLinked: Boolean,
    screenWidth: Dp,
    screenHeight: Dp,
    buttonY: Dp,
    dialogAnim: Animatable<Float, AnimationVector1D>,
    onSpeedChange: (Float) -> Unit,
    onPitchLinkedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isDismissing by remember { mutableStateOf(false) }
    val bezier = remember { CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f) }

    LaunchedEffect(Unit) {
        dialogAnim.snapTo(0f)
        dialogAnim.animateTo(1f, animationSpec = tween(550, easing = bezier))
    }

    val dismissWithAnim: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            coroutineScope.launch {
                dialogAnim.animateTo(0f, animationSpec = tween(550, easing = bezier))
                onDismiss()
            }
        }
    }

    BackHandler(enabled = true) {
        dismissWithAnim()
    }

    val density = LocalDensity.current
    val t = dialogAnim.value
    val smoothT = bezier.transform(t.coerceIn(0f, 1f))


    // Morphing Frosted Box geometry: from bottom pill to minimal bottom card
    val minBoxWidth = if (isPitchLinked) 80.dp else 66.dp
    val minBoxHeight = 36.dp
    val minBoxLeft = (screenWidth - minBoxWidth) / 2
    val minBoxTop = buttonY
    val minBoxRadius = 12.dp

    val openBoxWidth = (screenWidth - 28.dp).coerceAtLeast(310.dp)
    val openBoxHeight = 205.dp
    val openBoxLeft = (screenWidth - openBoxWidth) / 2
    val openBoxBottom = buttonY + 36.dp
    val openBoxTop = openBoxBottom - openBoxHeight
    val openBoxRadius = 22.dp

    val boxW = lerpDp(minBoxWidth, openBoxWidth, smoothT)
    val boxH = lerpDp(minBoxHeight, openBoxHeight, smoothT)
    val boxX = lerpDp(minBoxLeft, openBoxLeft, smoothT)
    val boxY = lerpDp(minBoxTop, openBoxTop, smoothT)
    val boxR = lerpDp(minBoxRadius, openBoxRadius, smoothT)

    // Number & tune icon scaling and positioning inside the box
    val numberCenterY = lerpDp(boxH / 2, 32.dp, smoothT)
    val rowHeight = lerpDp(20.dp, 36.dp, smoothT)
    val textSize = lerpFloat(13f, 30f, smoothT).sp
    val iconSize = lerpDp(13.dp, 18.dp, smoothT)
    val iconGap = lerpDp(4.dp, 6.dp, smoothT)

    // Secondary controls appearance: close button, speed slider, scale & pitch toggle fade in smoothly
    val contentAlpha = q(smoothT, 0.35f, 0.95f)
    val contentShiftY = with(density) { (12.dp * (1f - contentAlpha)).toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f * smoothT))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { dismissWithAnim() }
    ) {
        // Physical Morphing Frosted Box (Minimal bottom sheet anchored to the lower screen)
        Box(
            modifier = Modifier
                .offset(x = boxX, y = boxY)
                .size(width = boxW, height = boxH)
                .clip(RoundedCornerShape(boxR))
                .border(
                    1.dp,
                    Color.White.copy(alpha = lerpFloat(0.22f, 0.28f, smoothT)),
                    RoundedCornerShape(boxR)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume click inside */ }
        ) {
            // Dark frosted blur backdrop layer inside the box (BEHIND the buttons, slider, and text)
            val darkBaseAlpha = lerpFloat(0.22f, 0.94f, smoothT)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(boxR))
                    .background(Color(0xFF141418).copy(alpha = darkBaseAlpha))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x35FFFFFF),
                                Color(0x10FFFFFF),
                                Color(0x22FFFFFF)
                            )
                        )
                    )
                    .blur(20.dp)
            )

            // Number & Tune Icon: expands and travels seamlessly from pill center to top of the minimal card
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (numberCenterY - rowHeight / 2).coerceAtLeast(0.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isPitchLinked) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = ArtworkTextPrimary,
                        modifier = Modifier
                            .padding(end = iconGap)
                            .size(iconSize)
                    )
                }
                Text(
                    text = "%.2fx".format(java.util.Locale.US, currentSpeed),
                    fontSize = textSize,
                    fontWeight = if (smoothT > 0.45f) FontWeight.Bold else FontWeight.SemiBold,
                    color = ArtworkTextPrimary
                )
            }

            if (contentAlpha > 0.001f) {
                // Controls Column: Slider, Scale & Pitch Toggle (Fades in with upward settle)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 54.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .graphicsLayer {
                            alpha = contentAlpha
                            translationY = contentShiftY
                        }
                ) {
                    // Continuous Speed Slider Bar & Perfectly Aligned Scale
                    Slider(
                        value = currentSpeed,
                        onValueChange = { raw ->
                            val rounded = kotlin.math.round(raw * 100f) / 100f
                            // Magnetic subtle snap to exact 1.00x default
                            val finalSpeed = if (kotlin.math.abs(rounded - 1.00f) <= 0.015f) 1.00f else rounded
                            onSpeedChange(finalSpeed)
                        },
                        valueRange = 0.50f..2.00f,
                        colors = SliderDefaults.colors(
                            thumbColor = ArtworkTextPrimary,
                            activeTrackColor = ArtworkTextPrimary,
                            inactiveTrackColor = ArtworkTrackBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Accurately Aligned Scale
                    SpeedScaleBar(
                        currentSpeed = currentSpeed,
                        onSpeedSelected = onSpeedChange
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pitch Link Toggle (Fades in with upward settle)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x18FFFFFF))
                            .border(
                                1.dp,
                                if (isPitchLinked) Color(0x40FFFFFF) else Color(0x1AFFFFFF),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onPitchLinkedChange(!isPitchLinked) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Collega velocità al pitch",
                            color = ArtworkTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isPitchLinked) ArtworkTextPrimary else Color(0x33FFFFFF)),
                            contentAlignment = if (isPitchLinked) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (isPitchLinked) Color(0xFF0B0B0F) else Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedScaleBar(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val minSpeed = 0.50f
    val maxSpeed = 2.00f
    val range = maxSpeed - minSpeed

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val totalWidth = maxWidth
        val thumbRadius = 10.dp
        val trackWidth = (totalWidth - thumbRadius * 2).coerceAtLeast(0.dp)

        Column(modifier = Modifier.fillMaxWidth()) {
            // Ticks row
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                val trackStartPx = thumbRadius.toPx()
                val trackWidthPx = trackWidth.toPx()

                val tickSpeeds = listOf(
                    0.50f to true,
                    0.75f to false,
                    1.00f to true,
                    1.25f to false,
                    1.50f to true,
                    1.75f to false,
                    2.00f to true
                )

                tickSpeeds.forEach { (sp, isMajor) ->
                    val frac = (sp - minSpeed) / range
                    val x = trackStartPx + trackWidthPx * frac
                    val tickHeight = if (sp == 1.00f) 8.dp.toPx() else if (isMajor) 5.dp.toPx() else 3.dp.toPx()
                    val strokeW = if (sp == 1.00f) 2.dp.toPx() else 1.dp.toPx()
                    val color = if (sp == 1.00f) Color.White else if (isMajor) Color(0x99FFFFFF) else Color(0x4DFFFFFF)

                    drawLine(
                        color = color,
                        start = Offset(x, 0f),
                        end = Offset(x, tickHeight),
                        strokeWidth = strokeW
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Labels row with precise geometric positioning
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            ) {
                // 0.50x at the start
                Text(
                    text = "0.50x",
                    fontSize = 11.sp,
                    fontWeight = if (kotlin.math.abs(currentSpeed - 0.50f) < 0.02f) FontWeight.Bold else FontWeight.Normal,
                    color = if (kotlin.math.abs(currentSpeed - 0.50f) < 0.02f) ArtworkTextPrimary else ArtworkTextMuted,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { onSpeedSelected(0.50f) }
                )

                // 1.00x exactly at frac = 0.3333f of the track
                val frac1 = (1.00f - minSpeed) / range
                Box(
                    modifier = Modifier
                        .offset(x = thumbRadius + trackWidth * frac1 - 16.dp)
                        .width(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "1.00x",
                        fontSize = 11.sp,
                        fontWeight = if (kotlin.math.abs(currentSpeed - 1.00f) < 0.02f) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (kotlin.math.abs(currentSpeed - 1.00f) < 0.02f) ArtworkTextPrimary else Color(0xCCFFFFFF),
                        modifier = Modifier.clickable { onSpeedSelected(1.00f) }
                    )
                }

                // 1.50x exactly at frac = 0.6667f of the track
                val frac15 = (1.50f - minSpeed) / range
                Box(
                    modifier = Modifier
                        .offset(x = thumbRadius + trackWidth * frac15 - 16.dp)
                        .width(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "1.50x",
                        fontSize = 11.sp,
                        fontWeight = if (kotlin.math.abs(currentSpeed - 1.50f) < 0.02f) FontWeight.Bold else FontWeight.Normal,
                        color = if (kotlin.math.abs(currentSpeed - 1.50f) < 0.02f) ArtworkTextPrimary else ArtworkTextMuted,
                        modifier = Modifier.clickable { onSpeedSelected(1.50f) }
                    )
                }

                // 2.00x at the end
                Text(
                    text = "2.00x",
                    fontSize = 11.sp,
                    fontWeight = if (kotlin.math.abs(currentSpeed - 2.00f) < 0.02f) FontWeight.Bold else FontWeight.Normal,
                    color = if (kotlin.math.abs(currentSpeed - 2.00f) < 0.02f) ArtworkTextPrimary else ArtworkTextMuted,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable { onSpeedSelected(2.00f) }
                )
            }
        }
    }
}

@Composable
private fun PillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val animatedTint by animateColorAsState(
        targetValue = when {
            !enabled -> Color(0x38FFFFFF) // Greyed out!
            isActive -> ArtworkTextPrimary
            else -> Color(0xA6FFFFFF)
        },
        animationSpec = tween(300),
        label = "pillTint"
    )
    val animatedBg by animateColorAsState(
        targetValue = if (isActive && enabled) Color(0x29FFFFFF) else Color.Transparent,
        animationSpec = tween(300),
        label = "pillBg"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(animatedBg)
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = animatedTint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val bezierE = remember { CubicBezierEasing(0.22f, 0.8f, 0.2f, 1f) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = tween(200, easing = bezierE),
        label = "pressScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.80f else 1.0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "pressAlpha"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}

@Composable
private fun LyricsPanel(
    lyrics: List<LyricLineDto>,
    isLoading: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onCollapse: () -> Unit = {}
) {
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 15f && source == NestedScrollSource.UserInput) {
                    onCollapse()
                    return available
                }
                return Offset.Zero
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = ArtworkTextPrimary,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.5.dp
                )
                Text(
                    text = "Ricerca testi (BetterLyrics / Apple Music)...",
                    color = ArtworkTextSecondary,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    if (lyrics.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nessun testo sincronizzato disponibile per questo brano",
                color = ArtworkTextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    val controller = JellyMusicApp.instance.playerController
    val playTimeMs = remember { mutableLongStateOf(currentPositionMs) }

    LaunchedEffect(currentPositionMs) {
        playTimeMs.longValue = currentPositionMs
    }

    // Hardware VSYNC-aligned frame clock (120Hz on S25 Ultra)
    // Deferring playback read to the Draw phase completely skips recomposition thrashing!
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                withFrameMillis {
                    playTimeMs.longValue = controller.getCurrentPositionMs()
                }
            }
        } else {
            playTimeMs.longValue = currentPositionMs
        }
    }

    val hasTimestamps = lyrics.any { it.startTicks != null || it.backgroundVoice != null }
    val activeIndex by remember(lyrics) {
        derivedStateOf {
            val pos = playTimeMs.longValue
            if (hasTimestamps) {
                val found = lyrics.indexOfLast { it.earliestStartMs <= pos }
                if (found >= 0) found else 0
            } else {
                ((pos.toFloat() / durationMs.toFloat()) * lyrics.size)
                    .toInt().coerceIn(0, lyrics.lastIndex)
            }
        }
    }

    val listState = rememberLazyListState()
    val bezierEasing = remember { CubicBezierEasing(0.22f, 0.8f, 0.2f, 1f) }
    var isFirstLoad by remember(lyrics) { mutableStateOf(true) }

    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            if (isFirstLoad) {
                listState.scrollToItem(index = activeIndex, scrollOffset = 0)
                isFirstLoad = false
            } else {
                val info = listState.layoutInfo
                val item = info.visibleItemsInfo.firstOrNull { it.index == activeIndex }
                if (item != null) {
                    listState.animateScrollBy(
                        (item.offset - info.beforeContentPadding).toFloat(),
                        tween(durationMillis = 800, easing = bezierEasing)
                    )
                } else {
                    listState.animateScrollToItem(index = activeIndex, scrollOffset = 0)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 96.dp, bottom = 480.dp),
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.14f to Color.Black,
                            0.84f to Color.Black,
                            1.0f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(
                items = lyrics,
                key = { index, _ -> index }
            ) { index, line ->
                val distance = Math.abs(index - activeIndex)
                val isActive = index == activeIndex
                val isPast = index < activeIndex

                LyricLineItem(
                    line = line,
                    isActive = isActive,
                    isPast = isPast,
                    distance = distance,
                    playTimeMs = playTimeMs,
                    bezierEasing = bezierEasing,
                    onClick = {
                        val seekTarget = if (line.startTicks != null || line.backgroundVoice != null) {
                            line.earliestStartMs
                        } else {
                            (index.toFloat() / lyrics.size.toFloat() * durationMs).toLong()
                        }
                        onSeek(seekTarget)
                    }
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun LyricLineItem(
    line: LyricLineDto,
    isActive: Boolean,
    isPast: Boolean,
    distance: Int,
    playTimeMs: State<Long>,
    bezierEasing: CubicBezierEasing,
    onClick: () -> Unit
) {
    val targetAlpha = when {
        isActive -> 1.0f
        distance == 1 -> 0.45f
        distance == 2 -> 0.28f
        else -> 0.16f
    }
    val targetBlur = when {
        isActive -> 0.dp
        distance == 1 -> 1.5.dp
        distance == 2 -> 3.dp
        else -> 4.5.dp
    }
    val targetScale = if (isActive) 1.0f else 0.92f

    val relaxed = tween<Float>(durationMillis = 800, easing = bezierEasing)
    val animatedAlpha by animateFloatAsState(targetAlpha, relaxed, label = "lyricAlpha")
    val animatedScale by animateFloatAsState(targetScale, relaxed, label = "lyricScale")
    val animatedBlur by animateDpAsState(
        targetValue = targetBlur,
        animationSpec = tween(durationMillis = 800, easing = bezierEasing),
        label = "lyricBlur"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .blur(animatedBlur)
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Secondary voice / ad-lib ABOVE main text if it started before main voice
            if (line.backgroundVoice != null && line.isBgAbove) {
                BackgroundVoiceRow(
                    phrase = line.backgroundVoice,
                    playTimeMs = playTimeMs,
                    isActiveLine = isActive
                )
            }

            // Main voice line
            MainVoiceRow(
                line = line,
                playTimeMs = playTimeMs,
                isActiveLine = isActive
            )

            // Secondary voice / ad-lib BELOW main text if it started during or after main voice
            if (line.backgroundVoice != null && !line.isBgAbove) {
                BackgroundVoiceRow(
                    phrase = line.backgroundVoice,
                    playTimeMs = playTimeMs,
                    isActiveLine = isActive
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun MainVoiceRow(
    line: LyricLineDto,
    playTimeMs: State<Long>,
    isActiveLine: Boolean
) {
    if (isActiveLine && line.hasSyllables) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            line.words.forEach { word ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    word.syllables.forEach { syllable ->
                        SyllableItem(
                            syllable = syllable,
                            playTimeMs = playTimeMs,
                            isActiveLine = true,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp
                        )
                    }
                }
            }
        }
    } else {
        val isMainSinging = !line.isBgAbove || playTimeMs.value >= line.startMs
        Text(
            text = line.text.replace(Regex("<[^>]*>"), "").trim(),
            fontSize = 26.sp,
            fontWeight = if (isActiveLine && isMainSinging) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isActiveLine && isMainSinging) Color.White else Color.White.copy(alpha = 0.35f),
            lineHeight = 36.sp,
            letterSpacing = (-0.015).sp
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun BackgroundVoiceRow(
    phrase: LyricPhrase,
    playTimeMs: State<Long>,
    isActiveLine: Boolean
) {
    if (isActiveLine && phrase.words.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            phrase.words.forEach { word ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    word.syllables.forEach { syllable ->
                        SyllableItem(
                            syllable = syllable,
                            playTimeMs = playTimeMs,
                            isActiveLine = true,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
    } else {
        Text(
            text = phrase.text.replace(Regex("<[^>]*>"), "").trim(),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isActiveLine) Color.White else Color.White.copy(alpha = 0.35f),
            lineHeight = 24.sp,
            letterSpacing = (-0.01).sp
        )
    }
}

@Composable
private fun SyllableItem(
    syllable: LyricSpan,
    playTimeMs: State<Long>,
    isActiveLine: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    lineHeight: androidx.compose.ui.unit.TextUnit
) {
    val cleanText = remember(syllable.text) { syllable.text.replace(Regex("<[^>]*>"), "") }
    Box(contentAlignment = Alignment.CenterStart) {
        // Base muted text (un-sung state)
        Text(
            text = cleanText,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = Color.White.copy(alpha = 0.32f),
            lineHeight = lineHeight,
            letterSpacing = (-0.015).sp
        )
        // Clean crisp white text clipped smoothly by progress (GPU-accelerated scissor, zero halo!)
        if (isActiveLine) {
            Box(
                modifier = Modifier.drawWithContent {
                    val currentMs = playTimeMs.value
                    val startMs = syllable.startMs
                    val endMs = syllable.endMs
                    val duration = (endMs - startMs).coerceAtLeast(1L)
                    val progress = when {
                        currentMs < startMs -> 0f
                        currentMs >= endMs -> 1f
                        else -> ((currentMs - startMs).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    }

                    if (progress > 0f) {
                        if (progress < 1f) {
                            clipRect(left = 0f, top = 0f, right = size.width * progress, bottom = size.height) {
                                this@drawWithContent.drawContent()
                            }
                        } else {
                            this@drawWithContent.drawContent()
                        }
                    }
                }
            ) {
                Text(
                    text = cleanText,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = Color.White,
                    lineHeight = lineHeight,
                    letterSpacing = (-0.015).sp
                )
            }
        }
    }
}

@Composable
private fun QueuePanel(
    playbackState: PlaybackState,
    onPlayIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onCollapse: () -> Unit = {}
) {
    val client = JellyMusicApp.instance.jellyfinClient

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 15f && source == NestedScrollSource.UserInput) {
                    onCollapse()
                    return available
                }
                return Offset.Zero
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "IN CODA",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0x99FFFFFF),
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(playbackState.queue) { index, item ->
                val isCurrent = index == playbackState.currentIndex

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCurrent) Color(0x1AFFFFFF) else Color.Transparent)
                        .clickable { onPlayIndex(index) }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(48.dp)) {
                        AsyncImage(
                            model = client.getArtworkUrl(item.id, maxWidth = 160),
                            contentDescription = "Thumb",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                .background(ArtworkSurface),
                            contentScale = ContentScale.Crop
                        )
                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x33000000), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = ArtworkTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.artistName,
                            color = ArtworkTextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Tasti funzionali di spostamento brani nella coda
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(
                            onClick = { onMoveItem(index, index - 1) },
                            enabled = index > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowUp,
                                contentDescription = "Sposta su",
                                tint = if (index > 0) ArtworkTextPrimary else Color(0x33FFFFFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { onMoveItem(index, index + 1) },
                            enabled = index < playbackState.queue.size - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Sposta giù",
                                tint = if (index < playbackState.queue.size - 1) ArtworkTextPrimary else Color(0x33FFFFFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun extractArtworkPalette(bitmap: Bitmap): Triple<Color, Color, Color> {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val hsv = FloatArray(3)
    data class HsvColor(val argb: Int, val h: Float, val s: Float, val v: Float)

    val validColors = ArrayList<HsvColor>(pixels.size)
    for (pixel in pixels) {
        val alpha = (pixel ushr 24) and 0xFF
        if (alpha < 128) continue
        android.graphics.Color.colorToHSV(pixel, hsv)
        val s = hsv[1]
        val v = hsv[2]
        // Discard near-black and pure-gray to find vibrant, colorful tones
        if (v > 0.12f && (s > 0.15f || (v > 0.25f && v < 0.90f))) {
            validColors.add(HsvColor(pixel, hsv[0], s, v))
        }
    }

    if (validColors.isEmpty()) {
        return Triple(AmbientAmber, AmbientIndigo, AmbientDeepBlue)
    }

    // 1. Primary vibrant color (strong saturation and brightness)
    val c1 = validColors.maxByOrNull { it.s * 1.6f + it.v } ?: validColors.first()

    // 2. Secondary accent: distinct hue (>= 30° difference) with good saturation
    val c2 = validColors
        .filter {
            val diff = Math.abs(it.h - c1.h)
            val hueDiff = if (diff > 180f) 360f - diff else diff
            hueDiff >= 30f && it.s > 0.18f
        }
        .maxByOrNull { it.s * 1.3f + it.v }
        ?: validColors.maxByOrNull {
            val diff = Math.abs(it.h - c1.h)
            if (diff > 180f) 360f - diff else diff
        }
        ?: c1

    // 3. Deeper complementary or shadow mood color from the artwork
    val c3 = validColors
        .filter { it.v in 0.15f..0.50f }
        .maxByOrNull { it.s }
        ?: validColors.minByOrNull { it.v }
        ?: c1

    fun boostVibrancy(argb: Int): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        hsv[1] = (hsv[1] * 1.35f).coerceIn(0.28f, 0.95f)
        hsv[2] = (hsv[2] * 1.15f).coerceIn(0.35f, 0.92f)
        return android.graphics.Color.HSVToColor(hsv)
    }

    return Triple(
        Color(boostVibrancy(c1.argb)),
        Color(boostVibrancy(c2.argb)),
        Color(boostVibrancy(c3.argb))
    )
}
