# Sonora (Mp3Player) 🎵

A sleek, modern, and lightweight offline music player for Android built with **Jetpack Compose**, **AndroidX Media3 (ExoPlayer)**, and **Room Database**.

<p align="center">
  <a href="https://github.com/aajli-Abdallah/Mp3Player/releases/tag/v1.2.3"><img src="https://img.shields.io/badge/version-1.2.3-brightgreen.svg" alt="Version 1.2.3" /></a>
  <img src="https://img.shields.io/badge/platform-Android%208.0%2B%20(API%2026%E2%80%9335)-green.svg" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-purple.svg" alt="Kotlin 2.0.21" />
  <img src="https://img.shields.io/badge/Compose-BOM%202024.12.01-blue.svg" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Media3-1.5.1-orange.svg" alt="Media3" />
  <img src="https://img.shields.io/badge/Database-Room%202.6.1-red.svg" alt="Room DB" />
</p>

---

## ✨ Features

### 🎧 Media3 ExoPlayer Playback Engine
- Background playback powered by AndroidX `MediaSessionService` with lockscreen and system notification controls.
- Seamless repeat, shuffle, and seeking with real-time audio duration synchronization.
- **One-Tap Shuffle Play**: Instant full-library shuffle or shuffle directly within active search results.

### 📊 Smart Song Ranking System
- Autonomous listening analytics with zero battery drain.
- Scoring rules: **+1 point** for a valid listen (30s or half short track), **+2 points** for repeated tracks, and **+2 points completion bonus** for full plays (≥90%).
- **Ranking Tab**: Leaderboard with quick smart playlists for **Top 10, Top 25, Top 50, Top 100**, **All Ranked**, or custom slider ranges.
- **Save as Playlist**: Freeze any Top N ranking snapshot into a full, editable user playlist with one tap.

### 📂 Playlists & Favorites (Room Database)
- Robust local storage (`sonora.db`) powered by Room & KSP.
- **Favorites**: Heart your favorite tracks directly from the song list, Now Playing screen, or playlist views.
- **Full Playlist Management**: Create, rename, delete, reorder tracks (up/down), and launch playback or shuffle.

### ⚙️ Song Action Menu & Inspection
- Contextual menu accessible across all screens: **Play Next**, **Add to Queue**, **Add to Playlist**, **Favorite**, **Hide Song**, and **Song Info**.
- **Song Info Inspector**: Detailed metadata analysis including file path, file size, bitrate, sample rate, and date added.

### 🎨 Themes & Adaptive Branding
- Edge-to-edge Material 3 UI with violet-to-pink gradient styling.
- **Theme Modes**: System default, Light, Dark, and true **AMOLED Black**.
- **Branding**: Adaptive launcher icon and themed monochrome icon featuring author initials **"AA"** drawn as sound-wave peaks across vinyl grooves.

### 🔒 Privacy, Deduplication & Backup
- **Smart Deduplication**: Eliminates duplicate audio tracks by Title or Title + Artist (configurable in Settings).
- **Hidden Songs**: Hide unwanted audio tracks with easy unhiding from Settings.
- **Backup & Restore**: Export all ratings, listening history, favorites, and playlists to a JSON file and restore anytime.
- **100% Offline & Private**: Zero internet permissions requested.

---

## 🏗️ Architecture & Tech Stack

```
app/src/main/java/com/yourname/mp3player/
├── core/
│   ├── AppInfo.kt               # App identity, author & contact details
│   └── ui/                      # App theme, palette, album art loader & About dialog
├── data/
│   ├── db/                      # Room Entities, DAOs (RankingDao, UserDataDao) & AppDatabase
│   ├── BackupManager.kt         # JSON backup export & restore
│   ├── DedupMusicRepository.kt  # Song deduplication & hidden file filtering
│   ├── MediaStoreMusicRepository.kt # MediaStore scanner & MediaScanner re-indexing
│   ├── RoomRankingRepository.kt # Ranking persistence & scoring
│   ├── RoomUserDataRepository.kt# Playlists & favorites persistence
│   └── SettingsRepository.kt    # SharedPreferences (theme, dedupe modes)
├── domain/                      # Models, playback contracts & deduplication rules
│   └── usecase/                 # RankingUseCases & SongDedupe logic
├── feature/
│   ├── home/                    # Bottom navigation (Songs | Playlists | Ranking)
│   ├── library/                 # Songs view, search filter, shuffle play button
│   ├── miniplayer/              # Floating mini-player bar
│   ├── nowplaying/              # Fullscreen player with gestures & ranking chips
│   ├── player/                  # Shared PlayerViewModel
│   ├── playlists/               # Playlists screen & playlist detail
│   ├── ranking/                 # Ranking leaderboard & "Save as playlist"
│   ├── settings/                # Settings screen (theme, dedupe, backup/restore)
│   └── songmenu/                # Universal contextual song actions
└── player/                      # Media3 PlaybackService & Media3PlayerController
```

- **Kotlin:** 2.0.21
- **UI:** Jetpack Compose (BOM `2024.12.01`), Material 3
- **Media Engine:** AndroidX Media3 `1.5.1`
- **Database:** AndroidX Room `2.6.1` with Google KSP `2.0.21-1.0.28`
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 15 (API 35)

---

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/aajli-Abdallah/Mp3Player.git
   cd Mp3Player
   ```
2. Open in **Android Studio (Koala or Ladybug+)**.
3. Let Gradle sync and press **Run**.

---

## 👤 Author & Copyright

**Designed and developed by AAJLI Abdallah**  
- ✉️ Email: [ingenieur.aajli@gmail.com](mailto:ingenieur.aajli@gmail.com)  
- 🐙 GitHub: [@aajli-Abdallah](https://github.com/aajli-Abdallah)  
- 🌐 Website: [aajli.com](https://aajli.com)  

```
Copyright (c) 2026 AAJLI Abdallah. All rights reserved.
```
