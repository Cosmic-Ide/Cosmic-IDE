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
import org.cosmic.ide.android.task.java.JavaCompiler;
import org.cosmic.ide.android.task.kotlin.KotlinCompiler;
import org.cosmic.ide.util.Constants;

public class CompileTask extends Thread {

    private boolean showExecuteDialog;

    private final MainActivity activity;

    private final CompilerListeners listener;
    private final Compilers compilers;

    private final String STAGE_KOTLINC;
    private final String STAGE_JAVAC;
    private final String STAGE_D8;

    public CompileTask(MainActivity context, CompilerListeners listener) {
        this.activity = context;
        this.listener = listener;
        this.compilers =
                new Compilers(
                        new JavaCompiler(App.getDefaultPreferences()),
                        new D8Task());

        STAGE_KOTLINC = context.getString(R.string.compilation_stage_kotlinc);
        STAGE_JAVAC = context.getString(R.string.compilation_stage_javac);
        STAGE_D8 = context.getString(R.string.compilation_stage_d8);
    }

    @Override
    public void run() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        compileKotlin();
        if (!listener.isSuccessTillNow()) return;

        compileJava();
        if (!listener.isSuccessTillNow()) return;

        compileDex();
        if (!listener.isSuccessTillNow()) return;

        executeDex();
    }

    public void setExecution(boolean enable) {
        showExecuteDialog = enable;
    }

    private void compileKotlin() {
        try {
            listener.onCurrentBuildStageChanged(STAGE_KOTLINC);
            new KotlinCompiler().doFullTask(activity.getProject());
        } catch (CompilationFailedException e) {
            listener.onFailed(e.getLocalizedMessage());
        } catch (Throwable e) {
            listener.onFailed(Log.getStackTraceString(e));
        }
    }

    private void compileJava() {
        try {
            listener.onCurrentBuildStageChanged(STAGE_JAVAC);
            compilers.getJava().doFullTask(activity.getProject());
        } catch (CompilationFailedException e) {
            listener.onFailed(e.getLocalizedMessage());
        } catch (Throwable e) {
            listener.onFailed(Log.getStackTraceString(e));
        }
    }

    private void compileDex() {
        try {
            listener.onCurrentBuildStageChanged(STAGE_D8);
            compilers.getDex().doFullTask(activity.getProject());
        } catch (Exception e) {
            listener.onFailed(e.getLocalizedMessage());
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
                final var intent = new Intent(activity, ConsoleActivity.class);
                intent.putExtra(
                        Constants.PROJECT_PATH, activity.getProject().getProjectDirPath());
                if (classes.length == 1) {
                    intent.putExtra("class_to_execute", classes[0]);
                    activity.startActivity(intent);
                    return;
                }
                activity.listDialog(
                        activity.getString(R.string.select_class_run),
                        classes,
                        (dialog, item) -> {
                            intent.putExtra("class_to_execute", classes[item]);
                            activity.startActivity(intent);
                        });
            }
        } catch (Throwable e) {
            listener.onFailed(Log.getStackTraceString(e));
        }
    }

    public interface CompilerListeners {
        void onCurrentBuildStageChanged(String stage);

        void onSuccess();

        void onFailed(String errorMessage);

        boolean isSuccessTillNow();
    }
}
