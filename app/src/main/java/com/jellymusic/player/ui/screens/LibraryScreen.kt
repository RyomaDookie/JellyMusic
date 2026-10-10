package com.jellymusic.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
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

enum class LibraryCategory(val displayName: String) {
    PLAYLISTS("Playlist"),
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
    val gridProgressState = animateFloatAsState(
        targetValue = if (isGridView) 1f else 0f,
        animationSpec = tween(
            durationMillis = 550,
            easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
        ),
        label = "gridMorph"
    )
    val gridProgressProvider: () -> Float = { gridProgressState.value }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val availableWidth = (screenWidth - 40.dp).coerceAtLeast(280.dp)
    val colGap = 14.dp
    val colWidth = (availableWidth - colGap) / 2

    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    var tracks by remember { mutableStateOf(controller.cachedTracks) }
    var playlists by remember { mutableStateOf(cachedPlaylists) }
    var favoritesList by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }

    var currentSort by remember { mutableStateOf(TrackSortOption.DATE_ADDED_DESC) }
    var showSortMenu by remember { mutableStateOf(false) }

    var selectedAlbum by remember { mutableStateOf<JellyfinItem?>(null) }
    var selectedPlaylist by remember { mutableStateOf<JellyfinItem?>(null) }
    var playlistTracks by remember { mutableStateOf<List<JellyfinItem>>(emptyList()) }
    var isPlaylistTracksLoading by remember { mutableStateOf(false) }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var trackToAddToPlaylist by remember { mutableStateOf<JellyfinItem?>(null) }

    // Intercept back navigation
    BackHandler(enabled = selectedAlbum != null || selectedPlaylist != null) {
        when {
            selectedAlbum != null -> selectedAlbum = null
            selectedPlaylist != null -> selectedPlaylist = null
        }
    }

    LaunchedEffect(Unit) {
        client.getAudioTracks().onSuccess {
            tracks = it
            controller.updateCachedTracks(it)
        }

        client.getPlaylists().onSuccess {
            playlists = it
            cachedPlaylists = it
        }

        client.getFavorites().onSuccess {
            favoritesList = it
        }
    }

    // Refresh favorites when state changes
    val realFavorites = remember(tracks, favoritesList) {
        val serverFavIds = favoritesList.map { it.id }.toSet()
        tracks.filter { it.isFavorite || serverFavIds.contains(it.id) }
    }

    // Synthesize albums from tracks (group by album title)
    val albumsList = remember(tracks) {
        tracks.filter { !it.album.isNullOrBlank() }
            .groupBy { it.album!!.trim() }
            .map { (albumTitle, trList) ->
                val first = trList.first()
                JellyfinItem(
                    id = first.id, // ID of first track gives accurate album artwork
                    name = albumTitle,
                    type = "MusicAlbum",
                    artists = first.artists,
                    album = albumTitle,
                    container = first.container,
                    dateCreated = first.dateCreated
                )
            }
    }

    // Function to get all tracks for an album
    fun getTracksForAlbum(alb: JellyfinItem): List<JellyfinItem> {
        val albTitle = alb.album ?: alb.name
        return tracks.filter { trk ->
            trk.album?.equals(albTitle, ignoreCase = true) == true
        }.sortedBy { it.indexNumber ?: 0 }
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

    val displayedAlbums = remember(albumsList, searchQuery, currentSort) {
        val filtered = if (searchQuery.isBlank()) albumsList
        else albumsList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.artistName.contains(searchQuery, ignoreCase = true)
        }
        sortAlbums(filtered, currentSort)
    }

    val displayedPlaylists = remember(playlists, searchQuery, currentSort) {
        val filtered = if (searchQuery.isBlank()) playlists
        else playlists.filter { it.name.contains(searchQuery, ignoreCase = true) }
        sortPlaylists(filtered, currentSort)
    }

    val displayedFavorites = remember(realFavorites, searchQuery, currentSort) {
        val filtered = if (searchQuery.isBlank()) realFavorites
        else realFavorites.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.artistName.contains(searchQuery, ignoreCase = true)
        }
        sortTracks(filtered, currentSort)
    }

    val albumTrackCounts = remember(tracks) {
        tracks.filter { !it.album.isNullOrBlank() }
            .groupingBy { (it.album ?: "").trim().lowercase() }
            .eachCount()
    }

    val albumPairs = remember(displayedAlbums) { displayedAlbums.chunked(2) }
    val trackPairs = remember(displayedTracks) { displayedTracks.chunked(2) }
    val playlistPairs = remember(displayedPlaylists) { displayedPlaylists.chunked(2) }
    val favoritePairs = remember(displayedFavorites) { displayedFavorites.chunked(2) }

    fun toggleFavorite(targetTrack: JellyfinItem) {
        val newFav = !targetTrack.isFavorite
        scope.launch {
            client.setFavorite(targetTrack.id, newFav)
            controller.updateTrackFavorite(targetTrack.id, newFav)
            tracks = controller.cachedTracks
            client.getFavorites().onSuccess {
                favoritesList = it
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleBlack)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // --- TOP HEADER (CLEAN APPLE STYLE, NO SERVER BADGE OR JELLYMUSIC TEXT) ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                                LibraryCategory.ALBUMS -> "${albumsList.size} Album • Sincronizzato con Jellyfin"
                                LibraryCategory.TRACKS -> "${tracks.size} Brani • Sincronizzato con Jellyfin"
                                LibraryCategory.PLAYLISTS -> "${playlists.size} Playlist • Sincronizzato con Jellyfin"
                                LibraryCategory.FAVORITES -> "${realFavorites.size} Preferiti • Sincronizzato con Jellyfin"
                            },
                            color = AppleTextMuted,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSearchVisible) Color.White.copy(alpha = 0.2f) else Color(0x14FFFFFF))
                                .clickable { isSearchVisible = !isSearchVisible },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cerca",
                                tint = if (isSearchVisible) Color.White else AppleTextMuted,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x14FFFFFF))
                                .clickable { onOpenSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Profilo",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
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
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text("Cerca nella libreria...", color = Color(0xFF636366), fontSize = 14.sp)
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

            // --- DETAIL SCREENS: ALBUM OR PLAYLIST OPENED ---
            if (selectedAlbum != null) {
                val alb = selectedAlbum!!
                val albTracks = getTracksForAlbum(alb)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
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
                                    text = "${albTracks.size} brani",
                                    color = AppleTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (albTracks.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color.White)
                                    .clickable {
                                        controller.playQueue(albTracks, 0, kbps)
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

                if (albTracks.isEmpty()) {
                    item {
                        EmptyPlaceholder("Nessun brano trovato per questo album")
                    }
                } else {
                    itemsIndexed(albTracks) { index, trk ->
                        val isPlayingNow = playbackState.currentTrack?.id == trk.id && playbackState.isPlaying
                        TrackListItem(
                            track = trk,
                            index = index + 1,
                            isPlaying = isPlayingNow,
                            onPlay = { controller.playQueue(albTracks, index, kbps) },
                            onToggleFavorite = { toggleFavorite(trk) },
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
                            .padding(horizontal = 20.dp, vertical = 12.dp)
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
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AppleSurface)
                                    .border(1.dp, AppleBorder, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = client.getArtworkUrl(pl.id, maxWidth = 300),
                                    contentDescription = pl.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
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
                        EmptyPlaceholder("Nessun brano in questa playlist")
                    }
                } else {
                    itemsIndexed(playlistTracks) { index, trk ->
                        val isPlayingNow = playbackState.currentTrack?.id == trk.id && playbackState.isPlaying
                        TrackListItem(
                            track = trk,
                            index = index + 1,
                            isPlaying = isPlayingNow,
                            onPlay = { controller.playQueue(playlistTracks, index, kbps) },
                            onToggleFavorite = { toggleFavorite(trk) },
                            onAddToPlaylist = { trackToAddToPlaylist = trk }
                        )
                    }
                }
            } else {
                // --- CATEGORIES LIST CARD (IOS SETTINGS PATTERN - NO ARTISTS TAB) ---
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
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
                                icon = Icons.Default.Album,
                                title = "Album",
                                count = albumsList.size.toString(),
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
                                count = realFavorites.size.toString(),
                                isSelected = selectedCategory == LibraryCategory.FAVORITES,
                                showDivider = false,
                                onClick = { selectedCategory = LibraryCategory.FAVORITES }
                            )
                        }
                    }
                }

                // --- SUB-HEADER: TITLE, SORT, AND GRID/LIST TOGGLE CONTROLS ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (selectedCategory) {
                                LibraryCategory.ALBUMS -> "Tutti gli Album"
                                LibraryCategory.TRACKS -> "Tutti i Brani"
                                LibraryCategory.PLAYLISTS -> "Le tue Playlist"
                                LibraryCategory.FAVORITES -> "Brani Preferiti"
                            },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.4).sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedCategory == LibraryCategory.PLAYLISTS) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0x14FFFFFF))
                                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                                        .clickable { showCreatePlaylistDialog = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Nuova",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

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

                // --- CONTENT AREA (SMOOTH MORPHING GRID & LIST VIEWS FOR ALL CATEGORIES) ---
                when (selectedCategory) {
                    LibraryCategory.ALBUMS -> {
                        if (displayedAlbums.isEmpty()) {
                            item { EmptyPlaceholder("Nessun album trovato") }
                        } else {
                            itemsIndexed(
                                items = albumPairs,
                                key = { _, pair -> pair[0].id + "_" + (pair.getOrNull(1)?.id ?: "single") }
                            ) { _, pair ->
                                val alb0 = pair[0]
                                val alb1 = pair.getOrNull(1)
                                AdaptiveAlbumPairRow(
                                    pair = pair,
                                    gridProgressProvider = gridProgressProvider,
                                    colWidth = colWidth,
                                    availableWidth = availableWidth,
                                    playbackState = playbackState,
                                    trackCountA = albumTrackCounts[(alb0.album ?: alb0.name).trim().lowercase()] ?: 0,
                                    trackCountB = alb1?.let { albumTrackCounts[(it.album ?: it.name).trim().lowercase()] ?: 0 } ?: 0,
                                    onSelectAlbum = { selectedAlbum = it },
                                    onPlayAlbum = { alb ->
                                        val trks = getTracksForAlbum(alb)
                                        if (trks.isNotEmpty()) controller.playQueue(trks, 0, kbps)
                                    }
                                )
                            }
                        }
                    }

                    LibraryCategory.TRACKS -> {
                        if (displayedTracks.isEmpty()) {
                            item { EmptyPlaceholder("Nessun brano trovato") }
                        } else {
                            itemsIndexed(
                                items = trackPairs,
                                key = { _, pair -> pair[0].id + "_" + (pair.getOrNull(1)?.id ?: "single") }
                            ) { _, pair ->
                                AdaptiveTrackPairRow(
                                    pair = pair,
                                    gridProgressProvider = gridProgressProvider,
                                    colWidth = colWidth,
                                    availableWidth = availableWidth,
                                    playbackState = playbackState,
                                    onPlay = { target ->
                                        val idx = displayedTracks.indexOf(target)
                                        controller.playQueue(displayedTracks, idx.coerceAtLeast(0), kbps)
                                    },
                                    onToggleFavorite = { toggleFavorite(it) },
                                    onAddToPlaylist = { trackToAddToPlaylist = it }
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
                            itemsIndexed(
                                items = playlistPairs,
                                key = { _, pair -> pair[0].id + "_" + (pair.getOrNull(1)?.id ?: "single") }
                            ) { _, pair ->
                                AdaptivePlaylistPairRow(
                                    pair = pair,
                                    gridProgressProvider = gridProgressProvider,
                                    colWidth = colWidth,
                                    availableWidth = availableWidth,
                                    onOpenPlaylist = { pl ->
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
                                )
                            }
                        }
                    }

                    LibraryCategory.FAVORITES -> {
                        if (displayedFavorites.isEmpty()) {
                            item { EmptyPlaceholder("Nessun brano preferito su Jellyfin") }
                        } else {
                            itemsIndexed(
                                items = favoritePairs,
                                key = { _, pair -> pair[0].id + "_" + (pair.getOrNull(1)?.id ?: "single") }
                            ) { _, pair ->
                                AdaptiveTrackPairRow(
                                    pair = pair,
                                    gridProgressProvider = gridProgressProvider,
                                    colWidth = colWidth,
                                    availableWidth = availableWidth,
                                    playbackState = playbackState,
                                    onPlay = { target ->
                                        val idx = displayedFavorites.indexOf(target)
                                        controller.playQueue(displayedFavorites, idx.coerceAtLeast(0), kbps)
                                    },
                                    onToggleFavorite = { toggleFavorite(it) },
                                    onAddToPlaylist = { trackToAddToPlaylist = it }
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
 * Grouped category row matching iOS Settings pattern.
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

private fun lerpDp(start: Dp, stop: Dp, fraction: Float): Dp =
    (start.value + (stop.value - start.value) * fraction).dp

private fun lerpSp(start: TextUnit, stop: TextUnit, fraction: Float): TextUnit =
    (start.value + (stop.value - start.value) * fraction).sp

@Composable
private fun AdaptivePairContainer(
    hasB: Boolean,
    gridProgressProvider: () -> Float,
    colWidth: Dp,
    availableWidth: Dp,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val listRowHeightPx = remember(density) { with(density) { 66.dp.roundToPx() } }
    val gridRowHeightPx = remember(density, colWidth) { with(density) { (colWidth + 52.dp).roundToPx() } }
    val listTotalHeightPx = remember(listRowHeightPx, hasB) { if (hasB) listRowHeightPx * 2 else listRowHeightPx }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .layout { measurable, constraints ->
                val p = gridProgressProvider()
                val targetH = (listTotalHeightPx + (gridRowHeightPx - listTotalHeightPx) * p).toInt()
                val placeable = measurable.measure(
                    constraints.copy(
                        minHeight = targetH,
                        maxHeight = targetH
                    )
                )
                layout(placeable.width, targetH) {
                    placeable.place(0, 0)
                }
            }
    ) {
        content()
    }
}

@Composable
private fun AdaptiveLibraryItem(
    modifier: Modifier = Modifier,
    artworkUrl: String?,
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    gridProgressProvider: () -> Float,
    colWidth: Dp,
    availableWidth: Dp,
    trailingWidth: Dp = 0.dp,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val density = LocalDensity.current
    val colWidthPx = remember(density, colWidth) { with(density) { colWidth.toPx() } }
    val listArtSizePx = remember(density) { with(density) { 50.dp.toPx() } }
    val listArtYPx = remember(density) { with(density) { 8.dp.toPx() } }
    val textXOffsetPx = remember(density) { with(density) { (50.dp + 14.dp).toPx() } }
    val textYListPx = remember(density) { with(density) { 14.dp.toPx() } }
    val textYGridPx = remember(density, colWidth) { with(density) { (colWidth + 8.dp).toPx() } }
    val maxTextWidthPx = remember(density, availableWidth, trailingWidth) {
        with(density) { (availableWidth - 64.dp - trailingWidth).toPx().coerceAtLeast(60.dp.toPx()) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // Artwork Box (sized once at colWidth; scaled, translated and clipped smoothly on GPU)
        Box(
            modifier = Modifier
                .size(colWidth)
                .graphicsLayer {
                    val p = gridProgressProvider()
                    val targetSize = listArtSizePx + (colWidthPx - listArtSizePx) * p
                    val scale = targetSize / colWidthPx
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationY = listArtYPx * (1f - p)
                    val radiusPx = (10.dp.toPx() + 4.dp.toPx() * p)
                    shape = RoundedCornerShape(radiusPx)
                    clip = true
                }
                .background(AppleSurface)
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Title & Artist Column (translated smoothly from beside artwork to under artwork)
        Column(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val p = gridProgressProvider()
                    val targetW = (maxTextWidthPx + (colWidthPx - maxTextWidthPx) * p).toInt()
                    val placeable = measurable.measure(constraints.copy(minWidth = targetW, maxWidth = targetW))
                    layout(targetW, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                .graphicsLayer {
                    val p = gridProgressProvider()
                    translationX = textXOffsetPx * (1f - p)
                    translationY = textYListPx * (1f - p) + textYGridPx * p
                }
        ) {
            Text(
                text = title,
                color = if (isPlaying) Color.White else Color(0xFFF0F0F0),
                fontSize = 14.5.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = AppleTextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Trailing actions (heart, duration, playlist, play button) fading out on GPU
        if (trailingContent != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer {
                        val p = gridProgressProvider()
                        alpha = (1f - p * 2.2f).coerceIn(0f, 1f)
                        translationX = with(density) { 16.dp.toPx() * p }
                    }
            ) {
                trailingContent()
            }
        }
    }
}

@Composable
private fun AdaptiveAlbumPairRow(
    pair: List<JellyfinItem>,
    gridProgressProvider: () -> Float,
    colWidth: Dp,
    availableWidth: Dp,
    playbackState: PlaybackState,
    trackCountA: Int,
    trackCountB: Int,
    onSelectAlbum: (JellyfinItem) -> Unit,
    onPlayAlbum: (JellyfinItem) -> Unit
) {
    val client = JellyMusicApp.instance.jellyfinClient
    val alb0 = pair[0]
    val isPlaying0 = playbackState.currentTrack?.let { trk ->
        trk.album?.equals(alb0.name, ignoreCase = true) == true
    } == true && playbackState.isPlaying

    val alb1 = pair.getOrNull(1)
    val isPlaying1 = alb1?.let { a1 ->
        playbackState.currentTrack?.let { trk ->
            trk.album?.equals(a1.name, ignoreCase = true) == true
        } == true && playbackState.isPlaying
    } ?: false

    val density = LocalDensity.current
    val listRowHeightPx = remember(density) { with(density) { 66.dp.toPx() } }
    val colWidthPx = remember(density, colWidth) { with(density) { colWidth.toPx() } }
    val colGapPx = remember(density) { with(density) { 14.dp.toPx() } }

    AdaptivePairContainer(
        hasB = alb1 != null,
        gridProgressProvider = gridProgressProvider,
        colWidth = colWidth,
        availableWidth = availableWidth
    ) {
        // Item A
        AdaptiveLibraryItem(
            modifier = Modifier,
            artworkUrl = client.getArtworkUrl(alb0.id, maxWidth = 320),
            title = alb0.name,
            subtitle = alb0.artistName,
            isPlaying = isPlaying0,
            gridProgressProvider = gridProgressProvider,
            colWidth = colWidth,
            availableWidth = availableWidth,
            trailingWidth = 42.dp,
            onClick = { onSelectAlbum(alb0) },
            trailingContent = {
                IconButton(
                    onClick = { onPlayAlbum(alb0) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Riproduci",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )

        // Item B
        if (alb1 != null) {
            AdaptiveLibraryItem(
                modifier = Modifier.graphicsLayer {
                    val p = gridProgressProvider()
                    translationX = (colWidthPx + colGapPx) * p
                    translationY = listRowHeightPx * (1f - p)
                },
                artworkUrl = client.getArtworkUrl(alb1.id, maxWidth = 320),
                title = alb1.name,
                subtitle = alb1.artistName,
                isPlaying = isPlaying1,
                gridProgressProvider = gridProgressProvider,
                colWidth = colWidth,
                availableWidth = availableWidth,
                trailingWidth = 42.dp,
                onClick = { onSelectAlbum(alb1) },
                trailingContent = {
                    IconButton(
                        onClick = { onPlayAlbum(alb1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Riproduci",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun AdaptiveTrackPairRow(
    pair: List<JellyfinItem>,
    gridProgressProvider: () -> Float,
    colWidth: Dp,
    availableWidth: Dp,
    playbackState: PlaybackState,
    onPlay: (JellyfinItem) -> Unit,
    onToggleFavorite: (JellyfinItem) -> Unit,
    onAddToPlaylist: (JellyfinItem) -> Unit
) {
    val client = JellyMusicApp.instance.jellyfinClient
    val trk0 = pair[0]
    val isPlaying0 = playbackState.currentTrack?.id == trk0.id && playbackState.isPlaying

    val trk1 = pair.getOrNull(1)
    val isPlaying1 = trk1?.let { playbackState.currentTrack?.id == it.id && playbackState.isPlaying } ?: false

    val density = LocalDensity.current
    val listRowHeightPx = remember(density) { with(density) { 66.dp.toPx() } }
    val colWidthPx = remember(density, colWidth) { with(density) { colWidth.toPx() } }
    val colGapPx = remember(density) { with(density) { 14.dp.toPx() } }

    AdaptivePairContainer(
        hasB = trk1 != null,
        gridProgressProvider = gridProgressProvider,
        colWidth = colWidth,
        availableWidth = availableWidth
    ) {
        // Item A
        AdaptiveLibraryItem(
            modifier = Modifier,
            artworkUrl = client.getArtworkUrl(trk0.id, maxWidth = 320),
            title = trk0.name,
            subtitle = trk0.artistName,
            isPlaying = isPlaying0,
            gridProgressProvider = gridProgressProvider,
            colWidth = colWidth,
            availableWidth = availableWidth,
            trailingWidth = 76.dp,
            onClick = { onPlay(trk0) },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { onToggleFavorite(trk0) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (trk0.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Preferito",
                            tint = if (trk0.isFavorite) Color.White else AppleTextMuted.copy(alpha = 0.6f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = { onAddToPlaylist(trk0) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "Aggiungi a playlist",
                            tint = AppleTextMuted.copy(alpha = 0.6f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        )

        // Item B
        if (trk1 != null) {
            AdaptiveLibraryItem(
                modifier = Modifier.graphicsLayer {
                    val p = gridProgressProvider()
                    translationX = (colWidthPx + colGapPx) * p
                    translationY = listRowHeightPx * (1f - p)
                },
                artworkUrl = client.getArtworkUrl(trk1.id, maxWidth = 320),
                title = trk1.name,
                subtitle = trk1.artistName,
                isPlaying = isPlaying1,
                gridProgressProvider = gridProgressProvider,
                colWidth = colWidth,
                availableWidth = availableWidth,
                trailingWidth = 76.dp,
                onClick = { onPlay(trk1) },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { onToggleFavorite(trk1) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (trk1.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Preferito",
                                tint = if (trk1.isFavorite) Color.White else AppleTextMuted.copy(alpha = 0.6f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = { onAddToPlaylist(trk1) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Aggiungi a playlist",
                                tint = AppleTextMuted.copy(alpha = 0.6f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun AdaptivePlaylistPairRow(
    pair: List<JellyfinItem>,
    gridProgressProvider: () -> Float,
    colWidth: Dp,
    availableWidth: Dp,
    onOpenPlaylist: (JellyfinItem) -> Unit
) {
    val client = JellyMusicApp.instance.jellyfinClient
    val pl0 = pair[0]
    val pl1 = pair.getOrNull(1)

    val density = LocalDensity.current
    val listRowHeightPx = remember(density) { with(density) { 66.dp.toPx() } }
    val colWidthPx = remember(density, colWidth) { with(density) { colWidth.toPx() } }
    val colGapPx = remember(density) { with(density) { 14.dp.toPx() } }

    AdaptivePairContainer(
        hasB = pl1 != null,
        gridProgressProvider = gridProgressProvider,
        colWidth = colWidth,
        availableWidth = availableWidth
    ) {
        // Item A
        AdaptiveLibraryItem(
            modifier = Modifier,
            artworkUrl = client.getArtworkUrl(pl0.id, maxWidth = 320),
            title = pl0.name,
            subtitle = "Playlist Jellyfin",
            isPlaying = false,
            gridProgressProvider = gridProgressProvider,
            colWidth = colWidth,
            availableWidth = availableWidth,
            trailingWidth = 32.dp,
            onClick = { onOpenPlaylist(pl0) },
            trailingContent = {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AppleTextMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        // Item B
        if (pl1 != null) {
            AdaptiveLibraryItem(
                modifier = Modifier.graphicsLayer {
                    val p = gridProgressProvider()
                    translationX = (colWidthPx + colGapPx) * p
                    translationY = listRowHeightPx * (1f - p)
                },
                artworkUrl = client.getArtworkUrl(pl1.id, maxWidth = 320),
                title = pl1.name,
                subtitle = "Playlist Jellyfin",
                isPlaying = false,
                gridProgressProvider = gridProgressProvider,
                colWidth = colWidth,
                availableWidth = availableWidth,
                trailingWidth = 32.dp,
                onClick = { onOpenPlaylist(pl1) },
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AppleTextMuted.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    }
}

/**
 * Track Row item in list view with favorite toggle button and playlist add.
 */
@Composable
private fun TrackListItem(
    track: JellyfinItem,
    index: Int,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val client = JellyMusicApp.instance.jellyfinClient
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(horizontal = 20.dp, vertical = 7.dp),
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
            Text(
                text = "${track.artistName} • ${track.album ?: "Singolo"}",
                color = AppleTextMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

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

            Spacer(modifier = Modifier.width(6.dp))

            // Jellyfin Sync Favorite Heart
            IconButton(
                onClick = { onToggleFavorite() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Preferito",
                    tint = if (track.isFavorite) Color.White else AppleTextMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = { onAddToPlaylist() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = "Aggiungi a playlist",
                    tint = AppleTextMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(19.dp)
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

private fun sortAlbums(list: List<JellyfinItem>, sort: TrackSortOption): List<JellyfinItem> {
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

private fun sortPlaylists(list: List<JellyfinItem>, sort: TrackSortOption): List<JellyfinItem> {
    return when (sort) {
        TrackSortOption.TITLE_ASC -> list.sortedBy { it.name.lowercase() }
        TrackSortOption.TITLE_DESC -> list.sortedByDescending { it.name.lowercase() }
        else -> list
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
