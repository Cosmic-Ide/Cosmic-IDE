/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide

import android.app.Application
import android.os.Build
import android.util.Log
import com.sun.tools.javac.ConfigProvider
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.cosmicide.common.Analytics
import org.cosmicide.common.Prefs
import org.cosmicide.plugin.api.Hook
import org.cosmicide.plugin.api.HookManager
import org.cosmicide.util.FileUtil
import org.cosmicide.util.jdksDir
import org.lsposed.hiddenapibypass.HiddenApiBypass
import top.canyie.pine.Pine
import java.lang.ref.WeakReference
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

        Analytics.init(this@App)

        Analytics.logEvent(
            "user_metrics",
            "name" to Prefs.clientName,
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

        instance = WeakReference(this)
        HookManager.context = WeakReference(this)

        setupHooks()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions()
        }

        val jdkDir = jdksDir().resolve(Prefs.currentJDK)
        ConfigProvider.setJavaHome(jdkDir.absolutePath)

        loadTextmateTheme()

        Analytics.setAnalyticsCollectionEnabled(Prefs.analyticsEnabled)
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
}
