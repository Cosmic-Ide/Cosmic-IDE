package org.cosmic.ide.ui.treeview.model

import android.content.Context
import android.graphics.drawable.Drawable
import java.io.File
import org.cosmic.ide.ui.treeview.file.TreeFile

class TreeJavaFile(file: File) : TreeFile(file) {

    override fun getIcon(context: Context): Drawable? {
        return super.getIcon(context)
    }
}
