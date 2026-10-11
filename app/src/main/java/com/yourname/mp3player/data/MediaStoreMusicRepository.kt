package com.yourname.mp3player.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.yourname.mp3player.domain.MusicRepository
import com.yourname.mp3player.domain.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

class MediaStoreMusicRepository(private val context: Context) : MusicRepository {

    private val songs = MutableStateFlow<List<Song>>(emptyList())

    override fun getSongs(): Flow<List<Song>> = songs.asStateFlow()

    override suspend fun refresh() {
        // 1) Register MP3 files that exist on disk but are not yet in MediaStore
        indexNewFiles()
        // 2) Read the (now up to date) MediaStore
        songs.value = withContext(Dispatchers.IO) { query() }
    }

    /** Finds *.mp3 in Music/Download and asks Android to index the ones it doesn't know yet. */
    private suspend fun indexNewFiles() {
        val toScan = withContext(Dispatchers.IO) {
            val known = knownPaths()
            val roots = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            )
            roots.flatMap { dir -> findMp3(dir) }
                .map { it.absolutePath }
                .filter { it !in known }
                .distinct()
                .take(1000)
        }
        if (toScan.isEmpty()) return
        withTimeoutOrNull(20_000) {
            suspendCancellableCoroutine<Unit> { cont ->
                val remaining = AtomicInteger(toScan.size)
                MediaScannerConnection.scanFile(context, toScan.toTypedArray(), null) { _, _ ->
                    if (remaining.decrementAndGet() <= 0 && cont.isActive) cont.resume(Unit)
                }
            }
        }
    }

    private fun findMp3(dir: File): List<File> = runCatching {
        dir.walkTopDown().maxDepth(6)
            .filter { it.isFile && it.extension.equals("mp3", ignoreCase = true) }
            .toList()
    }.getOrDefault(emptyList())

    @Suppress("DEPRECATION")
    private fun knownPaths(): Set<String> {
        val out = HashSet<String>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media.DATA), null, null, null
        )?.use { c ->
            val i = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (c.moveToNext()) c.getString(i)?.let { out += it }
        }
        return out
    }

    private fun query(): List<Song> {
        val list = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )
        // Accept any .mp3 (some files are not flagged IS_MUSIC or have an odd mime type)
        val selection =
            "(${MediaStore.Audio.Media.MIME_TYPE} IN (?, ?) OR ${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ?)"
        val args = arrayOf("audio/mpeg", "audio/mp3", "%.mp3")
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, args, sort
        )?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val album = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (c.moveToNext()) {
                val songId = c.getLong(id)
                list += Song(
                    id = songId,
                    title = c.getString(title) ?: "Unknown",
                    artist = c.getString(artist)?.takeUnless { it == "<unknown>" } ?: "Unknown artist",
                    album = c.getString(album) ?: "Unknown album",
                    durationMs = c.getLong(dur),
                    uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId
                    ),
                    albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), c.getLong(albumId)
                    )
                )
            }
        }
        return list
    }
}
