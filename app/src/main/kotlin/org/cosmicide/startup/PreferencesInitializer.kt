package org.cosmicide.startup

import android.content.Context
import androidx.startup.Initializer
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.util.FileUtil

class PreferencesInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        FileUtil.init(context.getExternalFilesDir(null)!!)
        Prefs.init(context.applicationContext)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}