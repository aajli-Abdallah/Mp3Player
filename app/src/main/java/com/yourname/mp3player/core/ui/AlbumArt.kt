package com.yourname.mp3player.core.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yourname.mp3player.domain.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object ArtLoader {
    private val cache = LruCache<String, ImageBitmap>(50)
    private val missing = HashSet<String>()

    suspend fun load(context: Context, song: Song, px: Int): ImageBitmap? {
        val key = "${song.id}_$px"
        cache.get(key)?.let { return it }
        if (key in missing) return null
        val bmp = withContext(Dispatchers.IO) { runCatching { decode(context, song, px) }.getOrNull() }
        return if (bmp != null) {
            bmp.asImageBitmap().also { cache.put(key, it) }
        } else {
            missing.add(key); null
        }
    }

    private fun decode(context: Context, song: Song, px: Int): Bitmap? =
        if (Build.VERSION.SDK_INT >= 29) {
            context.contentResolver.loadThumbnail(song.uri, Size(px, px), null)
        } else {
            song.albumArtUri?.let { u ->
                context.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it) }
            }
        }
}

@Composable
fun AlbumArt(
    song: Song?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    iconSize: Dp = 24.dp,
    px: Int = 192
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = song?.id) {
        value = song?.let { ArtLoader.load(context, it, px) }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(bmp, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(
                Icons.Default.MusicNote, null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
