package com.pranav.java.ide;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;

public final class SettingActivity extends AppCompatActivity {
    private String[] javaVersions = {
        "1.3", "1.4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17"
    };

    private String[] javaCompilers = {"Javac", "Eclipse Compiler for Java"};

    private String[] javaFormatters = {"Google Java Formatter", "Eclipse Java Formatter"};

    private String[] javaDisassemblers = {"Javap", "Eclipse Class Disassembler"};

    private Spinner javaVersions_spinner;
    private Spinner javaCompilers_spinner;
    private Spinner javaFormatters_spinner;
    private Spinner javaDisassemblers_spinner;
    private Button classpath_bttn;

    private AlertDialog alertDialog;
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        final Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        settings = getSharedPreferences("compiler_settings", MODE_PRIVATE);

        javaVersions_spinner = findViewById(R.id.javaVersions_spinner);
        javaCompilers_spinner = findViewById(R.id.javaCompilers_spinner);
        javaFormatters_spinner = findViewById(R.id.javaFormatters_spinner);
        javaDisassemblers_spinner = findViewById(R.id.javaDisassemblers_spinner);
        classpath_bttn = findViewById(R.id.classpath_bttn);

        var versionAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, javaVersions);
        versionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        var compilerAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, javaCompilers);
        compilerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        var formatterAdapter =
                new ArrayAdapter<String>(
                        this, android.R.layout.simple_spinner_item, javaFormatters);
        formatterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        var disassemblerAdapter =
                new ArrayAdapter<String>(
                        this, android.R.layout.simple_spinner_item, javaDisassemblers);
        disassemblerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        javaVersions_spinner.setAdapter(versionAdapter);

        javaCompilers_spinner.setAdapter(compilerAdapter);

        javaFormatters_spinner.setAdapter(formatterAdapter);

        javaDisassemblers_spinner.setAdapter(disassemblerAdapter);

        /* Check if Classpath stored in SharedPref is empty - if yes, change button text */
        if (settings.getString("classpath", "").equals("")) {
            classpath_bttn.setText(getString(R.string.classpath_not_specified));
        } else {
            classpath_bttn.setText(getString(R.string.classpath_edit));
        }

        /* Select Version in Spinner based on SharedPreferences Value */
        var version = settings.getString("version", "7");
        var count = 0;
        for (var vers : javaVersions) {
            if (vers.equals(version)) {
                javaVersions_spinner.setSelection(count);
                break;
            }
            count++;
        }

        var compiler = settings.getString("compiler", "Javac");
        count = 0;
        for (var comp : javaCompilers) {
            if (comp.equals(compiler)) {
                javaCompilers_spinner.setSelection(count);
                break;
            }
            count++;
        }

        var formatter = settings.getString("formatter", "Google Java Formatter");
        count = 0;
        for (var form : javaFormatters) {
            if (form.equals(formatter)) {
                javaFormatters_spinner.setSelection(count);
                break;
            }
            count++;
        }

        var disassembler = settings.getString("disassembler", "Javap");
        count = 0;
        for (var dis : javaDisassemblers) {
            if (dis.equals(disassembler)) {
                javaDisassemblers_spinner.setSelection(count);
                break;
            }
            count++;
        }

        /* Save Selected Java Version in SharedPreferences */
        javaVersions_spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> adapterView, View view, int i, long l) {
                        settings.edit().putString("version", javaVersions[i]).apply();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {}
                });

        javaCompilers_spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> adapterView, View view, int i, long l) {
                        settings.edit().putString("compiler", javaCompilers[i]).apply();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {}
                });

        javaFormatters_spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> adapterView, View view, int i, long l) {
                        settings.edit().putString("formatter", javaFormatters[i]).apply();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {}
                });

        javaDisassemblers_spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> adapterView, View view, int i, long l) {
                        settings.edit().putString("disassembler", javaDisassemblers[i]).apply();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {}
                });

        buildClasspathDialog();

        classpath_bttn.setOnClickListener(
                v -> {
                    alertDialog.show();

                    TextInputEditText classpath_edt = alertDialog.findViewById(R.id.classpath_edt);
                    Button save_classpath_bttn =
                            alertDialog.findViewById(R.id.save_classpath_bttn);

                    if (!settings.getString("classpath", "").equals("")) {
                        classpath_edt.setText(settings.getString("classpath", ""));
                    }

                    save_classpath_bttn.setOnClickListener(
                            view -> {
                                var enteredClasspath = classpath_edt.getText().toString();
                                settings.edit().putString("classpath", enteredClasspath).apply();

                                /* Check if specified classpath is empty - if yes, change button text */
                                if (enteredClasspath.equals("")) {
                                    classpath_bttn.setText(
                                            getString(R.string.classpath_not_specified));
                                } else {
                                    classpath_bttn.setText(getString(R.string.classpath_edit));
                                }

                                /* Dismiss Dialog If Showing */
                                if (alertDialog.isShowing()) alertDialog.dismiss();
                            });
                });
    }

    void buildClasspathDialog() {
        var builder = new AlertDialog.Builder(SettingActivity.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        var dialogView = getLayoutInflater().inflate(R.layout.classpath_dialog, viewGroup, false);
        builder.setView(dialogView);
        alertDialog = builder.create();
    }

    @Override
    protected void onDestroy() {
        if (alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
        super.onDestroy();
    }
}
