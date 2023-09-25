/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

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