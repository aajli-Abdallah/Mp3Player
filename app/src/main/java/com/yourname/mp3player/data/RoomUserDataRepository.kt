package com.yourname.mp3player.data

import com.yourname.mp3player.data.db.FavoriteEntity
import com.yourname.mp3player.data.db.HiddenEntity
import com.yourname.mp3player.data.db.PlaylistEntity
import com.yourname.mp3player.data.db.UserDataDao
import com.yourname.mp3player.domain.Playlist
import com.yourname.mp3player.domain.UserDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RoomUserDataRepository(
    private val dao: UserDataDao,
    scope: CoroutineScope
) : UserDataRepository {

    override val favorites: StateFlow<List<Long>> =
        dao.observeFavorites().stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val hidden: StateFlow<Set<Long>> =
        dao.observeHidden().map { it.toSet() }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    override val playlists: StateFlow<List<Playlist>> =
        combine(dao.observePlaylists(), dao.observePlaylistSongs()) { pls, rows ->
            val byPlaylist = rows.groupBy { it.playlistId }
            pls.map { p ->
                Playlist(
                    p.id, p.name,
                    (byPlaylist[p.id] ?: emptyList()).sortedBy { it.position }.map { it.songId }
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun setFavorite(songId: Long, favorite: Boolean) {
        if (favorite) dao.insertFavorite(FavoriteEntity(songId, System.currentTimeMillis()))
        else dao.deleteFavorite(songId)
    }

    override suspend fun hide(songId: Long) = dao.insertHidden(HiddenEntity(songId))
    override suspend fun unhide(songId: Long) = dao.deleteHidden(songId)
    override suspend fun unhideAll() = dao.deleteAllHidden()

    override suspend fun createPlaylist(name: String): Long =
        dao.insertPlaylist(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))

    override suspend fun renamePlaylist(id: Long, name: String) = dao.renamePlaylist(id, name)
    override suspend fun deletePlaylist(id: Long) = dao.deletePlaylist(id)

    override suspend fun addToPlaylist(playlistId: Long, songIds: List<Long>) =
        dao.appendToPlaylist(playlistId, songIds)

    override suspend fun removeFromPlaylist(playlistId: Long, songId: Long) =
        dao.deletePlaylistSong(playlistId, songId)

    override suspend fun setPlaylistOrder(playlistId: Long, songIds: List<Long>) =
        dao.setPlaylistOrder(playlistId, songIds)
}
