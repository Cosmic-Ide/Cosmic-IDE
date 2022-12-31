package org.cosmic.ide.activity

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import org.cosmic.ide.R
import org.cosmic.ide.ui.preference.Settings
import org.cosmic.ide.util.addSystemWindowInsetToPadding

abstract class BaseActivity : AppCompatActivity() {

    protected val settings: Settings by lazy { Settings() }

    override fun onCreate(savedInstanceState: Bundle?) {
        val isDynamic = settings.isDynamicTheme
        when {
            isDynamic -> setTheme(R.style.Theme_CosmicIde_Monet)
        }
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        getRootActivityView().addSystemWindowInsetToPadding(
            left = true,
            right = true
        )
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(
            true
        ) {
            override fun handleOnBackPressed() {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                    isTaskRoot &&
                    supportFragmentManager.backStackEntryCount == 0
                ) {
                    finishAfterTransition()
                }
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(
            this,
            callback
        )

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressedDispatcher.onBackPressed()

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun getRootActivityView(): View =
        window.decorView.findViewById(android.R.id.content)
}
