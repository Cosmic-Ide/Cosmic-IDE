package org.cosmicide.rewrite

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.cosmicide.rewrite.databinding.ActivityCrashBinding
import org.cosmicide.rewrite.util.CommonUtils

class CrashActivity : AppCompatActivity() {

    companion object {
        const val REPORT_CRASH = "org.cosmicide.rewrite.REPORT_CRASH"
        const val STACKTRACE = "stacktrace"
        const val DEFAULT_ERROR_MESSAGE = "Unable to get stacktrace."
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

        val trace = extras.getString(STACKTRACE, DEFAULT_ERROR_MESSAGE)
        binding.errorText.text = trace

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
