package org.cosmic.ide

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Process
import android.util.Log

import androidx.preference.PreferenceManager

import com.google.android.material.color.DynamicColors
import com.itsaky.androidide.utils.Environment

import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.completion.KindDrawable
import org.cosmic.ide.ui.utils.dpToPx
import org.cosmic.ide.ui.theme.CustomThemeHelper
import org.cosmic.ide.ui.theme.DarkThemeHelper

import java.io.File

class ApplicationLoader : Application() {

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        val context: Context = ApplicationLoader.applicationContext()
        val dataDirectory = context.getExternalFilesDir(null)?.getAbsolutePath()
        val resources = context.getResources()
        Environment.init(File(dataDirectory, "compiler-modules"))
        FileUtil.setDataDirectory(dataDirectory)
        dpToPx.initalizeResources(resources)
        KindDrawable.setResources(resources)
        DynamicColors.applyToActivitiesIfAvailable(this)
        CustomThemeHelper.initialize(this)
        DarkThemeHelper.initialize(this)

        Thread.setDefaultUncaughtExceptionHandler {
            _, throwable ->
            val intent = Intent(getApplicationContext(), DebugActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.putExtra("error", Log.getStackTraceString(throwable))
            val pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_ONE_SHOT)

            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 100, pendingIntent)
            Process.killProcess(Process.myPid())
            System.exit(1)
        }
    }

    companion object {
        public var instance: ApplicationLoader? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }

        @JvmStatic
        fun getDefaultSharedPreferences() : SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(ApplicationLoader.applicationContext())
        }
    }
}