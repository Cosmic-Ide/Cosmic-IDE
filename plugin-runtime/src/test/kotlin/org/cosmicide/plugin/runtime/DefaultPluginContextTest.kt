package org.cosmicide.plugin.runtime

import org.cosmicide.plugin.api.DefaultExtensionRegistry
import org.cosmicide.plugin.api.DefaultServiceRegistry
import org.cosmicide.plugin.api.PluginDescriptor
import org.cosmicide.plugin.api.PluginLogger
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultPluginContextTest {
    @Test
    fun `dispose all uses reverse registration order and continues after failure`() {
        val events = mutableListOf<String>()
        val logger = RecordingLogger()
        val context = context(logger)
        context.registerDisposable { events += "first" }
        context.registerDisposable {
            events += "second"
            error("broken cleanup")
        }
        context.registerDisposable { events += "third" }

        context.disposeAll()
        context.disposeAll()

        assertEquals(listOf("third", "second", "first"), events)
        assertEquals(listOf("Plugin disposable failed"), logger.warnings)
    }

    @Test
    fun `individual disposable is idempotent and removed from context cleanup`() {
        var calls = 0
        val context = context(RecordingLogger())
        val registration = context.registerDisposable { calls++ }

        registration.dispose()
        registration.dispose()
        context.disposeAll()

        assertEquals(1, calls)
    }

    @Test
    fun `registration remains idempotent after context cleanup`() {
        var calls = 0
        val context = context(RecordingLogger())
        val registration = context.registerDisposable { calls++ }

        context.disposeAll()
        registration.dispose()

        assertEquals(1, calls)
    }

    @Test
    fun `late disposable is immediately disposed exactly once`() {
        val context = context(RecordingLogger())
        var calls = 0
        context.disposeAll()
        val disposable = context.registerDisposable { calls++ }
        assertEquals(1, calls)
        disposable.dispose()
        context.disposeAll()
        assertEquals(1, calls)
    }

    @Test
    fun `late contributions are rejected and cleanup registration during close is disposed`() {
        val context = context(RecordingLogger())
        var calls = 0
        context.registerDisposable { context.registerDisposable { calls++ } }
        context.disposeAll()
        assertEquals(1, calls)
        org.junit.Assert.assertThrows(IllegalStateException::class.java) {
            context.extensions.register(
                org.cosmicide.plugin.api.ExtensionPoint(
                    "test",
                    String::class.java
                ), "late"
            )
        }
    }

    @Test
    fun `close racing registration never loses disposables`() {
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            repeat(100) {
                val context = context(RecordingLogger())
                val count = java.util.concurrent.atomic.AtomicInteger()
                val start = java.util.concurrent.CountDownLatch(1)
                val register =
                    executor.submit { start.await(); context.registerDisposable { count.incrementAndGet() } }
                val close = executor.submit { start.await(); context.disposeAll() }
                start.countDown()
                register.get(5, java.util.concurrent.TimeUnit.SECONDS)
                close.get(5, java.util.concurrent.TimeUnit.SECONDS)
                assertEquals(1, count.get())
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `context maps legacy core rejects foreign ownership and never removes other plugin`() {
        val registry = DefaultExtensionRegistry()
        val point = org.cosmicide.plugin.api.ExtensionPoint("owner", String::class.java)
        val context = DefaultPluginContext(
            PluginDescriptor("one", "One", "1", "Plugin"),
            registry, DefaultServiceRegistry(), RecordingLogger()
        )
        registry.register(point, "foreign", "two")
        context.extensions.register(point, "own")
        assertEquals(listOf("one", "two"), registry.registrations(point).map { it.ownerPluginId })
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            context.extensions.register(point, "spoof", "two")
        }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            context.extensions.unregisterOwner("two")
        }
        context.extensions.unregisterOwner(org.cosmicide.plugin.api.PluginIds.CORE)
        context.extensions.register(point, "replacement")
        context.disposeAll()
        assertEquals(listOf("foreign"), registry.extensions(point))
        org.junit.Assert.assertThrows(IllegalStateException::class.java) {
            context.extensions.unregisterOwner("one")
        }
    }

    @Test
    fun `context sees live host replacements`() {
        val host = DefaultServiceRegistry()
        val key = org.cosmicide.plugin.api.ServiceKey("live", String::class.java)
        val localKey = org.cosmicide.plugin.api.ServiceKey("local", String::class.java)
        host.register(key, "before")
        host.register(localKey, "host")
        val context = DefaultPluginContext(
            PluginDescriptor("one", "One", "1", "Plugin"),
            DefaultExtensionRegistry(), host, RecordingLogger()
        )
        host.register(key, "replacement")
        assertEquals("replacement", context.services.get(key))
        host.services[key] = "direct"
        assertEquals("direct", context.services.get(key))
        assertEquals("host", context.services.get(localKey))
        host.services.remove(key)
        org.junit.Assert.assertNull(context.services.get(key))
        context.disposeAll()
        assertEquals("host", host.get(localKey))
    }

    private fun context(logger: PluginLogger) = DefaultPluginContext(
        descriptor = PluginDescriptor(
            "org.example.plugin",
            "Plugin",
            "1",
            "org.example.Plugin"
        ),
        extensions = DefaultExtensionRegistry(),
        services = DefaultServiceRegistry(),
        logger = logger
    )

    private class RecordingLogger : PluginLogger {
        val warnings = mutableListOf<String>()
        override fun debug(message: String) = Unit
        override fun info(message: String) = Unit
        override fun warn(message: String, throwable: Throwable?) {
            warnings += message
        }

        override fun error(message: String, throwable: Throwable?) = Unit
    }
}
