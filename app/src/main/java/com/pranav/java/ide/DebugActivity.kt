package com.pranav.java.ide

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DebugActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = getIntent()
        val error = intent?.getStringExtra("error")!!

        MaterialAlertDialogBuilder(this)
                .setTitle("An error occurred...")
                .setMessage(error)
                .setPositiveButton("QUIT", { _, _ -> finish()})
                .setNegativeButton(
                        "COPY",
                        { _, _ ->
                            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                                    .setPrimaryClip(ClipData.newPlainText("clipboard", error))
                        })
                .create()
                .show()
    }
}
