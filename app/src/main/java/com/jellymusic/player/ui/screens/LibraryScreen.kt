package com.jellymusic.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.jellymusic.player.JellyMusicApp
import com.jellymusic.player.data.JellyfinItem
import com.jellymusic.player.playback.PlaybackState
import com.jellymusic.player.preferences.UserCustomizations
import kotlinx.coroutines.launch

private val AppleBlack = Color(0xFF000000)
private val AppleSurface = Color(0xFF141416)
private val AppleBorder = Color(0x14FFFFFF)
private val AppleTextMuted = Color(0xFF8E8E93)
private val AppleTextSecondary = Color(0xFFC5C6CF)

// Survives tab switching to avoid blank flicker
private var cachedPlaylists: List<JellyfinItem> = emptyList()
private var cachedAlbums: List<JellyfinItem> = emptyList()

enum class LibraryCategory(val displayName: String) {
    PLAYLISTS("Playlist"),
    ARTISTS("Artisti"),
    ALBUMS("Album"),
    TRACKS("Brani"),
    FAVORITES("Preferiti")
}

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
    onOpenSettings: () -> Unit = {}
) {
    val client = JellyMusicApp.instance.jellyfinClient
    val controller = JellyMusicApp.instance.playerController
    val scope = rememberCoroutineScope()
    val kbps = customizations.streamingQuality.kbps

    var selectedCategory by remember { mutableStateOf(LibraryCategory.ALBUMS) }
    var isGridView by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    var tracks by remember { mutableStateOf(controller.cachedTracks) }
    var albums by remember { mutableStateOf(cachedAlbums) }
    var playlists by remember { mutableStateOf(cachedPlaylists) }
    var isLoading by remember { mutableStateOf(false) }

    var currentSort by remember { mutableStateOf(TrackSortOption.DATE_ADDED_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }

    var selectedAlbum by remember { mutableStateOf<JellyfinItem?>(null) }
    var albumTracks by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isAlbumTracksLoading by remember { mutableStateOf(false) }

    var selectedPlaylist by remember { mutableStateOf<JellyfinItem?>(null) }
    var playlistTracks by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isPlaylistTracksLoading by remember { mutableStateOf(false) }

    var selectedArtist by remember { mutableStateOf<String?>(null) }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var trackToAddToPlaylist by remember { mutableStateOf<JellyfinItem?>(null) }

    // Intercept back navigation
    BackHandler(enabled = selectedAlbum != null || selectedPlaylist != null || selectedArtist != null) {
        when {
            selectedAlbum != null -> selectedAlbum = null
            selectedPlaylist != null -> selectedPlaylist = null
            selectedArtist != null -> selectedArtist = null
        }
    }

    LaunchedEffect(Unit) {
        isLoading = tracks.isEmpty()
        client.getAudioTracks().onSuccess {
            tracks = it
            controller.updateCachedTracks(it)
            isLoading = false
        }.onFailure { isLoading = false }

        client.getAlbums().onSuccess {
            albums = it
            cachedAlbums = it
        }

        client.getPlaylists().onSuccess {
            playlists = it
            cachedPlaylists = it
        }
    }

    val favoriteTracks = remember(tracks) { tracks.filter { it.isFavorite } }
    val artistsList = remember(tracks) {
        tracks.map { it.artistName }.filter { it.isNotBlank() && it != "Unknown Artist" }.distinct().sorted()
    }

    // Synthesize albums from tracks if albums API is empty
    val effectiveAlbums = remember(albums, tracks) {
        if (albums.isNotEmpty()) albums
        else {
            tracks.filter { !it.album.isNullOrBlank() }
                .groupBy { it.album ?: "" }
                .map { (albumTitle, trList) ->
                    val first = trList.first()
                    JellyfinItem(
                        id = first.albumId ?: first.id,
                        name = albumTitle,
                        type = "MusicAlbum",
                        artists = first.artists,
                        album = albumTitle,
                        container = first.container
                    )
                }
        }
    }

    // Filter & Sort
    val displayedTracks = remember(tracks, searchQuery, currentSort) {
        val filtered = if (searchQuery.isBlank()) tracks
        else tracks.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.artistName.contains(searchQuery, ignoreCase = true) ||
            (it.album?.contains(searchQuery, ignoreCase = true) == true)
        }
        sortTracks(filtered, currentSort)
    }

    val displayedAlbums = remember(effectiveAlbums, searchQuery, currentSort) {
        val filtered = if (searchQuery.isBlank()) effectiveAlbums
        else effectiveAlbums.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.artistName.contains(searchQuery, ignoreCase = true)
        }
        when (currentSort) {
            TrackSortOption.TITLE_ASC -> filtered.sortedBy { it.name.lowercase() }
            TrackSortOption.TITLE_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            TrackSortOption.ARTIST_ASC -> filtered.sortedBy { it.artistName.lowercase() }
            else -> filtered
        }
    }

    val displayedPlaylists = remember(playlists, searchQuery) {
        if (searchQuery.isBlank()) playlists
        else playlists.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val displayedArtists = remember(artistsList, searchQuery) {
        if (searchQuery.isBlank()) artistsList
        else artistsList.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    val displayedFavorites = remember(favoriteTracks, searchQuery, currentSort) {
        val filtered = if (searchQuery.isBlank()) favoriteTracks
        else favoriteTracks.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.artistName.contains(searchQuery, ignoreCase = true)
        }
        sortTracks(filtered, currentSort)
    }

    val selectedArtistTracks = remember(tracks, selectedArtist) {
        if (selectedArtist != null) tracks.filter { it.artistName == selectedArtist }
        else emptyList()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleBlack)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
        ) {
            // --- TOP APP BAR (from code.html) ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x0DFFFFFF))
                                .border(1.dp, Color(0x14FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "Server Jellyfin Connesso",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x0FFFFFFF))
                                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = "DIRECT • FLAC",
                                color = AppleTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Text(
                        text = "JellyMusic",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.4).sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSearchVisible) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                                .clickable { isSearchVisible = !isSearchVisible },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cerca",
                                tint = if (isSearchVisible) Color.White else AppleTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x0FFFFFFF))
                                .clickable { onOpenSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Profilo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // --- ANIMATED SEARCH FIELD ---
            item {
                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text("Cerca per titolo, artista, album...", color = Color(0xFF636366), fontSize = 14.sp)
                            },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = AppleTextMuted, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Cancella", tint = AppleTextMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(alpha = 0.4f),
                                unfocusedBorderColor = Color(0x18FFFFFF),
                                focusedContainerColor = Color(0xFF141416),
                                unfocusedContainerColor = Color(0xFF141416)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // If an individual Album or Playlist is opened, show its detail header
            if (selectedAlbum != null) {
                val alb = selectedAlbum!!
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        IconButton(onClick = { selectedAlbum = null }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Album", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = client.getArtworkUrl(alb.id, maxWidth = 320),
                                contentDescription = alb.name,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, AppleBorder, RoundedCornerShape(14.dp))
                                    .background(AppleSurface),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alb.name,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = alb.artistName,
                                    color = AppleTextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${albumTracks.size} brani • ${getQualityBadge(alb)}",
                                    color = AppleTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (albumTracks.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color.White)
                                    .clickable {
                                        controller.playQueue(albumTracks, 0, kbps)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Riproduci tutto", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                if (isAlbumTracksLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                        }
                    }
                } else {
                    itemsIndexed(albumTracks) { index, trk ->
                        val isPlayingNow = playbackState.currentTrack?.id == trk.id && playbackState.isPlaying
                        TrackListItem(
                            track = trk,
                            index = index + 1,
                            isPlaying = isPlayingNow,
                            onPlay = { controller.playQueue(albumTracks, index, kbps) },
                            onAddToPlaylist = { trackToAddToPlaylist = trk }
                        )
                    }
                }
            } else if (selectedPlaylist != null) {
                val pl = selectedPlaylist!!
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        IconButton(onClick = { selectedPlaylist = null }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Playlist", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AppleSurface)
                                    .border(1.dp, AppleBorder, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = AppleTextMuted, modifier = Modifier.size(44.dp))
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pl.name,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${playlistTracks.size} brani salvati su Jellyfin",
                                    color = AppleTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (playlistTracks.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color.White)
                                    .clickable {
                                        controller.playQueue(playlistTracks, 0, kbps)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Riproduci playlist", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                if (isPlaylistTracksLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                        }
                    }
                } else if (playlistTracks.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("Nessun brano in questa playlist", color = AppleTextMuted, fontSize = 14.sp)
                        }
                    }
                } else {
                    itemsIndexed(playlistTracks) { index, trk ->
                        val isPlayingNow = playbackState.currentTrack?.id == trk.id && playbackState.isPlaying
                        TrackListItem(
                            track = trk,
                            index = index + 1,
                            isPlaying = isPlayingNow,
                            onPlay = { controller.playQueue(playlistTracks, index, kbps) },
                            onAddToPlaylist = { trackToAddToPlaylist = trk }
                        )
                    }
                }
            } else if (selectedArtist != null) {
                val artName = selectedArtist!!
                val artistTracks = selectedArtistTracks
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        IconButton(onClick = { selectedArtist = null }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Artisti", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = artName,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${artistTracks.size} brani nella tua libreria",
                            color = AppleTextMuted,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (artistTracks.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color.White)
                                    .clickable {
                                        controller.playQueue(artistTracks, 0, kbps)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Riproduci tutto", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                itemsIndexed(artistTracks) { index, trk ->
                    val isPlayingNow = playbackState.currentTrack?.id == trk.id && playbackState.isPlaying
                    TrackListItem(
                        track = trk,
                        index = index + 1,
                        isPlaying = isPlayingNow,
                        onPlay = { controller.playQueue(artistTracks, index, kbps) },
                        onAddToPlaylist = { trackToAddToPlaylist = trk }
                    )
                }
            } else {
                // --- MAIN LIBRARY VIEW ---

                // 1. Page Title & Action
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Libreria",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (selectedCategory) {
                                    LibraryCategory.ALBUMS -> "${effectiveAlbums.size} Album • Sincronizzato con Jellyfin"
                                    LibraryCategory.TRACKS -> "${tracks.size} Brani • Sincronizzato con Jellyfin"
                                    LibraryCategory.PLAYLISTS -> "${playlists.size} Playlist • Sincronizzato con Jellyfin"
                                    LibraryCategory.ARTISTS -> "${artistsList.size} Artisti • Sincronizzato con Jellyfin"
                                    LibraryCategory.FAVORITES -> "${favoriteTracks.size} Preferiti • Sincronizzato con Jellyfin"
                                },
                                color = AppleTextMuted,
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x12FFFFFF))
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                                .clickable { showCreatePlaylistDialog = true }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Nuova",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 2. Navigation Categories Card (iOS Settings Pattern from code.html)
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(AppleSurface)
                            .border(1.dp, AppleBorder, RoundedCornerShape(18.dp))
                    ) {
                        Column {
                            CategoryRow(
                                icon = Icons.Default.QueueMusic,
                                title = "Playlist",
                                count = playlists.size.toString(),
                                isSelected = selectedCategory == LibraryCategory.PLAYLISTS,
                                showDivider = true,
                                onClick = { selectedCategory = LibraryCategory.PLAYLISTS }
                            )
                            CategoryRow(
                                icon = Icons.Default.Mic,
                                title = "Artisti",
                                count = artistsList.size.toString(),
                                isSelected = selectedCategory == LibraryCategory.ARTISTS,
                                showDivider = true,
                                onClick = { selectedCategory = LibraryCategory.ARTISTS }
                            )
                            CategoryRow(
                                icon = Icons.Default.Album,
                                title = "Album",
                                count = effectiveAlbums.size.toString(),
                                isSelected = selectedCategory == LibraryCategory.ALBUMS,
                                showDivider = true,
                                onClick = { selectedCategory = LibraryCategory.ALBUMS }
                            )
                            CategoryRow(
                                icon = Icons.Default.MusicNote,
                                title = "Brani",
                                count = tracks.size.toString(),
                                isSelected = selectedCategory == LibraryCategory.TRACKS,
                                showDivider = true,
                                onClick = { selectedCategory = LibraryCategory.TRACKS }
                            )
                            CategoryRow(
                                icon = Icons.Default.Favorite,
                                title = "Preferiti",
                                count = favoriteTracks.size.toString(),
                                isSelected = selectedCategory == LibraryCategory.FAVORITES,
                                showDivider = false,
                                onClick = { selectedCategory = LibraryCategory.FAVORITES }
                            )
                        }
                    }
                }

                // 3. Sub-header & Controls
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when (selectedCategory) {
                                    LibraryCategory.ALBUMS -> "Tutti gli Album"
                                    LibraryCategory.TRACKS -> "Tutti i Brani"
                                    LibraryCategory.PLAYLISTS -> "Le tue Playlist"
                                    LibraryCategory.ARTISTS -> "Tutti gli Artisti"
                                    LibraryCategory.FAVORITES -> "Brani Preferiti"
                                },
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.4).sp
                            )
                            Text(
                                text = "sincronizzati",
                                color = AppleTextMuted,
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Sort Pill Menu
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0x10FFFFFF))
                                        .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
                                        .clickable { showSortMenu = true }
                                        .padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Ordina:",
                                        color = AppleTextMuted,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = currentSort.chipLabel,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = AppleTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    modifier = Modifier.background(Color(0xFF1C1C1E))
                                ) {
                                    TrackSortOption.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option.displayName,
                                                    color = if (currentSort == option) Color.White else AppleTextMuted,
                                                    fontWeight = if (currentSort == option) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                currentSort = option
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // View Toggle Button: Grid vs List
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x10FFFFFF))
                                    .border(1.dp, Color(0x14FFFFFF), CircleShape)
                                    .clickable { isGridView = !isGridView },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Default.GridView else Icons.Default.ViewList,
                                    contentDescription = "Cambia vista",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Content Area Based on Selected Category and View Mode
                when (selectedCategory) {
                    LibraryCategory.ALBUMS -> {
                        if (displayedAlbums.isEmpty()) {
                            item {
                                EmptyPlaceholder("Nessun album trovato")
                            }
                        } else if (isGridView) {
                            itemsIndexed(displayedAlbums.chunked(2)) { _, pair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    AlbumCard(
                                        album = pair[0],
                                        isPlaying = playbackState.currentTrack?.albumId == pair[0].id && playbackState.isPlaying,
                                        qualityBadge = getQualityBadge(pair[0]),
                                        onClick = {
                                            selectedAlbum = pair[0]
                                            isAlbumTracksLoading = true
                                            scope.launch {
                                                client.getAlbumTracks(pair[0].id).onSuccess {
                                                    albumTracks = it
                                                    isAlbumTracksLoading = false
                                                }
                                            }
                                        },
                                        onQuickPlay = {
                                            scope.launch {
                                                client.getAlbumTracks(pair[0].id).onSuccess { list ->
                                                    if (list.isNotEmpty()) {
                                                        controller.playQueue(list, 0, kbps)
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (pair.size > 1) {
                                        AlbumCard(
                                            album = pair[1],
                                            isPlaying = playbackState.currentTrack?.albumId == pair[1].id && playbackState.isPlaying,
                                            qualityBadge = getQualityBadge(pair[1]),
                                            onClick = {
                                                selectedAlbum = pair[1]
                                                isAlbumTracksLoading = true
                                                scope.launch {
                                                    client.getAlbumTracks(pair[1].id).onSuccess {
                                                        albumTracks = it
                                                        isAlbumTracksLoading = false
                                                    }
                                                }
                                            },
                                            onQuickPlay = {
                                                scope.launch {
                                                    client.getAlbumTracks(pair[1].id).onSuccess { list ->
                                                        if (list.isNotEmpty()) {
                                                            controller.playQueue(list, 0, kbps)
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            // Albums in list format
                            itemsIndexed(displayedAlbums) { _, alb ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedAlbum = alb
                                            isAlbumTracksLoading = true
                                            scope.launch {
                                                client.getAlbumTracks(alb.id).onSuccess {
                                                    albumTracks = it
                                                    isAlbumTracksLoading = false
                                                }
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = client.getArtworkUrl(alb.id, maxWidth = 160),
                                        contentDescription = alb.name,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(AppleSurface)
                                            .border(1.dp, AppleBorder, RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = alb.name,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = alb.artistName,
                                            color = AppleTextMuted,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x14FFFFFF))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = getQualityBadge(alb),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LibraryCategory.TRACKS -> {
                        if (displayedTracks.isEmpty()) {
                            item {
                                EmptyPlaceholder("Nessun brano trovato")
                            }
                        } else {
                            itemsIndexed(displayedTracks) { index, trk ->
                                val isPlayingNow = playbackState.currentTrack?.id == trk.id && playbackState.isPlaying
                                TrackListItem(
                                    track = trk,
                                    index = index + 1,
                                    isPlaying = isPlayingNow,
                                    onPlay = { controller.playQueue(displayedTracks, index, kbps) },
                                    onAddToPlaylist = { trackToAddToPlaylist = trk }
                                )
                            }
                        }
                    }

                    LibraryCategory.PLAYLISTS -> {
                        if (displayedPlaylists.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Nessuna playlist su Jellyfin", color = AppleTextMuted, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.White)
                                            .clickable { showCreatePlaylistDialog = true }
                                            .padding(horizontal = 18.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Crea playlist su Jellyfin", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(displayedPlaylists) { _, pl ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPlaylist = pl
                                            playlistTracks = emptyList()
                                            isPlaylistTracksLoading = true
                                            scope.launch {
                                                client.getPlaylistTracks(pl.id).onSuccess {
                                                    playlistTracks = it
                                                    isPlaylistTracksLoading = false
                                                }
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(AppleSurface)
                                            .border(1.dp, AppleBorder, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.name,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Playlist Jellyfin",
                                            color = AppleTextMuted,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = AppleTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    LibraryCategory.ARTISTS -> {
                        if (displayedArtists.isEmpty()) {
                            item {
                                EmptyPlaceholder("Nessun artista trovato")
                            }
                        } else {
                            itemsIndexed(displayedArtists) { _, art ->
                                val artistTrackCount = tracks.count { it.artistName == art }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedArtist = art }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(AppleSurface)
                                                .border(1.dp, AppleBorder, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Mic, contentDescription = null, tint = AppleTextMuted, modifier = Modifier.size(20.dp))
                                        }

                                        Column {
                                            Text(
                                                text = art,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$artistTrackCount brani",
                                                color = AppleTextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = AppleTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    LibraryCategory.FAVORITES -> {
                        if (displayedFavorites.isEmpty()) {
                            item {
                                EmptyPlaceholder("Nessun brano preferito")
                            }
                        } else {
                            itemsIndexed(displayedFavorites) { index, trk ->
                                val isPlayingNow = playbackState.currentTrack?.id == trk.id && playbackState.isPlaying
                                TrackListItem(
                                    track = trk,
                                    index = index + 1,
                                    isPlaying = isPlayingNow,
                                    onPlay = { controller.playQueue(displayedFavorites, index, kbps) },
                                    onAddToPlaylist = { trackToAddToPlaylist = trk }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS (Playlist creation and track add) ---
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

/**
 * Grouped category row matching iOS Settings pattern in code.html.
 */
@Composable
private fun CategoryRow(
    icon: ImageVector,
    title: String,
    count: String,
    isSelected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) Color(0x14FFFFFF) else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else AppleTextMuted,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = count,
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = count,
                        color = AppleTextMuted,
                        fontSize = 13.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else AppleTextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp)
                    .height(0.5.dp)
                    .background(Color(0x10FFFFFF))
            )
        }
    }
}

/**
 * 2-Column Album Card matching code.html with squircle border, quality badge and quick play button.
 */
@Composable
private fun AlbumCard(
    album: JellyfinItem,
    isPlaying: Boolean,
    qualityBadge: String,
    onClick: () -> Unit,
    onQuickPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val client = JellyMusicApp.instance.jellyfinClient
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(AppleSurface)
                .border(1.dp, AppleBorder, RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = client.getArtworkUrl(album.id, maxWidth = 300),
                contentDescription = album.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Audio Quality Micro-Badge (from code.html)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = qualityBadge,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Quick Play Button Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onQuickPlay() },
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    AnimatedEqualizer()
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Riproduci",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = album.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = album.artistName,
            color = AppleTextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Track Row item in list view.
 */
@Composable
private fun TrackListItem(
    track: JellyfinItem,
    index: Int,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val client = JellyMusicApp.instance.jellyfinClient
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = client.getArtworkUrl(track.id, maxWidth = 140),
            contentDescription = track.name,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, AppleBorder, RoundedCornerShape(8.dp))
                .background(AppleSurface),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name,
                color = if (isPlaying) Color.White else Color(0xFFE2E2E2),
                fontSize = 15.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${track.artistName} • ${track.album ?: "Singolo"}",
                    color = AppleTextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isPlaying) {
                AnimatedEqualizer()
            } else {
                Text(
                    text = track.formattedDuration,
                    color = AppleTextMuted,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { onAddToPlaylist() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = "Aggiungi a playlist",
                    tint = AppleTextMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = AppleTextMuted, fontSize = 14.sp)
    }
}

private fun getQualityBadge(item: JellyfinItem): String {
    val container = item.container?.lowercase() ?: item.mediaSources?.firstOrNull()?.container?.lowercase()
    val bitrate = item.mediaSources?.firstOrNull()?.bitrate ?: 0
    return when {
        container == "flac" -> "FLAC 96kHz"
        container == "alac" -> "Hi-Res Lossless"
        bitrate >= 500_000 -> "Direct Stream"
        bitrate >= 320_000 -> "Hi-Res 320k"
        else -> "Lossless"
    }
}

private fun sortTracks(list: List<JellyfinItem>, sort: TrackSortOption): List<JellyfinItem> {
    return when (sort) {
        TrackSortOption.DATE_ADDED_DESC -> {
            if (list.any { !it.dateCreated.isNullOrEmpty() }) list.sortedByDescending { it.dateCreated }
            else list
        }
        TrackSortOption.DATE_ADDED_ASC -> {
            if (list.any { !it.dateCreated.isNullOrEmpty() }) list.sortedBy { it.dateCreated }
            else list
        }
        TrackSortOption.TITLE_ASC -> list.sortedBy { it.name.lowercase() }
        TrackSortOption.TITLE_DESC -> list.sortedByDescending { it.name.lowercase() }
        TrackSortOption.ARTIST_ASC -> list.sortedBy { it.artistName.lowercase() }
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
        modifier = Modifier.size(width = 16.dp, height = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(bar1Height.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(bar2Height.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(bar3Height.dp)
                .clip(RoundedCornerShape(1.dp))
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2E))
                        .clickable { onCreateNewRequested() }
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
