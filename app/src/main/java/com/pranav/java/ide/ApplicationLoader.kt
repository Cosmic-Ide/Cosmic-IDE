package com.pranav.java.ide

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log

import com.pranav.lib_android.util.FileUtil

class ApplicationLoader: Application() {

    private var uncaughtExceptionHandler: Thread.UncaughtExceptionHandler
	private var mContext: Context

    override fun onCreate() {
		mContext = getApplicationContext()
		FileUtil.initializeContext(mContext)
        this.uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

		Thread.setDefaultUncaughtExceptionHandler(thread, throwable -> {
			val intent = Intent(getApplicationContext(), DebugActivity::class.java)
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
			intent.putExtra("error", Log.getStackTraceString(throwable))
			PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 11111, intent, PendingIntent.FLAG_ONE_SHOT)

			AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE)
			am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, pendingIntent)

			Process.killProcess(Process.myPid())
			exitProcess(1)

			uncaughtExceptionHandler.uncaughtException(thread, throwable)
		})
		super.onCreate()
	}
	
	fun getContext(): Context = mContext
}
