/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */
package org.cosmicide.plugin.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PluginContextExtensionsTest {
    private val point = ExtensionPoint("test.contributions", String::class.java)

    @Test
    fun `register supplies owner and priority and tracks only its contribution`() {
        val context = FakeContext()
        context.extensions.register(point, "foreign", "other", 10)
        context.register(point, "own", priority = 20)
        val registrations = context.extensions.registrations(point)
        assertEquals(listOf("own", "foreign"), registrations.map { it.extension })
        assertEquals("test.plugin", registrations.first().ownerPluginId)
        assertEquals(20, registrations.first().priority)
        assertEquals(1, context.tracked.size)

        context.disposeAll()
        assertEquals(listOf("foreign"), context.extensions.extensions(point))
    }

    @Test
    fun `default priority and returned handle support early removal`() {
        val context = FakeContext()
        val handle = context.register(point, "own")
        assertEquals(0, context.extensions.registrations(point).single().priority)
        assertSame(context.tracked.single(), handle)
        handle.dispose()
        assertTrue(context.extensions.extensions(point).isEmpty())
        context.disposeAll()
        assertTrue(context.extensions.extensions(point).isEmpty())
    }

    @Test
    fun `onDispose defers cleanup and returns context tracked handle`() {
        val context = FakeContext()
        var calls = 0
        val handle = context.onDispose { calls++ }
        assertEquals(0, calls)
        assertSame(context.tracked.single(), handle)
        context.disposeAll()
        assertEquals(1, calls)
    }

    @Test
    fun `launch inherits service scope and host cancellation runs finalizer`() = runBlocking {
        val context = FakeContext()
        val lifecycle = SupervisorJob()
        val service = object : PluginCoroutineScope {
            override val scope = CoroutineScope(lifecycle + Dispatchers.Unconfined)
        }
        context.services.register(PluginCoroutineScope.KEY, service)
        var started = false
        var finalized = false
        try {
            val child = context.launch {
                started = true
                try {
                    awaitCancellation()
                } finally {
                    finalized = true
                }
            }
            assertTrue(started)
            assertTrue(lifecycle.children.any { it === child })
            lifecycle.cancel()
            child.join()
            assertTrue(child.isCancelled)
            assertTrue(finalized)
            val late = context.launch { fail("Cancelled scope must not run work") }
            late.join()
            assertTrue(late.isCancelled)
        } finally {
            lifecycle.cancel()
        }
    }

    @Test
    fun `launch fails explicitly without lifecycle service instead of detaching work`() {
        val context = FakeContext()
        assertThrows(IllegalStateException::class.java) {
            context.launch { fail("Missing scope must not run work") }
        }
    }

    // Deliberately unscoped registry: helpers must work without the Android runtime facade.
    private class FakeContext : PluginContext {
        override val descriptor = PluginDescriptor("test.plugin", "Test", "1", "TestPlugin")
        override val extensions = DefaultExtensionRegistry()
        override val services = DefaultServiceRegistry()
        override val logger = object : PluginLogger {
            override fun debug(message: String) = Unit
            override fun info(message: String) = Unit
            override fun warn(message: String, throwable: Throwable?) = Unit
            override fun error(message: String, throwable: Throwable?) = Unit
        }
        val tracked = mutableListOf<Disposable>()
        override fun registerDisposable(disposable: Disposable): Disposable =
            disposable.also(tracked::add)

        fun disposeAll() {
            val pending = tracked.toList().asReversed()
            tracked.clear()
            pending.forEach(Disposable::dispose)
        }
    }
}
