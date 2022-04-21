package com.pranav.java.ide;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public final class SettingActivity extends AppCompatActivity {
    private String[] javaVersions = {"1.3", "1.4", "5.0", "6.0", "7.0", "8.0", "9.0", "10.0", "11.0", "12.0", "13.0", "14.0", "15.0", "16.0", "17.0"};

    private Spinner javaVersions_spinner;
    private MaterialButton classpath_bttn;

    private AlertDialog alertDialog;
    private SharedPreferences settings;

    private String TAG = "SettingsActivity";

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

        javaVersions_spinner = findViewById(R.id.javaVersion_spinner);
        classpath_bttn = findViewById(R.id.classpath_bttn);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, javaVersions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        javaVersions_spinner.setAdapter(adapter);

        /* Check if Classpath stored in SharedPref is empty - if yes, change button text */
        if (settings.getString("classpath", "").equals("")) {
            classpath_bttn.setText(getString(R.string.classpath_not_specified));
        } else {
            classpath_bttn.setText(getString(R.string.classpath_edit));
        }

        /* Select Version in Spinner based on SharedPreferences Value */
        int count = 0;
        for (String version : javaVersions) {
            if (version.equals(settings.getString("javaVersion", "7.0"))) {
                javaVersions_spinner.setSelection(count);
                break;
            }
            count++;
        }

        /* Save Selected Java Version in SharedPreferences */
        javaVersions_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                settings.edit().putString("javaVersion", String.valueOf(javaVersions[i])).apply();
                Log.e(TAG, "Selected Java Version (By User): " + javaVersions[i]);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        buildClasspathDialog();

        classpath_bttn.setOnClickListener(v -> {
            alertDialog.show();

            TextInputEditText classpath_edt = alertDialog.findViewById(R.id.classpath_edt);
            MaterialButton save_classpath_bttn = alertDialog.findViewById(R.id.save_classpath_bttn);

            if (!settings.getString("classpath", "").equals("")) {
                classpath_edt.setText(settings.getString("classpath", ""));
            }

            save_classpath_bttn.setOnClickListener(view -> {
                String enteredClasspath = classpath_edt.getText().toString();
                settings.edit().putString("classpath", enteredClasspath).apply();

                /* Check if specified classpath is empty - if yes, change button text */
                if (enteredClasspath.equals("")) {
                    classpath_bttn.setText(getString(R.string.classpath_not_specified));
                } else {
                    classpath_bttn.setText(getString(R.string.classpath_edit));
                }

                /* Dismiss Dialog If Showing */
                if (alertDialog.isShowing()) alertDialog.dismiss();
            });
        });
    }

    void buildClasspathDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = getLayoutInflater().inflate(R.layout.classpath_dialog, viewGroup, false);
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