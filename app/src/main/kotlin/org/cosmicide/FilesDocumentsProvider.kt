/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import java.io.File
import java.io.FileNotFoundException

class FilesDocumentsProvider : DocumentsProvider() {

    private val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        DocumentsContract.Document.COLUMN_FLAGS
    )

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<String>?): Cursor {
        val result = MatrixCursor(
            projection ?: arrayOf(
                DocumentsContract.Root.COLUMN_ROOT_ID,
                DocumentsContract.Root.COLUMN_DOCUMENT_ID,
                DocumentsContract.Root.COLUMN_MIME_TYPES,
                DocumentsContract.Root.COLUMN_ICON,
                DocumentsContract.Root.COLUMN_TITLE,
                DocumentsContract.Root.COLUMN_SUMMARY,
                DocumentsContract.Root.COLUMN_FLAGS,
                DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
            )
        )

        val context = context ?: return result

        result.addRow(
            arrayOf<Any>(
                ROOT_ID,
                ROOT_ID,
                "*/*",
                R.mipmap.ic_launcher,
                "Cosmic IDE Files",
                "Application data directory",
                DocumentsContract.Root.FLAG_LOCAL_ONLY or
                        DocumentsContract.Root.FLAG_SUPPORTS_CREATE or
                        DocumentsContract.Root.FLAG_SUPPORTS_SEARCH or
                        DocumentsContract.Root.FLAG_SUPPORTS_RECENTS or
                        DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD,
                context.dataDir.freeSpace
            )
        )

        return result
    }

    override fun queryDocument(documentId: String?, projection: Array<String>?): Cursor {
        val cols = projection ?: this.projection
        val result = MatrixCursor(cols)
        val file = getFileForDocumentId(documentId)

        if (file.exists()) {
            result.addRow(buildDocumentRow(file, documentId ?: ROOT_ID, cols))
        }

        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<String>?,
        sortOrder: String?
    ): Cursor {
        val cols = projection ?: this.projection
        val result = MatrixCursor(cols)
        val parent = getFileForDocumentId(parentDocumentId)

        val files = parent.listFiles() ?: return result

        files.forEach { file ->
            val documentId = getDocumentIdForFile(file)
            result.addRow(buildDocumentRow(file, documentId, cols))
        }

        return result
    }

    @Throws(FileNotFoundException::class)
    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor? {
        val file = getFileForDocumentId(documentId)

        // External apps might call openDocument before createDocument loop completes
        val isWrite = mode?.contains("w") == true
        if (isWrite) {
            file.parentFile?.mkdirs()
        }

        val accessMode = ParcelFileDescriptor.parseMode(mode ?: "r")
        return try {
            ParcelFileDescriptor.open(file, accessMode)
        } catch (e: Exception) {
            throw FileNotFoundException("Failed to open document $documentId: ${e.localizedMessage}")
        }
    }

    override fun createDocument(
        parentDocumentId: String?,
        mimeType: String?,
        displayName: String?
    ): String? {
        val parent = getFileForDocumentId(parentDocumentId)

        if (!parent.exists()) {
            parent.mkdirs()
        }

        if (!parent.isDirectory || !parent.canWrite()) {
            return null
        }

        val file = File(parent, displayName ?: "document")

        return try {
            if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                if (file.mkdir()) getDocumentIdForFile(file) else null
            } else {
                if (file.createNewFile()) getDocumentIdForFile(file) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun deleteDocument(documentId: String?) {
        val file = getFileForDocumentId(documentId)
        if (file.exists()) {
            file.deleteRecursively()
        }
    }

    override fun renameDocument(documentId: String?, displayName: String?): String? {
        val file = getFileForDocumentId(documentId)
        val newFile = File(file.parent, displayName ?: return null)
        return if (file.exists() && file.renameTo(newFile)) getDocumentIdForFile(newFile) else null
    }

    override fun copyDocument(sourceDocumentId: String?, targetParentDocumentId: String?): String? {
        val source = getFileForDocumentId(sourceDocumentId)
        val targetParent = getFileForDocumentId(targetParentDocumentId)

        if (!source.exists() || !targetParent.isDirectory) return null

        val destination = File(targetParent, source.name)
        if (destination.exists()) {
            destination.deleteRecursively()
        }

        return if (source.copyRecursively(destination)) {
            getDocumentIdForFile(destination)
        } else {
            null
        }
    }

    override fun moveDocument(
        sourceDocumentId: String?,
        sourceParentDocumentId: String?,
        targetParentDocumentId: String?
    ): String? {
        val source = getFileForDocumentId(sourceDocumentId)
        val targetParent = getFileForDocumentId(targetParentDocumentId)

        if (!source.exists() || !targetParent.isDirectory) return null

        val destination = File(targetParent, source.name)
        if (destination.exists()) {
            destination.deleteRecursively()
        }

        return if (source.renameTo(destination)) {
            getDocumentIdForFile(destination)
        } else {
            null
        }
    }

    private fun buildDocumentRow(
        file: File,
        documentId: String,
        projection: Array<String>
    ): Array<Any> {
        val row = mutableListOf<Any>()

        val flags = if (file.isDirectory) {
            DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE or
                    DocumentsContract.Document.FLAG_SUPPORTS_DELETE or
                    DocumentsContract.Document.FLAG_SUPPORTS_RENAME or
                    DocumentsContract.Document.FLAG_SUPPORTS_MOVE or
                    DocumentsContract.Document.FLAG_SUPPORTS_COPY
        } else {
            DocumentsContract.Document.FLAG_SUPPORTS_DELETE or
                    DocumentsContract.Document.FLAG_SUPPORTS_RENAME or
                    DocumentsContract.Document.FLAG_SUPPORTS_MOVE or
                    DocumentsContract.Document.FLAG_SUPPORTS_COPY
        }

        val mimeType = if (file.isDirectory) {
            DocumentsContract.Document.MIME_TYPE_DIR
        } else {
            getMimeType(file)
        }

        projection.forEach { col ->
            when (col) {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID -> row.add(documentId)
                DocumentsContract.Document.COLUMN_DISPLAY_NAME -> row.add(file.name)
                DocumentsContract.Document.COLUMN_MIME_TYPE -> row.add(mimeType)
                DocumentsContract.Document.COLUMN_SIZE -> row.add(file.length())
                DocumentsContract.Document.COLUMN_LAST_MODIFIED -> row.add(file.lastModified())
                DocumentsContract.Document.COLUMN_FLAGS -> row.add(flags)
                else -> row.add("")
            }
        }

        return row.toTypedArray()
    }

    @SuppressLint("SetWorldReadable", "SetWorldWritable")
    private fun getFileForDocumentId(documentId: String?): File {
        val file = if (documentId == ROOT_ID) {
            context?.dataDir ?: File("/")
        } else {
            File(context?.dataDir, documentId ?: "")
        }

        try {
            file.setReadable(true, false)
            if (file.extension != "dex") file.setWritable(true, false)
        } catch (e: Exception) {
            // Ignore permission errors
        }

        return file
    }

    private fun getDocumentIdForFile(file: File): String {
        val filesDir = context?.dataDir ?: return ROOT_ID
        return if (file == filesDir) {
            ROOT_ID
        } else {
            file.absolutePath.removePrefix(filesDir.absolutePath).removePrefix("/")
        }
    }

    private fun getMimeType(file: File): String {
        val extension = file.extension
        return when (extension.lowercase()) {
            "txt" -> "text/plain"
            "pdf" -> "application/pdf"
            "xml" -> "text/xml"
            "json" -> "application/json"
            "java" -> "text/x-java"
            "kt" -> "text/x-kotlin"
            "py" -> "text/x-python"
            "js" -> "text/javascript"
            "ts" -> "text/typescript"
            "html" -> "text/html"
            "css" -> "text/css"
            "md" -> "text/markdown"
            "png", "jpg", "jpeg", "gif", "webp" -> "image/${extension.lowercase()}"
            else -> "application/octet-stream"
        }
    }

    override fun querySearchDocuments(
        rootId: String?,
        query: String?,
        projection: Array<String>?
    ): Cursor {
        val cols = projection ?: this.projection
        val result = MatrixCursor(cols)
        val root = getFileForDocumentId(rootId)

        fun searchFiles(dir: File, searchQuery: String) {
            val files = dir.listFiles() ?: return

            files.forEach { file ->
                if (file.name.contains(searchQuery, ignoreCase = true)) {
                    val documentId = getDocumentIdForFile(file)
                    result.addRow(buildDocumentRow(file, documentId, cols))
                }

                if (file.isDirectory) {
                    searchFiles(file, searchQuery)
                }
            }
        }

        if (!query.isNullOrEmpty()) {
            searchFiles(root, query)
        }

        return result
    }

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean {
        return if (parentDocumentId != null && documentId != null) {
            val parent = getFileForDocumentId(parentDocumentId)
            val doc = getFileForDocumentId(documentId)
            doc.absolutePath.startsWith(parent.absolutePath)
        } else {
            false
        }
    }

    companion object {
        private const val ROOT_ID = "Files"
    }
}
