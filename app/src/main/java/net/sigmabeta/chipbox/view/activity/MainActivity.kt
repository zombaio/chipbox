package net.sigmabeta.chipbox.view.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_now_playing.*
import net.sigmabeta.chipbox.R
import net.sigmabeta.chipbox.dagger.injector.ActivityInjector
import net.sigmabeta.chipbox.presenter.ActivityPresenter
import net.sigmabeta.chipbox.presenter.MainPresenter
import net.sigmabeta.chipbox.util.*
import net.sigmabeta.chipbox.view.adapter.MainTabPagerAdapter
import net.sigmabeta.chipbox.view.interfaces.FragmentContainer
import net.sigmabeta.chipbox.view.interfaces.MainView
import net.sigmabeta.chipbox.view.interfaces.TopLevelFragment
import javax.inject.Inject

class MainActivity : BaseActivity(), MainView, FragmentContainer {
    lateinit var presenter: MainPresenter
        @Inject set

    var drawerToggle: ActionBarDrawerToggle? = null

    var pagerAdapter: MainTabPagerAdapter? = null

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Restore the drawer's state.
        drawerToggle?.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        // Persist the drawer's state.
        drawerToggle?.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle the case where the nav drawer icon was clicked.
        if (drawerToggle?.onOptionsItemSelected(item) ?: false) {
            return true
        }

        // If something else was clicked, handle it ourselves.
        return presenter.onOptionsItemSelected(item.itemId)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        presenter.onActivityResult(requestCode, resultCode)
    }

    override fun launchFileListActivity() {
        FileListActivity.launch(this)
    }

    override fun setActivityTitle(title: String) {
        setTitle(title)
    }

    override fun setTrackTitle(title: String) {
        text_playing_song_title.text = title
    }

    override fun setArtist(artist: String) {
        text_playing_song_artist.text = artist
    }

    override fun setGameBoxart(gameId: Long) {
        val imagesFolderPath = "file://" + getExternalFilesDir(null).absolutePath + "/images/"
        val imagePath = imagesFolderPath + gameId.toString() + "/local.png"

        image_playing_game_box_art.loadImageLowQuality(imagePath)
    }

    override fun showPauseButton() {
        fab_play_pause.setImageResource(R.drawable.ic_pause_white_48dp)
    }

    override fun showPlayButton() {
        fab_play_pause.setImageResource(R.drawable.ic_play_48dp)
    }

    override fun showNowPlaying(animate: Boolean) {
        coordinator_main.setPadding(0, 0, 0, resources.getDimension(R.dimen.height_now_playing).toInt())

        if (animate) {
            layout_now_playing.slideViewOnscreen()
        }
    }

    override fun hideNowPlaying(animate: Boolean) {
        coordinator_main.setPadding(0, 0, 0, 0)

        if (animate) {
            if (getFragment()?.isScrolledToBottom() ?: false) {
                coordinator_main.translationY = -(resources.getDimension(R.dimen.height_now_playing))
                coordinator_main.slideViewToProperLocation()
            }

            layout_now_playing.slideViewOffscreen().withEndAction {
                layout_now_playing.visibility = View.GONE
            }
        } else {
            layout_now_playing.visibility = View.GONE
        }
    }

    override fun launchPlayerActivity() {
        PlayerActivity.launch(this)
    }

    override fun launchScanActivity() {
        ScanActivity.launch(this)
    }

    override fun inject() {
        ActivityInjector.inject(this)
    }

    override fun getPresenter(): ActivityPresenter {
        return presenter
    }

    override fun configureViews() {
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        setUpNavigationDrawer()
        setUpViewPagerTabs()

        layout_now_playing.setOnClickListener { presenter.onNowPlayingClicked() }
        fab_play_pause.setOnClickListener { presenter.onPlayFabClicked() }
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun getContentLayout(): FrameLayout {
        return frame_content
    }

    private fun setUpNavigationDrawer() {
        drawerToggle = ActionBarDrawerToggle(this,
                layout_drawer,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close)

        layout_drawer.setDrawerListener(drawerToggle)

        drawer_navigation.setNavigationItemSelectedListener {
            return@setNavigationItemSelectedListener presenter.onOptionsItemSelected(it.itemId)
                    ?: false
        }
    }

    private fun setUpViewPagerTabs() {
        pagerAdapter = MainTabPagerAdapter(supportFragmentManager, this)
        pager_categories.adapter = pagerAdapter

        tabs_categories.setupWithViewPager(pager_categories)
    }

    private fun getFragment(): TopLevelFragment? {
        val selectedPosition = pager_categories.currentItem

        logVerbose("[MainActivity] Selected fragment position is $selectedPosition")
        val adapter = pagerAdapter

        if (adapter != null) {
            val fragment = adapter.fragments[selectedPosition]
            logDebug("[MainActivity] Selected fragment: ${fragment.getTitle()}")

            return fragment
        } else {
            return null
        }
    }
}

