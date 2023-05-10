package org.cosmicide.rewrite.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

/**
 * A ViewModel for managing a list of files and the current position within the list.
 */
class FileViewModel : ViewModel() {

    private val _files = MutableLiveData<List<File>>(emptyList())
    val files: LiveData<List<File>> get() = _files

    private val _currentPosition = MutableLiveData(-1)
    val currentPosition: LiveData<Int> get() = _currentPosition

    /**
     * Returns the current file at the current position, or null if the list is empty or the current position is out of range.
     */
    val currentFile: File?
        get() = files.value?.getOrNull(currentPosition.value ?: -1)

    /**
     * Sets the current position to the given position.
     */
    fun setCurrentPosition(pos: Int) {
        _currentPosition.value = pos
    }

    /**
     * Adds the given file to the list of files.
     * If the file is not already in the list, it is added to the end and the current position is set to the last index.
     * If the file is already in the list, the current position is set to its index.
     */
    fun addFile(file: File) {
        val index = _files.value?.indexOf(file) ?: -1
        if (index == -1) {
            val newList = _files.value.orEmpty() + file
            _files.value = newList
            setCurrentPosition(newList.lastIndex)
        } else {
            setCurrentPosition(index)
        }
    }

    /**
     * Removes the given file from the list of files.
     */
    fun removeFile(file: File) {
        val newList = _files.value.orEmpty().filterNot { it == file }
        _files.value = newList
    }

    /**
     * Removes all files from the list except for the given file, and sets the current position to 0.
     */
    fun removeOthers(file: File) {
        _files.value = listOf(file)
        setCurrentPosition(0)
    }

    /**
     * Removes all files from the list.
     * Sets the current position to -1 to indicate that there is no current file.
     */
    fun removeAll() {
        _files.value = emptyList()
        setCurrentPosition(-1)
    }

    /**
     * Removes the file immediately to the right of the given position.
     * If the given position is the last position in the list, nothing happens.
     */
    fun removeRight(pos: Int) {
        val currentPos = currentPosition.value ?: return
        if (pos == _files.value?.lastIndex) return
        val newList = _files.value.orEmpty().toMutableList()
        if (pos == currentPos) {
            newList.removeAt(0)
        } else {
            newList.removeAt(pos + 1)
        }
        _files.value = newList
        setCurrentPosition(currentPos)
    }

    /**
     * Removes the file immediately to the left of the given position.
     * If the given position is the first position in the list, nothing happens.
     */
    fun removeLeft(pos: Int) {
        val currentPos = currentPosition.value ?: return
        if (pos == 0) return
        val newList = _files.value.orEmpty().toMutableList()
        newList.removeAt(pos - 1)
        _files.value = newList
        setCurrentPosition(currentPos - 1)
    }

    /**
     * Opens the given file by adding it to the list of files and setting it as the current file.
     * Returns true if the file was successfully added, false otherwise.
     */
    fun openFile(file: File): Boolean {
        addFile(file)
        return true
    }

    /**
     * Updates the list of files with the given new files.
     * Any files in the new list that are already in the current list are not added again.
     */
    fun updateFiles(newFiles: List<File>) {
        val newList = (_files.value.orEmpty() + newFiles).distinctBy { it.absolutePath }
        _files.value = newList
    }
}