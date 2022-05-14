package com.pranav.java.ide;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public final class DebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var intent = getIntent();
        var errorMessage = intent.getStringExtra("error");

        new AlertDialog.Builder(this)
                .setTitle("An error occurred")
                .setMessage(errorMessage)
                .setPositiveButton("Quit", (dialog, which) -> finish())
                .create()
                .show();
    }
}
