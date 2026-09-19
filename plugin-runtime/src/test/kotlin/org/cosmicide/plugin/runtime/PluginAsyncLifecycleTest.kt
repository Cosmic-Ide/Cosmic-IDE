package org.cosmicide.plugin.runtime

import kotlinx.coroutines.*
import org.cosmicide.plugin.api.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class PluginAsyncLifecycleTest {
    private val registry = DefaultExtensionRegistry()
    private val runtime = PluginRuntimeController(registry)
    private val point = ExtensionPoint("test", String::class.java)
    private val warnings = AtomicInteger()
    private val logger = object : PluginLogger {
        override fun debug(message: String) = Unit
        override fun info(message: String) = Unit
        override fun warn(message: String, throwable: Throwable?) {
            warnings.incrementAndGet()
        }

        override fun error(message: String, throwable: Throwable?) = Unit
    }

    private fun register(
        id: String = "root", activate: (PluginContext) -> Unit = {},
        deactivate: () -> Unit = {}
    ) {
        val descriptor = PluginDescriptor(id, id, "1.0.0", "Plugin")
        runtime.register(
            PluginRuntimeController.Candidate(
                descriptor,
                id,
                create = {
                    object : CosmicPlugin {
                        override fun activate(context: PluginContext) = activate(context)
                        override fun deactivate() = deactivate()
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

    @Test(timeout = 10000)
    fun `async loads deduplicate successful activation and publish immutable transitions`() =
        runBlocking {
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val calls = AtomicInteger()
            register(activate = {
                calls.incrementAndGet()
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
            })
            val before = runtime.states.value
            try {
                val first = async(Dispatchers.Default) { runtime.loadAsync("root") }
                assertTrue(entered.await(5, TimeUnit.SECONDS))
                assertEquals(
                    PluginRuntimeTransition.ACTIVATING,
                    runtime.states.value.single().transition
                )
                assertNull(before.single().transition)
                val second = async(Dispatchers.Default) { runtime.loadAsync("root") }
                release.countDown()
                assertSame(
                    (first.await() as PluginLoadResult.Loaded).plugin,
                    (second.await() as PluginLoadResult.Loaded).plugin
                )
                assertEquals(1, calls.get())
                assertEquals(PluginState.ACTIVE, runtime.states.value.single().state)
                assertThrows(UnsupportedOperationException::class.java) {
                    (runtime.states.value as MutableList).clear()
                }
                Unit
            } finally {
                release.countDown(); runtime.unloadAsync("root")
            }
        }

    @Test(timeout = 10000)
    fun `async unload serializes behind activation and is idempotent`() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val stops = AtomicInteger()
        register(
            activate = { entered.countDown(); check(release.await(5, TimeUnit.SECONDS)) },
            deactivate = { stops.incrementAndGet() })
        try {
            val load = async(Dispatchers.Default) { runtime.loadAsync("root") }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val unload = async(Dispatchers.Default) { runtime.unloadAsync("root") }
            release.countDown()
            load.await(); unload.await()
            runtime.unloadAsync("root")
            assertEquals(1, stops.get())
            assertEquals(PluginState.DISABLED, runtime.states.value.single().state)
        } finally {
            release.countDown()
        }
    }

    @Test(timeout = 10000)
    fun `cancelled queued load never activates`() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val calls = AtomicInteger()
        register(
            "first",
            activate = { entered.countDown(); check(release.await(5, TimeUnit.SECONDS)) })
        register("second", activate = { calls.incrementAndGet() })
        try {
            val first = async(Dispatchers.Default) { runtime.loadAsync("first") }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val queued = async(start = CoroutineStart.UNDISPATCHED) { runtime.loadAsync("second") }
            queued.cancelAndJoin()
            release.countDown(); first.await()
            assertEquals(0, calls.get())
            assertEquals(
                PluginState.DISCOVERED,
                runtime.states.value.first { it.id == "second" }.state
            )
        } finally {
            release.countDown(); runtime.unloadAsync("first")
        }
    }

    @Test(timeout = 10000)
    fun `cancelling admitted load does not pretend to interrupt callback`() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        register(activate = { entered.countDown(); check(release.await(5, TimeUnit.SECONDS)) })
        try {
            val request = async(Dispatchers.Default) { runtime.loadAsync("root") }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            request.cancel()
            assertFalse(request.isCompleted)
            release.countDown(); request.join()
            assertEquals(PluginState.ACTIVE, runtime.states.value.single().state)
        } finally {
            release.countDown(); runtime.unloadAsync("root")
        }
    }

    @Test(timeout = 10000)
    fun `sync and async callback reentry fail before dispatch`() = runBlocking {
        register(activate = {
            val error = assertThrows(PluginLifecycleException::class.java) {
                runBlocking { runtime.unloadAsync("root") }
            }
            assertEquals(PluginFailureKind.REENTRANT, error.kind)
            assertThrows(PluginLifecycleException::class.java) { runtime.unload("root") }
        })
        assertTrue(runtime.loadAsync("root") is PluginLoadResult.Loaded)
        runtime.unloadAsync("root")
    }

    @Test(timeout = 10000)
    fun `async failed activation drains supervised jobs and tracked resources`() = runBlocking {
        val finalized = CompletableDeferred<Unit>()
        val disposed = AtomicInteger()
        lateinit var context: PluginContext
        lateinit var child: Job
        register(activate = {
            context = it
            it.extensions.register(point, "value", "root")
            it.registerDisposable { disposed.incrementAndGet() }
            child =
                it.services.require(PluginCoroutineScope.KEY).scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        awaitCancellation()
                    } finally {
                        withContext(NonCancellable) { delay(20); finalized.complete(Unit) }
                    }
                }
            error("activation failure")
        })
        assertTrue(runtime.loadAsync("root") is PluginLoadResult.Failed)
        assertTrue(child.isCompleted)
        assertTrue(finalized.isCompleted)
        assertEquals(1, disposed.get())
        assertTrue(registry.extensions(point).isEmpty())
        assertNull(context.services.get(PluginCoroutineScope.KEY))
        assertEquals(PluginFailureKind.ACTIVATION, runtime.states.value.single().failureKind)
        context.registerDisposable { disposed.incrementAndGet() }
        assertEquals(2, disposed.get())
    }

    @Test(timeout = 10000)
    fun `unload cancellation still drains jobs and finalizer late registration`() = runBlocking {
        val finalizing = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val disposed = AtomicInteger()
        lateinit var child: Job
        register(activate = { context ->
            child =
                context.services.require(PluginCoroutineScope.KEY).scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        awaitCancellation()
                    } finally {
                        withContext(NonCancellable) {
                            finalizing.complete(Unit); release.await()
                            context.registerDisposable { disposed.incrementAndGet() }
                        }
                    }
                }
        })
        runtime.loadAsync("root")
        val unload = async { runtime.unloadAsync("root") }
        finalizing.await()
        unload.cancel()
        assertFalse(unload.isCompleted)
        release.complete(Unit); unload.join()
        assertTrue(child.isCompleted)
        assertEquals(1, disposed.get())
        assertEquals(PluginState.DISABLED, runtime.states.value.single().state)
    }

    @Test(timeout = 10000)
    fun `context scope is local supervised and cannot restart after close`() = runBlocking {
        val host = DefaultServiceRegistry()
        val key = ServiceKey("host", String::class.java)
        host.register(key, "value")
        val descriptor = PluginDescriptor("root", "root", "1", "Plugin")
        val first = DefaultPluginContext(descriptor, registry, host, logger)
        val second = DefaultPluginContext(descriptor, registry, host, logger)
        val scope = first.services.require(PluginCoroutineScope.KEY).scope
        val sibling = scope.launch(start = CoroutineStart.UNDISPATCHED) { awaitCancellation() }
        scope.launch { error("child failure") }.join()
        assertTrue(sibling.isActive)
        assertEquals(1, warnings.get())
        assertNull(host.get(PluginCoroutineScope.KEY))
        assertNotSame(scope, second.services.require(PluginCoroutineScope.KEY).scope)
        assertEquals("value", first.services.get(key))
        first.disposeAll(); first.awaitCancellation()
        assertTrue(sibling.isCompleted)
        val restarted = scope.launch { fail("closed scope executed") }
        restarted.join(); assertTrue(restarted.isCancelled)
        assertTrue(second.services.require(PluginCoroutineScope.KEY).scope.isActive)
        second.disposeAll(); second.awaitCancellation()
    }

    @Test(timeout = 10000)
    fun `legacy callbacks and disposals retain caller thread without joining finalizers`() =
        runBlocking {
            val caller = Thread.currentThread()
            val release = CompletableDeferred<Unit>()
            lateinit var child: Job
            register(activate = { context ->
                assertSame(caller, Thread.currentThread())
                context.registerDisposable { assertSame(caller, Thread.currentThread()) }
                child =
                    context.services.require(PluginCoroutineScope.KEY).scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        try {
                            awaitCancellation()
                        } finally {
                            withContext(NonCancellable) { release.await() }
                        }
                    }
            }, deactivate = { assertSame(caller, Thread.currentThread()) })
            runtime.load("root"); runtime.unload("root")
            assertFalse(child.isCompleted)
            release.complete(Unit); child.join()
        }
}
