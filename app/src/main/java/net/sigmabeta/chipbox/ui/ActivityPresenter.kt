package net.sigmabeta.chipbox.ui

import android.os.Bundle
import net.sigmabeta.chipbox.dagger.component.FragmentComponent
import net.sigmabeta.chipbox.util.logWarning

abstract class ActivityPresenter<V : BaseView> : BasePresenter<V>() {
    var fragmentComponent: FragmentComponent? = null
        get () {
            if (field == null) {
                field = view?.getTypedApplication()?.appComponent?.plusFragments();
            }
            return field
        }

    var recreated = false

    fun onCreate(arguments: Bundle?, savedInstanceState: Bundle?, view: V) {
        this.view = view

        if (savedInstanceState == null) {
            repository.reopen()
            setupStartTime = System.currentTimeMillis()
            setup(arguments)
        } else {
            recreated = true
            onReCreate(arguments, savedInstanceState)
        }
    }

    fun onDestroy(finishing: Boolean, destroyedView: V) {
        if (destroyedView === this.view) {
            clearView()

            if (finishing) {
                setupStartTime = -1
                teardown()
                repository.close()

                needsSetup = true
                loading = false
                recreated = false
            } else {
                onTempDestroy()
            }
        } else if (this.view == null) {
            logWarning("Cannot clear reference to view: Presenter has already cleared reference.")
        } else {
            handleError(InvalidClearViewException(view), null)
        }
    }

    /**
     * Perform actions that need to be performed in order for a
     * re-enter animation to not screw up.
     */
    abstract fun onReenter()

    /**
     * Perform actions that only need to be performed o subsequent creations
     * of a Activity; i.e. after rotation or low-mem activity destruction. Generally,
     * check if the operations performed in setup() need to be redone, and if so, do
     * them.
     */
    abstract fun onReCreate(arguments: Bundle?, savedInstanceState: Bundle)

    /**
     * Perform any temporary teardown operations because the View
     * is not really destroying; more likely a configuration change.
     */
    abstract fun onTempDestroy()
}
