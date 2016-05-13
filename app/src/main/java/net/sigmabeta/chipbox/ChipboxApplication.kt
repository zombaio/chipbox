package net.sigmabeta.chipbox

import android.app.Application
import android.os.Build
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import net.sigmabeta.chipbox.dagger.Initializer
import net.sigmabeta.chipbox.dagger.component.AppComponent
import net.sigmabeta.chipbox.util.logDebug

public class ChipboxApplication : Application() {
    /**
     * Static methods and members go in a 'companion' object.
     */
    companion object {
        lateinit var appComponent: AppComponent
    }

    /**
     * Calls the superclass constructor, then initializes the singleton
     * Dagger Components.
     */
    override fun onCreate() {
        super.onCreate()

        logDebug("[ChipboxApplication] Starting Application.")
        logDebug("[ChipboxApplication] Build type: ${BuildConfig.BUILD_TYPE}")

        logDebug("[ChipboxApplication] Android version: ${Build.VERSION.RELEASE}")
        logDebug("[ChipboxApplication] Device manufacturer: ${Build.MANUFACTURER}")
        logDebug("[ChipboxApplication] Device model: ${Build.MODEL}")

        System.loadLibrary("gme")
        FlowManager.init(FlowConfig.Builder(this).build())

        appComponent = Initializer.initAppComponent(this)
    }
}
