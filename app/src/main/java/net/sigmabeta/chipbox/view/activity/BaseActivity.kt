package net.sigmabeta.chipbox.view.activity

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import net.sigmabeta.chipbox.ChipboxApplication
import net.sigmabeta.chipbox.presenter.ActivityPresenter
import net.sigmabeta.chipbox.util.logError
import net.sigmabeta.chipbox.view.interfaces.BaseView

abstract class BaseActivity : AppCompatActivity(), BaseView {
    /**
     * Calls the superclass constructor, and then automatically
     * requests an injection of the Activity's dependencies.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(getLayoutId())

        if (ChipboxApplication.appComponent == null) {
            logError("[DaggerActivity] AppComponent null.")
        }

        inject()

        configureViews()
        getPresenter().onCreate(intent.extras, savedInstanceState, this)
    }

    override fun onResume() {
        super.onResume()
        getPresenter().onResume()
    }

    override fun onPause() {
        super.onPause()
        getPresenter().onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        getPresenter().onDestroy(isFinishing)
    }

    override fun showToastMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showErrorSnackbar(message: String, action: View.OnClickListener?, actionLabel: Int?) {
        val snackbar = Snackbar.make(getContentLayout(), message, Snackbar.LENGTH_LONG)

        if (action != null && actionLabel != null) {
            snackbar.setAction(actionLabel, action)
        }

        snackbar.show()
    }

    /**
     * Must be overridden to request the activity's dependencies
     * and do any other necessary setup.
     */
    protected abstract fun inject()

    protected abstract fun getPresenter(): ActivityPresenter

    /**
     * Perform any necessary run-time setup of views: RecyclerViews, ClickListeners,
     * etc.
     */
    protected abstract fun configureViews()

    protected abstract fun getLayoutId(): Int

    protected abstract fun getContentLayout(): FrameLayout
}