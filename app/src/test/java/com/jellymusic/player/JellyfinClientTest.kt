package com.jellymusic.player

import com.jellymusic.player.data.JellyfinClient
import com.jellymusic.player.data.JellyfinItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JellyfinClientTest {

    @Test
    fun testStreamUrlGeneration() {
        val client = JellyfinClient()
        client.updateCredentials("https://demo.jellyfin.org", "token123", "user456")

        val directUrl = client.getStreamUrl("item789", maxBitrateKbps = 0)
        assertEquals("https://demo.jellyfin.org/Audio/item789/stream?static=true&api_key=token123", directUrl)

        val transcodeUrl = client.getStreamUrl("item789", maxBitrateKbps = 320)
        assertEquals("https://demo.jellyfin.org/Audio/item789/stream?audioCodec=mp3&maxStreamingBitrate=320000&api_key=token123", transcodeUrl)
    }

    @Test
    fun testArtworkUrlGeneration() {
        val client = JellyfinClient()
        client.updateCredentials("http://192.168.1.50:8096/", "tokenABC", "userXYZ")

        val artworkUrl = client.getArtworkUrl("track999", maxWidth = 400)
        assertEquals("http://192.168.1.50:8096/Items/track999/Images/Primary?maxWidth=400&api_key=tokenABC", artworkUrl)
    }

    @Test
    fun testTrackDurationFormatting() {
        val item = JellyfinItem(
            id = "1",
            name = "Test Song",
            runTimeTicks = 205_000_0000L // 205 seconds = 3:25
        )
        assertEquals(205_000L, item.durationMs)
        assertEquals("3:25", item.formattedDuration)
    }

    @Test
    fun testSearchQueryEncoding() {
        val raw = "Rock & Roll #1"
        val encoded = java.net.URLEncoder.encode(raw.trim(), "UTF-8")
        assertEquals("Rock+%26+Roll+%231", encoded)
    }
}
