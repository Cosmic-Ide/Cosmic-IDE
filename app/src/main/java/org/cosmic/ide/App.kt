package org.cosmic.ide

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.itsaky.androidide.config.JavacConfigProvider
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.cosmic.ide.activity.DebugActivity
import org.cosmic.ide.common.util.CoroutineUtil
import org.cosmic.ide.common.util.FileUtil
import org.cosmic.ide.manager.ToolsManager
import org.cosmic.ide.ui.preference.Settings
import org.eclipse.tm4e.core.registry.IThemeSource
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.system.exitProcess

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        context = this
        AppCompatDelegate.setDefaultNightMode(Settings().theme)
        FileUtil.setDataDirectory(getExternalFilesDir(null)?.absolutePath!!)
        ToolsManager.init(null)
        CoroutineUtil.inParallel {
            JavacConfigProvider.disableModules()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                HiddenApiBypass.addHiddenApiExemptions("Lsun/misc/Unsafe;")
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
                        FileProviderRegistry.getInstance().tryGetInputStream("textmate/darcula.json"), "darcula.json", null
                    ),
                    "darcula"
                )
            )
            themeRegistry.loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream("textmate/QuietLight.tmTheme"), "QuietLight.tmTheme", null
                    ),
                    "QuietLight"
                )
            )
        }

        Thread.setDefaultUncaughtExceptionHandler {
            _, throwable ->
            val intent = Intent(context, DebugActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra("error", throwable.stackTraceToString())
            throwable.printStackTrace()
            startActivity(intent)
            exitProcess(0)
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        @JvmStatic
        fun getDefaultPreferences(): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }
}
