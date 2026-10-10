package com.jellymusic.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jellymusic.player.JellyMusicApp
import com.jellymusic.player.preferences.AccentPalette
import com.jellymusic.player.preferences.PlayerLayoutStyle
import com.jellymusic.player.preferences.StreamingQuality
import com.jellymusic.player.preferences.ThemeMode
import com.jellymusic.player.preferences.UserCustomizations
import com.jellymusic.player.ui.theme.ArtworkBackground
import com.jellymusic.player.ui.theme.ArtworkBorder
import com.jellymusic.player.ui.theme.ArtworkControlFill
import com.jellymusic.player.ui.theme.ArtworkSurface
import com.jellymusic.player.ui.theme.ArtworkTextMuted
import com.jellymusic.player.ui.theme.ArtworkTextPrimary
import com.jellymusic.player.ui.theme.ArtworkTextSecondary
import com.jellymusic.player.ui.theme.CyberpunkPrimary
import com.jellymusic.player.ui.theme.EmeraldPrimary
import com.jellymusic.player.ui.theme.JellyfinPrimary
import com.jellymusic.player.ui.theme.SakuraPrimary
import com.jellymusic.player.ui.theme.SunsetPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    customizations: UserCustomizations,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = JellyMusicApp.instance.preferences
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Personalizzazioni & Impostazioni",
                        color = ArtworkTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = ArtworkTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ArtworkBackground)
            )
        },
        containerColor = ArtworkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Sezione 1: Aspetto & Temi
            SectionHeader(title = "Aspetto & Tema", icon = Icons.Default.Palette)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ArtworkSurface)
                    .border(1.dp, ArtworkBorder, RoundedCornerShape(14.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = "Modalità Tema",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ArtworkTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = customizations.themeMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x29FFFFFF) else Color.Transparent)
                                    .border(1.dp, if (isSelected) ArtworkControlFill else ArtworkBorder, RoundedCornerShape(8.dp))
                                    .clickable { scope.launch { prefs.updateThemeMode(mode) } }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = mode.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) ArtworkTextPrimary else ArtworkTextMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = "Tavolozza di Accento",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ArtworkTextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val palettes = listOf(
                            AccentPalette.DYNAMIC to Color(0xFFE2DFD8),
                            AccentPalette.JELLYFIN_PURPLE to JellyfinPrimary,
                            AccentPalette.CYBERPUNK_CYAN to CyberpunkPrimary,
                            AccentPalette.SUNSET_AMBER to SunsetPrimary,
                            AccentPalette.EMERALD_FOREST to EmeraldPrimary,
                            AccentPalette.SAKURA_PINK to SakuraPrimary
                        )

                        palettes.forEach { (palette, color) ->
                            val isSelected = customizations.accentPalette == palette
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { scope.launch { prefs.updateAccentPalette(palette) } }
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, ArtworkTextPrimary, CircleShape)
                                        else Modifier.border(1.dp, ArtworkBorder, CircleShape)
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sezione 2: Stile Layout Player
            SectionHeader(title = "Stile Layout del Player", icon = Icons.Default.ViewCarousel)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ArtworkSurface)
                    .border(1.dp, ArtworkBorder, RoundedCornerShape(14.dp))
                    .padding(18.dp)
            ) {
                Column {
                    PlayerLayoutStyle.entries.forEach { style ->
                        val isSelected = customizations.playerLayout == style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0x1AFFFFFF) else Color.Transparent)
                                .clickable { scope.launch { prefs.updatePlayerLayout(style) } }
                                .padding(vertical = 10.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (style) {
                                    PlayerLayoutStyle.MODERN_HERO -> "Modern Hero (Copertina immersiva con ombre)"
                                    PlayerLayoutStyle.COMPACT_CARD -> "Compact Card (Scheda compatta con elevazione)"
                                    PlayerLayoutStyle.MINIMALIST -> "Minimalist (Tipografico e discreto)"
                                },
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) ArtworkTextPrimary else ArtworkTextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    var localRadius by remember(customizations.cornerRadiusDp) {
                        mutableFloatStateOf(customizations.cornerRadiusDp.toFloat())
                    }

                    Text(
                        text = "Raggio degli Angoli: ${localRadius.toInt()} dp",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ArtworkTextPrimary
                    )

                    Slider(
                        value = localRadius,
                        onValueChange = { localRadius = it },
                        onValueChangeFinished = {
                            scope.launch { prefs.updateCornerRadius(localRadius.toInt()) }
                        },
                        valueRange = 4f..28f,
                        steps = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sezione 3: Qualità Audio & Streaming
            SectionHeader(title = "Qualità Audio Jellyfin", icon = Icons.Default.HighQuality)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ArtworkSurface)
                    .border(1.dp, ArtworkBorder, RoundedCornerShape(14.dp))
                    .padding(18.dp)
            ) {
                Column {
                    StreamingQuality.entries.forEach { quality ->
                        val isSelected = customizations.streamingQuality == quality
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0x1AFFFFFF) else Color.Transparent)
                                .clickable { scope.launch { prefs.updateStreamingQuality(quality) } }
                                .padding(vertical = 10.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = quality.label,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) ArtworkTextPrimary else ArtworkTextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sezione 4: Debug & Logcat
            SectionHeader(title = "Debug & Diagnostica", icon = Icons.Default.Info)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ArtworkSurface)
                    .border(1.dp, ArtworkBorder, RoundedCornerShape(14.dp))
                    .padding(18.dp)
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Salva Logcat in LOGS",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = ArtworkTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Registra i log e i crash dell'app nella cartella LOGS della memoria principale.",
                                fontSize = 12.sp,
                                color = ArtworkTextMuted,
                                lineHeight = 16.sp
                            )
                        }

                        androidx.compose.material3.Switch(
                            checked = customizations.debugLogging,
                            onCheckedChange = { isChecked ->
                                scope.launch {
                                    prefs.updateDebugLogging(isChecked)
                                }
                            }
                        )
                    }

                    if (customizations.debugLogging) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val logsDir = remember(context) { com.jellymusic.player.debug.DebugLogManager.getLogsDirectory(context).absolutePath }
                        val activeFile = com.jellymusic.player.debug.DebugLogManager.getCurrentLogFilePath()

                        Text(
                            text = "Cartella:\n$logsDir",
                            fontSize = 11.sp,
                            color = ArtworkTextSecondary,
                            lineHeight = 15.sp
                        )
                        if (activeFile != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "File attivo:\n${activeFile.substringAfterLast(java.io.File.separator)}",
                                fontSize = 11.sp,
                                color = EmeraldPrimary,
                                lineHeight = 15.sp
                            )
                        }

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                            data = android.net.Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                        context.startActivity(intent)
                                    }
                                }
                            ) {
                                Text("Concedi permesso accesso cartelle", fontSize = 12.sp, color = JellyfinPrimary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sezione 5: Disconnessione
            OutlinedButton(
                onClick = {
                    scope.launch {
                        prefs.clearAuth()
                        onLogout()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40FF5252)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Disconnetti", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Disconnetti da Jellyfin", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ArtworkTextPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = ArtworkTextMuted,
            letterSpacing = 1.sp
        )
    }
}
