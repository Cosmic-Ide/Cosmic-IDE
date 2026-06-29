package org.cosmicide.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.cosmicide.ui.editor.CodeEditorState
import java.io.File

class EditorViewModel : ViewModel() {
    var openFiles by mutableStateOf(emptyList<File>())
        private set

    var activeFile by mutableStateOf<File?>(null)
        private set

    val fileEditorStates = mutableStateMapOf<File, CodeEditorState>()

    val fileContentCache = mutableStateMapOf<File, String>()


    fun getOrInitEditorState(file: File): CodeEditorState {
        return fileEditorStates.getOrPut(file) { CodeEditorState() }
    }

    fun switchTab(newFile: File, currentText: String?) {
        activeFile?.let { currentFile ->
            if (currentText != null) {
                fileContentCache[currentFile] = currentText
            }
        }
        if (!openFiles.contains(newFile)) {
            openFiles = openFiles + newFile
        }
        activeFile = newFile
    }

    fun closeTab(file: File) {
        val newList = openFiles - file
        openFiles = newList
        if (activeFile == file) {
            activeFile = newList.lastOrNull()
        }
        fileContentCache.remove(file)
    }

    fun updateCache(file: File, content: String) {
        fileContentCache[file] = content
    }
}