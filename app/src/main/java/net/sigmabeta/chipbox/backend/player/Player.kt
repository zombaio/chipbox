package net.sigmabeta.chipbox.backend.player

import android.content.Context
import android.media.AudioManager
import android.media.session.PlaybackState
import net.sigmabeta.chipbox.backend.Backend
import net.sigmabeta.chipbox.backend.BackendView
import net.sigmabeta.chipbox.backend.PlayerService
import net.sigmabeta.chipbox.backend.UiUpdater
import net.sigmabeta.chipbox.model.audio.AudioBuffer
import net.sigmabeta.chipbox.model.audio.AudioConfig
import net.sigmabeta.chipbox.model.events.PositionEvent
import net.sigmabeta.chipbox.model.events.StateEvent
import net.sigmabeta.chipbox.model.repository.Repository
import net.sigmabeta.chipbox.util.logError
import net.sigmabeta.chipbox.util.logInfo
import net.sigmabeta.chipbox.util.logVerbose
import java.util.concurrent.ArrayBlockingQueue
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class Player @Inject constructor(val playlist: Playlist,
                                 val audioConfig: AudioConfig,
                                 val audioManager: AudioManager,
                                 val repositoryProvider: Provider<Repository>,
                                 val updater: UiUpdater,
                                 val settings: Settings,
                                 val context: Context) : AudioManager.OnAudioFocusChangeListener {
    var backendView: BackendView? = null

    var state = PlaybackState.STATE_STOPPED
        set (value) {
            field = value
            updater.send(StateEvent(value))
        }

    var position = 0L

    var playbackTimePosition: Long = 0

    var focusLossPaused = false

    private var reader: Reader? = null
    private var writer: Writer? = null

    var backend: Backend? = null
        get() {
            return reader?.backend
        }

    fun start(trackId: String?) {
        if (state == PlaybackState.STATE_PLAYING) {
            logError("[Player] Received start command, but already PLAYING a track.")
            return
        }

        val resuming = (state == PlaybackState.STATE_PAUSED && trackId == null)

        val focusResult = requestAudioFocus()
        if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            state = PlaybackState.STATE_PLAYING

            if (backendView == null) {
                PlayerService.start(context)
            } else {
                backendView?.play()
            }

            val emptyBuffers = ArrayBlockingQueue<AudioBuffer>(READ_AHEAD_BUFFER_SIZE)
            val fullBuffers = ArrayBlockingQueue<AudioBuffer>(READ_AHEAD_BUFFER_SIZE)

            val firstTrackId = trackId ?: playlist.playingTrackId

            if (firstTrackId == null) {
                logError("Cannot start playback without a track ID.")
                return
            }

            Thread({
                writer = Writer(this,
                        audioConfig,
                        audioManager,
                        emptyBuffers,
                        fullBuffers)

                writer?.loop()
                writer = null
            }, "writer").start()

            Thread({
                reader = Reader(this,
                        playlist,
                        repositoryProvider.get(),
                        audioConfig,
                        emptyBuffers,
                        fullBuffers,
                        firstTrackId,
                        resuming)

                reader?.loop()
                reader = null
            }, "reader").start()

        } else {
            logError("[Player] Unable to gain audio focus.")
        }
    }

    fun play(playbackQueue: MutableList<String?>, position: Int) {
        if (position < playbackQueue.size) {
            logVerbose("[Player] Playing new playlist, starting from track ${position} of ${playbackQueue.size}.")

            playlist.playbackQueue = playbackQueue
            playlist.playbackQueuePosition = position

            val trackId = playlist.getTrackIdAt(position, true)

            reader?.let {
                it.queuedTrackId = trackId
            } ?: let {
                start(trackId)
            }
        } else {
            logError("[Player] Tried to start new playlist, but invalid track number: ${position} of ${playbackQueue.size}")
        }
    }

    fun play(position: Int) {
        // TODO This is a bad design, see comment below
        // Don't use getTrackAt() here because we don't want shuffle to affect explicit user input
//            val trackId = playbackQueue.get(position)
//            queuedTrackId = trackId

        val trackId = playlist.getTrackIdAt(position, true)

        reader?.let {
            it.queuedTrackId = trackId
        } ?: let {
            start(trackId)
        }
    }

    fun skipToNext() {
        val nextTrack = playlist.getNextTrack()
        reader?.let {
            it.queuedTrackId = nextTrack
        } ?: let {
            start(nextTrack)
        }

        backendView?.skipToNext()
    }

    fun skipToPrev() {
        if (playbackTimePosition > 3000) {
            seek(0)
        } else {
            if (playlist.playbackQueuePosition > 0) {
                val prevTrack = playlist.getPrevTrack()
                reader?.let {
                    it.queuedTrackId = prevTrack
                } ?: let {
                    start(prevTrack)
                }
            } else {
                seek(0)
            }
        }
    }

    fun pause() {
        if (state != PlaybackState.STATE_PLAYING) {
            logError("[Player] Received pause command, but not currently PLAYING.")
            return
        }

        logVerbose("[Player] Pausing playback.")

        state = PlaybackState.STATE_PAUSED

        backendView?.pause()
    }

    fun stop() {
        if (state == PlaybackState.STATE_STOPPED) {
            logError("[Player] Received stop command, but already STOPPED.")
            return
        }

        logVerbose("[Player] Stopping playback.")

        state = PlaybackState.STATE_STOPPED

        audioManager.abandonAudioFocus(this)

        backendView?.stop()
    }

    fun seek(seekPosition: Long) {
        reader?.queuedSeekPosition = seekPosition
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                logVerbose("[Player] Focus lost. Pausing...")
                pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                logVerbose("[Player] Focus lost temporarily. Pausing...")

                if (state == PlaybackState.STATE_PLAYING) {
                    focusLossPaused = true
                }

                pause()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                logVerbose("[Player] Focus lost temporarily, but can duck. Lowering volume...")
                writer?.ducking = true
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                logVerbose("[Player] Focus gained. Resuming...")

                writer?.ducking = false

                if (focusLossPaused) {
                    start(null)
                    focusLossPaused = false
                }
            }
        }
    }

    private fun requestAudioFocus(): Int {
        return audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN)
    }

    /**
     * New functions from the refactor
     * TODO Prune / Move these
     */

    fun onPlaybackPositionUpdate(millisPlayed: Long) {
        updater.send(PositionEvent(millisPlayed))
    }

    /**
     * Internal Events
     */

    fun onTrackChange(trackId: String?, gameId: String?) {
        settings.onTrackChange()
        playlist.playingTrackId = trackId
        playlist.playingGameId = gameId
    }

    fun onPlaylistFinished() {
        logInfo("[Player] No more tracks to start.")
        stop()
    }

    /**
     * Error Handlers
     */

    fun errorReadFailed(error: String) {
        logError("[Player] GME Error: ${error}")
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
//        stop()
    }

    fun errorAllBuffersFull() {
        logError("[Player] Couldn't get an empty AudioBuffer after ${TIMEOUT_BUFFERS_FULL_MS}ms.")
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        //stop()
    }


    companion object {
        val ERROR_AUDIO_TRACK_NULL = -100

        val READ_AHEAD_BUFFER_SIZE = 2

        val REPEAT_OFF = 0
        val REPEAT_ALL = 1
        val REPEAT_ONE = 2
        val REPEAT_INFINITE = 3

        val TIMEOUT_BUFFERS_FULL_MS = 1000L
    }
}