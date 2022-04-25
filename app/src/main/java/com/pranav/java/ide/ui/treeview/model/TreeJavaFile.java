package com.pranav.java.ide.ui.treeview.model;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.appcompat.content.res.AppCompatResources;

import com.pranav.java.ide.R;

import java.io.File;

public class TreeJavaFile extends com.pranav.java.ide.ui.treeview.file.TreeFile {

    public TreeJavaFile(File file) {
        super(file);
    }

    @Override
    public Drawable getIcon(Context context) {
        return super.getIcon(context);
    }
}