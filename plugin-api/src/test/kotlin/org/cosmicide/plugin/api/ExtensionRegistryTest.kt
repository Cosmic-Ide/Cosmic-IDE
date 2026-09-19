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

    @Test
    fun `equal duplicates have independent tokens even after owner removal`() {
        val registry = DefaultExtensionRegistry()
        val stale = registry.register(point, Handler("equal"))
        val second = registry.register(point, Handler("equal"))
        stale.dispose()
        stale.dispose()
        assertEquals(1, registry.extensions(point).size)
        registry.unregisterOwner(PluginIds.CORE)
        registry.register(point, Handler("equal"))
        second.dispose()
        assertEquals(1, registry.extensions(point).size)
    }

    @Test
    fun `snapshot is immutable cached per id and requested runtime type with stable ties`() {
        val registry = DefaultExtensionRegistry()
        val broad = ExtensionPoint("shared", Any::class.java)
        val strings = ExtensionPoint("shared", String::class.java)
        registry.register(broad, "first")
        registry.register(strings, "second")
        registry.register(broad, 42)
        val snapshot = registry.registrations(strings)
        assertEquals(listOf("first", "second"), snapshot.map { it.extension })
        org.junit.Assert.assertSame(broad, snapshot.first().point)
        org.junit.Assert.assertSame(snapshot, registry.registrations(strings))
        val revision = registry.revision
        registry.register(point, Handler("unrelated"))
        assertTrue(registry.revision > revision)
        org.junit.Assert.assertSame(snapshot, registry.registrations(strings))
        assertFails<UnsupportedOperationException> { (snapshot as MutableList).clear() }
        registry.register(strings, "third")
        assertEquals(2, snapshot.size)
        assertEquals(3, registry.registrations(strings).size)
        assertEquals(4, registry.registrations(broad).size)
    }

    @Test
    fun `concurrent snapshots remain ordered stable and safe during mutation`() {
        val registry = DefaultExtensionRegistry()
        registry.register(point, Handler("anchor"), "anchor", Int.MAX_VALUE)
        val pool = java.util.concurrent.Executors.newFixedThreadPool(3)
        try {
            val jobs = (0..2).map { worker ->
                pool.submit {
                    repeat(500) { iteration ->
                        val token = registry.register(
                            point,
                            Handler("$worker-$iteration"),
                            "owner.$worker",
                            iteration
                        )
                        val snapshot = registry.registrations(point)
                        val saved = snapshot.toList()
                        assertEquals("anchor", snapshot.first().extension.name)
                        assertEquals(snapshot.sortedWith(compareByDescending<ExtensionRegistration<Handler>> { it.priority }
                            .thenBy { it.ownerPluginId }), snapshot)
                        token.dispose()
                        assertEquals(saved, snapshot)
                    }
                }
            }
            jobs.forEach { it.get(10, java.util.concurrent.TimeUnit.SECONDS) }
            assertEquals(listOf(Handler("anchor")), registry.extensions(point))
        } finally {
            pool.shutdownNow()
        }
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
