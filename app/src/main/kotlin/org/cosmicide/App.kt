/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.UiModeManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import com.sun.tools.javac.ConfigProvider
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.cosmicide.common.Analytics
import org.cosmicide.common.Prefs
import org.cosmicide.fragment.PluginsFragment
import org.cosmicide.rewrite.plugin.api.Hook
import org.cosmicide.rewrite.plugin.api.HookManager
import org.cosmicide.rewrite.plugin.api.PluginLoader
import org.cosmicide.rewrite.util.FileUtil
import org.cosmicide.util.jdksDir
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.sui.Sui
import top.canyie.pine.Pine
import java.io.File
import java.lang.ref.WeakReference
import java.net.URL
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

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate() {
        super.onCreate()

        if (FileUtil.isInitialized.not()) return

        Analytics.init(this@App)

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

        HiddenApiBypass.addHiddenApiExemptions()

        val jdkDir = jdksDir().resolve(Prefs.currentJDK)
        ConfigProvider.setJavaHome(jdkDir.absolutePath)

        Log.d("App", "JDK set to: ${ConfigProvider.getJavaHome()}")

        extractGlibcAssetsOnce()
//        compileJavaSource()
//        executeJavaClass()

        DynamicColors.applyToActivitiesIfAvailable(this)

        loadTextmateTheme()

        val theme = getTheme(Prefs.appTheme)
        val uiModeManager = getSystemService(UiModeManager::class.java)
        if (uiModeManager.nightMode == theme) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            uiModeManager.setApplicationNightMode(theme)
        } else {
            AppCompatDelegate.setDefaultNightMode(if (theme == UiModeManager.MODE_NIGHT_AUTO) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else theme)
        }

        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().detectLeakedRegistrationObjects().penaltyLog().build())

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

    @SuppressLint("SetWorldReadable")
    fun extractGlibcAssetsOnce() {
        val targetDir = File(filesDir, "glibc")

        targetDir.mkdirs()

        try {
            // List all the .so files inside your app's assets/glibc folder
            val assetManager = assets
            val files = assetManager.list("glibc") ?: return

            for (fileName in files) {
                val assetFile = "glibc/$fileName"
                val outputFile = File(targetDir, fileName)
                if (outputFile.exists()) continue

                assetManager.open(assetFile).use { inputStream ->
                    outputFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                // Optional: Explicitly make sure the permissions layout allows reading
                outputFile.setReadable(true, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getTheme(theme: String): Int {
        return when (theme) {
            "light" -> UiModeManager.MODE_NIGHT_NO
            "dark" -> UiModeManager.MODE_NIGHT_YES
            else -> UiModeManager.MODE_NIGHT_AUTO
        }
    }

    fun loadTextmateTheme() {
        val fileProvider = AssetsFileResolver(assets)
        FileProviderRegistry.getInstance().addFileProvider(fileProvider)

        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
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
                override fun before(param: Pine.CallFrame) {
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
                override fun before(param: Pine.CallFrame) {
                    try {
                        // Call the original method.
                        HookManager.invokeOriginal(
                            param.method,
                            param.thisObject,
                            *param.args
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
            override fun before(param: Pine.CallFrame) {
                println(param.args[0])
            }
        })
    }


    private fun getPublicIp(): String {
        return try {
            val ip = URL("https://api.ipify.org").readText()
            ip
        } catch (_: Exception) {
            ""
        }
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
}
