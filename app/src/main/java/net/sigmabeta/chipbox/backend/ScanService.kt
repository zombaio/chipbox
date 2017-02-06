package net.sigmabeta.chipbox.backend

import android.app.IntentService
import android.content.Intent
import android.util.Log
import net.sigmabeta.chipbox.ChipboxApplication
import net.sigmabeta.chipbox.model.events.FileScanCompleteEvent
import net.sigmabeta.chipbox.model.events.FileScanFailedEvent
import net.sigmabeta.chipbox.model.repository.LibraryScanner
import net.sigmabeta.chipbox.model.repository.Repository
import net.sigmabeta.chipbox.util.logError
import net.sigmabeta.chipbox.util.logVerbose
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ScanService : IntentService("Scanner") {
    lateinit var repository: Repository
        @Inject set

    lateinit var updater: UiUpdater
        @Inject set

    lateinit var scanner: LibraryScanner
        @Inject set

    override fun onHandleIntent(intent: Intent?) {
        inject()

        scanner.scanLibrary()
                .buffer(17, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        {
                            val lastEvent = it.last()
                            updater.send(lastEvent)
                        },
                        {
                            // OnError. it: Throwable
                            showFailedNotification()
                            updater.send(FileScanFailedEvent(it.message ?: "Unknown error."))
                            logError("[FileListPresenter] File scanning error: ${Log.getStackTraceString(it)}")
                        },
                        {
                            // OnCompleted.
                            updater.send(FileScanCompleteEvent(12, 24))
                        }
                )
    }

    private fun showFailedNotification() {

    }

    private fun inject() {
        logVerbose("[ServiceInjector] Injecting BackendView.")
        (application as ChipboxApplication).appComponent.inject(this)
    }
}
