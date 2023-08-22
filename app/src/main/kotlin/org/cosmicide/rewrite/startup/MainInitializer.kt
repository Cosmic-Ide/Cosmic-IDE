package org.cosmicide.rewrite.startup

import android.content.Context
import androidx.startup.Initializer

class MainInitializer : Initializer<Unit> {

    override fun create(context: Context) {}

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(
            DebugInitializer::class.java,
            PreferencesInitializer::class.java
        )
    }
}