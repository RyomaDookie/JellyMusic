# Graph Report - jellyfin-player  (2026-10-11)

## Corpus Check
- 25 files · ~29,239 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 19 file(s) not represented in the graph (top: .xml 12, .properties 2, (none) 1)

## Summary
- 607 nodes · 1381 edges · 33 communities (14 shown, 19 thin omitted)
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS · INFERRED: 2 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `dc1e82eb`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- JellyfinItem
- PlayerController
- PlaybackService.kt
- JellyMusicApp.kt
- LibraryScreen
- LoginScreen.kt
- LibraryScreen.kt
- MiniPlayer.kt
- HomeScreen
- PlayerScreen
- Composable
- SettingsScreen
- AppPreferences
- .onCreate
- AccentPalette
- UserCustomizations
- Offset
- Caratteristiche & Customizzazioni
- StreamingQuality
- ThemeMode
- SheetMode
- extractArtworkPalette

## God Nodes (most connected - your core abstractions)
1. `JellyfinItem` - 36 edges
2. `PlayerController` - 32 edges
3. `JellyfinClient` - 29 edges
4. `PlayerScreen()` - 21 edges
5. `LyricsService` - 19 edges
6. `UserCustomizations` - 19 edges
7. `LibraryScreen()` - 19 edges
8. `LyricLineDto` - 18 edges
9. `PlaybackService` - 18 edges
10. `AppPreferences` - 18 edges

## Surprising Connections (you probably didn't know these)
- `JellyMusicApp` --references--> `JellyfinClient`  [EXTRACTED]
  app/src/main/java/com/jellymusic/player/JellyMusicApp.kt → app/src/main/java/com/jellymusic/player/data/JellyfinClient.kt
- `JellyMusicApp` --references--> `LyricsService`  [EXTRACTED]
  app/src/main/java/com/jellymusic/player/JellyMusicApp.kt → app/src/main/java/com/jellymusic/player/data/LyricsService.kt
- `JellyMusicApp` --references--> `PlayerController`  [EXTRACTED]
  app/src/main/java/com/jellymusic/player/JellyMusicApp.kt → app/src/main/java/com/jellymusic/player/playback/PlayerController.kt
- `JellyMusicApp` --references--> `AppPreferences`  [EXTRACTED]
  app/src/main/java/com/jellymusic/player/JellyMusicApp.kt → app/src/main/java/com/jellymusic/player/preferences/AppPreferences.kt
- `MainNavigationHost()` --references--> `UserCustomizations`  [EXTRACTED]
  app/src/main/java/com/jellymusic/player/MainActivity.kt → app/src/main/java/com/jellymusic/player/preferences/AppPreferences.kt

## Import Cycles
- None detected.

## Communities (33 total, 19 thin omitted)

### Community 0 - "JellyfinItem"
Cohesion: 0.08
Nodes (17): JellyfinClient, AuthRequest, AuthResponse, ItemsResponse, JellyfinItem, LyricLineDto, LyricPhrase, LyricSpan (+9 more)

### Community 1 - "PlayerController"
Cohesion: 0.05
Nodes (3): ArtworkContentProvider, PlayerController, Listener

### Community 5 - "LibraryScreen"
Cohesion: 0.11
Nodes (28): PlaybackState, AdaptiveAlbumPairRow(), AdaptiveLibraryItem(), AdaptivePairContainer(), AdaptivePlaylistPairRow(), AdaptiveTrackPairRow(), AddToPlaylistDialog(), AnimatedEqualizer() (+20 more)

### Community 12 - "HomeScreen"
Cohesion: 0.28
Nodes (10): AppleIn(), ApplePlayBadge(), AppleTrackCard(), AppleTrackRowItem(), ArtworkAmbientGlow(), ContinueCard(), EqualizerBars(), HomeScreen() (+2 more)

### Community 14 - "PlayerScreen"
Cohesion: 0.22
Nodes (10): DragSample, formatMs(), lerpDp(), lerpFloat(), PlaybackSpeedDialog(), PlayerScreen(), pressScale(), q() (+2 more)

### Community 16 - "Composable"
Cohesion: 0.33
Nodes (6): BackgroundVoiceRow(), LyricLineItem(), LyricsPanel(), MainVoiceRow(), PillButton(), SyllableItem()

### Community 17 - "SettingsScreen"
Cohesion: 0.22
Nodes (4): BottomNavItem(), MainNavigationHost(), SectionHeader(), SettingsScreen()

### Community 21 - "AccentPalette"
Cohesion: 0.25
Nodes (7): AccentPalette, CYBERPUNK_CYAN, DYNAMIC, EMERALD_FOREST, JELLYFIN_PURPLE, SAKURA_PINK, SUNSET_AMBER

### Community 22 - "UserCustomizations"
Cohesion: 0.25
Nodes (6): PlayerLayoutStyle, COMPACT_CARD, MINIMALIST, MODERN_HERO, UserCustomizations, JellyMusicTheme()

### Community 23 - "Offset"
Cohesion: 0.32
Nodes (3): NestedScrollConnection, QueuePanel(), NestedScrollConnection

### Community 24 - "Caratteristiche & Customizzazioni"
Cohesion: 0.25
Nodes (7): Caratteristiche & Customizzazioni, Come aprire ed eseguire il progetto:, 🌐 Connessione a Server Jellyfin, 🎵 Funzionalità Audio & Riproduzione, JellyMusic - Android Jellyfin Music Player, 🎨 Personalizzazione Visiva & Temi, Requisiti e Compilazione

### Community 25 - "StreamingQuality"
Cohesion: 0.33
Nodes (5): StreamingQuality, DIRECT, HIGH_320, LOW_128, MEDIUM_192

### Community 26 - "ThemeMode"
Cohesion: 0.33
Nodes (5): ThemeMode, AMOLED, DARK, LIGHT, SYSTEM

### Community 27 - "SheetMode"
Cohesion: 0.50
Nodes (4): SheetMode, ART, LYRICS, QUEUE

## Knowledge Gaps
- **34 isolated node(s):** `SYSTEM`, `LIGHT`, `DARK`, `AMOLED`, `DYNAMIC` (+29 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 217 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **19 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `JellyMusicApp` connect `JellyMusicApp.kt` to `JellyfinItem`, `PlayerController`, `PlayerScreen.kt`, `PlaybackService.kt`, `LoginScreen.kt`, `HomeScreen.kt`, `SettingsScreen.kt`, `LibraryScreen.kt`, `MiniPlayer.kt`, `AppPreferences`?**
  _High betweenness centrality (0.157) - this node is a cross-community bridge._
- **What connects `SYSTEM`, `LIGHT`, `DARK` to the rest of the system?**
  _34 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `JellyfinItem` be split into smaller, more focused modules?**
  _Cohesion score 0.08397337429595494 - nodes in this community are weakly interconnected._
- **Why does `UserCustomizations` connect `UserCustomizations` to `PlayerScreen.kt`, `LibraryScreen`, `MainActivity.kt`, `HomeScreen.kt`, `SettingsScreen.kt`, `LibraryScreen.kt`, `HomeScreen`, `Theme.kt`, `PlayerScreen`, `SettingsScreen`, `AppPreferences.kt`, `AppPreferences`, `.onCreate`, `AccentPalette`, `StreamingQuality`, `ThemeMode`?**
  _High betweenness centrality (0.092) - this node is a cross-community bridge._
- **Should `PlayerController` be split into smaller, more focused modules?**
  _Cohesion score 0.05021173623714459 - nodes in this community are weakly interconnected._
- **Why does `JellyfinItem` connect `JellyfinItem` to `PlayerController`, `PlaybackService.kt`, `LibraryScreen`, `HomeScreen.kt`, `LibraryScreen.kt`, `HomeScreen`?**
  _High betweenness centrality (0.090) - this node is a cross-community bridge._
- **Should `PlayerScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.04081632653061224 - nodes in this community are weakly interconnected._