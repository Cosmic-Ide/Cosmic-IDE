package com.pranav.java.ide.ui.treeview.file;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.pranav.java.ide.R;
import com.pranav.java.ide.ui.treeview.model.TreeFolder;
import com.pranav.java.ide.ui.treeview.model.TreeJavaFile;

import java.io.File;
import java.util.Objects;

public class TreeFile {

    @Nullable
    public static TreeFile fromFile(File file) {
        if (file == null) {
            return null;
        }
        if (file.isDirectory()) {
            return new TreeFolder(file);
        }
        if (file.getName().endsWith(".java")) {
            return new TreeJavaFile(file);
        }
        return new TreeFile(file);
    }

    private final File mFile;

    public TreeFile(File file) {
        mFile = file;
    }

    public File getFile() {
        return mFile;
    }

    public Drawable getIcon(Context context) {
        return AppCompatResources.getDrawable(context, R.drawable.java_file);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var treeFile = (TreeFile) o;
        return Objects.equals(mFile, treeFile.mFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mFile);
    }
}
