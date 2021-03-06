package net.sigmabeta.chipbox.ui.main

import android.media.session.PlaybackState
import android.os.Bundle
import net.sigmabeta.chipbox.R
import net.sigmabeta.chipbox.backend.UiUpdater
import net.sigmabeta.chipbox.backend.player.Player
import net.sigmabeta.chipbox.backend.player.Playlist
import net.sigmabeta.chipbox.model.domain.Game
import net.sigmabeta.chipbox.model.events.*
import net.sigmabeta.chipbox.model.repository.LibraryScanner
import net.sigmabeta.chipbox.ui.ActivityPresenter
import net.sigmabeta.chipbox.util.logError
import net.sigmabeta.chipbox.util.logWarning
import rx.android.schedulers.AndroidSchedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainPresenter @Inject constructor(val player: Player,
                                        val scanner: LibraryScanner,
                                        val playlist: Playlist,
                                        val updater: UiUpdater) : ActivityPresenter<MainView>() {
    var game: Game? = null

    fun onOptionsItemSelected(itemId: Int): Boolean {
        when (itemId) {
            R.id.drawer_refresh -> view?.startScanner()
            R.id.drawer_settings -> view?.launchSettingsActivity()
            R.id.drawer_help -> view?.launchOnboarding()
        }

        return true
    }

    fun onNowPlayingClicked() {
        view?.launchPlayerActivity()
    }

    fun onPlayFabClicked() {
        when (player.state) {
            PlaybackState.STATE_PLAYING -> player.pause()

            PlaybackState.STATE_PAUSED -> player.start(null)

            PlaybackState.STATE_STOPPED -> player.start(null)
        }
    }

    override fun setup(arguments: Bundle?) {
        needsSetup = false
    }

    override fun onReCreate(arguments: Bundle?, savedInstanceState: Bundle) = Unit

    override fun onTempDestroy() = Unit

    override fun teardown() {
        game = null
    }

    override fun updateViewState() {
        updateHelper()

        val subscription = updater.asObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        is TrackEvent -> displayTrack(it.trackId, true)
                        is PositionEvent -> {
                            /* no-op */
                        }
                        is GameEvent -> displayGame(it.gameId, false)
                        is StateEvent -> displayState(it.state)
                        is FileScanEvent -> view?.showScanning(it.type, it.name)
                        is FileScanFailedEvent -> {
                            view?.showFileScanError(it.reason)
                            view?.hideScanning()
                        }
                        is FileScanCompleteEvent -> {
                            view?.showFileScanSuccess(it.newTracks, it.updatedTracks)
                            view?.hideScanning()
                        }
                        else -> logWarning("[PlayerFragmentPresenter] Unhandled ${it}")
                    }
                }

        subscriptions.add(subscription)
    }

    override fun onClick(id: Int) = Unit

    override fun onReenter() {
        updateHelper()
    }

    private fun updateHelper() {
        playlist.playingTrackId?.let {
            displayTrack(it, false)
        }

        playlist.playingGameId?.let {
            displayGame(it, true)
        }

        displayState(player.state)
    }

    private fun displayState(newState: Int) {
        when (newState) {
            PlaybackState.STATE_PLAYING -> {
                view?.showPauseButton()
                view?.showNowPlaying()
            }

            PlaybackState.STATE_PAUSED -> {
                view?.showPlayButton()
                view?.showNowPlaying()
            }

            PlaybackState.STATE_STOPPED -> {
                view?.hideNowPlaying()
            }
        }

        if (scanner.state == LibraryScanner.STATE_SCANNING) {
            view?.showScanning(null, null)
        } else {
            view?.hideScanning()
        }
    }

    private fun displayTrack(trackId: String?, animate: Boolean) {
        if (trackId != null) {
            val track = repository.getTrackSync(trackId)

            if (track != null) {
                view?.setTrackTitle(track.title.orEmpty(), animate)
                view?.setArtist(track.artistText.orEmpty(), animate)
            } else {
                logError("Cannot load track with id $trackId")
            }
        }
    }

    private fun displayGame(gameId: String?, force: Boolean) {
        if (gameId != null) {
            val game = repository.getGameSync(gameId)

            if (force || this.game !== game) {
                view?.setGameBoxArt(game?.artLocal, !force)
            }

            this.game = game
        }
    }
}
