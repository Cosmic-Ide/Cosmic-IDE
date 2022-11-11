package org.cosmic.ide

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.itsaky.androidide.config.JavacConfigProvider
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.cosmic.ide.activity.DebugActivity
import org.cosmic.ide.common.util.CoroutineUtil
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.util.dpToPx
import org.eclipse.tm4e.core.registry.IThemeSource
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.system.exitProcess

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        context = this
        DynamicColors.applyToActivitiesIfAvailable(this)
        FileUtil.setDataDirectory(context.getExternalFilesDir(null)?.getAbsolutePath()!!)
        CoroutineUtil.inParallel {
            JavacConfigProvider.disableModules()
            dpToPx.resources = context.getResources()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                HiddenApiBypass.addHiddenApiExemptions("Lsun/misc/Unsafe")
            }
            FileProviderRegistry.getInstance().addFileProvider(
                AssetsFileResolver(
                    context.assets
                )
            )
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
            val themeRegistry = ThemeRegistry.getInstance()
            themeRegistry.loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream("textmate/darcula.json"), "darcula", null
                    )
                )
            )
            themeRegistry.loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream("textmate/QuietLight.tmTheme"), "QuietLight", null
                    )
                )
            )
        }

        Thread.setDefaultUncaughtExceptionHandler {
            _, throwable ->
            val intent = Intent(context, DebugActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("error", throwable.stackTraceToString())
            Log.e("Crash", throwable.message, throwable)
            val pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 200, pendingIntent)
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    }

    companion object {
        lateinit var context: Context

        @JvmStatic
        fun getDefaultPreferences() = PreferenceManager.getDefaultSharedPreferences(context)

        fun isDarkMode(context: Context): Boolean {
            val darkModeFlag = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return darkModeFlag == Configuration.UI_MODE_NIGHT_YES
        }
    }
}
