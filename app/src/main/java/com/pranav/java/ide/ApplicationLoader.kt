package com.pranav.java.ide

import android.app.PendingIntent
import android.content.Intent
import android.util.Log

import androidx.multidex.MultiDexApplication

import com.google.android.material.color.DynamicColors

import com.pranav.common.util.FileUtil
import com.pranav.java.ide.ui.utils.dpToPx

class ApplicationLoader : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        val mContext = getApplicationContext()
        val dataDirectory = mContext.getExternalFilesDir(null)?.getAbsolutePath()
        FileUtil.setDataDirectory(dataDirectory)
        dpToPx.initalizeResources(mContext.getResources())

        Thread.setDefaultUncaughtExceptionHandler {
                _, throwable ->
                    val intent = Intent(getApplicationContext(), DebugActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.putExtra("error", Log.getStackTraceString(throwable))
                    startActivity(intent)
                    System.exit(1)
                }
    }
}
