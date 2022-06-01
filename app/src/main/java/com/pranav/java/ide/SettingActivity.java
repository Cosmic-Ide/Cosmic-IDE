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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.pranav.common.Indexer;
import com.pranav.common.util.FileUtil;

public final class SettingActivity extends AppCompatActivity {
    private String[] javaVersions = {
        "1.3", "1.4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17"
    };

    private String[] javaCompilers = {"Javac", "Eclipse Compiler for Java"};

    private String[] javaFormatters = {"Google Java Formatter", "Eclipse Java Formatter"};

    private String[] javaDisassemblers = {"Javap", "Eclipse Class Disassembler"};

    private AlertDialog classpathDialog;
    private AlertDialog argumentsDialog;
    private AlertDialog javaPathDialog;
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

        Spinner javaVersions_spinner = findViewById(R.id.javaVersions_spinner);
        Spinner javaCompilers_spinner = findViewById(R.id.javaCompilers_spinner);
        Spinner javaFormatters_spinner = findViewById(R.id.javaFormatters_spinner);
        Spinner javaDisassemblers_spinner = findViewById(R.id.javaDisassemblers_spinner);
        MaterialButton classpath_bttn = findViewById(R.id.classpath_bttn);
        MaterialButton arguments_bttn = findViewById(R.id.arguments_bttn);
        MaterialButton java_path_bttn = findViewById(R.id.save_java_path_bttn);

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
            classpath_bttn.setText(getString(R.string.edit));
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
                    classpathDialog.show();

                    TextInputEditText classpath_edt =
                            classpathDialog.findViewById(R.id.classpath_edt);
                    MaterialButton save_classpath_bttn =
                            classpathDialog.findViewById(R.id.save_classpath_bttn);

                    if (!settings.getString("classpath", "").equals("")) {
                        classpath_edt.setText(settings.getString("classpath", ""));
                    }

                    save_classpath_bttn.setOnClickListener(
                            view -> {
                                var enteredClasspath = classpath_edt.getText().toString();
                                settings.edit().putString("classpath", enteredClasspath).apply();

                                classpath_bttn.setText(getString(R.string.edit));

                                /* Dismiss Dialog If Showing */
                                if (classpathDialog.isShowing()) classpathDialog.dismiss();
                            });
                });
        buildArgumentsDialog();

        arguments_bttn.setOnClickListener(
                v -> {
                    argumentsDialog.show();

                    TextInputEditText arguments_edt =
                            argumentsDialog.findViewById(R.id.arguments_edt);
                    MaterialButton save_arguments_bttn =
                            argumentsDialog.findViewById(R.id.save_arguments_bttn);

                    if (!settings.getString("program_arguments", "").equals("")) {
                        arguments_edt.setText(settings.getString("program_arguments", ""));
                    }

                    save_arguments_bttn.setOnClickListener(
                            view -> {
                                var enteredArgs = arguments_edt.getText().toString();
                                settings.edit().putString("program_arguments", enteredArgs).apply();

                                arguments_bttn.setText(getString(R.string.edit));

                                /* Dismiss Dialog If Showing */
                                if (argumentsDialog.isShowing()) argumentsDialog.dismiss();
                            });
                });
        buildJavaPathDialog();

        java_path_bttn.setOnClickListener(
                v -> {
                    javaPathDialog.show();

                    var index = new Indexer("editor");

                    TextInputEditText path_edt =
                            javaPathDialog.findViewById(R.id.java_path_edt);
                    MaterialButton save_java_path_bttn =
                            javaPathDialog.findViewById(R.id.save_java_path_bttn);

                    path_edt.setText(FileUtil.getJavaDir());

                    save_java_path_bttn.setOnClickListener(
                            view -> {
                                var enteredPath = path_edt.getText().toString();
                                if (enteredPath.isEmpty()) {
                                    FileUtil.setJavaDirectory(FileUtil.getDataDir() + "/java/");
                                } else {
                                    FileUtil.setJavaDirectory(enteredPath);
                                }

                                save_java_path_bttn.setText(getString(R.string.edit));

                                /* Dismiss Dialog If Showing */
                                if (javaPathDialog.isShowing()) javaPathDialog.dismiss();
                            });
                });
    }

    private void buildClasspathDialog() {
        var builder = new AlertDialog.Builder(SettingActivity.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        var dialogView = getLayoutInflater().inflate(R.layout.classpath_dialog, viewGroup, false);
        builder.setView(dialogView);
        classpathDialog = builder.create();
    }

    private void buildArgumentsDialog() {
        var builder = new AlertDialog.Builder(SettingActivity.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        var dialogView = getLayoutInflater().inflate(R.layout.arguments_dialog, viewGroup, false);
        builder.setView(dialogView);
        argumentsDialog = builder.create();
    }

    private void buildJavaPathDialog() {
        var builder = new AlertDialog.Builder(SettingActivity.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        var dialogView = getLayoutInflater().inflate(R.layout.enter_custom_java_path, viewGroup, false);
        builder.setView(dialogView);
        javaPathDialog = builder.create();
    }

    @Override
    protected void onDestroy() {
        if (classpathDialog.isShowing()) {
            classpathDialog.dismiss();
        }
        if (argumentsDialog.isShowing()) {
            argumentsDialog.dismiss();
        }
        if (javaPathDialog.isShowing()) {
            javaPathDialog.dismiss();
        }
        super.onDestroy();
    }
}
