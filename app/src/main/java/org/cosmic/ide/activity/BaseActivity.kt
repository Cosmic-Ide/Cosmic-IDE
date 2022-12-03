package org.cosmic.ide.activity

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.viewbinding.ViewBinding
import org.cosmic.ide.R
import org.cosmic.ide.ui.preference.Settings
import org.cosmic.ide.util.addSystemWindowInsetToPadding

abstract class BaseActivity<Binding : ViewBinding> : AppCompatActivity() {

    public lateinit var binding: Binding

    protected val settings: Settings by lazy { Settings() }

    override fun onCreate(savedInstanceState: Bundle?) {
        val isDynamic = settings.isDynamicTheme
        when {
            isDynamic -> setTheme(R.style.Theme_CosmicIde_Monet)
        }
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false)
        getRootActivityView().addSystemWindowInsetToPadding(true, false, true, false)
    }

    protected fun setContentView(binding: Binding) {
        this.binding = binding
        setContentView(binding.root)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
            isTaskRoot &&
            supportFragmentManager.backStackEntryCount == 0
        ) {
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }

    private fun getRootActivityView(): View {
        return getWindow().getDecorView().findViewById(android.R.id.content)
    }
}