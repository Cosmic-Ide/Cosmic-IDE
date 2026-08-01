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
