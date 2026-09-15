/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import org.cosmicide.MainActivity
import org.cosmicide.R

/**
 * Foreground service that keeps the app process alive in the background, like Termux.
 *
 * Features:
 * - Foreground service with persistent notification
 * - WakeLock support to prevent CPU from sleeping
 * - Notification actions (stop service)
 * - Auto-restart with START_STICKY
 * - Proper cleanup on destroy
 */
class KeepAliveService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStopping = false

    override fun onCreate() {
        super.onCreate()
        Log.v(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(TAG, "onStartCommand action=${intent?.action}")

        when (intent?.action) {
            ACTION_STOP -> {
                actionStopService()
                return START_NOT_STICKY
            }
            ACTION_ACQUIRE_WAKE_LOCK -> {
                actionAcquireWakeLock()
            }
            ACTION_RELEASE_WAKE_LOCK -> {
                actionReleaseWakeLock()
            }
        }

        runStartForeground()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        actionReleaseWakeLock()
        super.onDestroy()
    }

    private fun runStartForeground() {
        setupNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun runStopForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun actionStopService() {
        isServiceStopping = true
        actionReleaseWakeLock()
        runStopForeground()
        stopSelf()
    }

    private fun actionAcquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$packageName:keep_alive"
            ).apply {
                acquire(10 * 60 * 1000L) // 10 minutes
            }
        }
        updateNotification()
    }

    private fun actionReleaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
        if (!isServiceStopping) {
            updateNotification()
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.keep_alive_notification_channel),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.keep_alive_notification_channel_description)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, KeepAliveService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val wakeLockHeld = wakeLock?.isHeld == true
        val wakeLockText = if (wakeLockHeld) {
            getString(R.string.notification_action_wake_unlock)
        } else {
            getString(R.string.notification_action_wake_lock)
        }
        val wakeLockAction = if (wakeLockHeld) ACTION_RELEASE_WAKE_LOCK else ACTION_ACQUIRE_WAKE_LOCK

        val wakeLockIntent = PendingIntent.getService(
            this, 1,
            Intent(this, KeepAliveService::class.java).apply {
                action = wakeLockAction
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val wakeLockIcon = if (wakeLockHeld) {
            android.R.drawable.ic_lock_idle_lock
        } else {
            android.R.drawable.ic_lock_lock
        }

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.keep_alive_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(wakeLockIcon, wakeLockText, wakeLockIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_action_stop), stopIntent)
            .build()
    }

    companion object {
        private const val TAG = "KeepAliveService"
        private const val CHANNEL_ID = "keepalive"
        private const val NOTIFICATION_ID = 1

        const val ACTION_STOP = "org.cosmicide.service.action.STOP"
        const val ACTION_ACQUIRE_WAKE_LOCK = "org.cosmicide.service.action.ACQUIRE_WAKE_LOCK"
        const val ACTION_RELEASE_WAKE_LOCK = "org.cosmicide.service.action.RELEASE_WAKE_LOCK"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, KeepAliveService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, KeepAliveService::class.java).apply {
                    action = ACTION_STOP
                }
            )
        }
    }
}
