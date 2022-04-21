﻿package com.pranav.java.ide.compiler;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.common.io.Files;
import com.pranav.java.ide.MainActivity;
import com.pranav.java.ide.R;
import com.pranav.lib_android.exception.CompilationFailedException;
import com.pranav.lib_android.task.java.CompileJavaTask;
import com.pranav.lib_android.task.java.D8Task;
import com.pranav.lib_android.task.java.ExecuteJavaTask;
import com.pranav.lib_android.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class CompileTask implements Runnable {

    private long d8Time = 0;
    private long ecjTime = 0;

    private boolean errorsArePresent = false;

    private Context context;

    private CompilerListeners listener;

    public static String STAGE_CLEAN;
    public static String STAGE_ECJ;
    public static String STAGE_D8TASK;
    public static String STAGE_LOADING_DEX;

    public CompileTask(Context context, CompilerListeners listener) {
        this.context = (MainActivity) context;
        this.listener = listener;

        STAGE_CLEAN = context.getString(R.string.stage_clean);
        STAGE_ECJ = context.getString(R.string.stage_ecj);
        STAGE_D8TASK = context.getString(R.string.stage_d8task);
        STAGE_LOADING_DEX = context.getString(R.string.stage_loading_dex);
    }

    @Override
    public void run() {
        Looper.prepare();

        try {
            // Delete previous build files
            listener.OnCurrentBuildStageChanged(STAGE_CLEAN);
            FileUtil.deleteFile(FileUtil.getBinDir());
            ((MainActivity) context).file(FileUtil.getBinDir()).mkdirs();
            final File mainFile = ((MainActivity) context).file(
                    FileUtil.getJavaDir() + "Main.java");
            Files.createParentDirs(mainFile);
            // a simple workaround to prevent calls to system.exit
            Files.write(((MainActivity) context).editor.getText().toString()
                    .replace("System.exit(",
                            "System.err.print(\"Exit code \" + ")
                    .getBytes(), mainFile);
        } catch (final IOException e) {
            ((MainActivity) context).dialog("Cannot save program", e.getMessage(), true);
            Log.e("FAILED", "On Line 56");
            listener.OnFailed();
        }

        // code that runs ecj
        long time = System.currentTimeMillis();
        errorsArePresent = true;
        try {
            listener.OnCurrentBuildStageChanged(STAGE_ECJ);
            CompileJavaTask javaTask = new CompileJavaTask(((MainActivity) context).builder);
            javaTask.doFullTask();
            errorsArePresent = false;
        } catch (CompilationFailedException e) {
            ((MainActivity) context).showErr(e.getMessage());
            listener.OnFailed();
        } catch (Throwable e) {
            ((MainActivity) context).showErr(e.getMessage());
            listener.OnFailed();
        }
        if (errorsArePresent) {
            return;
        }

        ecjTime = System.currentTimeMillis() - time;
        time = System.currentTimeMillis();

        // run d8
        try {
            listener.OnCurrentBuildStageChanged(STAGE_D8TASK);
            new D8Task().doFullTask();
        } catch (Exception e) {
            errorsArePresent = true;
            ((MainActivity) context).showErr(e.getMessage());
            Log.e("FAILED", "On Line 86");
            listener.OnFailed();
            return;
        }
        d8Time = System.currentTimeMillis() - time;
        // code that loads the final dex
        try {
            listener.OnCurrentBuildStageChanged(STAGE_LOADING_DEX);
            final String[] classes = ((MainActivity) context).getClassesFromDex();
            if (classes == null) {
                return;
            }
            listener.OnSuccess();
            ((MainActivity) context).listDialog("Select a class to execute", classes, (dialog, item) -> {
                ExecuteJavaTask task = new ExecuteJavaTask(((MainActivity) context).builder, classes[item]);
                try {
                    task.doFullTask();
                } catch (InvocationTargetException e) {
                    ((MainActivity) context).dialog("Failed...",
                            "Runtime error: " +
                                    e.getMessage() +
                                    "\n\n" +
                                    e.getMessage(),
                            true);
                } catch (Exception e) {
                    ((MainActivity) context).dialog("Failed..",
                            "Couldn't execute the dex: "
                                    + e.toString()
                                    + "\n\nSystem logs:\n"
                                    + task.getLogs(),
                            true);
                }
                StringBuilder s = new StringBuilder();
                s.append("Success! ECJ took: ");
                s.append(String.valueOf(ecjTime));
                s.append("ms, ");
                s.append("D8");
                s.append(" took: ");
                s.append(String.valueOf(d8Time));
                s.append("ms");

                ((MainActivity) context).dialog(s.toString(), task.getLogs(), true);
            });
        } catch (Throwable e) {
            listener.OnFailed();
            ((MainActivity) context).showErr(e.getMessage());
        }
    }

    public static interface CompilerListeners {
        public void OnCurrentBuildStageChanged(String stage);

        public void OnSuccess();

        public void OnFailed();
    }

}
