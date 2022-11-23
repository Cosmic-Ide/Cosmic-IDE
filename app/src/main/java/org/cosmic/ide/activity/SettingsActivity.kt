package org.cosmic.ide.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.cosmic.ide.R
import org.cosmic.ide.databinding.ActivitySettingsBinding
import org.cosmic.ide.fragment.settings.SettingsHeadersFragment
import org.cosmic.ide.util.addSystemWindowInsetToPadding

class SettingsActivity :
        BaseActivity<ActivitySettingsBinding>(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override val layoutRes = R.layout.activity_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.appBar.liftOnScrollTargetViewId = androidx.preference.R.id.recycler_view

        if (supportFragmentManager.findFragmentById(R.id.container) == null) {
            openDefaultFragment()
        }
    }

    override fun onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)
        binding.collapsingToolbar?.title = title
    }

    override fun onPreferenceStartFragment(
            caller: PreferenceFragmentCompat,
            pref: Preference
    ) : Boolean {
        val fm = supportFragmentManager
        val fragment = fm.fragmentFactory.instantiate(classLoader, pref.fragment ?: return false)
        fragment.arguments = pref.extras
        fragment.setTargetFragment(caller, 0)
        openFragment(fragment)
        return true
    }

    fun openFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.container, fragment)
            addToBackStack(null)
        }
    }

    private fun openDefaultFragment() {
        val fragment = SettingsHeadersFragment()
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.container, fragment)
        }
    }
}