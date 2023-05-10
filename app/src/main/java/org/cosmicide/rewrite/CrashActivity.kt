package org.cosmicide.rewrite

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.cosmicide.rewrite.databinding.ActivityCrashBinding
import org.cosmicide.rewrite.util.CommonUtils

/**
 * Activity that displays the stack trace of a crash and allows the user to copy it to the clipboard.
 */
class CrashActivity : AppCompatActivity() {

    companion object {
        const val STACKTRACE = "stacktrace"
        const val DEFAULT_ERROR_MESSAGE = "Unable to get stacktrace."

        /**
         * Creates an intent to start this activity with the given stack trace.
         *
         * @param context the context for start this activity
         * @param stackTrace the stack trace string to display
         */
        fun newIntent(context: String, stackTrace: String): Intent {
            val intent = Intent(context, CrashActivity::class.java)
            intent.putExtra(STACKTRACE, stackTrace)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityCrashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extras = intent.extras
        if (extras == null) {
            finishAffinity()
            return
        }

        binding.errorText.text = extras.getString(STACKTRACE, DEFAULT_ERROR_MESSAGE)

        binding.copyButton.setOnClickListener {
            CommonUtils.copyToClipboard(it.context, binding.errorText.text.toString())
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_close -> {
                    onBackPressed()
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }
}