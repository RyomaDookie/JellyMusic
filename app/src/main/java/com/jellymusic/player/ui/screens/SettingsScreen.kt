package com.jellymusic.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jellymusic.player.JellyMusicApp
import com.jellymusic.player.preferences.UserCustomizations
import com.jellymusic.player.ui.theme.ArtworkBackground
import com.jellymusic.player.ui.theme.ArtworkBorder
import com.jellymusic.player.ui.theme.ArtworkSurface
import com.jellymusic.player.ui.theme.ArtworkTextMuted
import com.jellymusic.player.ui.theme.ArtworkTextPrimary
import com.jellymusic.player.ui.theme.ArtworkTextSecondary
import com.jellymusic.player.ui.theme.EmeraldPrimary
import com.jellymusic.player.ui.theme.JellyfinPrimary
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
                        "Impostazioni",
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
