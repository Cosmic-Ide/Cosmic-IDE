package org.cosmic.ide.ui.treeview.file

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import org.cosmic.ide.R
import org.cosmic.ide.ui.treeview.model.TreeFolder
import org.cosmic.ide.ui.treeview.model.TreeJavaFile
import java.io.File
import java.util.Objects

open class TreeFile {

    companion object {
        @JvmStatic
        fun fromFile(file: File): TreeFile? {
            if (file.isDirectory()) {
                return TreeFolder(file)
            }
            if (file.getName().endsWith(".java")) {
                return TreeJavaFile(file)
            }
            return TreeFile(file)
        }
    }

    private var mFile: File

    constructor(file: File) {
        mFile = file
    }

    fun getFile() = mFile

    open fun getIcon(context: Context): Drawable? {
        return when (getFile().extension) {
            "java" -> AppCompatResources.getDrawable(context, R.drawable.file_type_java)
            "kt" -> AppCompatResources.getDrawable(context, R.drawable.file_type_kt)
            else -> AppCompatResources.getDrawable(context, R.drawable.ic_file)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this == other) {
            return true
        }
        if (other == null || this::class != other::class) {
            return false
        }
        val treeFile = other as TreeFile
        return Objects.equals(mFile, treeFile.mFile)
    }

    override fun hashCode(): Int {
        return Objects.hash(mFile)
    }
}
