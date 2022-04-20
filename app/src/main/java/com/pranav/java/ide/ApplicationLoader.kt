package com.pranav.java.ide

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import com.pranav.lib_android.util.FileUtil

class ApplicationLoader : Application() {

	private lateinit var mContext: Context

	override fun onCreate() {
	super.onCreate()
		mContext = getApplicationContext()
		FileUtil.initializeContext(mContext)
		val uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

		Thread.setDefaultUncaughtExceptionHandler({
				thread, throwable -> 
					val intent = Intent(mContex, DebugActivity.class)
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
					intent.putExtra("error", Log.getStackTraceString(throwable))
					val pendingIntent =
							PendingIntent.getActivity(
									mContext, 11111, intent, PendingIntent.FLAG_ONE_SHOT)

					val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
					am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, pendingIntent)

					Process.killProcess(Process.myPid())
					System.exit(0)

					uncaughtExceptionHandler.uncaughtException(thread, throwable)
				})
	}

	fun getContext(): Context =  mContext
}
