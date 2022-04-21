package com.pranav.java.ide;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;
import com.pranav.lib_android.util.FileUtil;

public final class ApplicationLoader extends Application {

	private Context mContext;

	@Override
	public void onCreate() {
		mContext = getApplicationContext();
		FileUtil.initializeContext(mContext);
		Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
				Thread.getDefaultUncaughtExceptionHandler();

		Thread.setDefaultUncaughtExceptionHandler(
				(thread, throwable) -> {
					Intent intent = new Intent(getApplicationContext(), DebugActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
					intent.putExtra("error", Log.getStackTraceString(throwable));
					PendingIntent pendingIntent =
							PendingIntent.getActivity(
									getApplicationContext(), 11111, intent, PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE);

					AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
					am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, pendingIntent);

					Process.killProcess(Process.myPid());
					System.exit(1);

					uncaughtExceptionHandler.uncaughtException(thread, throwable);
				});
		super.onCreate();
	}
}
