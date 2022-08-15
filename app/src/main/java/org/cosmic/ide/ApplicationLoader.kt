package org.cosmic.ide

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log

import com.google.android.material.color.DynamicColors
import com.itsaky.androidide.utils.Environment

import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.completion.KindDrawable
import org.cosmic.ide.ui.utils.dpToPx

import java.io.File

class ApplicationLoader : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        val dataDirectory = context.getExternalFilesDir(null)?.getAbsolutePath()
        val resources = context.getResources()
        Environment.init(File(dataDirectory, "compiler-modules"))
        FileUtil.setDataDirectory(dataDirectory)
        dpToPx.initalizeResources(resources)
        KindDrawable.setResources(resources)
        DynamicColors.applyToActivitiesIfAvailable(this)

        Thread.setDefaultUncaughtExceptionHandler {
            _, throwable ->
            val intent = Intent(getApplicationContext(), DebugActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.putExtra("error", Log.getStackTraceString(throwable))
            val pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, intent, PendingIntent.FLAG_ONE_SHOT)

            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 250, pendingIntent)
            Process.killProcess(Process.myPid())
            System.exit(1)
        }
    }

    companion object {
        @Suppress("StaticFieldLeak")
        @JvmStatic
        var context: Context? = null
    }
}
