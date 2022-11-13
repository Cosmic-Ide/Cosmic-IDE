package org.cosmic.ide.activity

import android.os.Bundle
import org.cosmic.ide.databinding.ActivitySettingBinding
import org.cosmic.ide.util.addSystemWindowInsetToPadding

/**
 * A container [Activity] for the settings menu
 */
class SettingActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(getLayoutInflater())
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { _ -> finish() }

        binding.appBar.liftOnScrollTargetViewId = androidx.preference.R.id.recycler_view
    }
}
