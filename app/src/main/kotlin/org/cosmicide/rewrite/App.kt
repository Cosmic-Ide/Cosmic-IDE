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
import android.os.StrictMode
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.developer.crashx.config.CrashConfig
import com.google.android.material.color.DynamicColors
import com.itsaky.androidide.config.JavacConfigProvider
import de.robv.android.xposed.XC_MethodHook
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.cosmicide.rewrite.common.Analytics
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.fragment.PluginsFragment
import org.cosmicide.rewrite.plugin.api.Hook
import org.cosmicide.rewrite.plugin.api.HookManager
import org.cosmicide.rewrite.util.FileUtil
import org.cosmicide.rewrite.util.MultipleDexClassLoader
import org.eclipse.tm4e.core.registry.IThemeSource
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File
import java.io.FileNotFoundException
import java.lang.ref.WeakReference
import java.lang.reflect.Modifier
import java.time.ZonedDateTime
import java.util.concurrent.Executors

class App : Application() {

    companion object {
        /**
         * The application instance.
         */
        @JvmStatic
        lateinit var instance: WeakReference<App>

        /**
         * The class loader used to load plugins.
         */
        @JvmStatic
        val loader = MultipleDexClassLoader.INSTANCE
    }

    override fun onCreate() {
        super.onCreate()
        instance = WeakReference(this)

        CrashConfig.Builder.create()
            .showRestartButton(true)
            .trackActivities(true)
            .apply()

        val externalStorage = getExternalFilesDir(null)!!

        Prefs.init(applicationContext)
        FileUtil.init(externalStorage)

        loadPlugins()

        // Some libraries may call System.exit() to exit the app, which crashes the app.
        // Currently, only JGit does this.
        HookManager.registerHook(object : Hook(
            method = "exit",
            argTypes = arrayOf(Int::class.java),
            type = System::class.java
        ) {
            override fun before(param: XC_MethodHook.MethodHookParam) {
                System.err.println("System.exit() called!")
                param.result = null
            }
        })

        // Fix crash in ViewPager2
        HookManager.registerHook(object : Hook(
            method = "onLayoutChildren",
            argTypes = arrayOf(RecyclerView.Recycler::class.java, RecyclerView.State::class.java),
            type = LinearLayoutManager::class.java
        ) {
            override fun before(param: XC_MethodHook.MethodHookParam) {
                try {
                    HookManager.invokeOriginal(
                        param.method,
                        param.thisObject,
                        param.args[0],
                        param.args[1]
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                param.result = null
            }
        })

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().apply {
                    detectLeakedRegistrationObjects()
                    detectActivityLeaks()
                    detectContentUriWithoutPermission()
                    detectFileUriExposure()
                    detectCleartextNetwork()
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        penaltyLog()
                        return@apply
                    }
                    permitNonSdkApiUsage()
                    penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                        Log.e("StrictMode", "VM violation", violation)
                        violation.printStackTrace()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        detectIncorrectContextUse()
                        detectUnsafeIntentLaunch()
                    }
                }.build()
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            HiddenApiBypass.addHiddenApiExemptions("Lsun/misc/Unsafe;")
        }

       DynamicColors.applyToActivitiesIfAvailable(this)


        extractFiles()
        disableModules()

        loadTextmateTheme()

        Analytics.logEvent(
            "startup",
            "theme" to Prefs.appTheme,
            "time" to ZonedDateTime.now().toString(),
            "device" to Build.DEVICE,
            "model" to Build.MODEL,
            "manufacturer" to Build.MANUFACTURER,
            "sdk" to Build.VERSION.SDK_INT.toString(),
            "abi" to Build.SUPPORTED_ABIS.joinToString(),
            "version" to BuildConfig.VERSION_NAME + if (BuildConfig.GIT_COMMIT.isNotEmpty()) " (${BuildConfig.GIT_COMMIT})" else "",
        )

        Analytics.setAnalyticsCollectionEnabled(Prefs.analyticsEnabled)
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

    fun getTheme(theme: String): Int {
        return when (theme) {
            "light" -> UiModeManager.MODE_NIGHT_NO
            "dark" -> UiModeManager.MODE_NIGHT_YES
            else -> UiModeManager.MODE_NIGHT_AUTO
        }
    }

    fun extractFiles() {
        extractAsset(
            "kotlin-stdlib-1.8.0.jar",
            FileUtil.classpathDir.resolve("kotlin-stdlib-1.8.0.jar")
        )
    }

    fun extractAsset(assetName: String, targetFile: File) {
        if (targetFile.exists()) {
            return
        }
        try {
            assets.open(assetName).use { inputStream ->
                targetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: FileNotFoundException) {
            Log.e("App", "Failed to extract asset: $assetName", e)
        }
    }

    fun disableModules() {
        JavacConfigProvider.disableModules()
    }

    fun loadTextmateTheme() {
        val fileProvider = AssetsFileResolver(assets)
        FileProviderRegistry.getInstance().addFileProvider(fileProvider)

        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")

        val themeRegistry = ThemeRegistry.getInstance()
        themeRegistry.loadTheme(loadTheme("darcula.json", "darcula"))
        themeRegistry.loadTheme(loadTheme("QuietLight.tmTheme.json", "QuietLight"))

        applyThemeBasedOnConfiguration()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyThemeBasedOnConfiguration()
    }

    fun applyThemeBasedOnConfiguration() {
        val themeName =
            when (getTheme(Prefs.appTheme)) {
                AppCompatDelegate.MODE_NIGHT_YES -> "darcula"
                AppCompatDelegate.MODE_NIGHT_NO -> "QuietLight"
                else -> {
                    when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                        Configuration.UI_MODE_NIGHT_YES -> "darcula"
                        else -> "QuietLight"
                    }
                }
            }
        ThemeRegistry.getInstance().setTheme(themeName)
    }

    fun loadPlugins() {
        PluginsFragment.getPlugins().forEach { plugin ->
            val dir = FileUtil.pluginDir.resolve(plugin.name)
            val pluginFile =
                dir.resolve("classes.dex")
            if (dir.resolve("config.json").exists().not()) {
                Log.e("Plugin", "Plugin ${plugin.name} is missing config.json")
                return@forEach
            }
            if (pluginFile.exists().not()) {
                Log.e("Plugin", "Plugin ${plugin.name} is missing classes.dex")
                return@forEach
            }
            runCatching {
                loader.loadDex(pluginFile)
                val className = plugin.name.lowercase() + ".Main"
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
            }.onSuccess {
                Log.d("Plugin", "Loaded plugin ${plugin.name}")
            }.onFailure {
                Log.e("Plugin", "Failed to load plugin ${plugin.name}", it)
            }
        }
    }

    fun loadTheme(fileName: String, themeName: String): ThemeModel {
        val inputStream =
            FileProviderRegistry.getInstance().tryGetInputStream("textmate/$fileName")
                ?: throw FileNotFoundException("Theme file not found: $fileName")
        val source = IThemeSource.fromInputStream(inputStream, fileName, null)
        return ThemeModel(source, themeName)
    }
}