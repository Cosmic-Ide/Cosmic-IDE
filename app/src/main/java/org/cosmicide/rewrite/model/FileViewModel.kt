package org.cosmicide.rewrite.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

/**
 * ViewModel for managing a list of files and the current position in the list.
 * Uses MutableLiveData to observe changes in the list and position.
 */
class FileViewModel : ViewModel() {

    private val _files = MutableLiveData<List<File>>(emptyList())
    val files: LiveData<List<File>> = _files

    private val _currentPosition = MutableLiveData(-1)
    val currentPosition: LiveData<Int> = _currentPosition

    val currentFile: File?
        get() = files.value?.getOrNull(currentPosition.value ?: -1)

    /**
     * Sets the current position in the list to the specified integer.
     *
     * @param pos an integer representing the new position in the list.
     */
    fun setCurrentPosition(pos: Int) {
        _currentPosition.value = pos
    }

    /**
     * Adds the specified [File] object to the list of files, if not already present.
     * Sets the current position in the list to the newly added file.
     *
     * @param file a [File] object to be added to the list.
     */
    fun addFile(file: File) {
        val files = _files.value.orEmpty().toMutableList()
        if (!files.contains(file)) {
            files.add(file)
            _files.value = files
        }
        setCurrentPosition(files.indexOf(file))
    }

    /**
     * Removes the specified [File] object from the list of files.
     *
     * @param file a [File] object to be removed from the list.
     */
    fun removeFile(file: File) {
        val files = _files.value.orEmpty().toMutableList().apply {
            remove(file)
        }
        _files.value = files
    }

    /**
     * Removes all files from the list except for the specified [File] object.
     * Sets the current position in the list to the specified file.
     *
     * @param file a [File] object to be kept in the list, all others are removed.
     */
    fun removeOthers(file: File) {
        _files.value = listOf(file)
        setCurrentPosition(0)
    }

    /**
     * Adds the specified [File] object to the list of files and returns true.
     *
     * @param file a [File] object to be added to the list.
     * @return true
     */
    fun openFile(file: File): Boolean {
        addFile(file)
        return true
    }

    /**
     * Updates the list of files with the specified list of [File] objects.
     * Sets the current position in the list to -1.
     *
     * @param newFiles a list of [File] objects to update the list.
     */
    fun updateFiles(newFiles: List<File>) {
        _files.value = newFiles
        setCurrentPosition(-1)
    }
}