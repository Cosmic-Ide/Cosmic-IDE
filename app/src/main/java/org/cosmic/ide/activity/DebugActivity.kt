package org.cosmic.ide.activity

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cosmic.ide.R
import org.cosmic.ide.util.AndroidUtilities

/** There is no need to extend this activity through the
 * BaseActivity, as it does not need insets, shared preferences, and so on.
 */
class DebugActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = getIntent()
        val error = intent?.getStringExtra("error")!!

        /** TODO: Use dialog via AndroidUtilities#showSimpleAlert(
         * Context, Title, Message, Positive, Negative, DialogInterface.OnClickListener)
         */
        MaterialAlertDialogBuilder(this, AndroidUtilities.getDialogFullWidthButtonsThemeOverlay())
                .setTitle(getString(R.string.error))
                .setMessage(error)
                .setPositiveButton(getString(R.string.quit), { _, _ -> finishAffinity() })
                .setNegativeButton(getString(R.string.copy_stacktrace), { _, which ->
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        AndroidUtilities.copyToClipboard(error)
                    }
                    finishAffinity()
                })
                .show()
    }
}