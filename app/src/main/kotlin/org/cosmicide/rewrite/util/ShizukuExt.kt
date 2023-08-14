package com.kieronquinn.app.darq.utils.extensions

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
