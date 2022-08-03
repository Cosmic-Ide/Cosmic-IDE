package org.cosmic.ide;

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

import org.cosmic.ide.common.util.FileUtil;
import org.cosmic.ide.ui.utils.UiUtilsKt;
import org.cosmic.ide.databinding.ActivitySettingBinding;

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

        UiUtilsKt.addSystemWindowInsetToPadding(binding.appbar, false, true, false, false);

        if (!compiler_settings.getString("classpath", "").equals("")) {
            binding.tilClassPath.getEditText().setText(compiler_settings.getString("classpath", ""));
        }

        if (!compiler_settings.getString("program_arguments", "").equals("")) {
            binding.tilProgramArguments.getEditText().setText(compiler_settings.getString("program_arguments", ""));
        }

        binding.etThemes.setAdapter(
                new ArrayAdapter<>(
                        this,
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        themes));

        binding.etJavaVersions.setAdapter(
                new ArrayAdapter<>(
                        this,
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        javaVersions));

        binding.etJavaCompilers.setAdapter(
                new ArrayAdapter<>(
                        this,
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        javaCompilers));

        binding.etJavaFormatters.setAdapter(
                new ArrayAdapter<>(
                        this,
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        javaFormatters));

        binding.etJavaDisassemblers.setAdapter(
                new ArrayAdapter<>(
                        this,
                        androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                        javaDisassemblers));

        var currentTheme = ui_settings.getString("current_theme", themes[0]);
        var count = 0;
        for (var theme : themes) {
            if (theme.equals(currentTheme)) {
                binding.etThemes.setListSelection(count);
                binding.etThemes.setText(
                    getSelectedItem(count, binding.etThemes), false);
                break;
            }
            count++;
        }

        var version = compiler_settings.getString("version", javaVersions[2]);
        count = 0;
        for (var vers : javaVersions) {
            if (vers.equals(version)) {
                binding.etJavaVersions.setListSelection(count);
                binding.etJavaVersions.setText(
                    getSelectedItem(count, binding.etJavaVersions), false);
                break;
            }
            count++;
        }

        var compiler = compiler_settings.getString("compiler", javaCompilers[0]);
        count = 0;
        for (var comp : javaCompilers) {
            if (comp.equals(compiler)) {
                binding.etJavaCompilers.setListSelection(count);
                binding.etJavaCompilers.setText(
                    getSelectedItem(count, binding.etJavaCompilers), false);
                break;
            }
            count++;
        }

        var formatter = compiler_settings.getString("formatter", javaFormatters[0]);
        count = 0;
        for (var form : javaFormatters) {
            if (form.equals(formatter)) {
                binding.etJavaFormatters.setListSelection(count);
                binding.etJavaFormatters.setText(
                    getSelectedItem(count, binding.etJavaFormatters), false);
                break;
            }
            count++;
        }

        var disassembler = compiler_settings.getString("disassembler", javaDisassemblers[0]);
        count = 0;
        for (var dis : javaDisassemblers) {
            if (dis.equals(disassembler)) {
                binding.etJavaDisassemblers.setListSelection(count);
                binding.etJavaDisassemblers.setText(
                    getSelectedItem(count, binding.etJavaDisassemblers), false);
                break;
            }
            count++;
        }

        binding.etThemes.setOnItemClickListener(
            (parent, view, pos, id) -> setCurrentTheme(themes[pos])
        );

        binding.etJavaVersions.setOnItemClickListener(
                (parent, view, pos, id) -> {
                        if (javaVersions[pos].equals("18")
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
                        compiler_settings.edit().putString("version", javaVersions[pos]).apply();
                });

        binding.etJavaCompilers.setOnItemClickListener(
                (parent, view, pos, id) -> {
                    compiler_settings.edit().putString("compiler", javaCompilers[pos]).apply();
                });

        binding.etJavaFormatters.setOnItemClickListener(
                (parent, view, pos, id) -> {
                    compiler_settings.edit().putString("formatter", javaFormatters[pos]).apply();
                });

        binding.etJavaDisassemblers.setOnItemClickListener(
                (parent, view, pos, id) -> {
                    compiler_settings.edit().putString("disassembler", javaDisassemblers[pos]).apply();
                });
    }

    private String getSelectedItem(int position, AutoCompleteTextView view) {
        return view.getAdapter().getItem(position).toString();
    }

    @Override
    protected void onDestroy() {
        compiler_settings.edit().putString("classpath", binding.tilClassPath.getEditText().getText().toString()).apply();
        compiler_settings.edit().putString("program_arguments", binding.tilProgramArguments.getEditText().getText().toString()).apply();
        super.onDestroy();
    }
}
