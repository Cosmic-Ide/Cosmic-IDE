package com.pranav.java.ide.compiler;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;

import com.pranav.common.util.FileUtil;
import com.pranav.java.ide.MainActivity;
import com.pranav.java.ide.R;
import com.pranav.lib_android.exception.CompilationFailedException;
import com.pranav.lib_android.task.java.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class CompileTask extends Thread {

    private long d8Time = 0;
    private long ecjTime = 0;

    private boolean showExecuteDialog = false;

    private MainActivity activity;

    private CompilerListeners listener;

    public static String STAGE_CLEAN;
    public static String STAGE_JAVAC;
    public static String STAGE_ECJ;
    public static String STAGE_D8TASK;
    public static String STAGE_LOADING_DEX;

    public CompileTask(Context context, boolean isExecuteMethod, CompilerListeners listener) {
        this.activity = (MainActivity) context;
        this.listener = listener;
        this.showExecuteDialog = isExecuteMethod;

        STAGE_CLEAN = context.getString(R.string.stage_clean);
        STAGE_JAVAC = context.getString(R.string.stage_javac);
        STAGE_ECJ = context.getString(R.string.stage_ecj);
        STAGE_D8TASK = context.getString(R.string.stage_d8task);
        STAGE_LOADING_DEX = context.getString(R.string.stage_loading_dex);
    }

    @Override
    public void run() {
        Looper.prepare();

        var id = 1;
        var notification =
                new Notification.Builder(activity)
                        .setContentTitle("Building Project")
                        .setContentText("Building...")
                        .setSmallIcon(R.drawable.ic_project_logo)
                        .build();

        final var manager =
                (NotificationManager)
                        activity.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(id, notification);

        var prefs =
                activity.getSharedPreferences("compiler_settings", Context.MODE_PRIVATE);
        try {
            // Delete previous build files
            listener.onCurrentBuildStageChanged(STAGE_CLEAN);
            FileUtil.deleteFile(FileUtil.getBinDir());
            new File(FileUtil.getBinDir()).mkdirs();
            new File(activity.currentWorkingFilePath).getParentFile().mkdirs();
            // a simple workaround to prevent calls to system.exit
            FileUtil.writeFile(
                    activity.currentWorkingFilePath,
                    activity.editor
                            .getText()
                            .toString()
                            .replace("System.exit(", "System.err.print(\"Exit code \" + "));
        } catch (final IOException e) {
            activity.dialog("Cannot save program", e.getMessage(), true);
            listener.onFailed();
        }

        var errorsArePresent = false;

        // code that runs Javac
        var time = System.currentTimeMillis();
        errorsArePresent = true;
        try {
            if (prefs.getString("compiler", "Javac").equals("Javac")) {
                listener.onCurrentBuildStageChanged(STAGE_JAVAC);
                var javaTask = new JavacCompilationTask(activity.builder);
                javaTask.doFullTask();
            } else {
                listener.onCurrentBuildStageChanged(STAGE_ECJ);
                var javaTask = new ECJCompilationTask(activity.builder);
                javaTask.doFullTask();
            }
            errorsArePresent = false;
        } catch (CompilationFailedException e) {
            activity.showErr(e.getMessage());
            listener.onFailed();
        } catch (Throwable e) {
            activity.showErr(e.getMessage());
            listener.onFailed();
        }
        if (errorsArePresent) {
            return;
        }

        ecjTime = System.currentTimeMillis() - time;
        time = System.currentTimeMillis();

        // run d8
        try {
            listener.onCurrentBuildStageChanged(STAGE_D8TASK);
            new D8Task().doFullTask();
        } catch (Exception e) {
            errorsArePresent = true;
            activity.showErr(e.getMessage());
            listener.onFailed();
            return;
        }
        d8Time = System.currentTimeMillis() - time;
        // code that loads the final dex
        try {
            listener.onCurrentBuildStageChanged(STAGE_LOADING_DEX);
            final var classes = activity.getClassesFromDex();
            if (classes == null) {
                return;
            }
            listener.onSuccess();
            if (showExecuteDialog) {
                activity.listDialog(
                        "Select a class to execute",
                        classes,
                        (dialog, item) -> {
                            var task =
                                    new ExecuteJavaTask(activity.builder, classes[item]);
                            try {
                                task.doFullTask();
                            } catch (InvocationTargetException e) {
                                activity.dialog(
                                        "Failed...",
                                        "Runtime error: "
                                                + e.getMessage()
                                                + "\n\n"
                                                + e.getMessage(),
                                        true);
                            } catch (Exception e) {
                                activity.dialog(
                                        "Failed..",
                                        "Couldn't execute the dex: "
                                                + e.toString()
                                                + "\n\nSystem logs:\n"
                                                + task.getLogs(),
                                        true);
                            }
                            var s = new StringBuilder();
                            s.append("Success! Javac took: ");
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
            listener.onFailed();
            activity.showErr(e.getMessage());
        }
        manager.cancel(id);
    }

    public static interface CompilerListeners {
        public void onCurrentBuildStageChanged(String stage);

        public void onSuccess();

        public void onFailed();
    }
}
