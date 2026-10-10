package com.jellymusic.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jellymusic.player.JellyMusicApp
import com.jellymusic.player.ui.theme.AmbientAmber
import com.jellymusic.player.ui.theme.AmbientIndigo
import com.jellymusic.player.ui.theme.ArtworkBackground
import com.jellymusic.player.ui.theme.ArtworkBorder
import com.jellymusic.player.ui.theme.ArtworkControlFill
import com.jellymusic.player.ui.theme.ArtworkSurface
import com.jellymusic.player.ui.theme.ArtworkTextMuted
import com.jellymusic.player.ui.theme.ArtworkTextPrimary
import com.jellymusic.player.ui.theme.ArtworkTextSecondary
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = JellyMusicApp.instance.preferences
    val client = JellyMusicApp.instance.jellyfinClient
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf("https://") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArtworkBackground)
    ) {
        // Glowing Ambient Background Orbs
        Box(
            modifier = Modifier
                .offset(x = (-60).dp, y = (-50).dp)
                .size(340.dp)
                .blur(90.dp)
                .background(AmbientAmber.copy(alpha = 0.40f), CircleShape)
        )
        Box(
            modifier = Modifier
                .offset(x = 120.dp, y = 300.dp)
                .size(360.dp)
                .blur(90.dp)
                .background(AmbientIndigo.copy(alpha = 0.45f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ArtworkSurface)
                    .border(1.dp, ArtworkBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Logo",
                    tint = ArtworkTextPrimary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "JellyMusic",
                color = ArtworkTextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.02).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Collega il tuo server Jellyfin esterno",
                color = ArtworkTextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Text fields matching the dark glass design
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it; errorMessage = null },
                label = { Text("URL Server (es. https://jellyfin.example.com)", color = ArtworkTextMuted) },
                leadingIcon = { Icon(Icons.Default.Dns, contentDescription = "Server", tint = ArtworkTextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ArtworkSurface,
                    unfocusedContainerColor = ArtworkSurface,
                    focusedBorderColor = Color(0x40FFFFFF),
                    unfocusedBorderColor = ArtworkBorder,
                    focusedTextColor = ArtworkTextPrimary,
                    unfocusedTextColor = ArtworkTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorMessage = null },
                label = { Text("Nome utente", color = ArtworkTextMuted) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User", tint = ArtworkTextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ArtworkSurface,
                    unfocusedContainerColor = ArtworkSurface,
                    focusedBorderColor = Color(0x40FFFFFF),
                    unfocusedBorderColor = ArtworkBorder,
                    focusedTextColor = ArtworkTextPrimary,
                    unfocusedTextColor = ArtworkTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Password", color = ArtworkTextMuted) },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = ArtworkTextMuted) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ArtworkSurface,
                    unfocusedContainerColor = ArtworkSurface,
                    focusedBorderColor = Color(0x40FFFFFF),
                    unfocusedBorderColor = ArtworkBorder,
                    focusedTextColor = ArtworkTextPrimary,
                    unfocusedTextColor = ArtworkTextPrimary
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = errorMessage!!,
                    color = Color(0xFFFF5252),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    if (serverUrl.isBlank() || username.isBlank()) {
                        errorMessage = "Inserisci l'URL del server e il nome utente"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null

                    scope.launch {
                        val result = client.authenticate(serverUrl, username, password)
                        isLoading = false
                        result.fold(
                            onSuccess = { authResp ->
                                prefs.saveAuth(
                                    serverUrl = serverUrl,
                                    token = authResp.accessToken,
                                    userId = authResp.user.id,
                                    username = authResp.user.name
                                )
                                onLoginSuccess()
                            },
                            onFailure = { error ->
                                errorMessage = error.localizedMessage ?: "Connessione fallita. Controlla URL e credenziali."
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArtworkControlFill,
                    contentColor = Color.Black
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Connetti",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
