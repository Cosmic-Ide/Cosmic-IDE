package org.cosmicide.plugin.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionRegistryTest {
    private val point = ExtensionPoint("test.handlers", Handler::class.java)

    @Test
    fun `extension point rejects blank identity`() {
        assertFails<IllegalArgumentException> {
            ExtensionPoint(" ", Handler::class.java)
        }
    }

    @Test
    fun `registrations are ordered by priority then owner`() {
        val registry = DefaultExtensionRegistry()
        registry.register(point, Handler("low"), ownerPluginId = "z.owner", priority = 1)
        registry.register(point, Handler("second"), ownerPluginId = "b.owner", priority = 10)
        registry.register(point, Handler("first"), ownerPluginId = "a.owner", priority = 10)

        assertEquals(
            listOf("first", "second", "low"),
            registry.extensions(point).map(Handler::name)
        )
    }

    @Test
    fun `registration disposable removes only its registration`() {
        val registry = DefaultExtensionRegistry()
        val retained = Handler("retained")
        val removed = Handler("removed")
        registry.register(point, retained)
        val disposable = registry.register(point, removed)

        disposable.dispose()
        disposable.dispose()

        assertEquals(listOf(retained), registry.extensions(point))
    }

    @Test
    fun `unregister owner leaves other plugins intact`() {
        val registry = DefaultExtensionRegistry()
        registry.register(point, Handler("one"), "plugin.one")
        registry.register(point, Handler("two"), "plugin.two")
        registry.register(point, Handler("another"), "plugin.one")

        registry.unregisterOwner("plugin.one")

        assertEquals(listOf("two"), registry.extensions(point).map(Handler::name))
    }

    @Test
    fun `register validates owner and runtime type`() {
        val registry = DefaultExtensionRegistry()
        assertFails<IllegalArgumentException> {
            registry.register(point, Handler("bad"), ownerPluginId = " ")
        }

        @Suppress("UNCHECKED_CAST")
        val mismatchedPoint = point as ExtensionPoint<Any>
        val error = assertFails<IllegalArgumentException> {
            registry.register(mismatchedPoint, "not a handler")
        }
        assertTrue(error.message.orEmpty().contains(Handler::class.java.name))
    }

    private data class Handler(val name: String)
}

internal inline fun <reified T : Throwable> assertFails(block: () -> Unit): T {
    try {
        block()
    } catch (error: Throwable) {
        if (error is T) return error
        throw AssertionError("Expected ${T::class.java.name}, got ${error::class.java.name}", error)
    }
    throw AssertionError("Expected ${T::class.java.name}")
}
