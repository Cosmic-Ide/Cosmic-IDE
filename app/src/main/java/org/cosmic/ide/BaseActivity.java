package org.cosmic.ide;

import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;

import org.cosmic.ide.ui.utils.UiUtilsKt;

public abstract class BaseActivity extends AppCompatActivity {

    protected final String[] themes = {
        "System Default", "Light", "Dark"
    };

    protected final String[] javaVersions = {
        "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18"
    };

    protected final String[] javaCompilers = {
        "Javac", "Eclipse Compiler for Java"
    };

    protected final String[] javaFormatters = {
        "Google Java Formatter", "Eclipse Java Formatter"
    };

    protected final String[] javaDisassemblers = {
        "Javap", "Eclipse Class Disassembler"
    };

    protected SharedPreferences ui_settings;
    protected SharedPreferences compiler_settings;

    @NonNull
    private String currentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ui_settings = getSharedPreferences("ui_settings", MODE_PRIVATE);
        compiler_settings = getSharedPreferences("compiler_settings", MODE_PRIVATE);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        UiUtilsKt.addSystemWindowInsetToPadding(getRootActivityView(), true, false, true, false);

        currentTheme = ui_settings.getString("current_theme", themes[0]);
        checkCurrentTheme();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkCurrentTheme();
    }

    @NonNull
    private View getRootActivityView() {
        return getWindow().getDecorView().findViewById(android.R.id.content);
    }

    protected boolean isDarkMode() {
        int uiMode = getResources().getConfiguration().uiMode;
        return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    protected void setCurrentTheme(String theme) {
        if (ui_settings.getString("current_theme", themes[0]) == theme) return;
        int uiMode = -1;
        int pos = 0;
        switch (theme) {
            case "System Default":
                uiMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                pos = 0;
                break;
            case "Light":
                uiMode = AppCompatDelegate.MODE_NIGHT_NO;
                pos = 1;
                break;
            case "Dark":
                uiMode = AppCompatDelegate.MODE_NIGHT_YES;
                pos = 2;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(uiMode);
        ui_settings.edit().putString("current_theme", themes[pos]).apply();
        recreate();
    }

    private void checkCurrentTheme() {
        switch (ui_settings.getString("current_theme", themes[0])) {
            case "System Default":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "Light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "Dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
        if(currentTheme != ui_settings.getString("current_theme", themes[0])) recreate();
    }

    protected static int getColorAttr(Context context, @AttrRes int resId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(resId, typedValue, true);
        return typedValue.data;
    }
}