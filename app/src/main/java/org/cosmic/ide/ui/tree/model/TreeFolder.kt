package org.cosmic.ide.ui.treeview.model

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import org.cosmic.ide.R
import java.io.File

class TreeFolder(file: File) : TreeFile(file) {
    override fun getIcon(context: Context): Drawable? {
        return getDrawable(context, R.drawable.ic_folder)
    }
}
