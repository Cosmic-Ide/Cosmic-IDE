package org.cosmic.ide

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Process

import androidx.preference.PreferenceManager

import com.google.android.material.color.DynamicColors
import com.itsaky.androidide.utils.Environment

import org.cosmic.ide.activity.DebugActivity
import org.cosmic.ide.common.util.CoroutineUtil
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.completion.KindDrawable
import org.cosmic.ide.ui.theme.CustomThemeHelper
import org.cosmic.ide.ui.theme.DarkThemeHelper
import org.cosmic.ide.util.dpToPx

import java.io.File

class ApplicationLoader : Application() {

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        val context: Context = applicationContext
        val dataDirectory = context.getExternalFilesDir(null)?.getAbsolutePath()
        val resources = context.getResources() 
        Environment.init(File(dataDirectory, "compiler-modules"))
        FileUtil.setDataDirectory(dataDirectory)
        dpToPx.initalizeResources(resources)
        KindDrawable.setResources(resources)
        CoroutineUtil.inParallel {
            DynamicColors.applyToActivitiesIfAvailable(this)
            CustomThemeHelper.initialize(this)
            DarkThemeHelper.initialize(this)
        }

        Thread.setDefaultUncaughtExceptionHandler {
            _, throwable ->
            val intent = Intent(getApplicationContext(), DebugActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.putExtra("error", throwable.stackTraceToString())
            val pendingIntent = PendingIntent.getActivity(context, 1, intent, (PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE))

            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 200, pendingIntent)
            Process.killProcess(Process.myPid())
            System.exit(1)
        }
    }

    companion object {
        public var instance: ApplicationLoader? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }

        @JvmStatic
        fun getDefaultSharedPreferences(): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(ApplicationLoader.applicationContext())
        }

        fun isDarkMode(context: Context): Boolean {
            val darkModeFlag = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return darkModeFlag == Configuration.UI_MODE_NIGHT_YES
        }
    }
}
