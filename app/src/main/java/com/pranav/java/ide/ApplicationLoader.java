package com.pranav.java.ide;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

import com.pranav.common.util.FileUtil;
import com.pranav.java.ide.ui.utils.dpToPx;

public final class ApplicationLoader extends Application {

    @Override
    public void onCreate() {
        final var mContext = getApplicationContext();
        final var dataDirectory = mContext.getExternalFilesDir(null).getAbsolutePath();
        FileUtil.setDataDirectory(dataDirectory);
        dpToPx.initalizeResources(mContext.getResources());
        var uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(
                (thread, throwable) -> {
                    var intent = new Intent(getApplicationContext(), DebugActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("error", Log.getStackTraceString(throwable));
                    var pendingIntent =
                            PendingIntent.getActivity(
                                    getApplicationContext(),
                                    11111,
                                    intent,
                                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

                    var am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, pendingIntent);

                    Process.killProcess(Process.myPid());
                    System.exit(1);

                    uncaughtExceptionHandler.uncaughtException(thread, throwable);
                });
        super.onCreate();
    }
}
