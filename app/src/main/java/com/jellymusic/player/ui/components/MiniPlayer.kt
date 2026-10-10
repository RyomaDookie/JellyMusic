package com.jellymusic.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jellymusic.player.JellyMusicApp
import com.jellymusic.player.playback.PlaybackState
import com.jellymusic.player.ui.theme.ArtworkBorder
import com.jellymusic.player.ui.theme.ArtworkControlFill
import com.jellymusic.player.ui.theme.ArtworkSurface
import com.jellymusic.player.ui.theme.ArtworkTextPrimary
import com.jellymusic.player.ui.theme.ArtworkTextSecondary

@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val client = JellyMusicApp.instance.jellyfinClient
    val controller = JellyMusicApp.instance.playerController

    val progress = if (playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Floating translucent pill matching Main.dc.html
    Box(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xD916151F))
            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(14.dp))
            .clickable { onExpand() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(start = 9.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album cover thumbnail (44x44, radius 6dp)
            AsyncImage(
                model = client.getArtworkUrl(track.id, maxWidth = 140),
                contentDescription = "Cover",
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E1D26)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                Text(
                    text = track.name,
                    color = ArtworkTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1500,
                        initialDelayMillis = 2000,
                        spacing = MarqueeSpacing(24.dp),
                        velocity = 26.dp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artistName,
                    color = ArtworkTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play / Pause Button
            IconButton(
                onClick = { controller.togglePlayPause() },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pausa" else "Riproduci",
                    tint = ArtworkTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Next Track Button
            IconButton(
                onClick = { controller.skipNext() },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Successivo",
                    tint = ArtworkTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Bottom progress bar (2px height)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0x1AFFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(2.dp)
                    .background(ArtworkControlFill)
            )
        }
    }
}
