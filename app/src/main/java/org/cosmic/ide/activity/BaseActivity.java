package org.cosmic.ide.activity;

import androidx.appcompat.app.AppCompatDelegate;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;

import org.cosmic.ide.App;
import org.cosmic.ide.ui.preference.Settings;
import org.cosmic.ide.util.UiUtilsKt;

public abstract class BaseActivity extends AppCompatActivity {

    protected SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = App.getDefaultSharedPreferences();

        setupTheme();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        UiUtilsKt.addSystemWindowInsetToPadding(getRootActivityView(), true, false, true, false);
    }

    @NonNull
    private View getRootActivityView() {
        return getWindow().getDecorView().findViewById(android.R.id.content);
    }

    private void setupTheme() {
        var settingz = new Settings(this, null);
        AppCompatDelegate.setDefaultNightMode(settingz.theme);
    }
}
