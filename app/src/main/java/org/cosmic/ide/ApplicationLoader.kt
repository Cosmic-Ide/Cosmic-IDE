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
import com.itsaky.androidide.config.JavacConfigProvider
import org.cosmic.ide.activity.DebugActivity
import org.cosmic.ide.common.util.CoroutineUtil
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.ui.theme.DarkThemeHelper
import org.cosmic.ide.util.dpToPx
import java.io.File
import kotlin.system.exitProcess

class ApplicationLoader : Application() {

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        val dataDirectory = applicationContext.getExternalFilesDir(null)?.getAbsolutePath()
        val resources = applicationContext.getResources()
        JavacConfigProvider.disableModules()
//        System.setProperty(JavacConfigProvider.PROP_ANDROIDIDE_JAVA_HOME, dataDirectory)
        FileUtil.setDataDirectory(dataDirectory)
        dpToPx.initalizeResources(resources)
        CoroutineUtil.inParallel {
            DynamicColors.applyToActivitiesIfAvailable(this)
            DarkThemeHelper.initialize(this)
        }

        Thread.setDefaultUncaughtExceptionHandler {
            _, throwable ->
            val intent = Intent(applicationContext, DebugActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.putExtra("error", throwable.stackTraceToString())
            val pendingIntent = PendingIntent.getActivity(applicationContext, 1, intent, (PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE))

            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 200, pendingIntent)
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    }

    companion object {
        public var instance: ApplicationLoader? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }

        @JvmStatic
        fun getDefaultSharedPreferences(): SharedPreferences {
            return PreferenceManager.getDefaultSharedPreferences(applicationContext())
        }

        fun isDarkMode(context: Context): Boolean {
            val darkModeFlag = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return darkModeFlag == Configuration.UI_MODE_NIGHT_YES
        }
    }
}
