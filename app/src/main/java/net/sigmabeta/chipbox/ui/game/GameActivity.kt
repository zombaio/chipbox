package net.sigmabeta.chipbox.ui.game

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.support.v7.widget.LinearLayoutManager
import android.util.Pair
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.activity_game.*
import net.sigmabeta.chipbox.BuildConfig
import net.sigmabeta.chipbox.R
import net.sigmabeta.chipbox.model.domain.Game
import net.sigmabeta.chipbox.model.domain.Track
import net.sigmabeta.chipbox.ui.BaseActivity
import net.sigmabeta.chipbox.ui.ItemListView
import net.sigmabeta.chipbox.util.TRANSITION_SLIDE
import net.sigmabeta.chipbox.util.loadImageHighQuality
import net.sigmabeta.chipbox.util.shrinktoNothing
import javax.inject.Inject

class GameActivity : BaseActivity<GamePresenter, GameView>(), GameView, ItemListView<GameTrackViewHolder> {
    lateinit var presenter: GamePresenter
        @Inject set

    var adapter = GameTrackListAdapter(this)

    var alreadyFinishing = false

    /**
     * GameView
     */

    override fun setGame(game: Game?) {
        adapter.game = game
        val imagePath = game?.artLocal

        if (imagePath != null) {
            image_hero_boxart.loadImageHighQuality(imagePath, false, false, getPicassoCallback())
        } else {
            image_hero_boxart.loadImageHighQuality(Game.PICASSO_ASSET_ALBUM_ART_BLANK, false, false, getPicassoCallback())
        }

        collapsing_toolbar.title = game?.title ?: "Error"
    }

    override fun setPlayingTrack(track: Track) {
        adapter.playingTrackId = track.id
    }

    override fun setTracks(tracks: MutableList<Track>) {
        adapter.dataset = tracks
    }

    /**
     * ItemListView
     */

    override fun onItemClick(position: Int, clickedViewHolder: GameTrackViewHolder) {
        if (position > 0L) {
            presenter.onItemClick(position - 1)
        }
    }

    /**
     * BaseActivity
     */

    override fun showLoading() = Unit

    override fun hideLoading() = Unit

    override fun inject() {
        getTypedApplication().appComponent.inject(this)
    }

    override fun getPresenterImpl(): GamePresenter {
        return presenter
    }

    override fun configureViews() {
        val layoutManager = LinearLayoutManager(this)

        list_tracks.adapter = adapter
        list_tracks.layoutManager = layoutManager
        list_tracks.clipChildren = false

        button_fab.setOnClickListener {
            presenter.onClick(it.id)
        }

        window.enterTransition = TRANSITION_SLIDE
        window.exitTransition = TRANSITION_SLIDE
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_game
    }

    override fun getContentLayout(): FrameLayout {
        return findViewById(android.R.id.content) as FrameLayout
    }

    override fun getSharedImage(): View? = image_hero_boxart

    override fun shouldDelayTransitionForFragment() = false

    /**
     * Activity
     */

    override fun onBackPressed() {
        if (!alreadyFinishing) {
            alreadyFinishing = true
            button_fab.shrinktoNothing().withEndAction {
                supportFinishAfterTransition()
            }
        }
    }

    companion object {
        val ACTIVITY_TAG = "${BuildConfig.APPLICATION_ID}.game"

        val ARGUMENT_GAME_ID = "${ACTIVITY_TAG}.game_id"

        fun launch(context: Context, gameId: String) {
            val launcher = Intent(context, GameActivity::class.java)

            launcher.putExtra(ARGUMENT_GAME_ID, gameId)

            context.startActivity(launcher)
        }

        fun launch(activity: Activity, gameId: String, sharedViewPairs: Array<Pair<View, String>>?) {
            val launcher = Intent(activity, GameActivity::class.java)

            launcher.putExtra(ARGUMENT_GAME_ID, gameId)

            val options = if (sharedViewPairs != null) {
                ActivityOptions.makeSceneTransitionAnimation(activity, *sharedViewPairs)
            } else {
                ActivityOptions.makeSceneTransitionAnimation(activity)
            }

            activity.startActivity(launcher, options.toBundle())
        }
    }
}