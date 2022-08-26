package org.cosmic.ide

import android.os.Bundle
import android.view.View
import org.cosmic.ide.databinding.ActivitySettingBinding
import org.cosmic.ide.ui.utils.addSystemWindowInsetToPadding

class SettingActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(getLayoutInflater())
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar()?.setHomeButtonEnabled(true)
        binding.toolbar.setNavigationOnClickListener { _ -> onBackPressed() }

        binding.appbar.apply {
            setLiftOnScrollTargetViewId(androidx.preference.R.id.recycler_view)
            addSystemWindowInsetToPadding(false, true, false, false)
        }
        val recyclerView: View? = findViewById(androidx.preference.R.id.recycler_view)
        recyclerView?.addSystemWindowInsetToPadding(false, false, false, true)
    }
}
