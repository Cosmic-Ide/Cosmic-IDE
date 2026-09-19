/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */
package org.cosmicide.plugin.runtime

import android.content.Context
import android.content.ContextWrapper
import org.cosmicide.plugin.api.DefaultExtensionRegistry
import org.cosmicide.plugin.api.PluginDescriptor
import org.cosmicide.plugin.api.PluginLoadResult
import org.cosmicide.plugin.api.PluginState
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AndroidPluginManagerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context = object : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
        override fun getClassLoader(): ClassLoader =
            AndroidPluginManagerTest::class.java.classLoader!!
    }

    @Test
    fun `load rejects unsupported schema before missing entry class`() {
        val root = temporaryFolder.newFolder("plugins")
        val descriptor =
            PluginDescriptor("org.example.gate", "Gate", "1.0.0", "org.example.MissingPlugin")
        root.resolve(descriptor.id).apply { mkdirs() }.resolve("plugin.json").writeText(
            """{"id":"org.example.gate","name":"Gate","version":"1.0.0","schemaVersion":2}"""
        )
        val manager = AndroidPluginManager(context, DefaultExtensionRegistry(), root)

        val result = manager.load(descriptor)

        assertTrue("Expected Failed, got $result", result is PluginLoadResult.Failed)
        val failure = result as PluginLoadResult.Failed
        assertEquals(descriptor, failure.descriptor)
        assertEquals("schemaVersion must be the supported integer 1", failure.reason)
        assertTrue(failure.cause is IllegalArgumentException)
        assertFalse(manager.plugins.any { it.state == PluginState.ACTIVE })
        assertEquals(PluginState.FAILED, manager.plugins.single().state)
        assertEquals(failure.reason, manager.plugins.single().errorMessage)
    }

    @Test
    fun `load with supported schema reaches missing entry class validation`() {
        val root = temporaryFolder.newFolder("plugins")
        val descriptor =
            PluginDescriptor("org.example.gate", "Gate", "1.0.0", "org.example.MissingPlugin")
        root.resolve(descriptor.id).apply { mkdirs() }.resolve("plugin.json").writeText(
            """{"id":"org.example.gate","name":"Gate","version":"1.0.0","schemaVersion":1}"""
        )
        val manager = AndroidPluginManager(context, DefaultExtensionRegistry(), root)

        val result = manager.load(descriptor)

        assertTrue("Expected Failed, got $result", result is PluginLoadResult.Failed)
        val failure = result as PluginLoadResult.Failed
        assertEquals("JSONObject[\"entryClass\"] not found.", failure.reason)
        assertTrue(failure.cause is JSONException)
        assertFalse(manager.plugins.any { it.state == PluginState.ACTIVE })
        assertEquals(PluginState.FAILED, manager.plugins.single().state)
    }
}
