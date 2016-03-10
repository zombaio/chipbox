package net.sigmabeta.chipbox.presenter

import android.database.Cursor
import android.os.Bundle
import net.sigmabeta.chipbox.backend.Player
import net.sigmabeta.chipbox.dagger.scope.FragmentScoped
import net.sigmabeta.chipbox.model.database.SongDatabaseHelper
import net.sigmabeta.chipbox.model.objects.Game
import net.sigmabeta.chipbox.model.objects.Track
import net.sigmabeta.chipbox.util.logInfo
import net.sigmabeta.chipbox.view.fragment.SongListFragment
import net.sigmabeta.chipbox.view.interfaces.BaseView
import net.sigmabeta.chipbox.view.interfaces.SongListView
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*
import javax.inject.Inject

@FragmentScoped
class SongListPresenter @Inject constructor(val database: SongDatabaseHelper,
                                            val player: Player) : FragmentPresenter() {
    var view: SongListView? = null

    var artist = Track.PLATFORM_ALL.toLong()

    var songs: Cursor? = null

    var gameMap: HashMap<Long, Game>? = null

    fun loadGames(tracks: Cursor) {
        val subscription = database.getGamesForTrackCursor(tracks)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            logInfo("[SongListPresenter] Loaded ${it.size} games.")
                            gameMap = it
                            view?.setCursor(tracks)
                        }
                )

        subscriptions.add(subscription)
    }

    fun onItemClick(track: Track, position: Int) {
        val cursor = view?.getCursor()

        if (cursor != null) {
            val queue = SongDatabaseHelper.getPlaybackQueueFromCursor(cursor)
            player.play(queue, position)
        }
    }

    fun getImagePath(gameId: Long): String? {
        val game = gameMap?.get(gameId)

        return game?.artLocal
    }

    /**
     * FragmentPresenter
     */

    override fun setup(arguments: Bundle?) {
        artist = arguments?.getLong(SongListFragment.ARGUMENT_ARTIST) ?: -1

        val readOperation = if (artist == Track.PLATFORM_ALL.toLong()) {
            database.getSongList()
        } else {
            database.getSongListForArtist(artist)
        }

        val subscription = readOperation.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            logInfo("[SongListPresenter] Loaded ${it.count} tracks.")

                            songs = it
                            loadGames(it)
                        }
                )

        subscriptions.add(subscription)
    }

    override fun onReCreate(savedInstanceState: Bundle) {
    }

    override fun teardown() {
        artist = -1
        songs = null
        gameMap = null
    }

    override fun updateViewState() {
        val cursor = songs
        if (cursor != null) {
            loadGames(cursor)
        }
    }

    override fun setView(view: BaseView) {
        if (view is SongListView) this.view = view
    }

    override fun clearView() {
        view = null
    }
}