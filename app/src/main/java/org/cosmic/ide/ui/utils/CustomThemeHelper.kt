package org.cosmic.ide.ui.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle

import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity

import org.cosmic.ide.R
import org.cosmic.ide.compat.recreateCompat
import org.cosmic.ide.compat.setThemeCompat
import org.cosmic.ide.compat.themeResIdCompat
import org.cosmic.ide.Settings
import org.cosmic.ide.ui.utils.NightModeHelper
import org.cosmic.ide.ui.utils.SimpleActivityLifecycleCallbacks

object CustomThemeHelper {
    private val activityBaseThemes = mutableMapOf<Activity, Int>()

    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(object : SimpleActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                check(activityBaseThemes.containsKey(activity)) {
                    "Activity must extend BaseActivity: $activity"
                }
            }

            override fun onActivityDestroyed(activity: Activity) {
                activityBaseThemes.remove(activity)
            }
        })
    }

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
                // if (!NightModeHelper.isInNightMode(activity as AppCompatActivity)) {
                    // continue
                // }
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