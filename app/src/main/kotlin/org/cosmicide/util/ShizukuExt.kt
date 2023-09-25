/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.util

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import kotlin.coroutines.resume

fun Context.isShizukuInstalled(): Boolean {
    return packageManager.isAppInstalled(ShizukuProvider.MANAGER_APPLICATION_ID)
}

fun PackageManager.isAppInstalled(packageName: String): Boolean {
    return try {
        getInstalledApplications(0).any { it.packageName == packageName }
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

suspend fun awaitBinderReceived() = suspendCancellableCoroutine {
    val receiver = object : Shizuku.OnBinderReceivedListener {
        override fun onBinderReceived() {
            Shizuku.removeBinderReceivedListener(this)
            it.resume(Unit)
        }
    }
    Shizuku.addBinderReceivedListener(receiver)
    it.invokeOnCancellation {
        Shizuku.removeBinderReceivedListener(receiver)
    }
}
