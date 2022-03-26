package com.pranav.java.ide;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

public class DebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String errorMessage = intent.getStringExtra("error");

        new AlertDialog.Builder(this)
           .setTitle("An error occurred")
           .setMessage(errorMessage)
           .setPositiveButton("Quit", (dialog, which) -> finish())
           .create()
           .show();
    }
}