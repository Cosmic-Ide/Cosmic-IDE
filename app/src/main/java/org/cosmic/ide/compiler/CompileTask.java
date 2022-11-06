package org.cosmic.ide.compiler;

import android.content.Intent;
import android.os.Looper;
import android.util.Log;

import org.cosmic.ide.App;
import org.cosmic.ide.R;
import org.cosmic.ide.activity.ConsoleActivity;
import org.cosmic.ide.activity.MainActivity;
import org.cosmic.ide.android.exception.CompilationFailedException;
import org.cosmic.ide.android.task.dex.D8Task;
import org.cosmic.ide.android.task.java.JavacCompilationTask;
import org.cosmic.ide.android.task.kotlin.KotlinCompiler;
import org.cosmic.ide.util.Constants;

public class CompileTask extends Thread {

    private final boolean showExecuteDialog;

    private final MainActivity activity;

    private final CompilerListeners listener;

    private final String STAGE_CLEAN;
    private final String STAGE_KOTLINC;
    private final String STAGE_JAVAC;
    private final String STAGE_D8;

    public CompileTask(MainActivity context, boolean isExecuteMethod, CompilerListeners listener) {
        this.activity = context;
        this.listener = listener;
        this.showExecuteDialog = isExecuteMethod;

        STAGE_CLEAN = context.getString(R.string.stage_clean);
        STAGE_KOTLINC = context.getString(R.string.stage_kotlinc);
        STAGE_JAVAC = context.getString(R.string.stage_javac);
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

        compileKotlin();
        if (!listener.isSuccessTillNow()) return;

        compileJava();
        if (!listener.isSuccessTillNow()) return;

        compileDex();
        if (!listener.isSuccessTillNow()) return;

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
        final var prefs = App.getDefaultSharedPreferences();
        try {
            listener.onCurrentBuildStageChanged(STAGE_JAVAC);
            new JavacCompilationTask(prefs).doFullTask(activity.getProject());
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
                // if there is only one class, there is no need to show a dialog
                if (classes.length == 1) {
                    final var intent = new Intent(activity, ConsoleActivity.class);
                    intent.putExtra(
                            Constants.PROJECT_PATH, activity.getProject().getProjectDirPath());
                    intent.putExtra("class_to_execute", classes[0]);
                    activity.startActivity(intent);
                    return;
                }
                activity.listDialog(
                        "Select a class to execute",
                        classes,
                        (dialog, item) -> {
                            var intent = new Intent(activity, ConsoleActivity.class);
                            intent.putExtra(
                                    Constants.PROJECT_PATH,
                                    activity.getProject().getProjectDirPath());
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
