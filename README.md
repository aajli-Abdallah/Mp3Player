# Sonora (Mp3Player) 🎵

A modern, lightweight offline MP3 & audio player for Android built with **Jetpack Compose** and **AndroidX Media3 (ExoPlayer)**.

[![Version](https://img.shields.io/badge/version-1.1.0-brightgreen.svg)](#version-history)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.12.01-blue.svg)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Media3%20ExoPlayer-1.5.1-orange.svg)](https://developer.android.com/media/media3)

---

## ✨ Features

- 🎧 **Media3 ExoPlayer Engine**: Seamless background audio playback via Android `MediaSessionService` with system notification controls.
- 📊 **Smart Song Ranking System (v1.1.0)**:
  - Tracks listening habits with zero battery drain.
  - Earns **+1 point** per valid listen (30 seconds or half track length) and **+2 points** on repeat.
  - Dedicated **Ranking Tab** with instant smart playlists: **Top 10, Top 25, Top 50, Top 100**, and **All Ranked**.
  - One-tap *Play* and *Shuffle* for ranked playlists.
- 🎨 **Modern Material 3 UI**:
  - Edge-to-edge support with custom violet/pink theme (Light & Dark modes).
  - Floating mini-player with live playback progress and tap-to-expand.
  - Full Now Playing screen with album art thumbnailing (`loadThumbnail`), seek bar, shuffle, and repeat controls.
- 🔍 **Instant Search & Rescan**:
  - Filter songs dynamically by title, artist, or album.
  - Automatic `MediaStore` audio scanning with deep manual re-indexing for files dropped into storage.
- 🖼️ **Adaptive Branding**:
  - Custom gradient waveform launcher icon and themed monochrome icon for Android 13+.

---

## 🏗️ Architecture & Tech Stack

The app follows clean architecture patterns separating playback service, domain logic, and Compose UI:

```
app/src/main/java/com/yourname/mp3player/
├── core/ui/             # App theme, colors, typography, album art loader
├── data/                # MediaStore music scanner & JSON ranking persistence
├── domain/              # Song & Playback contracts, domain models, ranking rules
│   └── usecase/         # RankingUseCases (listening rules & scoring)
├── feature/
│   ├── home/            # Bottom navigation (Songs / Ranking tabs)
│   ├── library/         # Song list, search bar, empty/loading states
│   ├── miniplayer/      # Persistent bottom mini-player bar
│   ├── nowplaying/      # Slide-up now playing screen
│   ├── player/          # Shared PlayerViewModel & playback formatting
│   └── ranking/         # Ranking leaderboard & smart playlist generators
└── player/              # Media3 PlaybackService & Media3PlayerController
```

- **Language:** Kotlin 2.0.21
- **UI Framework:** Jetpack Compose (BOM 2024.12.01), Material 3
- **Media Engine:** AndroidX Media3 (ExoPlayer + Session) 1.5.1
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 15 (API 35)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- JDK 17
- Android SDK with Platform 35 installed

### Build & Run
1. Clone this repository:
   ```bash
   git clone https://github.com/aajli-Abdallah/Mp3Player.git
   ```
2. Open the project folder in Android Studio.
3. Allow Gradle to sync dependencies.
4. Run on a physical Android device or emulator running API 26+.

---

## 📋 Release Notes

### [v1.1.0] - 2026-10-12
- **Smart Ranking System**: Added listening points tracking (+1 per listen, +2 on repeat) persisted locally in JSON.
- **Ranking Screen**: Added dedicated Ranking tab with Top 10, Top 25, Top 50, Top 100, and All smart playlists.
- **Bottom Navigation**: Tab navigation between Song Library and Ranking.
- **Scanner Improvements**: Added forced rescan via `MediaScannerConnection` for newly downloaded/transferred audio files.
- **UI Polish**: Floating mini-player, adaptive waveform icon, and responsive album art rendering.

---

## 📄 License
This project is open source and available under the standard MIT License.
