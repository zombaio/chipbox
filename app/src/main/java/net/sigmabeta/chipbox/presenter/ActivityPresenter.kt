package net.sigmabeta.chipbox.presenter

import android.os.Bundle
import net.sigmabeta.chipbox.view.interfaces.BaseView

abstract class ActivityPresenter : BasePresenter() {
    fun onCreate(arguments: Bundle?, savedInstanceState: Bundle?, view: BaseView) {
        setView(view)

        if (savedInstanceState == null) {
            setup(arguments)
        } else {
            onReCreate(savedInstanceState)
        }
    }

    fun onDestroy(finishing: Boolean) {
        clearView()

        if (finishing) {
            teardown()
        } else {
            onTempDestroy()
        }
    }

    /**
     * Perform actions that only need to be performed on
     * subsequent creations of an activitiy; i.e. after a rotation.
     */
    abstract fun onReCreate(savedInstanceState: Bundle)

    /**
     * Perform any temporary teardown operations because the View
     * is not really destroying; more likely a configuration change.
     */
    abstract fun onTempDestroy()
}
