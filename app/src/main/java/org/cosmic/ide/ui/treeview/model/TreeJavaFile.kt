package org.cosmic.ide.ui.treeview.model

import android.content.Context
import android.graphics.drawable.Drawable
import org.cosmic.ide.ui.treeview.file.TreeFile
import java.io.File

class TreeJavaFile(file: File) : TreeFile(file) {

    override fun getIcon(context: Context): Drawable? {
        return super.getIcon(context)
    }
}
