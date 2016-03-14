package net.sigmabeta.chipbox.ui.file

import android.os.Bundle
import net.sigmabeta.chipbox.model.file.FileListItem
import net.sigmabeta.chipbox.ui.BaseView
import net.sigmabeta.chipbox.ui.FragmentPresenter
import net.sigmabeta.chipbox.util.generateFileList
import net.sigmabeta.chipbox.util.readSingleTrackFile
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File
import java.util.*
import javax.inject.Inject

class FileListPresenter @Inject constructor() : FragmentPresenter() {
    var view: FileListView? = null

    var files: ArrayList<FileListItem>? = null

    fun onItemClick(position: Long) {
        files?.get(position.toInt())?.let {
            val clickedFile = File(it.path)

            if (clickedFile.isDirectory) {
                val fileList = generateFileList(clickedFile)

                view?.onDirectoryClicked(it.path)
            } else {
                val track = readSingleTrackFile(it.path, 0)

                if (track != null) {
                    // TODO Show human-readable details.
                    view?.showToastMessage(track.toString())
                } else {
                    view?.onInvalidTrackClicked()
                }
            }
        }
    }

    override fun onReCreate(savedInstanceState: Bundle) {
    }

    override fun setup(arguments: Bundle?) {
        val path = arguments?.getString(FileListFragment.ARGUMENT_PATH)

        if (path != null) {
            val folder = File(path)

            val subscription = generateFileList(folder)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            {
                                files = it
                                if (it != null) {
                                    view?.setFiles(it)
                                }
                            },
                            {
                                view?.showErrorSnackbar("Error: ${it.message}", null, null)
                            }
                    )

            subscriptions.add(subscription)
        }
    }

    override fun teardown() {
        files = null
    }

    override fun updateViewState() {
        files?.let {
            view?.setFiles(it)
        }
    }

    override fun setView(view: BaseView) {
        if (view is FileListView) this.view = view
    }

    override fun clearView() {
        view = null
    }
}