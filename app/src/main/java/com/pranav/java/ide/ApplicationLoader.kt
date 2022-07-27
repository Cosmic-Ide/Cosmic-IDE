package com.pranav.java.ide

import android.app.Application
import android.app.PendingIntent
import android.app.AlarmManager
import android.content.Intent
import android.content.Context
import android.util.Log
import android.os.Process

import com.itsaky.androidide.utils.Environment
import com.pranav.common.util.FileUtil
import com.pranav.completion.KindDrawable
import com.pranav.java.ide.ui.utils.dpToPx

import java.io.File

class ApplicationLoader : Application() {

    override fun onCreate() {
        super.onCreate()
        val mContext = getApplicationContext()
        val dataDirectory = mContext.getExternalFilesDir(null)?.getAbsolutePath()
        val resources = mContext.getResources()
        Environment.init(File(dataDirectory, "compiler-modules"))
        FileUtil.setDataDirectory(dataDirectory)
        dpToPx.initalizeResources(resources)
        KindDrawable.setResources(resources)

        Thread.setDefaultUncaughtExceptionHandler {
                _, throwable ->
                    val intent = Intent(getApplicationContext(), DebugActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.putExtra("error", Log.getStackTraceString(throwable))
                    val pendingIntent = PendingIntent.getActivity(getApplicationContext(), 11111, intent, PendingIntent.FLAG_ONE_SHOT)

                    val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 250, pendingIntent)
                    Process.killProcess(Process.myPid())
                    System.exit(1)
                }
    }
}