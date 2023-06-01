/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite

import android.app.Application
import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.developer.crashx.config.CrashConfig
import com.google.android.material.color.DynamicColors
import com.itsaky.androidide.config.JavacConfigProvider
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.cosmicide.rewrite.common.Analytics
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.fragment.PluginsFragment
import org.cosmicide.rewrite.util.FileUtil
import org.cosmicide.rewrite.util.MultipleDexClassLoader
import org.eclipse.tm4e.core.registry.IThemeSource
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File
import java.io.FileNotFoundException
import java.lang.reflect.Modifier
import java.time.ZonedDateTime

class App : Application() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var indexFile: File
    private val loader: MultipleDexClassLoader by lazy {
        MultipleDexClassLoader(classLoader = javaClass.classLoader!!)
    }

    override fun onCreate() {
        super.onCreate()

        FileUtil.init(applicationContext)
        Prefs.init(applicationContext)
        Analytics.init(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            HiddenApiBypass.addHiddenApiExemptions("Lsun/misc/Unsafe;")
        }

        DynamicColors.applyToActivitiesIfAvailable(this)

        indexFile = FileUtil.dataDir.resolve(INDEX_FILE_NAME)
        extractFiles()
        disableModules()

        scope.launch {
            loadTextmateTheme()
        }

        loadPlugins()

        Analytics.logEvent("theme", "theme" to Prefs.appTheme)
        Analytics.logEvent(
            "startup",
            "time" to ZonedDateTime.now().toString(),
            "device" to Build.DEVICE,
            "model" to Build.MODEL,
            "manufacturer" to Build.MANUFACTURER,
            "sdk" to Build.VERSION.SDK_INT.toString(),
            "abi" to Build.SUPPORTED_ABIS.joinToString()
        )

        CrashConfig.Builder
            .create()
            .backgroundMode(CrashConfig.BACKGROUND_MODE_SHOW_CUSTOM)
            .errorActivity(CrashActivity::class.java)
            .enabled(true)
            .showRestartButton(true)
            .trackActivities(true)
            .apply()

        val theme = getTheme(Prefs.appTheme)
        val uiModeManager = getSystemService(UiModeManager::class.java)
        if (uiModeManager.nightMode == theme) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            uiModeManager.setApplicationNightMode(theme)
        } else {
            AppCompatDelegate.setDefaultNightMode(if (theme == UiModeManager.MODE_NIGHT_AUTO) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else theme)
        }
    }

    private fun getTheme(theme: String): Int {
        return when (theme) {
            "light" -> UiModeManager.MODE_NIGHT_NO
            "dark" -> UiModeManager.MODE_NIGHT_YES
            else -> UiModeManager.MODE_NIGHT_AUTO
        }
    }

    private fun extractFiles() {
        scope.launch { extractAsset(INDEX_FILE_NAME, indexFile) }
        scope.launch { extractAsset(ANDROID_JAR, FileUtil.classpathDir.resolve(ANDROID_JAR)) }
        scope.launch { extractAsset(KOTLIN_STDLIB, FileUtil.classpathDir.resolve(KOTLIN_STDLIB)) }
        scope.launch { extractAsset("rt.jar", FileUtil.dataDir.resolve("rt.jar")) }
    }

    private fun extractAsset(assetName: String, outputFile: File) {
        if (outputFile.exists()) {
            return
        }
        assets.open(assetName).use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun disableModules() {
        JavacConfigProvider.disableModules()
    }

    private fun loadTextmateTheme() {
        val fileProvider = AssetsFileResolver(assets)
        FileProviderRegistry.getInstance().addFileProvider(fileProvider)

        GrammarRegistry.getInstance().loadGrammars(LANGUAGES_FILE_PATH)

        val themeRegistry = ThemeRegistry.getInstance()
        themeRegistry.loadTheme(loadTheme(DARCULA_THEME_FILE_NAME, DARCULA_THEME_NAME))
        themeRegistry.loadTheme(loadTheme(QUIET_LIGHT_THEME_FILE_NAME, QUIET_LIGHT_THEME_NAME))

        applyThemeBasedOnConfiguration()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyThemeBasedOnConfiguration()
    }

    private fun applyThemeBasedOnConfiguration() {
        val themeName =
            when (getTheme(Prefs.appTheme)) {
                AppCompatDelegate.MODE_NIGHT_YES -> DARCULA_THEME_NAME
                AppCompatDelegate.MODE_NIGHT_NO -> QUIET_LIGHT_THEME_NAME
                else -> {
                    when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                        Configuration.UI_MODE_NIGHT_YES -> DARCULA_THEME_NAME
                        else -> QUIET_LIGHT_THEME_NAME
                    }
                }
            }
        ThemeRegistry.getInstance().setTheme(themeName)
    }

    fun loadPlugins() {
        scope.launch {
            PluginsFragment.getPlugins().forEach { plugin ->
                val pluginFile = FileUtil.dataDir.resolve(plugin.getName() + ".dex")
                loader.loadDex(pluginFile)
                val className = plugin.getName().toLowerCase() + ".Main"
                val clazz = loader.loader.loadClass(className)
                val method = clazz.getDeclaredMethod("main", Array<String>::class.java)
                if (Modifier.isStatic(method.modifiers)) {
                    method.invoke(null, arrayOf<String>())
                } else {
                    method.invoke(
                        clazz.getDeclaredConstructor().newInstance(),
                        arrayOf<String>()
                    )
                }
            }
        }
    }

    private fun loadTheme(fileName: String, themeName: String): ThemeModel {
        val inputStream =
            FileProviderRegistry.getInstance().tryGetInputStream("$TEXTMATE_DIR/$fileName")
                ?: throw FileNotFoundException("Theme file not found: $fileName")
        val source = IThemeSource.fromInputStream(inputStream, fileName, null)
        return ThemeModel(source, themeName)
    }

    companion object {
        private const val INDEX_FILE_NAME = "index.json"
        private const val ANDROID_JAR = "android.jar"
        private const val KOTLIN_STDLIB = "kotlin-stdlib-1.8.0.jar"

        private const val LANGUAGES_FILE_PATH = "textmate/languages.json"
        private const val TEXTMATE_DIR = "textmate"

        private const val DARCULA_THEME_FILE_NAME = "darcula.json"
        private const val DARCULA_THEME_NAME = "darcula"
        private const val QUIET_LIGHT_THEME_FILE_NAME = "QuietLight.tmTheme.json"
        private const val QUIET_LIGHT_THEME_NAME = "QuietLight"
    }
}