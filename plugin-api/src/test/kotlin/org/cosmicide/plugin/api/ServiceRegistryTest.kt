package org.cosmicide.plugin.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ServiceRegistryTest {
    private val key = ServiceKey("test.clock", Clock::class.java)

    @Test
    fun `service key rejects blank name`() {
        assertFails<IllegalArgumentException> { ServiceKey("", Clock::class.java) }
    }

    @Test
    fun `register get require and unregister share one contract`() {
        val registry = DefaultServiceRegistry()
        val clock = Clock(42)

        registry.register(key, clock)

        assertSame(clock, registry.get(key))
        assertSame(clock, registry.require(key))
        registry.unregister(key)
        assertNull(registry.get(key))
        val error = assertFails<IllegalStateException> { registry.require(key) }
        assertEquals("Required service 'test.clock' is not registered", error.message)
    }

    @Test
    fun `stale disposable cannot remove replacement service`() {
        val registry = DefaultServiceRegistry()
        val oldRegistration = registry.register(key, Clock(1))
        val replacement = Clock(2)
        registry.register(key, replacement)

        oldRegistration.dispose()

        assertSame(replacement, registry.get(key))
    }

    @Test
    fun `registration validates runtime type`() {
        val registry = DefaultServiceRegistry()

        @Suppress("UNCHECKED_CAST")
        val mismatchedKey = key as ServiceKey<Any>

        assertFails<IllegalArgumentException> {
            registry.register(mismatchedKey, "not a clock")
        }
    }

    @Test
    fun `equal and same instance replacements survive stale disposal`() {
        val registry = DefaultServiceRegistry()
        val instance = Clock(1)
        val first = registry.register(key, instance)
        val second = registry.register(key, instance)
        first.dispose()
        assertSame(instance, registry.get(key))
        val equal = Clock(1)
        registry.register(key, equal)
        second.dispose()
        assertSame(equal, registry.get(key))
    }

    @Test
    fun `public map and copy retain legacy direct mutation semantics`() {
        val registry = DefaultServiceRegistry()
        val old = registry.register(key, Clock(1))
        val replacement = Clock(1)
        registry.services[key] = replacement
        old.dispose()
        assertSame(replacement, registry.get(key))
        val copy = registry.copy()
        registry.services.clear()
        assertNull(registry.get(key))
        assertSame(replacement, copy.get(key))
    }

    private data class Clock(val tick: Int)
}
