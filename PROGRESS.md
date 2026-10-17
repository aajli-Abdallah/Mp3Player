# MP3 Player: PROGRESS TRACKER

App: Android MP3 player, Kotlin + Compose + Media3. Package: com.yourname.mp3player
Baseline v1.0.2 = working app before ranking. Current build: v1.2.3 (versionCode 10) = Room, favorites, playlists, song menu, backup, settings.

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
## ROADMAP (keep this list up to date)

### v1.1.1: small ranking follow-ups (DONE)
- [x] Rank + points chip on the Now Playing screen (e.g. "#3 . 12 pts")
- [x] Custom "Top N" (slider/number) in the Top Ranked card
- [x] Reset ranking (one song / everything) with confirmation
- [x] Rank badge or points in the Songs list

### v1.2: user playlists (Room)
- [ ] Migrate ranking JSON -> Room (one-time import), add playlists + favorites tables
- [ ] Create / rename / delete playlists, add-to-playlist from any song
- [ ] Favorites (heart) + "Favorites" smart playlist

### v1.3: player power features
- [ ] Queue screen (Up Next, reorder, remove, play next)
- [ ] Sleep timer, playback speed
- [ ] Equalizer + bass boost
- [ ] M4 leftovers: notification small icon, edge cases, error handling

### v1.4: browsing
- [ ] Albums / Artists / Folders views
- [ ] Home-screen widget (Glance)

### v2.0.0: "Sonora gets smart" (headline release)
- [ ] Listening history (every listen stored with date) -> stats screen:
      most played today / this week / this month / all time, ranking per period
- [ ] Smart playlists: Top Ranked (done), Rediscover (loved but not played for 30 days),
      Never played, Recently added, This week's top, Late-night mix
- [ ] Achievements + levels (e.g. 100 listens, 7-day streak, "first song at 50 pts")
- [ ] Sonora Wrapped: yearly / monthly recap card (top songs, total hours, top artist), shareable image
- [ ] Backup / restore: export-import ranking + playlists as a file, so reinstall never loses points
- [ ] Lyrics (embedded ID3 + .lrc files, synced scrolling)
- [ ] Dynamic theme from album art + theme picker (incl. AMOLED black)
- [ ] Rename package (com.yourname -> final), release signing, Play-Store-ready icon/screenshots

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

## v1.1.1 notes
- AppContainer.rankedSongs = single shared ranking StateFlow (used by Songs list, Now Playing chip, Ranking tab).
- RankingRepository gained reset(songId) / resetAll().
- Ranking tab: Custom chip + slider, per-row menu "Reset points", header button "Reset all" (both with confirmation).

## v1.1.2 notes (user remarks after testing)
- Duplicates: only ONE song per title (normalized: case/spaces, trailing "(1)" ignored). Winner = most points, then longest, then oldest id.
  Implemented as decorator data/DedupMusicRepository + domain/usecase/SongDedupe (rest of app unchanged).
  Caveat: same title by different artists is merged. Idea: setting to dedupe by title+artist.
- Ranking rule added: completion bonus +2 (RankingRules.POINTS_COMPLETION) when a song reaches its natural end
  after >= 90% real listening time. Full listen of a new song = 1 + 2 = 3 pts; repeated + completed = 2 + 2 = 4.
  Player emits ListenEvent(type = LISTEN | COMPLETION); SongStats/RankedSong now carry `completions`
  (saved as "done" in ranking.json, old files still load).

## v1.1.3 notes
- Copyright/About: core/AppInfo.kt (author, email, GitHub, website: edit there), core/ui/AboutDialog.kt,
  opened by the (i) icon in the Songs header; copyright line on the permission screen; COPYRIGHT.md at project root.
- "Songs play like 2x" report. Audit: no code sets speed/pitch; single ExoPlayer + single MediaController.
  Hardening: (1) progress bar / rules now use the player's REAL duration (syncDuration) instead of MediaStore's,
  (2) playback speed reset to 1.0 on every play().
  STILL OPEN: if audio itself is fast, cause is outside app logic (emulator audio / specific MP3 header or sample rate).
  Diagnosis steps given to user: compare Now Playing timer with a real stopwatch; try another music app; note which files.

## v1.2.0 (CODE COMPLETE, awaiting first build/test) : "Your library, your control"
Plan: Room+history, favorites, song menu (+Hide), playlists, backup/restore, settings.
- [x] PASS 1 (data layer): Room DB (sonora.db): song_stats, listen_history, favorites, hidden_songs, playlists, playlist_songs.
      KSP 2.0.21-1.0.28 + Room 2.6.1. ranking.json auto-migrated (renamed .migrated). history row written on every listen/completion.
      RoomRankingRepository, RoomUserDataRepository, SettingsRepository (theme, dedupe mode), DedupMusicRepository (dedupe then hide),
      BackupManager (JSON export/import, songs matched by title+artist), PlayerController.playNext/addToQueue.
- [x] PASS 2 (UI): theme modes (System/Light/Dark/AMOLED), song menu (feature/songmenu: play next/queue/playlist/favorite/info/hide), Library rewrite (heart icon, menu, settings gear), heart on Now Playing, Playlists tab + Settings route wired
- [x] PASS 3 (UI): Playlists tab (Favorites + user playlists, create/rename/delete, reorder up/down, remove, play/shuffle), Settings screen (theme, duplicate mode, hidden songs, export/restore backup, reset ranking, about)

### v1.2.0 file map (new)
- data/db/: Entities.kt, Daos.kt (RankingDao, UserDataDao), AppDatabase.kt (sonora.db v1)
- data/: RoomRankingRepository, RoomUserDataRepository, SettingsRepository (SharedPreferences), DedupMusicRepository (dedupe THEN hide), BackupManager
- domain/usecase/SongDedupe.kt (modes TITLE / TITLE_ARTIST / OFF, fingerprint for backup)
- feature/songmenu/ (SongActionsViewModel + SongMenuButton), feature/playlists/, feature/settings/
- Tabs: Songs | Playlists | Ranking. Settings = gear icon in Songs header (nav route "settings").
### v1.2.0 first-build checklist (things I could NOT compile-check)
- Gradle sync: KSP plugin 2.0.21-1.0.28 + Room 2.6.1 (if KSP version error: pick the KSP release matching Kotlin 2.0.21)
- Old ranking.json must import automatically (ranking.json -> ranking.json.migrated)
- Favorites heart, song menu, playlists CRUD, hide/unhide, backup export + restore, theme switch (AMOLED), duplicate mode switch
### Next (v1.3 ideas)
- Queue screen, sleep timer, playback speed, equalizer; sorting/filters; choose-music-folder
- Use listen_history for v2.0 stats / Wrapped

## v1.2.1: playlists everywhere (review of the "own playlists" feature)
- Review result: create / rename / delete / reorder (up-down) / play / shuffle were already in v1.2.0; GAP = "add from anywhere"
  (song menu only existed in the Songs list).
- SongMenuButton now has an optional `extra` slot and is used on: Songs list, Ranking rows (+ "Reset points"),
  Now Playing (next to the heart), playlist + Favorites detail rows.
- Ranking tab: "Save as playlist" (snapshot of Top 10/25/50/100/All/Custom as a real editable playlist) -> RankingViewModel.saveAsPlaylist.
- UserDataDao.deletePlaylist is now an explicit @Transaction (songs first, then playlist).
- Ideas not done: drag-to-reorder (currently up/down arrows), multi-select add, playlist cover mosaic, "save current queue as playlist".

## v1.2.2: song menu review
- Menu (SongMenu.kt) = Play next, Add to queue, Add to playlist, favorite toggle, Song info, Hide (undo in Settings).
  Used on: Songs list, Ranking rows (+Reset points), Now Playing, playlist/Favorites rows. Not on mini-player (by design).
- Song info now also shows file name, size, bitrate, sample rate (Android 12+ only), date added.
  Purpose: diagnose the "plays like 2x" report (bitrate / sample rate of the file).
- Known caveat: "Play next" with SHUFFLE on inserts at a random spot (ExoPlayer shuffle order), not strictly next.
- Idea: a Queue screen (v1.3) would make Play next / Add to queue visible and editable.

## v1.2.3: shuffle play + new icon
- Songs tab: full-width "Shuffle play" button under the search bar (plays what is shown; "Shuffle results" when searching).
  LibraryViewModel.shufflePlay(): shuffle ON + random start index.
- New app icon based on the author's initials: "AA" drawn as two sound-wave peaks on the violet->pink gradient with vinyl
  grooves behind. Adaptive icon: ic_launcher_background / _foreground / _monochrome (themed icons). Edit the pathData to tweak.
  Store-listing PNG: Sonora_icon_512.png (delivered next to the zip; not used by the app build).
