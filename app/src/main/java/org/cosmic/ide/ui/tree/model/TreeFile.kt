package org.cosmic.ide.ui.treeview.model

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import org.cosmic.ide.R
import java.io.File
import java.util.Objects

open class TreeFile {
    companion object {
        @JvmStatic
        fun fromFile(file: File): TreeFile {
            if (file.isDirectory) {
                return TreeFolder(file)
            }
            if (file.extension.equals("java")) {
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
        return when (mFile.extension) {
            "java" -> getDrawable(context, R.drawable.file_type_java)
            "kt" -> getDrawable(context, R.drawable.file_type_kt)
            else -> getDrawable(context, R.drawable.ic_file)
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
