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

    private val _currentPosition = MutableLiveData<Int>(-1)
    val currentPosition: LiveData<Int> get() = _currentPosition

    /**
     * Returns the current file at the current position, or null if the list is empty or the current position is out of range.
     */
    val currentFile: File?
        get() = files.value?.getOrNull(currentPosition.value ?: -1)

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
            _files.value = (_files.value ?: emptyList()) + file
            setCurrentPosition(_files.value?.lastIndex ?: -1)
        } else {
            setCurrentPosition(index)
        }
    }

    /**
     * Removes the given file from the list of files.
     */
    fun removeFile(file: File) {
        _files.value?.toMutableList()?.apply {
            remove(file)
            _files.value = this
        }
    }

    /**
     * Removes all files from the list except for the given file, and sets the current position to 0.
     */
    fun removeOthers(file: File) {
        _files.value = listOf(file)
        setCurrentPosition(0)
    }

    /**
     * Removes all files from the list totally
     */
    fun removeAll(){
        _files.value = emptyList()
        setCurrentPosition(0)
    }

    /**
     * Removes a file immediately close to the position at Right
     */
    fun removeRight(pos: Int){
        when (pos) {
            _files.value?.size?.minus(1) -> removeFile(_files.value!![(_files.value?.size?.minus(1)!!)!!])
            else -> _files.value?.toMutableList()?.apply {
                removeAt(pos + 1)
                _files.value = this
            }
        }
        setCurrentPosition(pos)
    }

    /**
     * Removes a file immediately close to the position at Left
     */
    fun removeLeft(pos: Int){
        when (pos) {
            0 -> removeFile(_files.value?.get(0)!!)
            else -> _files.value?.toMutableList()?.apply {
                removeAt(pos - 1)
                _files.value = this
            }
        }
        setCurrentPosition(pos)
    }

    /**
     * Adds the given file to the list of files and sets it as the current file.
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
    fun updateFiles(newFiles: MutableList<File>) {
        _files.value = (_files.value ?: emptyList()) + newFiles.filterNot { it in _files.value.orEmpty() }
    }
}
