/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */

package org.cosmicide.plugin.runtime

import android.content.ContextWrapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.cosmicide.plugin.api.DefaultExtensionRegistry
import org.cosmicide.plugin.api.DefaultServiceRegistry
import org.cosmicide.plugin.api.ExtensionPoint
import org.cosmicide.plugin.api.PluginDescriptor
import org.cosmicide.plugin.api.PluginEvents
import org.cosmicide.plugin.api.PluginSettings
import org.cosmicide.plugin.api.PluginStorage
import org.cosmicide.plugin.api.emit
import org.cosmicide.plugin.api.observe
import org.cosmicide.plugins.AndroidPluginServices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ModernPluginServicesTest {

    data class TestEvent(val message: String)

    interface SampleExtension {
        fun name(): String
    }

    private fun testDescriptor(id: String) = PluginDescriptor(
        id = id,
        name = "Test Plugin",
        version = "1.0.0",
        entryClass = "org.example.TestPlugin"
    )

    @Test
    fun `events emit and observe reactively`() = runBlocking {
        val events = DefaultPluginEvents()
        val flow = events.observe<TestEvent>()

        events.emit(TestEvent("Hello Modern Plugins!"))
        val received = flow.first()
        assertEquals("Hello Modern Plugins!", received.message)
    }

    @Test
    fun `extension registry observes changes reactively`() = runBlocking {
        val registry = DefaultExtensionRegistry()
        val point = ExtensionPoint("sample", SampleExtension::class.java)

        val flow = registry.observe(point)
        val initial = flow.first()
        assertEquals(0, initial.size)

        val ext = object : SampleExtension {
            override fun name() = "Plugin1"
        }
        val disposable = registry.register(point, ext)

        val updated = flow.first()
        assertEquals(1, updated.size)
        assertEquals("Plugin1", updated[0].name())

        disposable.dispose()
        val cleared = flow.first()
        assertEquals(0, cleared.size)
    }

    @Test
    fun `context automatically resolves modern services`() {
        val descriptor = testDescriptor("org.example.modern")
        val baseServices = DefaultServiceRegistry()
        val contextStub = ContextWrapper(null)
        baseServices.register(AndroidPluginServices.APPLICATION_CONTEXT, contextStub)

        val context = DefaultPluginContext(descriptor, DefaultExtensionRegistry(), baseServices)

        val events = context.services.get(PluginEvents.KEY)
        assertNotNull(events)

        val settings = context.services.get(PluginSettings.KEY)
        assertNotNull(settings)

        val storage = context.services.get(PluginStorage.KEY)
        assertNotNull(storage)
        assertEquals("org.example.modern", storage?.dataDir?.name)
    }
}
