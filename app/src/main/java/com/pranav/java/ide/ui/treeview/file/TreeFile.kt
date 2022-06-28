package com.pranav.java.ide.ui.treeview.file

import android.content.Context
import android.graphics.drawable.Drawable

import androidx.annotation.Nullable
import androidx.appcompat.content.res.AppCompatResources

import com.pranav.java.ide.R;
import com.pranav.java.ide.ui.treeview.model.TreeFolder
import com.pranav.java.ide.ui.treeview.model.TreeJavaFile

import java.io.File
import java.util.Objects

open class TreeFile {

    @Nullable
    @JvmStatic
    fun fromFile(file: File) : TreeFile? {
        if (file == null) {
            return null
        }
        if (file.isDirectory()) {
            return TreeFolder(file)
        }
        if (file.getName().endsWith(".java")) {
            return TreeJavaFile(file)
        }
        return TreeFile(file)
    }

    private lateinit var mFile: File

    constructor(file: File) {
        mFile = file
    }

    fun getFile() = mFile

    fun getIcon(context: Context) : Drawable? {
        return AppCompatResources.getDrawable(context, R.drawable.java_file)
    }

    override fun equals(o: Object): Boolean {
        if (this == o) {
            return true
        }
        if (o == null || getClass() != o.getClass()) {
            return false
        }
        val treeFile = o as TreeFile
        return Objects.equals(mFile, treeFile.mFile)
    }

    override fun hashCode() : Int {
        return Objects.hash(mFile)
    }
}
