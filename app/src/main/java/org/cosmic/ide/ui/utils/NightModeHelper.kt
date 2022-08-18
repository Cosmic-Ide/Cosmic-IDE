package org.cosmic.ide.ui.utils

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegateCompat

import org.cosmic.ide.ApplicationLoader
import org.cosmic.ide.Settings

// We take over the activity creation when setting the default night mode from AppCompat so that:
// 1. We can recreate all activities upon change, instead of only started activities.
// 2. We can have custom handling of the change, instead of being forced to either recreate or
//    update resources configuration which is shared among activities.
object NightModeHelper {
    private val activities = mutableSetOf<AppCompatActivity>()

    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(object : SimpleActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    check(activity in activities) { "Activity must extend BaseActivity: $activity" }
                }

                override fun onActivityDestroyed(activity: Activity) {
                    activities -= activity as AppCompatActivity
                }
            })
    }

    @JvmStatic
    fun apply(activity: AppCompatActivity) {
        activities += activity
        activity.delegate.localNightMode = nightMode
    }

    fun sync() {
        for (activity in activities) {
            val nightMode = nightMode
            if (activity is OnNightModeChangedListener) {
                if (getUiModeNight(activity.delegate.localNightMode, activity)
                    != getUiModeNight(nightMode, activity)) {
                    activity.onNightModeChangedFromHelper(nightMode)
                }
            } else {
                activity.delegate.localNightMode = nightMode
            }
        }
    }

    private val nightMode: Int
        get() = Settings.NIGHT_MODE.valueCompat.value

    /*
     * @see androidx.appcompat.app.AppCompatDelegateImpl#updateForNightMode(int, boolean)
     */
    private fun getUiModeNight(nightMode: Int, activity: AppCompatActivity): Int =
        when (AppCompatDelegateCompat.mapNightMode(activity.delegate, ApplicationLoader.applicationContext(), nightMode)) {
            AppCompatDelegate.MODE_NIGHT_YES -> Configuration.UI_MODE_NIGHT_YES
            AppCompatDelegate.MODE_NIGHT_NO -> Configuration.UI_MODE_NIGHT_NO
            else ->
                (activity.applicationContext.resources.configuration.uiMode
                    and Configuration.UI_MODE_NIGHT_MASK)
        }

    fun isInNightMode(activity: AppCompatActivity): Boolean =
        (getUiModeNight(activity.delegate.localNightMode, activity)
            == Configuration.UI_MODE_NIGHT_YES)

    interface OnNightModeChangedListener {
        fun onNightModeChangedFromHelper(nightMode: Int)
    }
}