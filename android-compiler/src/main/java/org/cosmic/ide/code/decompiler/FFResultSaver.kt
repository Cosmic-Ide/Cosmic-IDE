package org.cosmic.ide.code.decompiler

import org.jetbrains.java.decompiler.main.extern.IResultSaver
import java.util.jar.Manifest

class FFResultSaver(val className: String) : IResultSaver {
    var result = ""

    private fun saveClass(qualifiedName: String?, content: String?) {
        if (result.isEmpty() && qualifiedName == className) {
            result = content.toString()
        }
    }

    override fun saveClassFile(
        path: String,
        qualifiedName: String?,
        entryName: String,
        content: String?,
        mapping: IntArray?
    ) {
        saveClass(qualifiedName, content)
    }

    override fun saveClassEntry(
        path: String?,
        archiveName: String?,
        qualifiedName: String?,
        entryName: String?,
        content: String?
    ) {
        saveClass(qualifiedName, content)
    }

    override fun saveFolder(path: String?) {}

    override fun copyFile(source: String?, path: String?, entryName: String?) {}

    override fun createArchive(path: String?, archiveName: String?, manifest: Manifest?) {}

    override fun saveDirEntry(path: String?, archiveName: String?, entryName: String?) {}

    override fun copyEntry(
        source: String?,
        path: String?,
        archiveName: String?,
        entry: String?
    ) {}

    override fun closeArchive(path: String?, archiveName: String?) {}
}
