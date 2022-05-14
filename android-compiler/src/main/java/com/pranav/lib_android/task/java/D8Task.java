package com.pranav.lib_android.task.java;

import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;

import com.pranav.common.util.ConcurrentUtil;
import com.pranav.common.util.FileUtil;
import com.pranav.lib_android.interfaces.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class D8Task extends Task {

    private Exception ex = null;

    @Override
    public void doFullTask() throws Exception {
        ConcurrentUtil.execute(
                () -> {
                    try {
                        D8.run(
                                D8Command.builder()
                                        .setOutput(
                                                Paths.get(FileUtil.getBinDir()),
                                                OutputMode.DexIndexed)
                                        .addLibraryFiles(
                                                Paths.get(
                                                        FileUtil.getClasspathDir(), "android.jar"))
                                        .addProgramFiles(
                                                getClassFiles(
                                                        new File(FileUtil.getBinDir(), "classes")))
                                        .build());

                    } catch (Exception e) {
                        ex = e;
                    }
                });
        if (ex != null) {
            throw ex;
        }
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
