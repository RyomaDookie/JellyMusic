package com.jellymusic.player.ui.screens

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.jellymusic.player.JellyMusicApp
import com.jellymusic.player.data.JellyfinItem
import com.jellymusic.player.playback.PlaybackState
import com.jellymusic.player.preferences.UserCustomizations
import com.jellymusic.player.ui.theme.extractPaletteFromBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val AppleEase = CubicBezierEasing(0.22f, 0.8f, 0.2f, 1f)
private val AppleMuted = Color(0xFF8E8E93)
private val AppleSubtle = Color(0xFF636366)
private val AppleSurfaceVoid = Color(0xFF070709)
private val AppleCardBorder = Color(0x18FFFFFF)

@Composable
fun HomeScreen(
    customizations: UserCustomizations,
    playbackState: PlaybackState,
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = JellyMusicApp.instance
    val client = app.jellyfinClient
    val controller = app.playerController
    val kbps = customizations.streamingQuality.kbps

    // Shared tracks cache to prevent blank flashes
    var tracks by remember { mutableStateOf(controller.cachedTracks) }
    LaunchedEffect(Unit) {
        client.getAudioTracks().onSuccess {
            tracks = it
            controller.updateCachedTracks(it)
        }
    }

    // Real persistent playback history from DataStore
    val recentTrackIds by app.preferences.recentTrackIds.collectAsState(initial = emptyList())
    val recentTracks = remember(recentTrackIds, tracks) {
        if (recentTrackIds.isNotEmpty()) {
            val trackMap = tracks.associateBy { it.id }
            val matched = recentTrackIds.mapNotNull { trackMap[it] }
            if (matched.isNotEmpty()) matched else tracks.take(15)
        } else {
            if (tracks.any { !it.dateCreated.isNullOrEmpty() }) tracks.sortedByDescending { it.dateCreated }.take(15)
            else tracks.take(15)
        }
    }

    val favoriteTracks = remember(tracks) { tracks.filter { it.isFavorite } }
    val displayFeedTracks = remember(tracks) { tracks.take(20) }

    // Truly randomized Daily Mix (seeded by day so it's fresh each day, not ordered by date added)
    val dailyMixTracks = remember(tracks) {
        if (tracks.isEmpty()) emptyList()
        else {
            val daySeed = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            tracks.shuffled(kotlin.random.Random(daySeed))
        }
    }

    val dateText = remember {
        SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN).format(Date())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ITALIAN) else it.toString() }
    }
    val greetingText = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Buongiorno"
            in 12..17 -> "Buon pomeriggio"
            else -> "Buonasera"
        }
    }

    // Dynamic cover art palette extraction tied to currently playing track
    val activeTrack = playbackState.currentTrack ?: recentTracks.firstOrNull() ?: tracks.firstOrNull()
    var currentPalette by remember(activeTrack?.id) {
        mutableStateOf<Triple<Color, Color, Color>?>(null)
    }

    LaunchedEffect(activeTrack?.id) {
        if (activeTrack != null) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(client.getArtworkUrl(activeTrack.id, maxWidth = 250))
                    .allowHardware(false)
                    .size(48, 48)
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        ?: result.drawable.toBitmap(48, 48)
                    currentPalette = withContext(Dispatchers.Default) {
                        extractPaletteFromBitmap(bitmap)
                    }
                }
            } catch (_: Exception) {}
        } else {
            currentPalette = null
        }
    }

    val seen = remember { mutableSetOf<String>() }

    Box(modifier = modifier.fillMaxSize().background(AppleSurfaceVoid)) {
        // Soft, diffused ambient glow derived from current track's artwork
        ArtworkAmbientGlow(
            palette = currentPalette,
            isPlaying = playbackState.isPlaying
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 22.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // --- Header: Apple-style clean date & greeting ---
            item {
                AppleIn("header", 0, seen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = dateText.uppercase(Locale.ITALIAN),
                                color = AppleMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = greetingText,
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.02).sp
                            )
                        }

                        val profileSrc = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .pressSpring(profileSrc)
                                .clip(CircleShape)
                                .background(Color(0x1FFFFFFF))
                                .border(1.dp, AppleCardBorder, CircleShape)
                                .clickable(interactionSource = profileSrc, indication = null) { onOpenSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Profilo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }


            // --- Hero: Continue Listening (Apple Music Card) ---
            item {
                AppleIn("hero", 2, seen) {
                    ContinueCard(
                        playbackState = playbackState,
                        fallback = recentTracks.firstOrNull() ?: tracks.firstOrNull(),
                        tintColor = currentPalette?.first ?: Color.White,
                        onOpen = {
                            if (playbackState.currentTrack != null) onOpenPlayer()
                            else if (tracks.isNotEmpty()) controller.playQueue(tracks, 0, kbps)
                        },
                        onToggle = {
                            if (playbackState.currentTrack != null) controller.togglePlayPause()
                            else if (tracks.isNotEmpty()) controller.playQueue(tracks, 0, kbps)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // --- Curated Stations: Mix del giorno & Preferiti ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppleIn("mix", 3, seen, Modifier.weight(1f)) {
                        val src = remember { MutableInteractionSource() }
                        val cover = dailyMixTracks.firstOrNull() ?: tracks.firstOrNull()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .pressSpring(src)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF141418))
                                .border(1.dp, AppleCardBorder, RoundedCornerShape(20.dp))
                                .clickable(interactionSource = src, indication = null) {
                                    if (dailyMixTracks.isNotEmpty()) controller.playQueue(dailyMixTracks, 0, kbps)
                                    else if (tracks.isNotEmpty()) controller.playQueue(tracks.shuffled(), 0, kbps)
                                }
                        ) {
                            if (cover != null) {
                                AsyncImage(
                                    model = client.getArtworkUrl(cover.id, maxWidth = 500),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        0f to Color(0x33000000),
                                        0.50f to Color(0xB3070709),
                                        1f to Color(0xF5070709)
                                    )
                                )
                            )
                            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                                Text(
                                    "Mix del giorno",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.015).sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = if (dailyMixTracks.isNotEmpty()) {
                                        dailyMixTracks.take(4).map { it.artistName }.filter { it.isNotBlank() }.distinct().take(3).joinToString(", ")
                                    } else "Brani casuali della libreria",
                                    color = AppleMuted,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            ApplePlayBadge(Modifier.align(Alignment.TopEnd).padding(12.dp))
                        }
                    }

                    AppleIn("fav", 4, seen, Modifier.weight(1f)) {
                        val src = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .pressSpring(src)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF331622), Color(0xFF1A0D13), Color(0xFF0F080C))
                                    )
                                )
                                .border(1.dp, AppleCardBorder, RoundedCornerShape(20.dp))
                                .clickable(interactionSource = src, indication = null) {
                                    if (favoriteTracks.isNotEmpty()) controller.playQueue(favoriteTracks, 0, kbps)
                                    else onNavigateToTab(1)
                                }
                        ) {
                            Icon(
                                Icons.Default.Favorite, null,
                                tint = Color(0x18FFFFFF),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(120.dp)
                                    .padding(end = 4.dp)
                            )
                            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                                Text(
                                    "Preferiti",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.015).sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = if (favoriteTracks.isNotEmpty()) "${favoriteTracks.size} brani salvati" else "Nessun preferito",
                                    color = AppleMuted,
                                    fontSize = 11.sp
                                )
                            }
                            ApplePlayBadge(Modifier.align(Alignment.TopEnd).padding(12.dp))
                        }
                    }
                }
            }

            // --- Real Recently Played Carousel ---
            if (recentTracks.isNotEmpty()) {
                item {
                    AppleIn("newsTitle", 5, seen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Ascoltati di recente",
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.015).sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onNavigateToTab(1) }
                            ) {
                                Text(
                                    text = "Vedi tutti",
                                    color = AppleMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = AppleMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(recentTracks, key = { _, t -> "recent_${t.id}" }) { i, t ->
                            AppleIn(
                                key = "recent_${t.id}",
                                index = i.coerceAtMost(6),
                                seen = seen
                            ) {
                                AppleTrackCard(
                                    track = t,
                                    isCurrent = playbackState.currentTrack?.id == t.id,
                                    isPlaying = playbackState.isPlaying,
                                    onClick = { controller.playQueue(recentTracks, i, kbps) }
                                )
                            }
                        }
                    }
                }
            }

            // --- Feed: Brani / Consigliati (Apple Music Track Rows) ---
            if (displayFeedTracks.isNotEmpty()) {
                item {
                    AppleIn("feedTitle", 7, seen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Brani consigliati",
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.015).sp
                            )
                            Text(
                                text = "${displayFeedTracks.size} brani",
                                color = AppleSubtle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                itemsIndexed(displayFeedTracks, key = { _, t -> "feed_${t.id}" }) { idx, track ->
                    AppleIn("feedItem_${track.id}", 8 + (idx % 6), seen) {
                        AppleTrackRowItem(
                            index = idx + 1,
                            track = track,
                            isCurrent = playbackState.currentTrack?.id == track.id,
                            isPlaying = playbackState.isPlaying,
                            onClick = {
                                controller.playQueue(displayFeedTracks, idx, kbps)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Soft, fluid ambient glow inspired by Apple Music's Now Playing diffusion.
 * Colors are extracted from the album artwork and drift gently in the dark void.
 */
@Composable
private fun ArtworkAmbientGlow(
    palette: Triple<Color, Color, Color>?,
    isPlaying: Boolean
) {
    val t = rememberInfiniteTransition(label = "appleAmbient")
    val drift by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift"
    )
    val drift2 by t.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(26000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift2"
    )

    // Animated color transitions when switching tracks
    val c1Target = if (palette != null && isPlaying) palette.first.copy(alpha = 0.22f) else Color(0x181F2532)
    val c2Target = if (palette != null && isPlaying) palette.second.copy(alpha = 0.16f) else Color(0x10161A22)
    val c3Target = if (palette != null && isPlaying) palette.third.copy(alpha = 0.12f) else Color.Transparent

    val animatedC1 by animateColorAsState(targetValue = c1Target, animationSpec = tween(1200), label = "amb1")
    val animatedC2 by animateColorAsState(targetValue = c2Target, animationSpec = tween(1200), label = "amb2")
    val animatedC3 by animateColorAsState(targetValue = c3Target, animationSpec = tween(1200), label = "amb3")

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Upper ambient glow circle
        val p1 = Offset(w * (0.15f + 0.45f * drift), h * 0.12f)
        drawCircle(
            Brush.radialGradient(listOf(animatedC1, Color.Transparent), center = p1, radius = w * 1.0f),
            radius = w * 1.0f, center = p1
        )

        // Mid right ambient glow
        val p2 = Offset(w * (0.90f - 0.5f * drift), h * 0.45f)
        drawCircle(
            Brush.radialGradient(listOf(animatedC2, Color.Transparent), center = p2, radius = w * 0.9f),
            radius = w * 0.9f, center = p2
        )

        // Lower soft glow
        if (animatedC3 != Color.Transparent) {
            val p3 = Offset(w * (0.35f + 0.35f * drift2), h * 0.75f)
            drawCircle(
                Brush.radialGradient(listOf(animatedC3, Color.Transparent), center = p3, radius = w * 0.85f),
                radius = w * 0.85f, center = p3
            )
        }
    }
}

@Composable
private fun ContinueCard(
    playbackState: PlaybackState,
    fallback: JellyfinItem?,
    tintColor: Color,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val client = JellyMusicApp.instance.jellyfinClient
    val src = remember { MutableInteractionSource() }
    val track = playbackState.currentTrack ?: fallback
    val durationMs = playbackState.durationMs.coerceAtLeast(1L)
    val progress = (playbackState.currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)

    // Smooth Ken Burns zoom
    val zoom by rememberInfiniteTransition(label = "kb").animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(16000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "zoom"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(205.dp)
            .pressSpring(src)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141418))
            .border(1.dp, AppleCardBorder, RoundedCornerShape(20.dp))
            .clickable(interactionSource = src, indication = null) { onOpen() }
    ) {
        if (track != null) {
            AsyncImage(
                model = client.getArtworkUrl(track.id, maxWidth = 700),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = zoom; scaleY = zoom }
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x30000000),
                    0.50f to Color(0xA6070709),
                    1f to Color(0xFA070709)
                )
            )
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Telemetry Tag
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x38000000))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EqualizerBars(active = playbackState.isPlaying, color = Color.White)
                Text(
                    text = if (playbackState.isPlaying) "IN RIPRODUZIONE" else "CONTINUA AD ASCOLTARE",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Column(
                    Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                ) {
                    Text(
                        text = track?.name ?: "Nessun brano in riproduzione",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.015).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = track?.artistName ?: "Seleziona un brano",
                        color = AppleMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                    // Minimalist clean progress line
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x33FFFFFF))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress.coerceAtLeast(0.02f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                        )
                    }
                }

                // Apple Music Signature White Play/Pause Circle Button
                val btnSrc = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .pressSpring(btnSrc)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(interactionSource = btnSrc, indication = null) { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pausa" else "Riproduci",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleTrackCard(
    track: JellyfinItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val client = JellyMusicApp.instance.jellyfinClient
    val src = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .width(144.dp)
            .pressSpring(src)
            .clickable(interactionSource = src, indication = null) { onClick() }
    ) {
        Box(
            Modifier
                .size(144.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141418))
                .border(1.dp, AppleCardBorder, RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = client.getArtworkUrl(track.id, maxWidth = 360),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isCurrent) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xB3000000))
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    EqualizerBars(active = isPlaying, color = Color.White)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = track.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artistName,
            color = AppleMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AppleTrackRowItem(
    index: Int,
    track: JellyfinItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val client = JellyMusicApp.instance.jellyfinClient
    val src = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressSpring(src)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) Color(0x1AFFFFFF) else Color(0x06FFFFFF))
            .border(
                1.dp,
                if (isCurrent) Color(0x33FFFFFF) else Color(0x0AFFFFFF),
                RoundedCornerShape(12.dp)
            )
            .clickable(interactionSource = src, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number index or live equalizer
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrent) {
                EqualizerBars(active = isPlaying, color = Color.White)
            } else {
                Text(
                    text = "%02d".format(index),
                    color = AppleSubtle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Artwork thumbnail
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF141418))
                .border(1.dp, AppleCardBorder, RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = client.getArtworkUrl(track.id, maxWidth = 150),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.width(12.dp))

        // Title and artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = track.artistName,
                color = AppleMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (track.isFavorite) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Preferito",
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 4.dp)
            )
        }
    }
}

@Composable
private fun ApplePlayBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xF2FFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Riproduci",
            tint = Color.Black,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Three bouncing bars; flat when paused. */
@Composable
private fun EqualizerBars(active: Boolean, color: Color) {
    val t = rememberInfiniteTransition(label = "eq")
    val a by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")
    val b by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(740, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    val c by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(610, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "c")
    Row(
        modifier = Modifier.height(13.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(a, b, c).forEach { h ->
            Box(
                Modifier
                    .width(2.5.dp)
                    .fillMaxHeight(if (active) h else 0.25f)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Apple-style entrance: staggered fade + rise + scale driven by a soft spring.
 */
@Composable
private fun AppleIn(
    key: String,
    index: Int,
    seen: MutableSet<String>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val already = remember { key in seen }
    val alpha = remember { Animatable(if (already) 1f else 0f) }
    val p = remember { Animatable(if (already) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (already) return@LaunchedEffect
        seen.add(key)
        delay(index * 60L)
        launch { alpha.animateTo(1f, tween(500, easing = AppleEase)) }
        launch { p.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow)) }
    }

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            val s = 0.94f + 0.06f * p.value
            scaleX = s
            scaleY = s
            translationY = (1f - p.value) * 36.dp.toPx()
        }
    ) {
        content()
    }
}

/** Springy press (scales down fast, rebounds with a small overshoot on release). */
@Composable
private fun Modifier.pressSpring(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "pressSpring"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
