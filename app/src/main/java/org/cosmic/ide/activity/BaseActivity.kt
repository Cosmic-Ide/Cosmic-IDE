package org.cosmic.ide.activity

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View

import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope

import org.cosmic.ide.App
import org.cosmic.ide.ui.preference.Settings
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import com.kieronquinn.monetcompat.app.MonetCompatActivity

abstract class BaseActivity : MonetCompatActivity() {

    protected var settings: SharedPreferences

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
        lifecycleScope.launchWhenCreated {
            monet.awaitMonetReady()
        }
    }
}
