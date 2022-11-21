package org.cosmic.ide.activity

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View

import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat

import org.cosmic.ide.App
import org.cosmic.ide.ui.preference.Settings
import org.cosmic.ide.util.addSystemWindowInsetToPadding

abstract class BaseActivity : AppCompatActivity() {
    protected lateinit var settings: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupTheme()

        settings = App.getDefaultPreferences()

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false)
        getRootActivityView().addSystemWindowInsetToPadding(true, false, true, false)
    }

    private fun getRootActivityView(): View {
        return getWindow().getDecorView().findViewById(android.R.id.content)
    }

    private fun setupTheme() {
        val settingz = Settings(this, null)
        AppCompatDelegate.setDefaultNightMode(settingz.theme)
    }
}