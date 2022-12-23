/*
 * Copyright (C) 2015 Pedro Vicente Gomez Sanchez.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pedrovgs.lynx

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity created to show a LynxView with "match_parent" configuration for LynxView
 * "layout_height" and "layout_width" attributes. To configure LynxView and all the information to
 * show use Activity extras and a LynxConfig object. Use getIntent() method to obtain a valid intent
 * to start this Activity.
 *
 * @author Pedro Vicente Gomez Sanchez.
 */
class LynxActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lynx_activity)
        val lynxConfig = lynxConfig
        configureLynxView(lynxConfig)
    }

    private val lynxConfig: LynxConfig?
        get() {
            val extras = intent.extras
            var lynxConfig: LynxConfig? = LynxConfig()
            if (extras != null && extras.containsKey(LYNX_CONFIG_EXTRA)) {
                lynxConfig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable(LYNX_CONFIG_EXTRA, LynxConfig::class.java)
                } else {
                    extras.getSerializable(LYNX_CONFIG_EXTRA) as LynxConfig
                }
            }
            return lynxConfig
        }

    private fun configureLynxView(lynxConfig: LynxConfig?) {
        val lynxView = findViewById<View>(R.id.lynx_view) as LynxView
        lynxView.setLynxConfig(lynxConfig!!)
    }

    companion object {
        private const val LYNX_CONFIG_EXTRA = "extra_lynx_config"

        /**
         * Generates an Intent to start LynxActivity with a default LynxConfig object as configuration.
         *
         * @param context the application context
         * @return a new `Intent` to start [LynxActivity]
         */
        fun getIntent(context: Context?): Intent {
            return getIntent(context, LynxConfig())
        }

        /**
         * Generates an Intent to start LynxActivity with a LynxConfig configuration passed as
         * parameter.
         *
         * @param context the application context
         * @param lynxConfig the lynx configuration
         * @return a new `Intent` to start [LynxActivity]
         */
        private fun getIntent(context: Context?, lynxConfig: LynxConfig?): Intent {
            var config = lynxConfig
            if (config == null) {
                config = LynxConfig()
            }
            val intent = Intent(context, LynxActivity::class.java)
            intent.putExtra(LYNX_CONFIG_EXTRA, config)
            return intent
        }
    }
}