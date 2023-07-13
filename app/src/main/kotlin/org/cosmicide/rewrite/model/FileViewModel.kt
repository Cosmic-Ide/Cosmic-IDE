/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

/**
 * A ViewModel for managing a list of files and the current position within the list.
 */
class FileViewModel : ViewModel() {

    val files = MutableLiveData<List<File>>(mutableListOf())

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
        val index = files.value?.indexOf(file) ?: -1
        if (index == -1) {
            files.value = files.value?.toMutableList()?.apply { add(file) }
            setCurrentPosition(files.value!!.lastIndex)
        } else {
            setCurrentPosition(index)
        }
    }

    /**
     * Removes the given file from the list of files.
     */
    fun removeFile(file: File) {
        files.value = files.value?.toMutableList()?.apply { remove(file) }
        if (files.value!!.isEmpty()) {
            setCurrentPosition(-1)
        } else {
            setCurrentPosition(currentPosition.value?.minus(1) ?: 0)
        }
    }

    /**
     * Removes all files from the list except for the given file, and sets the current position to 0.
     */
    fun removeOthers(file: File) {
        files.value = mutableListOf(file)
        setCurrentPosition(0)
    }

    /**
     * Removes all files from the list.
     * Sets the current position to -1 to indicate that there is no current file.
     */
    fun removeAll() {
        files.value = mutableListOf()
        setCurrentPosition(-1)
    }

    /**
     * Removes the file immediately to the right of the given position.
     * If the given position is the last position in the list, nothing happens.
     */
    fun removeRight(pos: Int) {
        val currentPos = currentPosition.value ?: return
        if (pos == files.value?.lastIndex) return
        files.value = files.value?.toMutableList()?.apply { removeAt(pos + 1) }
        setCurrentPosition(currentPos)
    }

    /**
     * Removes the file immediately to the left of the given position.
     * If the given position is the first position in the list, nothing happens.
     */
    fun removeLeft(pos: Int) {
        val currentPos = currentPosition.value ?: return
        if (pos == 0) return
        files.value = files.value?.toMutableList()?.apply { removeAt(pos - 1) }
        setCurrentPosition(currentPos - 1)
    }

    /**
     * Updates the list of files with the given new files.
     * Any files in the new list that are already in the current list are not added again.
     */
    fun updateFiles(newFiles: List<File>) {
        files.value =
            (files.value.orEmpty() + newFiles).distinctBy { it.absolutePath }.toMutableList()
    }
}