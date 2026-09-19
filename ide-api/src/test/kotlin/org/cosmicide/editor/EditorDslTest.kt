/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */

package org.cosmicide.editor

import org.cosmicide.plugin.api.DefaultExtensionRegistry
import org.cosmicide.plugin.api.DefaultServiceRegistry
import org.cosmicide.plugin.api.Disposable
import org.cosmicide.plugin.api.MutableExtensionRegistry
import org.cosmicide.plugin.api.PluginContext
import org.cosmicide.plugin.api.PluginDescriptor
import org.cosmicide.plugin.api.PluginLogger
import org.cosmicide.plugin.api.ServiceRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EditorDslTest {

    private class FakePluginContext : PluginContext {
        override val descriptor =
            PluginDescriptor("test.plugin", "Test Plugin", "1.0.0", "TestClass")
        override val extensions: MutableExtensionRegistry = DefaultExtensionRegistry()
        override val services: ServiceRegistry = DefaultServiceRegistry()
        override val logger = object : PluginLogger {
            override fun info(message: String) {}
            override fun warn(message: String, throwable: Throwable?) {}
            override fun error(message: String, throwable: Throwable?) {}
            override fun debug(message: String) {}
        }

        override fun registerDisposable(disposable: Disposable): Disposable = disposable
    }

    @Test
    fun `registerEditorAction adds action provider`() {
        val context = FakePluginContext()
        val disposable = context.registerEditorAction(
            id = "test.action",
            label = "Test Action"
        ) { _ -> }

        assertNotNull(disposable)
        val providers = context.extensions.extensions(EditorExtensionPoints.EDITOR_ACTION_PROVIDER)
        assertEquals(1, providers.size)
        assertEquals("test.action", providers[0].id)
    }

    @Test
    fun `registerContextMenuAction adds context menu provider`() {
        val context = FakePluginContext()
        val disposable = context.registerContextMenuAction(
            id = "test.contextMenu",
            label = "Test Context Menu"
        ) { _ -> }

        assertNotNull(disposable)
        val providers = context.extensions.extensions(EditorExtensionPoints.CONTEXT_MENU_PROVIDER)
        assertEquals(1, providers.size)
        assertEquals("test.contextMenu", providers[0].id)
    }

    @Test
    fun `registerTextAction adds text action provider`() {
        val context = FakePluginContext()
        val disposable = context.registerTextAction(
            id = "test.textAction",
            label = "Test Text Action"
        ) { _ -> }

        assertNotNull(disposable)
        val providers = context.extensions.extensions(EditorExtensionPoints.TEXT_ACTION_PROVIDER)
        assertEquals(1, providers.size)
        assertEquals("test.textAction", providers[0].id)
    }

    @Test
    fun `subscribeEditorEvents adds event subscriber`() {
        val context = FakePluginContext()
        val disposable = context.subscribeEditorEvents { _, _, _, _ -> }

        assertNotNull(disposable)
        val subscribers = context.extensions.extensions(EditorExtensionPoints.EVENT_SUBSCRIBER)
        assertEquals(1, subscribers.size)
    }
}
