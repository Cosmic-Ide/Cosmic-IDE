package org.cosmicide.rewrite

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.developer.crashx.config.CrashConfig
import org.cosmicide.rewrite.databinding.ActivityCrashBinding
import org.cosmicide.rewrite.util.CommonUtils

/**
 * Activity that displays the stack trace of a crash and allows the user to copy it to the clipboard.
 */
class CrashActivity : AppCompatActivity() {

    private var config: CrashConfig? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityCrashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        config = com.developer.crashx.CrashActivity.getConfigFromIntent(intent)
        if (config == null) {
            finishAffinity()
            return
        }

        binding.errorText.text = com.developer.crashx.CrashActivity.getStackTraceFromIntent(intent)

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
        com.developer.crashx.CrashActivity.closeApplication(this, config!!)
    }
}