package org.cosmicide.startup

import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.startup.Initializer
import org.cosmicide.BuildConfig
import java.util.concurrent.Executors

class DebugInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    private fun enableStrictMode() {
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder().apply {
                detectLeakedRegistrationObjects()
                detectActivityLeaks()
                detectContentUriWithoutPermission()
                detectFileUriExposure()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    penaltyLog()
                    return@apply
                }
                permitNonSdkApiUsage()
                penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                    Log.d("StrictMode", "VM violation", violation)
                    violation.printStackTrace()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    detectUnsafeIntentLaunch()
               }
            }.build()
        )
    }
}