package com.jellymusic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.jellymusic.player.preferences.UserCustomizations
import com.jellymusic.player.ui.components.MiniPlayer
import com.jellymusic.player.ui.screens.HomeScreen
import com.jellymusic.player.ui.screens.LibraryScreen
import com.jellymusic.player.ui.screens.LoginScreen
import com.jellymusic.player.ui.screens.PlayerScreen
import com.jellymusic.player.ui.screens.SettingsScreen
import com.jellymusic.player.ui.theme.ArtworkBackground
import com.jellymusic.player.ui.theme.ArtworkBorder
import com.jellymusic.player.ui.theme.ArtworkTextMuted
import com.jellymusic.player.ui.theme.ArtworkTextPrimary
import com.jellymusic.player.ui.theme.JellyMusicTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideStatusBar()

        setContent {
            val app = JellyMusicApp.instance
            val customizations by app.preferences.customizations.collectAsState(initial = UserCustomizations())
            val token by app.preferences.accessToken.collectAsState(initial = null)
            val serverUrl by app.preferences.serverUrl.collectAsState(initial = null)
            val userId by app.preferences.userId.collectAsState(initial = null)

            val isAuthLoaded = token != null && serverUrl != null

            if (isAuthLoaded && !token.isNullOrEmpty() && !serverUrl.isNullOrEmpty()) {
                app.jellyfinClient.updateCredentials(serverUrl!!, token!!, userId.orEmpty())
            }

            JellyMusicTheme(customizations = customizations) {
                when {
                    !isAuthLoaded -> {
                        // Keep neutral dark background while reading preferences from storage - prevents LoginScreen flicker
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ArtworkBackground)
                        )
                    }
                    token.isNullOrEmpty() || serverUrl.isNullOrEmpty() -> {
                        LoginScreen(onLoginSuccess = {})
                    }
                    else -> {
                        MainNavigationHost(customizations = customizations)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideStatusBar()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideStatusBar()
        }
    }

    private fun hideStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
    }
}

@Composable
fun MainNavigationHost(
    customizations: UserCustomizations
) {
    val app = JellyMusicApp.instance
    val playbackState by app.playerController.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Home, 1 = Libreria, 2 = Impostazioni
    val animProgress = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val density = LocalDensity.current
        val p = animProgress.value.coerceIn(0f, 1f)

        val springSpec = spring<Float>(dampingRatio = 0.9431f, stiffness = 120f)

        val expandPlayer: () -> Unit = {
            coroutineScope.launch {
                animProgress.animateTo(1f, springSpec)
            }
        }

        val collapsePlayer: () -> Unit = {
            coroutineScope.launch {
                animProgress.animateTo(0f, springSpec)
            }
        }

        val smP = (p.coerceIn(0f, 1f)).let { it * it * (3f - 2f * it) }
        val baseScale = 1.0f + (0.94f - 1.0f) * smP
        val baseRadius = (24.dp * smP).coerceAtLeast(0.dp)
        val scrimAlpha = (0.55f * smP).coerceIn(0f, 1f)

        val qNav = if (0.6f <= 0f) 1f else ((p - 0f) / 0.6f).coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
        val navTranslationY = with(density) { (qNav * 80.dp.toPx()) }

        BackHandler(enabled = p > 0.01f) {
            collapsePlayer()
        }

        // Base Content Container (scales to 0.94, rounds to 24dp, dims by 0.55 matching Transition.dc.html)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = baseScale
                    scaleY = baseScale
                    transformOrigin = TransformOrigin(0.5f, 0.4f)
                    shape = RoundedCornerShape(baseRadius)
                    clip = true
                }
                .background(ArtworkBackground)
        ) {
            // Main Content Screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp)
            ) {
                when (activeTab) {
                    0 -> HomeScreen(
                        customizations = customizations,
                        playbackState = playbackState,
                        onOpenPlayer = expandPlayer,
                        onOpenSettings = { activeTab = 2 },
                        onNavigateToTab = { activeTab = it },
                        modifier = Modifier.fillMaxSize()
                    )
                    2 -> SettingsScreen(
                        customizations = customizations,
                        onBack = { activeTab = 0 },
                        onLogout = { activeTab = 0 }
                    )
                    else -> LibraryScreen(
                        customizations = customizations,
                        playbackState = playbackState,
                        onOpenSettings = { activeTab = 2 },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Dark dimming scrim matching Transition.dc.html brightness filter
            if (scrimAlpha > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = scrimAlpha))
                )
            }
        }

        // Bottom Navigation Bar (Transition.dc.html: navStyle with translateY(q(0, 0.6) * 80))
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(72.dp)
                .graphicsLayer {
                    translationY = navTranslationY
                }
                .background(Color(0xF50D0D11))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x22FFFFFF))
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = activeTab == 0,
                    onClick = { activeTab = 0 }
                )
                BottomNavItem(
                    icon = Icons.Default.Album,
                    label = "Libreria",
                    isSelected = activeTab == 1,
                    onClick = { activeTab = 1 }
                )
                BottomNavItem(
                    icon = Icons.Default.Settings,
                    label = "Impostazioni",
                    isSelected = activeTab == 2,
                    onClick = { activeTab = 2 }
                )
            }
        }

        // Unified Morphing Player Container (Transition.dc.html 100% faithful)
        if (playbackState.currentTrack != null) {
            PlayerScreen(
                playbackState = playbackState,
                customizations = customizations,
                animProgress = animProgress,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                onExpand = expandPlayer,
                onCollapse = collapsePlayer
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = Color(0xFFD926AA)
    val targetTint = if (isSelected) Color.White else Color(0xFF8B8A94)
    val animatedTint by androidx.compose.animation.animateColorAsState(
        targetValue = targetTint,
        animationSpec = tween(250),
        label = "navTint"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "navScale"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = animatedTint,
            modifier = Modifier
                .size(23.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = animatedTint
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(width = if (isSelected) 12.dp else 0.dp, height = 2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isSelected) activeColor else Color.Transparent)
        )
    }
}
