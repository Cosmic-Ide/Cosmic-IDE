package com.pranav.java.ide;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import com.google.android.material.color.DynamicColors;

import com.pranav.common.util.FileUtil;
import com.pranav.java.ide.ui.utils.dpToPx;

public final class ApplicationLoader extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
        final var mContext = getApplicationContext();
        final var dataDirectory = mContext.getExternalFilesDir(null).getAbsolutePath();
        FileUtil.setDataDirectory(dataDirectory);
        dpToPx.initalizeResources(mContext.getResources());

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

                    System.exit(1);
                });
    }
}
