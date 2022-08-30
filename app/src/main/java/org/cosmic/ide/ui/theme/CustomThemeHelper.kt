package org.cosmic.ide.ui.theme

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.annotation.StyleRes
import org.cosmic.ide.R
import org.cosmic.ide.ui.preference.Settings
import org.cosmic.ide.util.*

object CustomThemeHelper {
    private val activityBaseThemes = mutableMapOf<Activity, Int>()

    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(object : SimpleActivityLifecycleCallbacks {
            override fun onActivityDestroyed(activity: Activity) {
                activityBaseThemes.remove(activity)
            }
        })
    }

    @JvmStatic
    fun apply(activity: Activity) {
        val baseThemeRes = activity.themeResIdCompat
        activityBaseThemes[activity] = baseThemeRes
        val customThemeRes = getCustomThemeRes(baseThemeRes, activity)
        activity.setThemeCompat(customThemeRes)
    }

    fun sync() {
        for ((activity, baseThemeRes) in activityBaseThemes) {
            val currentThemeRes = activity.themeResIdCompat
            val customThemeRes = getCustomThemeRes(baseThemeRes, activity)
            if (currentThemeRes != customThemeRes) {
                if (activity is OnThemeChangedListener) {
                    (activity as OnThemeChangedListener).onThemeChanged(customThemeRes)
                } else {
                    activity.recreateCompat()
                }
            }
        }
    }

    private fun getCustomThemeRes(@StyleRes baseThemeRes: Int, context: Context): Int {
        val resources = context.resources
        val baseThemeName = resources.getResourceName(baseThemeRes)
        val customThemeName = if (Settings.MD3.valueCompat) {
            val defaultThemeName = resources.getResourceEntryName(R.style.Theme_CosmicIde)
            val material3ThemeName =
                resources.getResourceEntryName(R.style.Theme_CosmicIde_Material3)
            baseThemeName.replace(defaultThemeName, material3ThemeName)
        } else ""
        return resources.getIdentifier(customThemeName, null, null)
    }

    interface OnThemeChangedListener {
        fun onThemeChanged(@StyleRes theme: Int)
    }
}
