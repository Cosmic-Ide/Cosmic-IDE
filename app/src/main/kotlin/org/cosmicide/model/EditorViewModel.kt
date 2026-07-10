package org.cosmicide.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.io.File

data class EditorDocument(
    val file: File,
    val content: String,
    val savedContentHash: Int = content.editorContentHash()
) {
    val isDirty: Boolean
        get() = content.editorContentHash() != savedContentHash

    fun withContent(newContent: String): EditorDocument {
        return if (newContent == content) this else copy(content = newContent)
    }

    fun markSaved(): EditorDocument {
        return copy(savedContentHash = content.editorContentHash())
    }
}

class EditorViewModel : ViewModel() {
    var openFiles by mutableStateOf(emptyList<File>())
        private set

    var activeFile by mutableStateOf<File?>(null)
        private set

    private val documents = mutableStateMapOf<File, EditorDocument>()

    fun documentFor(file: File): EditorDocument? {
        return documents[file]
    }

    fun cachedContent(file: File): String? {
        return documents[file]?.content
    }

    fun ensureDocument(file: File, initialContent: String): EditorDocument {
        documents[file]?.let { return it }

        return EditorDocument(file = file, content = initialContent).also { document ->
            documents[file] = document
        }
    }

    fun openFile(file: File, currentEditorContent: String? = null) {
        saveActiveDocument(currentEditorContent)

        if (!openFiles.contains(file)) {
            openFiles = openFiles + file
        }
        activeFile = file
    }

    fun closeTab(file: File, currentEditorContent: String? = null) {
        saveActiveDocument(currentEditorContent)

        val newList = openFiles - file
        openFiles = newList
        if (activeFile == file) {
            activeFile = newList.lastOrNull()
        }
        documents.remove(file)
    }

    fun onActiveContentChanged(content: String) {
        val file = activeFile ?: return
        updateDocumentContent(file, content)
        saveDocument(file)
    }

    fun saveActiveDocument(currentEditorContent: String? = null) {
        val file = activeFile ?: return
        if (currentEditorContent != null) {
            updateDocumentContent(file, currentEditorContent)
        }
        saveDocument(file)
    }

    private fun updateDocumentContent(file: File, content: String) {
        val document = documents[file]
        documents[file] = document?.withContent(content)
            ?: EditorDocument(file = file, content = content)
    }

    private fun saveDocument(file: File) {
        val document = documents[file] ?: return
        file.writeText(document.content)
        documents[file] = document.markSaved()
    }
}

private fun String.editorContentHash(): Int {
    return toByteArray(Charsets.UTF_8).contentHashCode()
}
