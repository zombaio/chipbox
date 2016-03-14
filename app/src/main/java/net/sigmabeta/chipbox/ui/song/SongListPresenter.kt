package net.sigmabeta.chipbox.ui.song

import android.os.Bundle
import net.sigmabeta.chipbox.backend.Player
import net.sigmabeta.chipbox.dagger.scope.ActivityScoped
import net.sigmabeta.chipbox.model.database.SongDatabaseHelper
import net.sigmabeta.chipbox.model.objects.Game
import net.sigmabeta.chipbox.model.objects.Track
import net.sigmabeta.chipbox.ui.BaseView
import net.sigmabeta.chipbox.ui.FragmentPresenter
import net.sigmabeta.chipbox.util.logInfo
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*
import javax.inject.Inject

@ActivityScoped
class SongListPresenter @Inject constructor(val database: SongDatabaseHelper,
                                            val player: Player) : FragmentPresenter() {
    var view: SongListView? = null

    var artistId = Track.PLATFORM_ALL.toLong()

    var songs: ArrayList<Track>? = null

    var gameMap: HashMap<Long, Game>? = null

    fun onItemClick(position: Long) {
        songs?.let {
            player.play(it, position.toInt())
        }
    }

    /**
     * FragmentPresenter
     */

    override fun setup(arguments: Bundle?) {
        artistId = arguments?.getLong(SongListFragment.ARGUMENT_ARTIST) ?: -1

        val readOperation = if (artistId == Track.PLATFORM_ALL.toLong()) {
            database.getSongList()
        } else {
            database.getSongListForArtist(artistId)
        }

        val subscription = readOperation
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            logInfo("[SongListPresenter] Loaded ${it.size} tracks.")

                            songs = it
                            view?.setSongs(it)
                            loadGames(it)
                        },
                        {
                            view?.showErrorSnackbar("Error: ${it.message}", null, null)
                        }
                )


        subscriptions.add(subscription)
    }

    override fun onReCreate(savedInstanceState: Bundle) {
    }

    override fun teardown() {
        artistId = -1
        songs = null
        gameMap = null
    }

    override fun updateViewState() {
        songs?.let {
            view?.setSongs(it)
        }

        gameMap?.let {
            view?.setGames(it)
        }
    }

    override fun setView(view: BaseView) {
        if (view is SongListView) this.view = view
    }

    override fun clearView() {
        view = null
    }

    private fun loadGames(tracks: ArrayList<Track>) {
        val subscription = database.getGamesForTrackCursor(tracks)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            logInfo("[SongListPresenter] Loaded ${it.size} games.")
                            gameMap = it
                            view?.setGames(it)
                        }
                )

        subscriptions.add(subscription)
    }
}