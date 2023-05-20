/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite

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