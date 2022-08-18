package org.cosmic.ide;

import android.os.Bundle;
import android.view.View;

import org.cosmic.ide.databinding.ActivitySettingBinding;
import org.cosmic.ide.ui.utils.UiUtilsKt;

public class SettingActivity extends BaseActivity {

    private ActivitySettingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.appbar.setLiftOnScrollTargetViewId(androidx.preference.R.id.recycler_view);
        View recyclerView = findViewById(androidx.preference.R.id.recycler_view);
        UiUtilsKt.addSystemWindowInsetToPadding(recyclerView, false, false, false, true);
    }
}