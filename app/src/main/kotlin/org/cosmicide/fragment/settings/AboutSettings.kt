/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.fragment.settings

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.helpers.expandText
import de.Maxr1998.modernpreferences.helpers.onClick
import de.Maxr1998.modernpreferences.helpers.pref
import de.Maxr1998.modernpreferences.helpers.singleChoice
import de.Maxr1998.modernpreferences.helpers.switch
import de.Maxr1998.modernpreferences.preferences.choice.SelectionItem
import de.Maxr1998.modernpreferences.preferences.choice.SingleChoiceDialogPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.cosmicide.BuildConfig
import org.cosmicide.R
import org.cosmicide.extension.copyToClipboard
import org.cosmicide.fragment.InstallResourcesFragment
import org.cosmicide.rewrite.common.Analytics
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.util.FileUtil
import org.cosmicide.util.CommonUtils.isShizukuGranted
import org.cosmicide.util.ResourceUtil
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess

class AboutSettings(private val activity: FragmentActivity) : SettingsProvider {
    @SuppressLint("PrivateResource")
    override fun provideSettings(builder: PreferenceScreen.Builder) {
        builder.apply {
            expandText("about") {
                title = "About"
                summary = "A free and open-source IDE for Android."
                text =
                    "Cosmic IDE is a free and open-source IDE for Android. It is licensed under the GNU General Public License v3.0."
            }
            singleChoice(
                "donate",
                listOf(SelectionItem("paypal", "PayPal"), SelectionItem("patreon", "Patreon"))
            ) {
                title = "Donate"
                summary =
                    "Donate to the developers. This will help us to keep the project alive. The donations will be distributed among the developers. Thank you for your support!"

                selectionChangeListener =
                    SingleChoiceDialogPreference.OnSelectionChangeListener { _, selection ->
                        Analytics.logEvent("donate", selection)
                        when (selection) {
                            "paypal" -> {
                                activity.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.paypal.com/paypalme/PranavPurwar")
                                    )
                                )
                            }

                            "patreon" -> {
                                activity.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.patreon.com/cosmicide")
                                    )
                                )
                            }
                        }
                        true
                    }
            }

            pref("version") {
                title = "App version"
                summary =
                    BuildConfig.VERSION_NAME + if (BuildConfig.DEBUG) " (${BuildConfig.GIT_COMMIT})" else ""

                var count = 0
                onClick {
                    count++
                    if (count == 7) {
                        Analytics.logEvent("is_dev", Prefs.experimentsEnabled)
                        val editor = PreferenceManager.getDefaultSharedPreferences(activity).edit()
                        if (Prefs.experimentsEnabled) {
                            editor.putBoolean("experiments_enabled", false)
                            Toast.makeText(
                                activity,
                                "You are no longer a developer",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            editor.putBoolean("experiments_enabled", true)
                            Toast.makeText(activity, "You are a developer", Toast.LENGTH_LONG)
                                .show()
                        }
                        editor.apply()
                    }
                    val handler = Handler(Looper.myLooper()!!)
                    // We don't wanna show the copy to clipboard dialog multiple times
                    if (count == 1) {
                        val clipboardManager =
                            ContextCompat.getSystemService(activity, ClipboardManager::class.java)!!
                        val clip = clipboardManager.primaryClip?.getItemAt(0)?.text
                        if (clip == summary) {
                            return@onClick true
                        }
                        activity.copyToClipboard(summary.toString())
                    }
                    true
                }
            }

            pref("source") {
                title = "Source code"
                onClick {
                    activity.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/Cosmic-IDE/Cosmic-IDE")
                        )
                    )
                    true
                }
            }

            pref("manage_storage_permission") {
                title = "Manage storage permission"
                onClick {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            activity.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:" + activity.packageName)
                                )
                            )
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            activity.startActivity(intent)
                        }
                    } else {
                        // Manage storage permission for Android 10 and below
                        activity.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + activity.packageName)
                            )
                        )
                    }
                    Toast.makeText(
                        activity,
                        "Please enable the manage storage permission for Cosmic IDE",
                        Toast.LENGTH_LONG
                    ).show()
                    true
                }
            }

            val isShizukuGranted = activity.isShizukuGranted()

            pref("rish") {
                title = "Rish"
                summary =
                    if (isShizukuGranted) "Execute privileged commands with rish" else "Make sure you have Shizuku installed and running. And have authorized the app to use it"
                enabled = isShizukuGranted

                onClick {
                    val inflated = activity.layoutInflater.inflate(
                        androidx.preference.R.layout.preference_dialog_edittext,
                        null
                    )
                    MaterialAlertDialogBuilder(activity)
                        .setTitle("Rish")
                        .setMessage("Rish is a tool that allows you to execute privileged commands. It is not recommended to use this tool unless you know what you are doing.")
                        .setView(inflated)
                        .setPositiveButton("Execute") { _, _ ->
                            val editText = inflated.findViewById<EditText>(android.R.id.edit)
                            val command = editText.text.toString()
                            activity.lifecycleScope.async {
                                val output = exec(command)
                                withContext(Dispatchers.Main) {
                                    MaterialAlertDialogBuilder(activity)
                                        .setTitle("Output")
                                        .setMessage(output.joinToString("\n"))
                                        .setPositiveButton("Copy") { _, _ ->
                                            activity.copyToClipboard(output.joinToString("\n"))
                                        }
                                        .setNegativeButton("Close") { _, _ -> }
                                        .show()
                                }
                            }
                        }
                        .setNegativeButton("Cancel") { _, _ -> }
                        .show()

                    true
                }
            }

            pref("clear_cache") {
                title = "Clear cache"
                onClick {
                    activity.lifecycleScope.async {
                        ResourceUtil.resources.forEach {
                            FileUtil.dataDir.resolve(it).delete()
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(activity, "Cache cleared", Toast.LENGTH_LONG).show()

                            activity.supportFragmentManager.commit {
                                replace(R.id.fragment_container, InstallResourcesFragment())
                                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                            }
                        }
                    }
                    true
                }
            }

            pref("force_crash") {
                title = "Force crash"
                onClick {
                    throw RuntimeException("Forced crash")
                }
            }

            switch("analytics_preference") {
                title = "Analytics"
                summary = "Help us improve the app by sending anonymous usage data"
            }
        }
    }

    fun exec(vararg command: String): List<String> {
        val output = mutableListOf<String>()
        if (Shizuku.pingBinder()) {
            Log.i("ShizukuPermissionHandler", "Shizuku is running")
        }
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        m.isAccessible = true
        val process =
            m.invoke(null, arrayOf("sh", "-c", *command), null, "/") as ShizukuRemoteProcess
        process.apply {
            waitFor()
            Log.i("ShizukuPermissionHandler", "Process exited with code ${exitValue()}")
            inputStream.bufferedReader().use {
                output.addAll(it.readLines())
            }
            errorStream.bufferedReader().use {
                output.addAll(it.readLines().map { "error: it" })
            }
        }
        return output
    }
}
