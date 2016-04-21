package net.sigmabeta.chipbox.ui.main

import android.media.session.PlaybackState
import android.os.Bundle
import net.sigmabeta.chipbox.R
import net.sigmabeta.chipbox.backend.Player
import net.sigmabeta.chipbox.model.domain.Game
import net.sigmabeta.chipbox.model.domain.Track
import net.sigmabeta.chipbox.model.events.GameEvent
import net.sigmabeta.chipbox.model.events.PositionEvent
import net.sigmabeta.chipbox.model.events.StateEvent
import net.sigmabeta.chipbox.model.events.TrackEvent
import net.sigmabeta.chipbox.ui.ActivityPresenter
import net.sigmabeta.chipbox.ui.BaseView
import net.sigmabeta.chipbox.util.logWarning
import rx.android.schedulers.AndroidSchedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainPresenter @Inject constructor(val player: Player) : ActivityPresenter() {
    var view: MainView? = null

    var state = player.state

    var game: Game? = null

    fun onOptionsItemSelected(itemId: Int): Boolean {
        when (itemId) {
            R.id.drawer_add_folder -> view?.launchFileListActivity()
            R.id.drawer_refresh -> view?.launchScanActivity()
            R.id.drawer_settings -> view?.launchSettingsActivity()
        }

        return true
    }

    fun onNowPlayingClicked() {
        view?.launchPlayerActivity()
    }

    fun onPlayFabClicked() {
        when (player.state) {
            PlaybackState.STATE_PLAYING -> player.pause()

            PlaybackState.STATE_PAUSED -> player.play()

            PlaybackState.STATE_STOPPED -> player.play()
        }
    }

    override fun onReCreate(savedInstanceState: Bundle) {
    }

    override fun onTempDestroy() {
    }

    override fun setup(arguments: Bundle?) {
    }

    override fun teardown() {
        game = null
    }

    override fun updateViewState() {
        updateHelper()

        val subscription = player.updater.asObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        is TrackEvent -> displayTrack(it.track)
                        is PositionEvent -> {
                            /* no-op */
                        }
                        is GameEvent -> displayGame(it.game, false)
                        is StateEvent -> displayState(state, it.state)
                        else -> logWarning("[PlayerFragmentPresenter] Unhandled ${it}")
                    }
                }

        subscriptions.add(subscription)
    }

    override fun setView(view: BaseView) {
        if (view is MainView) this.view = view
    }

    override fun clearView() {
        view = null
    }

    override fun onReenter() {
        updateHelper()
    }

    private fun updateHelper() {
        player.playingTrack?.let {
            displayTrack(it)
        }

        player.playingGame?.let {
            displayGame(it, true)
        }

        displayState(state, player.state)
    }

    private fun displayState(oldState: Int, newState: Int) {
        when (newState) {
            PlaybackState.STATE_PLAYING -> {
                view?.showPauseButton()
                view?.showNowPlaying(oldState == PlaybackState.STATE_STOPPED)
            }

            PlaybackState.STATE_PAUSED -> {
                view?.showPlayButton()
                view?.showNowPlaying(oldState == PlaybackState.STATE_STOPPED)
            }

            PlaybackState.STATE_STOPPED -> {
                view?.hideNowPlaying(oldState != PlaybackState.STATE_STOPPED)
            }
        }

        this.state = newState
    }

    override fun getView(): BaseView? = view

    private fun displayTrack(track: Track) {
        view?.setTrackTitle(track.title.orEmpty())
        view?.setArtist(track.artistText.orEmpty())
    }

    private fun displayGame(game: Game?, force: Boolean) {
        if (force || this.game != game) {
            view?.setGameBoxArt(game?.artLocal, !force)
        }

        this.game = game
    }
}
