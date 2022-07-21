package com.pranav.java.ide.compiler;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import com.pranav.android.exception.CompilationFailedException;
import com.pranav.android.task.JavaBuilder;
import com.pranav.android.task.java.*;
import com.pranav.android.task.kotlin.KotlinCompiler;
import com.pranav.android.task.dex.D8Task;
import com.pranav.android.task.exec.ExecuteDexTask;
import com.pranav.common.util.FileUtil;
import com.pranav.java.ide.MainActivity;
import com.pranav.java.ide.R;
import com.pranav.project.mode.JavaProject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class CompileTask extends Thread {

    private long d8Time = 0;
    private long ecjTime = 0;

    private boolean showExecuteDialog = false;

    private final MainActivity activity;

    private final CompilerListeners listener;
    private final JavaBuilder builder;

    private final String STAGE_CLEAN;
    private final String STAGE_KOTLINC;
    private final String STAGE_JAVAC;
    private final String STAGE_ECJ;
    private final String STAGE_D8;
    private final String STAGE_LOADING_DEX;

    public CompileTask(MainActivity context, boolean isExecuteMethod, CompilerListeners listener) {
        this.activity = context;
        this.listener = listener;
        this.showExecuteDialog = isExecuteMethod;
        this.builder = new JavaBuilder(activity);

        STAGE_CLEAN = context.getString(R.string.stage_clean);
        STAGE_KOTLINC = context.getString(R.string.stage_kotlinc);
        STAGE_JAVAC = context.getString(R.string.stage_javac);
        STAGE_ECJ = context.getString(R.string.stage_ecj);
        STAGE_D8 = context.getString(R.string.stage_d8);
        STAGE_LOADING_DEX = context.getString(R.string.stage_loading_dex);
    }

    @Override
    public void run() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        final var prefs = activity.getSharedPreferences("compiler_settings", Context.MODE_PRIVATE);
        try {
            listener.onCurrentBuildStageChanged(STAGE_CLEAN);
            // a simple workaround to prevent calls to system.exit
            final String code =
                    activity.editor
                            .getText()
                            .toString()
                            .replace("System.exit(", "System.err.println(\"Exit code \" + ");
            final String currentPath = activity.currentWorkingFilePath;
            if (code != FileUtil.readFile(new File(currentPath))) {
                FileUtil.writeFile(currentPath, code);
            }
        } catch (final IOException e) {
            listener.onFailed(e.getMessage());
        }

        // Run kotlinc
        var time = System.currentTimeMillis();
        listener.onCurrentBuildStageChanged(STAGE_KOTLINC);
        try {
            new KotlinCompiler().doFullTask(activity.getProject());
        } catch (CompilationFailedException e) {
            listener.onFailed(e.getMessage());
            return;
        } catch (Throwable e) {
            listener.onFailed(Log.getStackTraceString(e));
            return;
        }
        // Compile Java Files
        try {
            if (prefs.getString("compiler", "Javac").equals("Javac")) {
                listener.onCurrentBuildStageChanged(STAGE_JAVAC);
                var javaTask = new JavacCompilationTask(prefs);
                javaTask.doFullTask(activity.getProject());
            } else {
                listener.onCurrentBuildStageChanged(STAGE_ECJ);
                var javaTask = new ECJCompilationTask(prefs);
                javaTask.doFullTask(activity.getProject());
            }
        } catch (CompilationFailedException e) {
            listener.onFailed(e.getMessage());
            return;
        } catch (Throwable e) {
            listener.onFailed(Log.getStackTraceString(e));
            return;
        }

        ecjTime = System.currentTimeMillis() - time;
        time = System.currentTimeMillis();

        // run d8
        listener.onCurrentBuildStageChanged(STAGE_D8);
        try {
            new D8Task().doFullTask(activity.getProject());
        } catch (Exception e) {
            listener.onFailed(e.getMessage());
            return;
        }
        d8Time = System.currentTimeMillis() - time;

        listener.onSuccess();

        // Code that executes the final dex
        try {
            listener.onCurrentBuildStageChanged(STAGE_LOADING_DEX);
            final var classes = activity.getClassesFromDex();
            if (classes == null) {
                return;
            }
            if (showExecuteDialog) {
                activity.listDialog(
                        "Select a class to execute",
                        classes,
                        (dialog, item) -> {
                            var task = new ExecuteDexTask(prefs, classes[item]);
                            try {
                                task.doFullTask(activity.getProject());
                            } catch (InvocationTargetException e) {
                                activity.dialog(
                                        "Failed...",
                                        "Runtime error: "
                                                + e.getMessage()
                                                + "\n\nSystem logs:\n"
                                                + task.getLogs(),
                                        true);
                                return;
                            } catch (Exception e) {
                                activity.dialog(
                                        "Failed...",
                                        "Couldn't execute the dex: "
                                                + e.toString()
                                                + "\n\nSystem logs:\n"
                                                + task.getLogs()
                                                + "\n"
                                                + Log.getStackTraceString(e),
                                        true);
                                return;
                            }
                            var s = new StringBuilder();

                            s.append("Compiling took: ");
                            s.append(String.valueOf(ecjTime));
                            s.append("ms, ");
                            s.append("D8");
                            s.append(" took: ");
                            s.append(String.valueOf(d8Time));
                            s.append("ms");

                            activity.dialog(s.toString(), task.getLogs(), true);
                        });
            }
        } catch (Throwable e) {
            listener.onFailed(e.getMessage());
        }
    }

    public static interface CompilerListeners {
        public void onCurrentBuildStageChanged(String stage);

        public void onSuccess();

        public void onFailed(String errorMessage);
    }
}
