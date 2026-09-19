package org.cosmicide.plugin.runtime

import org.cosmicide.plugin.api.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PluginRuntimeControllerTest {
    private val registry = DefaultExtensionRegistry()
    private val runtime = PluginRuntimeController(registry)
    private val events = mutableListOf<String>()
    private val point = ExtensionPoint("test", String::class.java)
    private val logger = object : PluginLogger {
        override fun debug(message: String) = Unit
        override fun info(message: String) = Unit
        override fun warn(message: String, throwable: Throwable?) = Unit
        override fun error(message: String, throwable: Throwable?) = Unit
    }

    private fun register(
        id: String, vararg deps: PluginDependency, fail: Boolean = false,
        origin: String = id, onActivate: (PluginContext) -> Unit = {}
    ) {
        val descriptor = PluginDescriptor(id, id, "1.0.0", "Plugin", dependencies = deps.toList())
        runtime.register(
            PluginRuntimeController.Candidate(
                descriptor,
                origin,
                create = {
                    object : CosmicPlugin {
                        override fun activate(context: PluginContext) {
                            events += "+$id"
                            // Intentionally omit explicit disposable tracking: context close must still clean it.
                            context.extensions.register(point, id, id)
                            context.registerDisposable { events += "dispose:$id" }
                            onActivate(context)
                            check(!fail) { "broken $id" }
                        }

                        override fun deactivate() {
                            events += "-$id"
                        }
                    }
                },
                context = {
                    DefaultPluginContext(
                        descriptor,
                        registry,
                        DefaultServiceRegistry(),
                        logger
                    )
                })
        )
    }

    @Test
    fun `builtins and installed factories share dependency lifecycle and unload refusal`() {
        register("app", PluginDependency("base"), origin = "installed")
        register("base", origin = "builtin")
        assertTrue(runtime.load("app") is PluginLoadResult.Loaded)
        assertEquals(listOf("+base", "+app"), events)
        val error = assertThrows(PluginLifecycleException::class.java) { runtime.unload("base") }
        assertEquals(PluginFailureKind.REQUIRED_DEPENDENTS, error.kind)
        assertEquals(listOf("app", "base"), registry.extensions(point).sorted())
        runtime.unload("app")
        runtime.unload("base")
        assertTrue(registry.extensions(point).isEmpty())
        assertEquals(
            listOf("+base", "+app", "-app", "dispose:app", "-base", "dispose:base"),
            events
        )
    }

    @Test
    fun `failed root unwinds new dependencies but never a previously active plugin`() {
        register("existing")
        runtime.load("existing")
        register("new", PluginDependency("existing"))
        register("root", PluginDependency("new"), fail = true)
        assertTrue(runtime.load("root") is PluginLoadResult.Failed)
        assertEquals(listOf("existing"), registry.extensions(point))
        assertFalse(events.contains("-existing"))
        assertTrue(events.containsAll(listOf("dispose:root", "-new", "dispose:new")))
    }

    @Test
    fun `failed optional provider does not block root and its orphan dependencies rollback`() {
        register("leaf")
        register("optional", PluginDependency("leaf"), fail = true)
        register("root", PluginDependency("optional", optional = true))
        assertTrue(runtime.load("root") is PluginLoadResult.Loaded)
        assertEquals(listOf("root"), registry.extensions(point))
        assertTrue(events.contains("-leaf"))
    }

    @Test
    fun `failed dependency blocks dependent code and records structured cause`() {
        register("base", fail = true)
        register("root", PluginDependency("base"))
        val result = runtime.load("root") as PluginLoadResult.Failed
        assertEquals(
            PluginFailureKind.DEPENDENCY_FAILED,
            (result.cause as PluginLifecycleException).kind
        )
        assertFalse(events.contains("+root"))
        assertTrue(registry.extensions(point).isEmpty())
    }

    @Test
    fun `missing and cycles fail before factory execution`() {
        register("root", PluginDependency("absent"))
        assertTrue(runtime.load("root") is PluginLoadResult.Failed)
        register("absent", PluginDependency("root"))
        assertTrue(runtime.load("root") is PluginLoadResult.Failed)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `duplicate origins fail rather than overwrite active version`() {
        register("root", origin = "builtin")
        runtime.load("root")
        register("root", origin = "installed")
        val result = runtime.load("root") as PluginLoadResult.Failed
        assertEquals(PluginFailureKind.DUPLICATE, (result.cause as PluginLifecycleException).kind)
        assertEquals(PluginState.ACTIVE, runtime.plugins.single().state)
        assertEquals(listOf("+root"), events)
    }

    @Test
    fun `concurrent load requests invoke activation exactly once and allow snapshot reads`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        register("root", onActivate = {
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
        })
        try {
            val first = executor.submit<PluginLoadResult> { runtime.load("root") }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            assertEquals(PluginState.DISCOVERED, runtime.plugins.single().state)
            val second = executor.submit<PluginLoadResult> { runtime.load("root") }
            release.countDown()
            assertSame(
                (first.get(5, TimeUnit.SECONDS) as PluginLoadResult.Loaded).plugin,
                (second.get(5, TimeUnit.SECONDS) as PluginLoadResult.Loaded).plugin
            )
            assertEquals(listOf("+root"), events)
        } finally {
            release.countDown(); executor.shutdownNow()
        }
    }

    @Test
    fun `callback lifecycle reentry is rejected without corrupting activation`() {
        register("root", onActivate = {
            val failure =
                assertThrows(PluginLifecycleException::class.java) { runtime.load("root") }
            assertEquals(PluginFailureKind.REENTRANT, failure.kind)
            assertThrows(PluginLifecycleException::class.java) { runtime.unload("root") }
        })
        assertTrue(runtime.load("root") is PluginLoadResult.Loaded)
        runtime.unload("root")
        assertTrue(registry.extensions(point).isEmpty())
    }

    @Test
    fun `package transaction refuses before mutation and restores previous candidate after failed replacement`() {
        register("base")
        register("dependent", PluginDependency("base"))
        runtime.load("dependent")
        var mutated = false
        assertThrows(PluginLifecycleException::class.java) {
            runtime.exclusive { runtime.unload("base"); mutated = true }
        }
        assertFalse(mutated)
        runtime.unload("dependent")
        runtime.exclusive {
            runtime.unload("base")
            register("base", fail = true)
            assertTrue(runtime.load("base") is PluginLoadResult.Failed)
            runtime.unload("base")
            register("base")
            assertTrue(runtime.load("base") is PluginLoadResult.Loaded)
        }
        assertEquals(listOf("base"), registry.extensions(point))
    }

    @Test
    fun `throwing deactivation still closes context and can reload`() {
        val descriptor = PluginDescriptor("root", "root", "1", "Plugin")
        runtime.register(
            PluginRuntimeController.Candidate(
                descriptor,
                "builtin",
                create = {
                    object : CosmicPlugin {
                        override fun activate(context: PluginContext) {
                            context.extensions.register(point, "root")
                        }

                        override fun deactivate() {
                            error("bad cleanup")
                        }
                    }
                },
                context = {
                    DefaultPluginContext(
                        descriptor,
                        registry,
                        DefaultServiceRegistry(),
                        logger
                    )
                })
        )
        runtime.load("root")
        runtime.unload("root")
        assertTrue(registry.extensions(point).isEmpty())
        assertTrue(runtime.load("root") is PluginLoadResult.Loaded)
    }
}
