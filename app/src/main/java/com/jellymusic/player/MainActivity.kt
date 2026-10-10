package com.jellymusic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.derivedStateOf
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
        enableHighRefreshRate()

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
        enableHighRefreshRate()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideStatusBar()
            enableHighRefreshRate()
        }
    }

    private fun enableHighRefreshRate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val display = display ?: return
                val modes = display.supportedModes
                val highestRefreshRateMode = modes.maxByOrNull { it.refreshRate }
                if (highestRefreshRateMode != null) {
                    val params = window.attributes
                    params.preferredDisplayModeId = highestRefreshRateMode.modeId
                    @Suppress("DEPRECATION")
                    params.preferredRefreshRate = highestRefreshRateMode.refreshRate
                    window.attributes = params
                }
            } catch (_: Exception) {}
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                @Suppress("DEPRECATION")
                val windowManager = getSystemService(android.content.Context.WINDOW_SERVICE) as? android.view.WindowManager
                val display = windowManager?.defaultDisplay
                val modes = display?.supportedModes
                val highestRefreshRateMode = modes?.maxByOrNull { it.refreshRate }
                if (highestRefreshRateMode != null) {
                    val params = window.attributes
                    params.preferredDisplayModeId = highestRefreshRateMode.modeId
                    @Suppress("DEPRECATION")
                    params.preferredRefreshRate = highestRefreshRateMode.refreshRate
                    window.attributes = params
                }
            } catch (_: Exception) {}
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

        val isPlayerExpanded by remember {
            derivedStateOf { animProgress.value > 0.01f }
        }

        BackHandler(enabled = isPlayerExpanded) {
            collapsePlayer()
        }

        // Base Content Container (scales to 0.94, rounds to 24dp, dims by 0.55 matching Transition.dc.html)
        // 100% GPU RenderNode transforms - ZERO recomposition during player drag/animation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = animProgress.value.coerceIn(0f, 1f)
                    val smP = p * p * (3f - 2f * p)
                    val baseScale = 1.0f + (0.94f - 1.0f) * smP
                    val baseRadius = (24.dp.toPx() * smP).coerceAtLeast(0f)
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
                val tabEase = remember { CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f) }
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(
                                initialOffsetX = { (it * 0.20f).toInt() },
                                animationSpec = tween(500, easing = tabEase)
                            ) + fadeIn(animationSpec = tween(450, easing = tabEase))).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { (-it * 0.20f).toInt() },
                                    animationSpec = tween(500, easing = tabEase)
                                ) + fadeOut(animationSpec = tween(350, easing = tabEase))
                            )
                        } else {
                            (slideInHorizontally(
                                initialOffsetX = { (-it * 0.20f).toInt() },
                                animationSpec = tween(500, easing = tabEase)
                            ) + fadeIn(animationSpec = tween(450, easing = tabEase))).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { (it * 0.20f).toInt() },
                                    animationSpec = tween(500, easing = tabEase)
                                ) + fadeOut(animationSpec = tween(350, easing = tabEase))
                            )
                        }
                    },
                    label = "tabTransition",
                    modifier = Modifier.fillMaxSize()
                ) { tab ->
                    when (tab) {
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
            }

            // Dark dimming scrim matching Transition.dc.html brightness filter (RenderNode alpha - no recomposition)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = animProgress.value.coerceIn(0f, 1f)
                        val smP = p * p * (3f - 2f * p)
                        alpha = (0.55f * smP).coerceIn(0f, 1f)
                    }
                    .background(Color.Black)
            )
        }

        // Bottom Navigation Bar - Floating transparent glass bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .height(56.dp)
                .graphicsLayer {
                    val p = animProgress.value.coerceIn(0f, 1f)
                    val qNav = if (p <= 0f) 0f else ((p - 0f) / 0.6f).coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
                    translationY = qNav * 80.dp.toPx()
                }
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0x3514141E))
                .background(Color(0x18FFFFFF))
                .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(28.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
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

        // Unified Morphing Player Container (Smooth animated entrance when mini player first appears)
        AnimatedVisibility(
            visible = playbackState.currentTrack != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 350f
                )
            ) + fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(200))
        ) {
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = animatedTint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = animatedTint
        )
    }
}
