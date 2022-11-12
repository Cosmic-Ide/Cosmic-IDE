package org.cosmic.ide.activity

import android.content.DialogInterface
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cosmic.ide.R
import org.cosmic.ide.util.AndroidUtilities

class DebugActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = getIntent()
        val error = intent?.getStringExtra("error")!!

        MaterialAlertDialogBuilder(this, AndroidUtilities.getDialogFullWidthButtonsThemeOverlay())
                .setTitle(getString(R.string.error))
                .setMessage(error)
                .setPositiveButton(getString(R.string.quit), { _, _ -> finish() })
                .setNegativeButton(getString(R.string.copy_stacktrace), { _, which ->
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        AndroidUtilities.copyToClipboard(error)
                    }
                })
                .show()
    }
}