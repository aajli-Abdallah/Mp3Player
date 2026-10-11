# MP3 Player: PROGRESS TRACKER

App: Android MP3 player, Kotlin + Compose + Media3. Package: com.yourname.mp3player
Baseline v1.0.2 = working app before ranking. Current build: v1.1.0 (versionCode 3) = ranking system.

## Rules we follow
- Build in small milestones; every milestone must compile and run.
- UI never touches ExoPlayer: only via `PlayerController` (domain/Contracts.kt).
- New feature = new folder in `feature/`.
- Update this file at the end of every milestone.

## Stack
Kotlin 2.0.21, AGP 8.7.3, Gradle 8.9, Compose BOM 2024.12.01, Media3 1.5.1, minSdk 26, target 35.
No Hilt yet (manual `AppContainer` in App.kt). Swap later if wanted.

## Milestones
- [x] M1: Skeleton, permissions, MediaStore list, tap-to-play, background service + notification
- [x] M2: Now Playing screen (art, seek bar, position polling), Navigation Compose (slide-up)
- [x] M3: Shuffle + repeat (synced from player), search (title/artist/album)
- [x] Branding: name "Sonora", adaptive gradient waveform icon (+ themed monochrome), violet/pink theme (light+dark), rounded album art (loadThumbnail), floating mini-player with progress
- [x] Fix: MP3 scan now matches any *.mp3 / audio/mpeg|mp3 (not only IS_MUSIC), loading + empty state + Rescan button
- [x] v1.1.0 Ranking: points per song (+1 listen, +2 repeat), JSON persistence, Ranking tab, smart playlist Top 10/25/50/100/All (Play + Shuffle), bottom navigation (Songs | Ranking)
- [ ] Next small ideas: rank/points chip on Now Playing, reset ranking, custom Top N, ranking badge in Songs list
- [ ] M4: Notification small icon, queue view, error handling, edge cases (audio focus tests), release build
- [ ] v1.1: Playlists + favorites (Room)
- [ ] v1.2: Sleep timer, equalizer
- [ ] v1.3: Album/artist/folder views, widget
- [ ] v2.0: Lyrics, themes, backup

## Known gaps / TODO notes
- Fix applied: Rescan now indexes unindexed *.mp3 in Music/Download via MediaScannerConnection (files dropped in /sdcard/Music were invisible to MediaStore). Awaiting device confirmation.
- If still empty: add a file picker (SAF OpenMultipleDocuments) as fallback source.
- Package is still com.yourname.mp3player (rename before publishing).
- Notification uses Media3 default small icon.
- Seek bar duration for a song restored after app restart may be 0 until next track.
- gradle-wrapper.jar not included: open in Android Studio.

## File map
- domain/Models.kt: Song, RepeatMode, PlaybackState
- domain/Contracts.kt: MusicRepository, PlayerController
- data/MediaStoreMusicRepository.kt: scans MP3s
- player/PlaybackService.kt: Media3 MediaSessionService (ExoPlayer)
- player/Media3PlayerController.kt: PlayerController impl via MediaController
- feature/library/: LibraryScreen (header, search, list) + LibraryViewModel
- feature/nowplaying/: NowPlayingScreen
- feature/miniplayer/: MiniPlayer
- feature/player/: PlayerViewModel (shared) + formatTime
- core/ui/: Theme.kt (AppTheme), AlbumArt.kt (thumbnail loader + cache)
- AppNav.kt: routes library/player
- MainActivity.kt: permission gate (edge-to-edge)
- App.kt: AppContainer

## RESUME PROMPT (paste in a new chat with the project zip or files)
"We are building an Android MP3 player (see PROGRESS.md in the attached project). Read PROGRESS.md, then continue with the next unchecked milestone. Keep changes small, give only the changed/new files, and update PROGRESS.md at the end."


## Ranking system design (v1.1.0)
- Rules in ONE place: domain/usecase/RankingUseCases.kt -> RankingRules
  (listen = 30 s or half of a short song; +1 per listen; +2 if same song as the previous counted listen = repeat).
- Player (Media3PlayerController) only measures real listening time and emits ListenEvent via `events`.
- RankingRecorder (created in AppContainer, lives in app scope) turns events into points -> RankingRepository.
- Storage: data/JsonRankingRepository.kt (filesDir/ranking.json, keyed by MediaStore song id). Swap for Room later.
- Ranking.build(): points desc, then most recent play, then title; 0-point songs are not ranked.
- UI: feature/home/HomeScreen (tabs + mini-player), feature/ranking/ (RankingScreen, RankingViewModel, TopOption).
- Known limits: keyed by MediaStore id (if a file is re-indexed with a new id its points are lost); ranking is cleared on uninstall.
