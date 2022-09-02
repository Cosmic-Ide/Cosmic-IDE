package org.cosmic.ide.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;

import org.cosmic.ide.ApplicationLoader;
import org.cosmic.ide.ui.theme.CustomThemeHelper;
import org.cosmic.ide.ui.theme.DarkThemeHelper;
import org.cosmic.ide.util.UiUtilsKt;

public abstract class BaseActivity extends AppCompatActivity {

    private boolean isDelegateCreated = false;

    protected SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CustomThemeHelper.apply(this);
        super.onCreate(savedInstanceState);

        settings = ApplicationLoader.getDefaultSharedPreferences();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        UiUtilsKt.addSystemWindowInsetToPadding(getRootActivityView(), true, false, true, false);
    }

    @Override
    public AppCompatDelegate getDelegate() {
        var delegate = super.getDelegate();
        if (!isDelegateCreated) {
            isDelegateCreated = true;
            DarkThemeHelper.apply(this);
        }
        return delegate;
    }

    @NonNull
    private View getRootActivityView() {
        return getWindow().getDecorView().findViewById(android.R.id.content);
    }
}
