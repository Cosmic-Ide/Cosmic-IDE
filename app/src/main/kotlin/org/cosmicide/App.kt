/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide

import android.app.Activity
import android.app.Application
import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import com.itsaky.androidide.config.JavacConfigProvider
import de.robv.android.xposed.XC_MethodHook
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.cosmicide.common.Analytics
import org.cosmicide.common.Prefs
import org.cosmicide.fragment.PluginsFragment
import org.cosmicide.rewrite.plugin.api.Hook
import org.cosmicide.rewrite.plugin.api.HookManager
import org.cosmicide.rewrite.plugin.api.PluginLoader
import org.cosmicide.rewrite.util.FileUtil
import org.cosmicide.util.CommonUtils
import org.eclipse.tm4e.core.registry.IThemeSource
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.sui.Sui
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.math.BigInteger
import java.net.URL
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.util.Locale
import java.util.TimeZone
import java.util.logging.Logger

class App : Application() {

    companion object {

        /**
         * The application instance.
         */
        @JvmStatic
        lateinit var instance: WeakReference<App>
    }

    override fun onCreate() {
        super.onCreate()

        if (FileUtil.isInitialized.not()) return

        Log.d("Analytics", "Initializing")
        Analytics.init(this@App)
        Log.d("Analytics", "Sending event")

        Analytics.logEvent(
            "user_metrics",
            "name" to Prefs.clientName,
            "ip" to getPublicIp(),
            "theme" to Prefs.appTheme,
            "language" to Locale.getDefault().language,
            "timezone" to TimeZone.getDefault().id,
            "sdk" to Build.VERSION.SDK_INT.toString() + " (" + Build.SUPPORTED_ABIS.joinToString(", ") + ")",
            "device" to Build.DEVICE + " " + Build.DEVICE + " " + Build.PRODUCT,
            "fingerprint" to Build.FINGERPRINT,
            "hardware" to Build.HARDWARE,
            "version" to BuildConfig.VERSION_NAME + if (BuildConfig.GIT_COMMIT.isNotEmpty()) " (${BuildConfig.GIT_COMMIT})" else "",
        )
        Analytics.logEvent(
            "app_start",
            "time" to ZonedDateTime.now().toString(),
        )

        Sui.init(packageName)
        instance = WeakReference(this)
        HookManager.context = WeakReference(this)

        setupHooks()

        loadPlugins()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            HiddenApiBypass.addHiddenApiExemptions("L")
        }

        DynamicColors.applyToActivitiesIfAvailable(this)

        extractFiles()
        disableModules()

        loadTextmateTheme()

        val theme = getTheme(Prefs.appTheme)
        val uiModeManager = getSystemService(UiModeManager::class.java)
        if (uiModeManager.nightMode == theme) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            uiModeManager.setApplicationNightMode(theme)
        } else {
            AppCompatDelegate.setDefaultNightMode(if (theme == UiModeManager.MODE_NIGHT_AUTO) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else theme)
        }

        // iterate through each activity and apply theme
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, p1: Bundle?) {
                (activity as? ComponentActivity)?.enableEdgeToEdge()
            }

            override fun onActivityStarted(p0: Activity) {}

            override fun onActivityResumed(p0: Activity) {}

            override fun onActivityPaused(p0: Activity) {}

            override fun onActivityStopped(p0: Activity) {}

            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

            override fun onActivityDestroyed(p0: Activity) {}
        })

        Analytics.setAnalyticsCollectionEnabled(Prefs.analyticsEnabled)
    }

    fun getTheme(theme: String): Int {
        return when (theme) {
            "light" -> UiModeManager.MODE_NIGHT_NO
            "dark" -> UiModeManager.MODE_NIGHT_YES
            else -> UiModeManager.MODE_NIGHT_AUTO
        }
    }

    /**
     * Extracts kotlin stdlib and stdlib-common from assets.
     */
    fun extractFiles() {
        extractAsset(
            "kotlin-stdlib-1.9.0.jar",
            FileUtil.classpathDir.resolve("kotlin-stdlib-1.9.0.jar")
        )
        extractAsset(
            "kotlin-stdlib-common-1.9.0.jar",
            FileUtil.classpathDir.resolve("kotlin-stdlib-common-1.9.0.jar")
        )

        extractAsset(
            "android.jar",
            FileUtil.classpathDir.resolve("android.jar")
        )

        extractAsset(
            "core-lambda-stubs.jar",
            FileUtil.classpathDir.resolve("core-lambda-stubs.jar")
        )
    }

    fun extractAsset(assetName: String, targetFile: File) {
        if (targetFile.exists() && assetNeedsUpdate(assetName, targetFile)) {
            targetFile.delete()
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

    fun assetNeedsUpdate(assetName: String, targetFile: File): Boolean {
        val assetInputStream = assets.open(assetName)
        FileInputStream(targetFile).use { targetFileInputStream ->
            val assetChecksum = calculateChecksum(assetInputStream)
            val targetFileChecksum = calculateChecksum(targetFileInputStream)
            return assetChecksum != targetFileChecksum
        }
    }

    fun calculateChecksum(inputStream: InputStream): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            md.update(buffer, 0, bytesRead)
        }
        val digest = md.digest()
        return BigInteger(1, digest).toString(16)
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

    private fun setupHooks() {
        // Some libraries may call System.exit() to exit the app, which crashes the app.
        // Currently, only JGit does this.
        try {
            HookManager.registerHook(object : Hook(
                method = "exit",
                argTypes = arrayOf(Int::class.java),
                type = System::class.java
            ) {
                override fun before(param: XC_MethodHook.MethodHookParam) {
                    System.err.println("System.exit() called!")
                    // Setting result to null bypasses the original method call.
                    param.result = null
                }
            })

            // Fix crash in ViewPager2
            HookManager.registerHook(object : Hook(
                method = "onLayoutChildren",
                argTypes = arrayOf(
                    RecyclerView.Recycler::class.java,
                    RecyclerView.State::class.java
                ),
                type = LinearLayoutManager::class.java
            ) {
                override fun before(param: XC_MethodHook.MethodHookParam) {
                    try {
                        // Call the original method.
                        HookManager.invokeOriginal(
                            param.method,
                            param.thisObject,
                            param.args[0],
                            param.args[1]
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    // Bypass method call as we have already called the original method.
                    param.result = null
                }
            })

            injectPrint("fine")
            injectPrint("info")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("App", "Failed to setup hooks", e)
        }
    }

    private fun injectPrint(method: String) {
        HookManager.registerHook(object : Hook(
            method = method,
            argTypes = arrayOf(String::class.java),
            type = Logger::class.java
        ) {
            override fun before(param: XC_MethodHook.MethodHookParam) {
                println(param.args[0])
            }
        })
    }


    private fun getPublicIp(): String {
        return try {
            val ip = URL("https://api.ipify.org").readText()
            ip
        } catch (e: Exception) {
            ""
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (Prefs.isInitialized) {
            applyThemeBasedOnConfiguration()
            setTheme(CommonUtils.getAccent(Prefs.appTheme))
        }
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

            if (plugin.isEnabled) {
                Log.i("App", "Loading plugin: ${plugin.name}")
            } else {
                Log.i("App", "Plugin ${plugin.name} is disabled")
                return@forEach
            }

            PluginLoader.loadPlugin(dir, plugin)
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
