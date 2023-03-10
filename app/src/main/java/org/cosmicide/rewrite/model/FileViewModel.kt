package org.cosmicide.rewrite.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class FileViewModel : ViewModel() {
    // The files currently opened in the editor
    private var mFiles: MutableLiveData<MutableList<File>>? = null

    // The current position of the CodeEditor
    val currentPosition = MutableLiveData(-1)
    val files: LiveData<MutableList<File>>
        get() {
            if (mFiles == null) {
                mFiles = MutableLiveData(ArrayList())
            }
            return mFiles!!
        }

    fun setFiles(files: MutableList<File>) {
        if (mFiles == null) {
            mFiles = MutableLiveData(ArrayList())
        }
        mFiles!!.value = files
    }

    fun getCurrentPosition(): LiveData<Int> {
        return currentPosition
    }

    fun setCurrentPosition(pos: Int) {
        currentPosition.value = pos
    }

    val currentFile: File?
        get() {
            val files = files.value ?: return null
            val currentPos = currentPosition.value
            if (currentPos == null || currentPos == -1) {
                return null
            }
            return if (files.size - 1 < currentPos) {
                null
            } else files[currentPos]
        }

    fun clear() {
        mFiles!!.value = ArrayList()
    }

    /**
     * Opens this file to the editor
     *
     * @param file The file to be opened
     * @return whether the operation was successful
     */
    fun openFile(file: File): Boolean {
        var index = -1
        val value: List<File>? = files.value
        if (value != null) {
            index = value.indexOf(file)
        }
        if (index != -1) {
            setCurrentPosition(index)
            return true
        }
        addFile(file)
        return true
    }

    fun addFile(file: File) {
        var files = files.value
        if (files == null) {
            files = ArrayList()
        }
        if (!files.contains(file)) {
            files.add(file)
            mFiles!!.value = files
        }
        setCurrentPosition(files.indexOf(file))
    }

    fun removeFile(file: File) {
        val files = files.value ?: return
        files.remove(file)
        mFiles!!.value = files
    }

    // Remove all the files except the given file
    fun removeOthers(file: File) {
        val files = files.value ?: return
        files.clear()
        files.add(file)
        setFiles(files)
    }
}