package org.cosmicide.plugins

import android.content.Context
import org.cosmicide.plugin.api.ServiceKey
import java.io.File

object AndroidPluginServices {
    @JvmField
    val APPLICATION_CONTEXT = ServiceKey(
        "android.applicationContext",
        Context::class.java
    )

    @JvmField
    val PLUGIN_DIRECTORY = ServiceKey(
        "android.pluginDirectory",
        File::class.java
    )
}