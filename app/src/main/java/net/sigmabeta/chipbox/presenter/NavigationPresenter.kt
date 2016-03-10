package net.sigmabeta.chipbox.presenter

import android.media.session.PlaybackState
import android.os.Bundle
import net.sigmabeta.chipbox.backend.Player
import net.sigmabeta.chipbox.model.events.PositionEvent
import net.sigmabeta.chipbox.model.events.StateEvent
import net.sigmabeta.chipbox.model.events.TrackEvent
import net.sigmabeta.chipbox.model.objects.Track
import net.sigmabeta.chipbox.util.logWarning
import net.sigmabeta.chipbox.view.activity.NavigationActivity
import net.sigmabeta.chipbox.view.interfaces.BaseView
import net.sigmabeta.chipbox.view.interfaces.NavigationView
import rx.android.schedulers.AndroidSchedulers
import javax.inject.Inject


class NavigationPresenter @Inject constructor(val player: Player) : ActivityPresenter() {
    var view: NavigationView? = null

    // A property is kept in order to be able to track changes in state.
    var state = player.state

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
        val fragmentTag = arguments?.getString(NavigationActivity.ARGUMENT_FRAGMENT_TAG)
        val fragmentArg = arguments?.getLong(NavigationActivity.ARGUMENT_FRAGMENT_ARG, -1)

        if (fragmentTag != null && fragmentArg != null) {
            view?.showFragment(fragmentTag, fragmentArg)
        }

        val title = arguments?.getString(NavigationActivity.ARGUMENT_TITLE)

        if (title != null) {
            view?.setTitle(title)
        }
    }

    override fun teardown() {
        state = -1
    }

    override fun updateViewState() {
        player.playingTrack?.let {
            displayTrack(it)
        }

        displayState(state, player.state)

        val subscription = player.updater.asObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        is TrackEvent -> displayTrack(it.track)
                        is PositionEvent -> { /* no-op */ }
                        is StateEvent -> displayState(state, it.state)
                        else -> logWarning("[PlayerFragmentPresenter] Unhandled ${it}")
                    }
                }

        subscriptions.add(subscription)
    }

    override fun setView(view: BaseView) {
        if (view is NavigationView) this.view = view
    }

    override fun clearView() {
        view = null
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

    private fun displayTrack(track: Track) {
        view?.setTrackTitle(track.title)
        view?.setArtist(track.artist)
        view?.setGameBoxart(track.gameId)
    }
}
