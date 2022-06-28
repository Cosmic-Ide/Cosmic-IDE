package com.pranav.java.ide.ui.treeview.model

import android.content.Context
import android.graphics.drawable.Drawable

import androidx.appcompat.content.res.AppCompatResources

import com.pranav.java.ide.R
import com.pranav.java.ide.ui.treeview.file.TreeFile

import java.io.File

class TreeFolder(file: File) : TreeFile() {

    init {
        super(file)
    }

    override fun getIcon(context: Context): Drawable {
        return AppCompatResources.getDrawable(context, R.drawable.folder)
    }
}
