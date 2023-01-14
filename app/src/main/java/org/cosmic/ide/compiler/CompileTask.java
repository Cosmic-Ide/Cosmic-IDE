package org.cosmic.ide.compiler;

import android.content.Intent;
import android.os.Looper;
import android.util.Log;
import org.cosmic.ide.App;
import org.cosmic.ide.R;
import org.cosmic.ide.android.exception.CompilationFailedException;
import org.cosmic.ide.android.task.dex.D8Task;
import org.cosmic.ide.android.task.java.JavaCompiler;
import org.cosmic.ide.android.task.kotlin.KotlinCompiler;
import org.cosmic.ide.fragment.HomeFragment;
import org.cosmic.ide.fragment.HomeFragmentDirections;
import org.cosmic.ide.util.Constants;

public class CompileTask extends Thread {

    private boolean showExecuteDialog;

    private final HomeFragment fragment;

    private final CompilerListeners listener;
    private final Compilers compilers;

    private final String STAGE_KOTLINC;
    private final String STAGE_JAVAC;
    private final String STAGE_D8;

    public CompileTask(HomeFragment fragment, CompilerListeners listener) {
        this.fragment = fragment;
        this.listener = listener;
        this.compilers =
                new Compilers(
                        new JavaCompiler(App.getDefaultPreferences()),
                        new D8Task());

        final var context = fragment.getActivity();
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
            new KotlinCompiler().doFullTask(fragment.getProject());
        } catch (CompilationFailedException e) {
            listener.onFailed(e.getLocalizedMessage());
        } catch (Throwable e) {
            listener.onFailed(Log.getStackTraceString(e));
        }
    }

    private void compileJava() {
        try {
            listener.onCurrentBuildStageChanged(STAGE_JAVAC);
            compilers.getJava().doFullTask(fragment.getProject());
        } catch (CompilationFailedException e) {
            listener.onFailed(e.getLocalizedMessage());
        } catch (Throwable e) {
            listener.onFailed(Log.getStackTraceString(e));
        }
    }

    private void compileDex() {
        try {
            listener.onCurrentBuildStageChanged(STAGE_D8);
            compilers.getDex().doFullTask(fragment.getProject());
        } catch (Exception e) {
            listener.onFailed(e.getLocalizedMessage());
        }
    }

    private void executeDex() {
        try {
            listener.onSuccess();
            final var classes = fragment.getClassesFromDex();
            if (classes == null) {
                return;
            }
            if (showExecuteDialog) {
                // if there is only one class, there is no need to show a dialog
                if (classes.length == 1) {
                    fragment.showConsoleFragmentFromCompileTask(fragment.getProject().getProjectDirPath(), classes[0]);
                    return;
                }
                fragment.listDialog(
                        fragment.getActivity().getString(R.string.select_class_run),
                        classes,
                        (dialog, item) -> {
                            fragment.showConsoleFragmentFromCompileTask(fragment.getProject().getProjectDirPath(), classes[item]);
                        });
            }
        } catch (Throwable e) {
            listener.onFailed(e.getLocalizedMessage());
        }
    }

    public interface CompilerListeners {
        boolean isSuccessTillNow();
        void onCurrentBuildStageChanged(String stage);
        void onFailed(String errorMessage);
        void onSuccess();
    }
}
