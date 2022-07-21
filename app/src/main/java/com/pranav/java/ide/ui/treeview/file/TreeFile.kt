package com.pranav.java.ide.ui.treeview.file

import android.content.Context
import android.graphics.drawable.Drawable

import androidx.annotation.Nullable
import androidx.appcompat.content.res.AppCompatResources

import com.pranav.java.ide.R
import com.pranav.java.ide.ui.treeview.model.TreeFolder
import com.pranav.java.ide.ui.treeview.model.TreeJavaFile

import java.io.File
import java.util.Objects

open class TreeFile {

    companion object {
        @Nullable
        @JvmStatic
        fun fromFile(file: File) : TreeFile? {
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

    open fun getIcon(context: Context) : Drawable? {
        return AppCompatResources.getDrawable(context, R.drawable.ic_file)
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

    override fun hashCode() : Int {
        return Objects.hash(mFile)
    }
}
