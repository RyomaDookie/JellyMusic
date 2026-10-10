package com.jellymusic.player.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class LyricsService(
    private val jellyfinClient: JellyfinClient
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // In-memory cache by track ID
    private val cache = mutableMapOf<String, List<LyricLineDto>>()

    suspend fun getLyrics(track: JellyfinItem): Result<List<LyricLineDto>> = withContext(Dispatchers.IO) {
        // Cache lookup
        cache[track.id]?.let { return@withContext Result.success(it) }

        val cleanTitle = sanitizeTitle(track.name)
        val cleanArtist = sanitizeArtist(track.artistName)

        // 1. Try BetterLyrics (word-by-word/syllable-by-syllable TTML)
        tryBetterLyrics(cleanTitle, cleanArtist)?.let { lines ->
            cache[track.id] = lines
            return@withContext Result.success(lines)
        }

        // 2. Try Paxsenix (Apple Music TTML via iTunes Search)
        tryPaxsenixAppleMusic(cleanTitle, cleanArtist)?.let { lines ->
            cache[track.id] = lines
            return@withContext Result.success(lines)
        }

        // 3. Fallback: Jellyfin server lyrics
        val jellyResult = jellyfinClient.getLyrics(track.id)
        if (jellyResult.isSuccess && !jellyResult.getOrNull().isNullOrEmpty()) {
            val lines = jellyResult.getOrNull()!!
            cache[track.id] = lines
            return@withContext Result.success(lines)
        }

        // 4. Fallback: LRCLIB (LRC synchronized lines)
        tryLrcLib(cleanTitle, cleanArtist)?.let { lines ->
            cache[track.id] = lines
            return@withContext Result.success(lines)
        }

        Result.failure(IOException("No lyrics found for ${track.name}"))
    }

    private fun tryBetterLyrics(title: String, artist: String): List<LyricLineDto>? {
        if (title.isBlank()) return null
        return try {
            val encSong = URLEncoder.encode(title, "UTF-8")
            val encArtist = URLEncoder.encode(artist, "UTF-8")
            val url = "https://api.betterlyrics.org/getLyrics?s=$encSong&a=$encArtist"
            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            val root = json.parseToJsonElement(body).jsonObject
            val ttml = root["ttml"]?.jsonPrimitive?.content
            if (!ttml.isNullOrBlank()) {
                val parsed = parseTtml(ttml)
                if (parsed.isNotEmpty()) return parsed
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun tryPaxsenixAppleMusic(title: String, artist: String): List<LyricLineDto>? {
        if (title.isBlank()) return null
        return try {
            val query = if (artist.isNotBlank()) "$title $artist" else title
            val encQuery = URLEncoder.encode(query, "UTF-8")
            val itunesUrl = "https://itunes.apple.com/search?term=$encQuery&entity=song&limit=1"
            val itunesReq = Request.Builder().url(itunesUrl).get().build()
            val itunesResp = httpClient.newCall(itunesReq).execute()
            if (!itunesResp.isSuccessful) return null
            val itunesBody = itunesResp.body?.string().orEmpty()
            if (itunesBody.isBlank()) return null

            val trackId = extractTrackIdFromItunes(itunesBody) ?: return null

            val paxUrl = "https://lyrics.paxsenix.org/apple-music/lyrics?id=$trackId&ttml=true"
            val paxReq = Request.Builder().url(paxUrl).get().build()
            val paxResp = httpClient.newCall(paxReq).execute()
            if (!paxResp.isSuccessful) return null
            val paxBody = paxResp.body?.string().orEmpty()
            if (paxBody.isBlank()) return null

            val paxJson = json.parseToJsonElement(paxBody).jsonObject
            val ttmlContent = paxJson["content"]?.jsonPrimitive?.content
            if (!ttmlContent.isNullOrBlank()) {
                val parsed = parseTtml(ttmlContent)
                if (parsed.isNotEmpty()) return parsed
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTrackIdFromItunes(itunesBody: String): Long? {
        val match = Regex(""""trackId":\s*(\d+)""").find(itunesBody)
        return match?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun tryLrcLib(title: String, artist: String): List<LyricLineDto>? {
        if (title.isBlank()) return null
        return try {
            val encSong = URLEncoder.encode(title, "UTF-8")
            val encArtist = URLEncoder.encode(artist, "UTF-8")
            val url = "https://lrclib.net/api/get?track_name=$encSong&artist_name=$encArtist"
            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            val root = json.parseToJsonElement(body).jsonObject
            val synced = root["syncedLyrics"]?.jsonPrimitive?.content
            if (!synced.isNullOrBlank()) {
                val parsed = parseLrc(synced)
                if (parsed.isNotEmpty()) return parsed
            }
            val plain = root["plainLyrics"]?.jsonPrimitive?.content
            if (!plain.isNullOrBlank()) {
                return plain.lines().filter { it.isNotBlank() }.map {
                    LyricLineDto(text = it.trim(), startTicks = null)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun sanitizeTitle(title: String): String {
        return title
            .replace(Regex("""\s*[\(\[](feat\.|ft\.|with\b|remastered|deluxe|bonus|version|explicit|official|audio|video|edit)[^\)\]]*[\)\]]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*-\s*(feat\.|ft\.|remastered|deluxe|bonus|version|explicit|official).*""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun sanitizeArtist(artist: String): String {
        val first = artist.split(Regex("""\s*(?:,|&|\/|\bfeat\.?\b|\bft\.?\b)\s*""", RegexOption.IGNORE_CASE)).firstOrNull().orEmpty()
        return if (first.isNotBlank()) first.trim() else artist.trim()
    }

    companion object {
        private val P_REGEX = Regex("""<p\b([^>]*)>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
        private val ATTR_REGEX = Regex("""([a-zA-Z0-9_:-]+)=["']([^"']*)["']""")
        private val SPAN_REGEX = Regex("""<span\b([^>]*)>((?:(?!<span).)*?)</span>(\s*)""", RegexOption.DOT_MATCHES_ALL)
        private val HTML_TAG_REGEX = Regex("""<[^>]*>""")

        fun parseTtml(ttml: String): List<LyricLineDto> {
            val lines = mutableListOf<LyricLineDto>()
            val pMatches = P_REGEX.findAll(ttml)

            for (pMatch in pMatches) {
                val pAttrsRaw = pMatch.groupValues[1]
                val pContent = pMatch.groupValues[2]

                val pAttrs = ATTR_REGEX.findAll(pAttrsRaw).associate {
                    it.groupValues[1] to it.groupValues[2]
                }
                val pBeginStr = pAttrs["begin"] ?: pAttrs["itunes:begin"]
                val pEndStr = pAttrs["end"] ?: pAttrs["itunes:end"]

                val pBeginMs = pBeginStr?.let { parseTimeMs(it) } ?: 0L
                val pEndMs = pEndStr?.let { parseTimeMs(it) } ?: 0L

                var currentContent = pContent
                val bgPhrases = mutableListOf<LyricPhrase>()

                // Extract all secondary vocal / ad-lib blocks marked with ttm:role="x-bg"
                while (true) {
                    val bgResult = extractBgBlock(currentContent) ?: break
                    currentContent = bgResult.remainingContent

                    val fallbackBgStart = bgResult.bgBeginMs ?: pBeginMs
                    val fallbackBgEnd = bgResult.bgEndMs ?: pEndMs

                    val (bgText, bgSpans, bgWords) = parseSpansAndWords(
                        bgResult.innerContent,
                        fallbackBgStart,
                        fallbackBgEnd,
                        stripParens = true
                    )
                    if (bgText.isNotBlank()) {
                        val bgStart = bgSpans.firstOrNull()?.startMs ?: fallbackBgStart
                        val bgEnd = bgSpans.lastOrNull()?.endMs ?: fallbackBgEnd
                        bgPhrases.add(LyricPhrase(
                            text = bgText,
                            startMs = bgStart,
                            endMs = bgEnd,
                            spans = bgSpans,
                            words = bgWords
                        ))
                    }
                }

                val backgroundVoice: LyricPhrase? = when (bgPhrases.size) {
                    0 -> null
                    1 -> bgPhrases.first()
                    else -> {
                        val combinedText = bgPhrases.joinToString(" ") { it.text }
                        val allSpans = bgPhrases.flatMap { it.spans }.sortedBy { it.startMs }
                        val allWords = bgPhrases.flatMap { it.words }.sortedBy { it.startMs }
                        LyricPhrase(
                            text = combinedText,
                            startMs = bgPhrases.minOf { it.startMs },
                            endMs = bgPhrases.maxOf { it.endMs },
                            spans = allSpans,
                            words = allWords
                        )
                    }
                }

                // Parse main voice
                val (mainText, mainSpans, mainWords) = parseSpansAndWords(currentContent, pBeginMs, pEndMs, stripParens = false)

                if (mainText.isNotBlank()) {
                    val lineStart = mainSpans.firstOrNull()?.startMs ?: pBeginMs
                    val lineEnd = mainSpans.lastOrNull()?.endMs ?: pEndMs
                    lines.add(LyricLineDto(
                        text = mainText,
                        startTicks = lineStart * 10_000L,
                        endTicks = lineEnd * 10_000L,
                        spans = mainSpans,
                        words = mainWords,
                        backgroundVoice = backgroundVoice
                    ))
                } else if (backgroundVoice != null) {
                    // Solo ad-lib line
                    lines.add(LyricLineDto(
                        text = backgroundVoice.text,
                        startTicks = backgroundVoice.startMs * 10_000L,
                        endTicks = backgroundVoice.endMs * 10_000L,
                        spans = backgroundVoice.spans,
                        words = backgroundVoice.words,
                        backgroundVoice = null
                    ))
                }
            }
            lines.sortBy { it.earliestStartMs }
            return lines
        }

        private data class BgBlockResult(
            val innerContent: String,
            val bgBeginMs: Long?,
            val bgEndMs: Long?,
            val remainingContent: String
        )

        private fun extractBgBlock(content: String): BgBlockResult? {
            val idx = content.indexOf("ttm:role=\"x-bg\"")
            if (idx < 0) return null
            val startTag = content.lastIndexOf("<span", idx)
            if (startTag < 0) return null

            var pos = startTag
            var depth = 0
            var endPos = -1
            while (pos < content.length) {
                val nextOpen = content.indexOf("<span", pos)
                val nextClose = content.indexOf("</span>", pos)
                if (nextClose < 0) break

                if (nextOpen in 0 until nextClose) {
                    depth++
                    pos = nextOpen + 5
                } else {
                    depth--
                    pos = nextClose + 7
                    if (depth == 0) {
                        endPos = pos
                        break
                    }
                }
            }

            if (endPos > startTag) {
                val bgXml = content.substring(startTag, endPos)
                val tagCloseIdx = bgXml.indexOf('>')
                if (tagCloseIdx >= 0) {
                    val openTag = bgXml.substring(0, tagCloseIdx + 1)
                    val attrs = ATTR_REGEX.findAll(openTag).associate { it.groupValues[1] to it.groupValues[2] }
                    val bgBeginMs = attrs["begin"]?.let { parseTimeMs(it) }
                    val bgEndMs = attrs["end"]?.let { parseTimeMs(it) }
                    val innerEnd = if (bgXml.endsWith("</span>")) bgXml.length - 7 else bgXml.length
                    val innerContent = if (innerEnd > tagCloseIdx + 1) bgXml.substring(tagCloseIdx + 1, innerEnd) else ""
                    val remaining = content.removeRange(startTag, endPos)
                    return BgBlockResult(
                        innerContent = innerContent,
                        bgBeginMs = bgBeginMs,
                        bgEndMs = bgEndMs,
                        remainingContent = remaining
                    )
                }
            }
            return null
        }

        private fun parseSpansAndWords(
            content: String,
            fallbackBeginMs: Long,
            fallbackEndMs: Long,
            stripParens: Boolean = false
        ): Triple<String, List<LyricSpan>, List<LyricWord>> {
            val spanMatches = SPAN_REGEX.findAll(content).toList()
            if (spanMatches.isEmpty()) {
                var clean = content.replace(HTML_TAG_REGEX, "")
                clean = unescapeXml(clean)
                clean = clean.replace(HTML_TAG_REGEX, "")
                if (stripParens) {
                    clean = clean.replace("(", "").replace(")", "")
                }
                clean = clean.trim()
                if (clean.isNotEmpty()) {
                    val span = LyricSpan(text = clean, startMs = fallbackBeginMs, endMs = fallbackEndMs)
                    val word = LyricWord(text = clean, startMs = fallbackBeginMs, endMs = fallbackEndMs, syllables = listOf(span))
                    return Triple(clean, listOf(span), listOf(word))
                }
                return Triple("", emptyList(), emptyList())
            }

            val rawSpans = mutableListOf<Pair<LyricSpan, Boolean>>()
            val textBuilder = StringBuilder()

            for (spanMatch in spanMatches) {
                val spanAttrsRaw = spanMatch.groupValues[1]
                val spanInner = spanMatch.groupValues[2]
                val trailingSpace = spanMatch.groupValues[3]

                val sAttrs = ATTR_REGEX.findAll(spanAttrsRaw).associate {
                    it.groupValues[1] to it.groupValues[2]
                }
                val sBeginStr = sAttrs["begin"]
                val sEndStr = sAttrs["end"]

                val sBeginMs = sBeginStr?.let { parseTimeMs(it) } ?: fallbackBeginMs
                val sEndMs = sEndStr?.let { parseTimeMs(it) } ?: fallbackEndMs

                // Strip ANY HTML tags inside spanInner or unescaped text
                var rawText = spanInner.replace(HTML_TAG_REGEX, "")
                rawText = unescapeXml(rawText).replace(HTML_TAG_REGEX, "")
                if (stripParens) {
                    rawText = rawText.replace("(", "").replace(")", "")
                }
                val hasSpace = trailingSpace.isNotEmpty() || rawText.endsWith(" ")
                val cleanText = rawText.replace(HTML_TAG_REGEX, "").trim()

                if (cleanText.isNotEmpty()) {
                    val span = LyricSpan(
                        text = cleanText,
                        startMs = sBeginMs,
                        endMs = sEndMs
                    )
                    rawSpans.add(span to hasSpace)
                    textBuilder.append(cleanText)
                    if (hasSpace) textBuilder.append(" ")
                }
            }

            // Group spans into atomic words for natural FlowRow wrapping
            val words = mutableListOf<LyricWord>()
            var currentSyllables = mutableListOf<LyricSpan>()

            for ((span, hasSpaceAfter) in rawSpans) {
                currentSyllables.add(span)
                if (hasSpaceAfter) {
                    val wText = currentSyllables.joinToString("") { it.text }
                        .replace(HTML_TAG_REGEX, "")
                        .trim()
                    if (wText.isNotEmpty()) {
                        words.add(LyricWord(
                            text = wText,
                            startMs = currentSyllables.first().startMs,
                            endMs = currentSyllables.last().endMs,
                            syllables = currentSyllables
                        ))
                    }
                    currentSyllables = mutableListOf()
                }
            }
            if (currentSyllables.isNotEmpty()) {
                val wText = currentSyllables.joinToString("") { it.text }
                    .replace(HTML_TAG_REGEX, "")
                    .trim()
                if (wText.isNotEmpty()) {
                    words.add(LyricWord(
                        text = wText,
                        startMs = currentSyllables.first().startMs,
                        endMs = currentSyllables.last().endMs,
                        syllables = currentSyllables
                    ))
                }
            }

            val finalFullText = textBuilder.toString()
                .replace(HTML_TAG_REGEX, "")
                .trim()
            return Triple(finalFullText, rawSpans.map { it.first }, words)
        }

        fun parseLrc(lrc: String): List<LyricLineDto> {
            val lrcRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\](.*)""")
            val parsedLines = mutableListOf<LyricLineDto>()
            for (raw in lrc.lines()) {
                val trimmed = raw.trim()
                if (trimmed.isEmpty()) continue
                val match = lrcRegex.find(trimmed)
                if (match != null) {
                    val min = match.groupValues[1].toLongOrNull() ?: 0L
                    val sec = match.groupValues[2].toLongOrNull() ?: 0L
                    val msPart = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                    val totalMs = (min * 60 + sec) * 1000 + msPart
                    val text = match.groupValues[4].replace(HTML_TAG_REGEX, "").trim()
                    if (text.isNotEmpty()) {
                        parsedLines.add(LyricLineDto(
                            text = text,
                            startTicks = totalMs * 10_000L,
                            endTicks = null,
                            spans = emptyList(),
                            words = emptyList()
                        ))
                    }
                } else if (!trimmed.startsWith("[")) {
                    val cleanText = trimmed.replace(HTML_TAG_REGEX, "").trim()
                    if (cleanText.isNotEmpty()) {
                        parsedLines.add(LyricLineDto(
                            text = cleanText,
                            startTicks = null,
                            endTicks = null,
                            spans = emptyList(),
                            words = emptyList()
                        ))
                    }
                }
            }
            return parsedLines
        }

        fun parseTimeMs(time: String): Long {
            val clean = time.trim()
            if (clean.isEmpty()) return 0L
            return try {
                if (clean.contains(":")) {
                    val parts = clean.split(":")
                    when (parts.size) {
                        2 -> {
                            val m = parts[0].toLongOrNull() ?: 0L
                            val sParts = parts[1].split(".")
                            val s = sParts[0].toLongOrNull() ?: 0L
                            val ms = if (sParts.size > 1) sParts[1].padEnd(3, '0').take(3).toLongOrNull() ?: 0L else 0L
                            (m * 60 + s) * 1000L + ms
                        }
                        3 -> {
                            val h = parts[0].toLongOrNull() ?: 0L
                            val m = parts[1].toLongOrNull() ?: 0L
                            val sParts = parts[2].split(".")
                            val s = sParts[0].toLongOrNull() ?: 0L
                            val ms = if (sParts.size > 1) sParts[1].padEnd(3, '0').take(3).toLongOrNull() ?: 0L else 0L
                            ((h * 60 + m) * 60 + s) * 1000L + ms
                        }
                        else -> 0L
                    }
                } else {
                    val sec = clean.toDoubleOrNull() ?: 0.0
                    (sec * 1000.0).toLong()
                }
            } catch (_: Exception) {
                0L
            }
        }

        private fun unescapeXml(text: String): String {
            return text
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
        }
    }
}
