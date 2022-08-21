package org.cosmic.ide.ui.theme

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegateCompat
import org.cosmic.ide.ApplicationLoader
import org.cosmic.ide.preference.Settings
import org.cosmic.ide.ui.utils.SimpleActivityLifecycleCallbacks
import org.cosmic.ide.ui.utils.valueCompat

// We take over the activity creation when setting the default night mode from AppCompat so that:
// 1. We can recreate all activities upon change, instead of only started activities.
// 2. We can have custom handling of the change, instead of being forced to either recreate or
//    update resources configuration which is shared among activities.
object DarkThemeHelper {
    private val activities = mutableSetOf<AppCompatActivity>()

    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(object : SimpleActivityLifecycleCallbacks {
            override fun onActivityDestroyed(activity: Activity) {
                activities -= activity as AppCompatActivity
            }
        })
    }

    @JvmStatic
    fun apply(activity: AppCompatActivity) {
        activities += activity
        activity.delegate.localNightMode = darkTheme
    }

    fun sync() {
        for (activity in activities) {
            val nightMode = darkTheme
            if (activity is OnDarkThemeChangedListener) {
                if (getUiModeNight(activity.delegate.localNightMode, activity)
                    != getUiModeNight(nightMode, activity)
                ) {
                    activity.onDarkThemeChangedFromHelper(nightMode)
                }
            } else {
                activity.delegate.localNightMode = nightMode
            }
        }
    }

    private val darkTheme: Int
        get() = Settings.DARK_THEME.valueCompat.value

    /*
     * @see androidx.appcompat.app.AppCompatDelegateImpl#updateForNightMode(int, boolean)
     */
    private fun getUiModeNight(nightMode: Int, activity: AppCompatActivity): Int =
        when (AppCompatDelegateCompat.mapNightMode(activity.delegate, ApplicationLoader.applicationContext(), nightMode)) {
            AppCompatDelegate.MODE_NIGHT_YES -> Configuration.UI_MODE_NIGHT_YES
            AppCompatDelegate.MODE_NIGHT_NO -> Configuration.UI_MODE_NIGHT_NO
            else ->
                (
                    activity.applicationContext.resources.configuration.uiMode
                        and Configuration.UI_MODE_NIGHT_MASK
                    )
        }

    fun isInDarkTheme(activity: AppCompatActivity): Boolean =
        (
            getUiModeNight(activity.delegate.localNightMode, activity)
                == Configuration.UI_MODE_NIGHT_YES
            )

    interface OnDarkThemeChangedListener {
        fun onDarkThemeChangedFromHelper(darkTheme: Int)
    }
}
