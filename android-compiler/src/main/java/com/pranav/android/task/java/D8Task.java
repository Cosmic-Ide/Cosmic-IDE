package com.pranav.android.task.java;

import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;

import com.pranav.android.interfaces.*;
import com.pranav.common.util.FileUtil;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class D8Task extends Task {

    private Exception ex = null;

    @Override
    public void doFullTask() throws Exception {
            D8.run(
                    D8Command.builder()
                            .setOutput(Paths.get(FileUtil.getBinDir()), OutputMode.DexIndexed)
                            .addLibraryFiles(Paths.get(FileUtil.getClasspathDir(), "android.jar"))
                            .addProgramFiles(
                                    getClassFiles(new File(FileUtil.getBinDir(), "classes")))
                            .build());
    }

    private ArrayList<Path> getClassFiles(File root) {
        var paths = new ArrayList<Path>();

        var files = root.listFiles();
        if (files != null) {
            for (var f : files) {
                if (f.isFile()) {
                    paths.add(f.toPath());
                } else {
                    paths.addAll(getClassFiles(f));
                }
            }
        }
        return paths;
    }

    @Override
    public String getTaskName() {
        return "D8 Task";
    }
}
