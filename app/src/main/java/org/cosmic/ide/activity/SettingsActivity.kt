package org.cosmic.ide.activity

import android.os.Bundle
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.activity.OnBackPressedCallback
import org.cosmic.ide.R
import org.cosmic.ide.databinding.ActivitySettingsBinding
import org.cosmic.ide.fragment.settings.RootSettingsFragment

class SettingsActivity :
    BaseActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.appBar.liftOnScrollTargetViewId = androidx.preference.R.id.recycler_view

        if (savedInstanceState == null) {
            val fragment = RootSettingsFragment()

            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.container, fragment)
            }
        } else {
            savedInstanceState.let {
                supportActionBar?.title = it.getCharSequence(TITLE_TAG)
            }
        }
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(
            true
        ) {
            override fun handleOnBackPressed() {
                val current = supportFragmentManager.fragments.last()
                if (current !is RootSettingsFragment) {
                    openFragment(RootSettingsFragment())
                    return
                }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(TITLE_TAG, supportActionBar?.title)
    }

    override fun onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)
        binding.collapsingToolbar.title = title
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val fm = supportFragmentManager
        val fragment = fm.fragmentFactory.instantiate(classLoader, pref.fragment ?: return false)
        fragment.arguments = pref.extras
        openFragment(fragment)
        supportActionBar?.title = pref.title ?: getString(R.string.settings)
        return true
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.container, fragment)
            addToBackStack(null)
        }
    }

    companion object {
        private const val TITLE_TAG = "settings_title"
    }
}
