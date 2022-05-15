package com.pranav.java.ide;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class DebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var intent = getIntent();
        var error = intent.getStringExtra("error");

        new MaterialAlertDialogBuilder(DebugActivity.this)
                .setTitle("An error occurred...")
                .setMessage(error)
                .setPositiveButton("QUIT", (dialog, which) -> finish())
                .setNegativeButton("COPY", (dialog, which) -> {
                    ((ClipboardManager) 
                            getSystemService(CLIPBOARD_SERVICE))
                               .setPrimaryClip(ClipData.newPlainText("clipboard", error));
                })
                .create()
                .show();
    }
}
