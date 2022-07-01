package com.pranav.java.ide

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.util.Log

import com.google.android.material.color.DynamicColors

import com.pranav.common.util.FileUtil
import com.pranav.completion.KindDrawable
import com.pranav.java.ide.ui.utils.dpToPx

class ApplicationLoader : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        val mContext = getApplicationContext()
        val dataDirectory = mContext.getExternalFilesDir(null)?.getAbsolutePath()
        val resources = mContext.getResources()
        FileUtil.setDataDirectory(dataDirectory)
        dpToPx.initalizeResources(resources)
        KindDrawable.setResources(resources)

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
