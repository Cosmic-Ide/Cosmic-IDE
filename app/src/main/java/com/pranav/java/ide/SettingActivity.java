package com.pranav.java.ide;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import com.pranav.common.util.FileUtil;
import com.pranav.java.ide.ui.utils.UiUtilsKt;

public class SettingActivity extends BaseActivity {

    private TextInputLayout classPath_til;
    private TextInputLayout programArguments_til;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        final MaterialToolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        View appBarLayout = findViewById(R.id.appbar);
        UiUtilsKt.addSystemWindowInsetToPadding(appBarLayout, false, true, false, false);

        classPath_til = findViewById(R.id.til_classPath);
        programArguments_til = findViewById(R.id.til_programArguments);

        AutoCompleteTextView themes_et = findViewById(R.id.et_themes);
        AutoCompleteTextView javaVersions_et = findViewById(R.id.et_javaVersions);
        AutoCompleteTextView javaCompilers_et = findViewById(R.id.et_javaCompilers);
        AutoCompleteTextView javaFormatters_et = findViewById(R.id.et_javaFormatters);
        AutoCompleteTextView javaDisassemblers_et = findViewById(R.id.et_javaDisassemblers);

        if (!compiler_settings.getString("classpath", "").equals("")) {
            classPath_til.getEditText().setText(compiler_settings.getString("classpath", ""));
        }

        if (!compiler_settings.getString("program_arguments", "").equals("")) {
            programArguments_til.getEditText().setText(compiler_settings.getString("program_arguments", ""));
        }

        themes_et.setAdapter(
                new ArrayAdapter<>(
                        this,
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        themes));

        javaVersions_et.setAdapter(
                new ArrayAdapter<>(
                        this,
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        javaVersions));

        javaCompilers_et.setAdapter(
                new ArrayAdapter<>(
                        this,
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        javaCompilers));

        javaFormatters_et.setAdapter(
                new ArrayAdapter<>(
                        this,
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        javaFormatters));

        javaDisassemblers_et.setAdapter(
                new ArrayAdapter<>(
                        this,
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        javaDisassemblers));

        var currentTheme = ui_settings.getString("current_theme", themes[0]);
        var count = 0;
        for (var theme : themes) {
            if (theme.equals(currentTheme)) {
                themes_et.setListSelection(count);
                themes_et.setText(
                    getSelectedItem(count, themes_et), false);
                break;
            }
            count++;
        }

        var version = compiler_settings.getString("version", javaVersions[2]);
        count = 0;
        for (var vers : javaVersions) {
            if (vers.equals(version)) {
                javaVersions_et.setListSelection(count);
                javaVersions_et.setText(
                    getSelectedItem(count, javaVersions_et), false);
                break;
            }
            count++;
        }

        var compiler = compiler_settings.getString("compiler", javaCompilers[0]);
        count = 0;
        for (var comp : javaCompilers) {
            if (comp.equals(compiler)) {
                javaCompilers_et.setListSelection(count);
                javaCompilers_et.setText(
                    getSelectedItem(count, javaCompilers_et), false);
                break;
            }
            count++;
        }

        var formatter = compiler_settings.getString("formatter", javaFormatters[0]);
        count = 0;
        for (var form : javaFormatters) {
            if (form.equals(formatter)) {
                javaFormatters_et.setListSelection(count);
                javaFormatters_et.setText(
                    getSelectedItem(count, javaFormatters_et), false);
                break;
            }
            count++;
        }

        var disassembler = compiler_settings.getString("disassembler", javaDisassemblers[0]);
        count = 0;
        for (var dis : javaDisassemblers) {
            if (dis.equals(disassembler)) {
                javaDisassemblers_et.setListSelection(count);
                javaDisassemblers_et.setText(
                    getSelectedItem(count, javaDisassemblers_et), false);
                break;
            }
            count++;
        }

        themes_et.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {
                        setCurrentTheme(themes[position]);
                    }
                });

        javaVersions_et.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (javaVersions[position].equals("18")
                                && compiler_settings.getString("compiler", javaCompilers[0]).equals(javaCompilers[0])) {
                            new MaterialAlertDialogBuilder(SettingActivity.this)
                                    .setTitle("Notice")
                                    .setMessage(
                                            "Please note that currently only ECJ supports Java 18."
                                                + " Javac with Java 18 is not currently supported.")
                                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                                    .show();
                            return;
                        }
                        compiler_settings.edit().putString("version", javaVersions[position]).apply();
                    }
                });

        javaCompilers_et.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {
                        compiler_settings.edit().putString("compiler", javaCompilers[position]).apply();
                    }
                });

        javaFormatters_et.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {
                        compiler_settings.edit().putString("formatter", javaFormatters[position]).apply();
                    }
                });

        javaDisassemblers_et.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {
                        compiler_settings.edit().putString("disassembler", javaDisassemblers[position]).apply();
                    }
                });
    }

    private String getSelectedItem(int position, AutoCompleteTextView view) {
        return view.getAdapter().getItem(position).toString();
    }

    @Override
    protected void onDestroy() {
        compiler_settings.edit().putString("classpath", classPath_til.getEditText().getText().toString()).apply();
        compiler_settings.edit().putString("program_arguments", programArguments_til.getEditText().getText().toString()).apply();
        super.onDestroy();
    }
}
