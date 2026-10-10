package com.jellymusic.player.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.jellymusic.player.JellyMusicApp
import com.jellymusic.player.data.JellyfinItem
import com.jellymusic.player.playback.PlaybackState
import com.jellymusic.player.preferences.UserCustomizations
import com.jellymusic.player.ui.theme.ArtworkBackground
import com.jellymusic.player.ui.theme.ArtworkBorder
import com.jellymusic.player.ui.theme.ArtworkButtonActive
import com.jellymusic.player.ui.theme.ArtworkControlFill
import com.jellymusic.player.ui.theme.ArtworkSurface
import com.jellymusic.player.ui.theme.ArtworkTextMuted
import com.jellymusic.player.ui.theme.ArtworkTextPrimary
import com.jellymusic.player.ui.theme.ArtworkTextSecondary

enum class LibraryTab(val displayName: String) {
    TRACKS("Brani"),
    PLAYLISTS("Playlist")
}

// Survives leaving the Library tab (the composable is disposed when another tab is shown)
private var cachedPlaylists: List<JellyfinItem> = emptyList()

enum class TrackSortOption(val displayName: String, val chipLabel: String) {
    DATE_ADDED_DESC("Data di aggiunta (Più recenti)", "Più recenti"),
    DATE_ADDED_ASC("Data di aggiunta (Meno recenti)", "Meno recenti"),
    TITLE_ASC("Titolo (A - Z)", "Titolo A-Z"),
    TITLE_DESC("Titolo (Z - A)", "Titolo Z-A"),
    ARTIST_ASC("Artista (A - Z)", "Artista")
}

@Composable
fun LibraryScreen(
    customizations: UserCustomizations,
    playbackState: PlaybackState,
    modifier: Modifier = Modifier,
    activeTab: Int = 1
) {
    val client = JellyMusicApp.instance.jellyfinClient
    val controller = JellyMusicApp.instance.playerController

    var selectedTab by remember { mutableStateOf(LibraryTab.TRACKS) }
    var searchQuery by remember { mutableStateOf("") }
    // Seeded from the shared cache: coming back to the tab shows the list instantly, refresh happens silently
    var tracks by remember { mutableStateOf(controller.cachedTracks) }
    var isLoading by remember { mutableStateOf(false) }
    var currentSort by remember { mutableStateOf(TrackSortOption.DATE_ADDED_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    var playlists by remember { mutableStateOf(cachedPlaylists) }
    var isPlaylistsLoading by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<JellyfinItem?>(null) }
    var playlistTracks by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isPlaylistTracksLoading by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var trackToAddToPlaylist by remember { mutableStateOf<JellyfinItem?>(null) }

    val scope = rememberCoroutineScope()

    BackHandler(enabled = selectedTab == LibraryTab.PLAYLISTS && selectedPlaylist != null) {
        selectedPlaylist = null
    }

    val infiniteSpinTransition = rememberInfiniteTransition(label = "refreshSpin")
    val spinAngle by infiniteSpinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    LaunchedEffect(Unit) {
        isLoading = tracks.isEmpty()
        client.getAudioTracks().onSuccess {
            tracks = it
            controller.updateCachedTracks(it)
            isLoading = false
        }.onFailure {
            isLoading = false
        }
        if (playlists.isEmpty()) {
            client.getPlaylists().onSuccess {
                playlists = it
                cachedPlaylists = it
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == LibraryTab.PLAYLISTS) {
            isPlaylistsLoading = playlists.isEmpty()
            client.getPlaylists().onSuccess {
                playlists = it
                cachedPlaylists = it
                isPlaylistsLoading = false
            }.onFailure {
                isPlaylistsLoading = false
            }
        }
    }

    val displayedTracks = remember(tracks, searchQuery, currentSort) {
        val filtered = if (searchQuery.isBlank()) tracks
        else tracks.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.artistName.contains(searchQuery, ignoreCase = true) ||
            (it.album?.contains(searchQuery, ignoreCase = true) == true)
        }

        when (currentSort) {
            TrackSortOption.DATE_ADDED_DESC -> {
                if (filtered.any { !it.dateCreated.isNullOrEmpty() }) {
                    filtered.sortedByDescending { it.dateCreated }
                } else {
                    filtered
                }
            }
            TrackSortOption.DATE_ADDED_ASC -> {
                if (filtered.any { !it.dateCreated.isNullOrEmpty() }) {
                    filtered.sortedBy { it.dateCreated }
                } else {
                    filtered.reversed()
                }
            }
            TrackSortOption.TITLE_ASC -> filtered.sortedBy { it.name.lowercase() }
            TrackSortOption.TITLE_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            TrackSortOption.ARTIST_ASC -> filtered.sortedBy { it.artistName.lowercase() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D11))
    ) {
        // Header con Titolo e Tasto Reload / Sincronizzazione
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Libreria",
                color = ArtworkTextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.02).sp
            )

            // Pulsante Reload / Sincronizza
            IconButton(
                onClick = {
                    if (!isRefreshing && !isLoading && !isPlaylistsLoading && !isPlaylistTracksLoading) {
                        isRefreshing = true
                        scope.launch {
                            try { client.refreshLibraryScan() } catch (_: Exception) {}
                            delay(1000)
                            if (selectedTab == LibraryTab.TRACKS) {
                                client.getAudioTracks().onSuccess {
                                    tracks = it
                                    controller.updateCachedTracks(it)
                                    isRefreshing = false
                                }.onFailure {
                                    isRefreshing = false
                                }
                            } else {
                                val currentPl = selectedPlaylist
                                if (currentPl != null) {
                                    client.getPlaylistTracks(currentPl.id).onSuccess {
                                        playlistTracks = it
                                        isRefreshing = false
                                    }.onFailure {
                                        isRefreshing = false
                                    }
                                } else {
                                    client.getPlaylists().onSuccess {
                                        playlists = it
                                        isRefreshing = false
                                    }.onFailure {
                                        isRefreshing = false
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x14FFFFFF))
                    .border(1.dp, Color(0x1FFFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sincronizza con Jellyfin",
                    tint = if (isRefreshing) ArtworkControlFill else ArtworkTextPrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            if (isRefreshing) {
                                rotationZ = spinAngle
                            }
                        }
                )
            }
        }

        // Tab Switcher: Brani / Playlist
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isTracks = selectedTab == LibraryTab.TRACKS
            val isPlaylists = selectedTab == LibraryTab.PLAYLISTS

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isTracks) androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF8A3FFC), Color(0xFFD926AA)))
                        else androidx.compose.ui.graphics.SolidColor(Color(0x14FFFFFF))
                    )
                    .border(
                        1.dp,
                        if (isTracks) Color(0x66FFFFFF) else Color(0x14FFFFFF),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        selectedTab = LibraryTab.TRACKS
                        selectedPlaylist = null
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Brani",
                    color = if (isTracks) Color.White else Color(0xFF9E9EB8),
                    fontSize = 14.sp,
                    fontWeight = if (isTracks) FontWeight.SemiBold else FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPlaylists) androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF8A3FFC), Color(0xFFD926AA)))
                        else androidx.compose.ui.graphics.SolidColor(Color(0x14FFFFFF))
                    )
                    .border(
                        1.dp,
                        if (isPlaylists) Color(0x66FFFFFF) else Color(0x14FFFFFF),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        selectedTab = LibraryTab.PLAYLISTS
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Playlist",
                    color = if (isPlaylists) Color.White else Color(0xFF9E9EB8),
                    fontSize = 14.sp,
                    fontWeight = if (isPlaylists) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (selectedTab == LibraryTab.TRACKS) {
            // Compact Search Bar in alto
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Cerca nei tuoi brani...",
                        color = ArtworkTextMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ArtworkTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Cancella",
                                tint = ArtworkTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ArtworkSurface,
                    unfocusedContainerColor = ArtworkSurface,
                    focusedBorderColor = Color(0x33FFFFFF),
                    unfocusedBorderColor = ArtworkBorder,
                    focusedTextColor = ArtworkTextPrimary,
                    unfocusedTextColor = ArtworkTextPrimary
                ),
                singleLine = true
            )

            // Track count & Sort Selector Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "RISULTATI (${displayedTracks.size})" else "TUTTI I BRANI (${displayedTracks.size})",
                    color = ArtworkTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )

                // Pulsante Selezione Ordinamento
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1AFFFFFF))
                            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(8.dp))
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Ordina brani",
                            tint = ArtworkTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = currentSort.chipLabel,
                            color = ArtworkTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = ArtworkTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        shape = RoundedCornerShape(12.dp),
                        containerColor = Color(0xFF1E1D26),
                        border = BorderStroke(1.dp, ArtworkBorder)
                    ) {
                        TrackSortOption.entries.forEach { option ->
                            val isSelected = currentSort == option
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        color = if (isSelected) ArtworkTextPrimary else ArtworkTextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = ArtworkTextPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = MenuDefaults.itemColors(
                                    textColor = ArtworkTextPrimary
                                ),
                                onClick = {
                                    currentSort = option
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ArtworkControlFill, strokeWidth = 2.dp)
                }
            } else if (displayedTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "Nessun brano trovato per \"$searchQuery\"" else "Nessun brano disponibile",
                            color = ArtworkTextMuted,
                            fontSize = 14.sp
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x1AFFFFFF))
                                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (!isRefreshing && !isLoading) {
                                            isRefreshing = true
                                            scope.launch {
                                                try { client.refreshLibraryScan() } catch (_: Exception) {}
                                                client.getAudioTracks().onSuccess {
                                                    tracks = it
                                                    isRefreshing = false
                                                }.onFailure {
                                                    isRefreshing = false
                                                }
                                            }
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = ArtworkTextPrimary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer {
                                            if (isRefreshing) rotationZ = spinAngle
                                        }
                                )
                                Text(
                                    text = if (isRefreshing) "Sincronizzazione in corso..." else "Sincronizza con Jellyfin",
                                    color = ArtworkTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    itemsIndexed(displayedTracks) { index, item ->
                        val isCurrentTrack = playbackState.currentTrack?.id == item.id
                        val isPlayingNow = isCurrentTrack && playbackState.isPlaying

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    controller.playQueue(
                                        items = displayedTracks,
                                        startIndex = index,
                                        bitrateKbps = customizations.streamingQuality.kbps
                                    )
                                }
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = client.getArtworkUrl(item.id, maxWidth = 160),
                                contentDescription = "Cover",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, ArtworkBorder, RoundedCornerShape(8.dp))
                                    .background(ArtworkSurface),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = ArtworkTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = if (isCurrentTrack) FontWeight.SemiBold else FontWeight.Medium,
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isPlayingNow) {
                                    AnimatedEqualizer()
                                } else {
                                    Text(
                                        text = item.formattedDuration,
                                        color = ArtworkTextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { trackToAddToPlaylist = item },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistAdd,
                                        contentDescription = "Aggiungi a playlist",
                                        tint = ArtworkTextMuted.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // PLAYLISTS TAB
            if (selectedPlaylist == null) {
                // Playlists List
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LE TUE PLAYLIST (${playlists.size})",
                        color = ArtworkTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { showCreatePlaylistDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nuova Playlist",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Nuova",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (isPlaylistsLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ArtworkControlFill, strokeWidth = 2.dp)
                    }
                } else if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Nessuna playlist trovata su Jellyfin",
                                color = ArtworkTextMuted,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White)
                                    .clickable { showCreatePlaylistDialog = true }
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Crea playlist su Jellyfin",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        itemsIndexed(playlists) { _, playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                                    .clickable {
                                        selectedPlaylist = playlist
                                        playlistTracks = emptyList()
                                        isPlaylistTracksLoading = true
                                        scope.launch {
                                            client.getPlaylistTracks(playlist.id).onSuccess {
                                                playlistTracks = it
                                                isPlaylistTracksLoading = false
                                            }.onFailure {
                                                playlistTracks = emptyList()
                                                isPlaylistTracksLoading = false
                                            }
                                        }
                                    }
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = client.getArtworkUrl(playlist.id, maxWidth = 160),
                                    contentDescription = "Cover Playlist",
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, ArtworkBorder, RoundedCornerShape(8.dp))
                                        .background(ArtworkSurface),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        color = ArtworkTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Playlist Jellyfin",
                                        color = ArtworkTextSecondary,
                                        fontSize = 13.sp
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = ArtworkTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Inside Selected Playlist View
                val currentPl = selectedPlaylist!!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedPlaylist = null }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Indietro",
                            tint = ArtworkTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    ) {
                        Text(
                            text = currentPl.name,
                            color = ArtworkTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${playlistTracks.size} brani",
                            color = ArtworkTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    if (playlistTracks.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                controller.playQueue(
                                    items = playlistTracks,
                                    startIndex = 0,
                                    bitrateKbps = customizations.streamingQuality.kbps
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Riproduci tutto",
                                tint = ArtworkTextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                if (isPlaylistTracksLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ArtworkControlFill, strokeWidth = 2.dp)
                    }
                } else if (playlistTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Questa playlist non contiene brani",
                            color = ArtworkTextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        itemsIndexed(playlistTracks) { index, item ->
                            val isCurrentTrack = playbackState.currentTrack?.id == item.id
                            val isPlayingNow = isCurrentTrack && playbackState.isPlaying

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .clickable {
                                        controller.playQueue(
                                            items = playlistTracks,
                                            startIndex = index,
                                            bitrateKbps = customizations.streamingQuality.kbps
                                        )
                                    }
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = client.getArtworkUrl(item.id, maxWidth = 160),
                                    contentDescription = "Cover",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, ArtworkBorder, RoundedCornerShape(8.dp))
                                        .background(ArtworkSurface),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        color = ArtworkTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = if (isCurrentTrack) FontWeight.SemiBold else FontWeight.Medium,
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

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isPlayingNow) {
                                        AnimatedEqualizer()
                                    } else {
                                        Text(
                                            text = item.formattedDuration,
                                            color = ArtworkTextMuted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { trackToAddToPlaylist = item },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistAdd,
                                            contentDescription = "Aggiungi a playlist",
                                            tint = ArtworkTextMuted.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { newName ->
                scope.launch {
                    val initialTracks = trackToAddToPlaylist?.let { listOf(it.id) } ?: emptyList()
                    val result = client.createPlaylist(newName, initialTracks)
                    if (result.isSuccess) {
                        client.getPlaylists().onSuccess {
                            playlists = it
                            cachedPlaylists = it
                        }
                    }
                    showCreatePlaylistDialog = false
                    trackToAddToPlaylist = null
                }
            }
        )
    }

    trackToAddToPlaylist?.let { targetTrack ->
        AddToPlaylistDialog(
            track = targetTrack,
            playlists = playlists,
            onCreateNewRequested = {
                showCreatePlaylistDialog = true
            },
            onPlaylistSelected = { pl ->
                scope.launch {
                    client.addItemToPlaylist(pl.id, listOf(targetTrack.id))
                    if (selectedPlaylist?.id == pl.id) {
                        client.getPlaylistTracks(pl.id).onSuccess {
                            playlistTracks = it
                        }
                    }
                    trackToAddToPlaylist = null
                }
            },
            onDismiss = { trackToAddToPlaylist = null }
        )
    }
}

@Composable
fun AnimatedEqualizer() {
    val transition = rememberInfiniteTransition(label = "eq")

    val bar1Height by transition.animateFloat(
        initialValue = 4f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1"
    )

    val bar2Height by transition.animateFloat(
        initialValue = 14f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(380),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2"
    )

    val bar3Height by transition.animateFloat(
        initialValue = 8f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3"
    )

    Row(
        modifier = Modifier
            .size(width = 18.dp, height = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar1Height.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar2Height.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar3Height.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White)
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1C1C1E))
                .border(BorderStroke(1.dp, Color(0xFF2C2C2E)), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nuova Playlist",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Verrà sincronizzata sul server Jellyfin",
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = {
                        Text("Titolo della playlist", color = Color(0xFF636366), fontSize = 14.sp)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color(0xFF3A3A3C),
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF2C2C2E))
                            .clickable(enabled = !isSubmitting) { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Annulla",
                            color = Color(0xFF8E8E93),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    val canCreate = name.trim().isNotEmpty() && !isSubmitting
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (canCreate) Color.White else Color.White.copy(alpha = 0.2f))
                            .clickable(enabled = canCreate) {
                                isSubmitting = true
                                onCreate(name.trim())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = "Crea",
                                color = if (canCreate) Color.Black else Color.Black.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddToPlaylistDialog(
    track: JellyfinItem,
    playlists: List<JellyfinItem>,
    onCreateNewRequested: () -> Unit,
    onPlaylistSelected: (JellyfinItem) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1C1C1E))
                .border(BorderStroke(1.dp, Color(0xFF2C2C2E)), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Aggiungi a playlist",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.name,
                    color = Color(0xFF8E8E93),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Option to create new playlist
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2E))
                        .clickable {
                            onCreateNewRequested()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Nuova playlist...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (playlists.isEmpty()) {
                    Text(
                        text = "Nessuna playlist esistente sul server.",
                        color = Color(0xFF636366),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((playlists.size.coerceAtMost(5) * 52).dp)
                    ) {
                        itemsIndexed(playlists) { _, pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onPlaylistSelected(pl) }
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pl.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFF636366),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF2C2C2E))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chiudi",
                        color = Color(0xFF8E8E93),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
