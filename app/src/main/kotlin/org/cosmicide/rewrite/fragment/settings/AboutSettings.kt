/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.helpers.onClick
import de.Maxr1998.modernpreferences.helpers.pref
import org.cosmicide.rewrite.BuildConfig

class AboutSettings(private val activity: FragmentActivity) : SettingsProvider {
    override fun provideSettings(builder: PreferenceScreen.Builder) {
        builder.apply {
            pref("version") {
                title = "App version"
                summary =
                    BuildConfig.VERSION_NAME + if (BuildConfig.DEBUG) " (${BuildConfig.GIT_COMMIT})" else ""
                onClick {
                    val clipboard =
                        activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("version", title)
                    clipboard.setPrimaryClip(clip)
                    true
                }
            }

            pref("license") {
                title = "License"
                onClick {
                    activity.startActivity(Intent(activity, OssLicensesMenuActivity::class.java))
                    true
                }
            }

            pref("source") {
                title = "Source code"
                onClick {
                    activity.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/Cosmic-IDE/rewrite")
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

            pref("force_crash") {
                title = "Force crash"
                onClick {
                    throw RuntimeException("Forced crash")
                }
            }
        }
    }
}