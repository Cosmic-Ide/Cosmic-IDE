package com.pranav.java.ide;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import com.pranav.common.util.FileUtil;
import com.pranav.java.ide.ui.utils.UiUtilsKt;

public final class SettingActivity extends AppCompatActivity {

    private String[] javaVersions = {
        "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18"
    };

    private String[] javaCompilers = {
        "Javac", "Eclipse Compiler for Java"
    };

    private String[] javaFormatters = {
        "Google Java Formatter", "Eclipse Java Formatter"
    };

    private String[] javaDisassemblers = {
        "Javap", "Eclipse Class Disassembler"
    };

    private SharedPreferences settings;

    private TextInputLayout classPath_til;
    private TextInputLayout programArguments_til;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        final MaterialToolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        var appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        UiUtilsKt.addSystemWindowInsetToPadding(appBarLayout, false, true, false, false);

        settings = getSharedPreferences("compiler_settings", MODE_PRIVATE);

        classPath_til = findViewById(R.id.til_classPath);
        programArguments_til = findViewById(R.id.til_programArguments);

        AutoCompleteTextView javaVersions_et = findViewById(R.id.et_javaVersions);
        AutoCompleteTextView javaCompilers_et = findViewById(R.id.et_javaCompilers);
        AutoCompleteTextView javaFormatters_et = findViewById(R.id.et_javaFormatters);
        AutoCompleteTextView javaDisassemblers_et = findViewById(R.id.et_javaDisassemblers);

        if (!settings.getString("classpath", "").equals("")) {
            classPath_til.getEditText().setText(settings.getString("classpath", ""));
        }

        if (!settings.getString("program_arguments", "").equals("")) {
            programArguments_til.getEditText().setText(settings.getString("program_arguments", ""));
        }

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

        var version = settings.getString("version", "7");
        var count = 0;
        for (var vers : javaVersions) {
            if (vers.equals(version)) {
                javaVersions_et.setListSelection(count);
                javaVersions_et.setText(
                    getSelectedItem(count, javaVersions_et), false);
                break;
            }
            count++;
        }

        var compiler = settings.getString("compiler", "Javac");
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

        var formatter = settings.getString("formatter", "Google Java Formatter");
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

        var disassembler = settings.getString("disassembler", "Javap");
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

        javaVersions_et.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (javaVersions[position].equals("18")
                                && settings.getString("compiler", "Javac").equals("Javac")) {
                            new MaterialAlertDialogBuilder(SettingActivity.this)
                                    .setTitle("Notice")
                                    .setMessage(
                                            "Please note that currently only ECJ supports Java 18."
                                                + " Javac with Java 18 is not currently supported.")
                                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {})
                                    .show();
                            return;
                        }
                        settings.edit().putString("version", javaVersions[position]).apply();
                    }
                });

        javaCompilers_et.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {
                        settings.edit().putString("compiler", javaCompilers[position]).apply();
                    }
                });

        javaFormatters_et.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {
                        settings.edit().putString("formatter", javaFormatters[position]).apply();
                    }
                });

        javaDisassemblers_et.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {
                        settings.edit().putString("disassembler", javaDisassemblers[position]).apply();
                    }
                });
    }

    private String getSelectedItem(int position, AutoCompleteTextView view) {
        return view.getAdapter().getItem(position).toString();
    }

    @Override
    protected void onDestroy() {
        settings.edit().putString("classpath", classPath_til.getEditText().getText().toString()).apply();
        settings.edit().putString("program_arguments", programArguments_til.getEditText().getText().toString()).apply();
        super.onDestroy();
    }
}
