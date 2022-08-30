package org.cosmic.ide.compiler;

import android.content.Intent;
import android.os.Looper;
import android.util.Log;

import org.cosmic.ide.R;
import org.cosmic.ide.ApplicationLoader;
import org.cosmic.ide.activity.MainActivity;
import org.cosmic.ide.activity.ConsoleActivity;
import org.cosmic.ide.android.exception.CompilationFailedException;
import org.cosmic.ide.android.task.dex.D8Task;
import org.cosmic.ide.android.task.exec.ExecuteDexTask;
import org.cosmic.ide.android.task.java.*;
import org.cosmic.ide.android.task.kotlin.KotlinCompiler;
import org.cosmic.ide.common.util.FileUtil;
import org.cosmic.ide.util.Constants;

import java.io.File;

public class CompileTask extends Thread {

    private long d8Time = 0;
    private long ecjTime = 0;

    private boolean showExecuteDialog = false;

    private final MainActivity activity;

    private final CompilerListeners listener;

    private final String STAGE_CLEAN;
    private final String STAGE_KOTLINC;
    private final String STAGE_JAVAC;
    private final String STAGE_ECJ;
    private final String STAGE_D8;

    public CompileTask(MainActivity context, boolean isExecuteMethod, CompilerListeners listener) {
        this.activity = context;
        this.listener = listener;
        this.showExecuteDialog = isExecuteMethod;

        STAGE_CLEAN = context.getString(R.string.stage_clean);
        STAGE_KOTLINC = context.getString(R.string.stage_kotlinc);
        STAGE_JAVAC = context.getString(R.string.stage_javac);
        STAGE_ECJ = context.getString(R.string.stage_ecj);
        STAGE_D8 = context.getString(R.string.stage_d8);
    }

    @Override
    public void run() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        try {
            listener.onCurrentBuildStageChanged(STAGE_CLEAN);
            activity.saveAll();
        } catch (Exception e) {
            listener.onFailed(e.getMessage());
            return;
        }

        var time = System.currentTimeMillis();
        compileKotlin();
        if (!listener.isSuccessTillNow()) return;

        compileJava();
        if (!listener.isSuccessTillNow()) return;

        ecjTime = System.currentTimeMillis() - time;
        time = System.currentTimeMillis();

        compileDex();
        if (!listener.isSuccessTillNow()) return;
 
        d8Time = System.currentTimeMillis() - time;

        executeDex();
    }

    private void compileKotlin() {
        try {
            listener.onCurrentBuildStageChanged(STAGE_KOTLINC);
            new KotlinCompiler().doFullTask(activity.getProject());
        } catch (CompilationFailedException e) {
            listener.onFailed(e.getMessage());
        } catch (Throwable e) {
            listener.onFailed(Log.getStackTraceString(e));
        }
    }

    private void compileJava() {
        final var prefs = ApplicationLoader.getDefaultSharedPreferences();
        try {
            if (prefs.getString("key_java_compiler", activity.getString(R.string.javac))
                    .equals(activity.getString(R.string.javac))) {
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
        } catch (Throwable e) {
            listener.onFailed(Log.getStackTraceString(e));
        }
    }

    private void compileDex() {
        try {
            listener.onCurrentBuildStageChanged(STAGE_D8);
            new D8Task().doFullTask(activity.getProject());
        } catch (Exception e) {
            listener.onFailed(e.getMessage());
        }
    }

    private void executeDex() {
        try {
            listener.onSuccess();
            final var classes = activity.getClassesFromDex();
            if (classes == null) {
                return;
            }
            if (showExecuteDialog) {
                activity.listDialog(
                        "Select a class to execute",
                        classes,
                        (dialog, item) -> {
                            var intent = new Intent(activity, ConsoleActivity.class);
                            intent.putExtra(Constants.PROJECT_PATH, activity.getProject().getProjectDirPath());
                            intent.putExtra("class_to_execute", classes[item]);
                            activity.startActivity(intent);
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

        public boolean isSuccessTillNow();
    }
}
