package com.pranav.java.ide

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.util.Log

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
                    startActivity(intent)
                    System.exit(1)
                }
    }
}
