/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */
package org.cosmicide.plugin.runtime

import org.cosmicide.plugin.api.CosmicPlugin
import org.cosmicide.plugin.api.MutableExtensionRegistry
import org.cosmicide.plugin.api.PluginDescriptor
import org.cosmicide.plugin.api.PluginHandle
import org.cosmicide.plugin.api.PluginLoadResult
import org.cosmicide.plugin.api.PluginState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections

/** Android-free lifecycle core. Factories are invoked only after dependency planning succeeds.
 *
 * Transitions and package transactions share one gate, NOT a registry lock. Callbacks execute on
 * the requesting thread. Same-thread lifecycle reentry is rejected; reads use a published snapshot.
 * Callbacks must not wait for another thread to perform a lifecycle transition (it waits for them).
 */
internal class PluginRuntimeController(private val extensions: MutableExtensionRegistry) {
    internal data class Candidate(
        val descriptor: PluginDescriptor,
        val origin: String,
        val create: () -> CosmicPlugin,
        val context: () -> DefaultPluginContext,
        val failure: Exception? = null
    )

    private data class Active(
        val candidate: Candidate,
        val plugin: CosmicPlugin,
        val context: DefaultPluginContext
    )

    private val gate = Any()
    private var inCallback = false
    @Volatile
    private var callbackThread: Thread? = null
    private val asyncCleanup = ThreadLocal<MutableList<DefaultPluginContext>>()
    private val asyncGate = Mutex()
    private val diagnostics = linkedMapOf<String, PluginRuntimeSnapshot>()
    private val observable = MutableStateFlow<List<PluginRuntimeSnapshot>>(emptyList())
    val states = observable.asStateFlow()
    private val candidates = linkedMapOf<String, Candidate>()
    private val duplicates = mutableSetOf<String>()
    private val active = linkedMapOf<String, Active>()
    private val handles = linkedMapOf<String, PluginHandle>()
    @Volatile
    private var snapshot: List<PluginHandle> = emptyList()
    val plugins: List<PluginHandle> get() = snapshot.toList()

    /** Queued cancellation skips mutation. Once admitted, a transaction completes non-cancellably.
     * Callback threading for this opt-in API is Dispatchers.IO, never implicitly the UI thread.
     * Cleanup joins outside the gate so cooperative finalizers can finish without a gate deadlock.
     */
    suspend fun <T> executeAsync(operation: () -> T): T {
        if (callbackThread === Thread.currentThread()) throw PluginLifecycleException(
            PluginFailureKind.REENTRANT,
            "Async lifecycle mutation from a plugin callback is not supported"
        )
        val requester = currentCoroutineContext()
        requester.ensureActive()
        return asyncGate.withLock {
            withContext(Dispatchers.IO + NonCancellable) {
                val closedContexts = mutableListOf<DefaultPluginContext>()
                try {
                    exclusive {
                        requester.ensureActive()
                        asyncCleanup.set(closedContexts)
                        try {
                            operation()
                        } finally {
                            asyncCleanup.remove()
                        }
                    }
                } finally {
                    closedContexts.forEach { it.awaitCancellation() }
                }
            }
        }
    }

    suspend fun loadAsync(id: String): PluginLoadResult = executeAsync { load(id) }
    suspend fun unloadAsync(id: String) = executeAsync { unload(id) }

    fun <T> exclusive(block: () -> T): T = synchronized(gate) {
        checkNotReentrant()
        block()
    }

    private fun checkNotReentrant() {
        if (inCallback) throw PluginLifecycleException(
            PluginFailureKind.REENTRANT,
            "Lifecycle mutation from a plugin callback is not supported"
        )
    }

    private fun <T> callback(block: () -> T): T {
        check(!inCallback)
        inCallback = true
        callbackThread = Thread.currentThread()
        return try {
            block()
        } finally {
            callbackThread = null; inCallback = false
        }
    }

    fun register(candidate: Candidate) = exclusive {
        val id = candidate.descriptor.id
        val old = candidates[id]
        if (old != null && old.origin != candidate.origin) {
            duplicates += id
        } else if (id !in active) {
            candidates[id] = candidate
            publish(PluginHandle(candidate.descriptor, PluginState.DISCOVERED))
        }
    }

    fun recordFailure(descriptor: PluginDescriptor, error: Exception): PluginLoadResult.Failed =
        exclusive {
            if (descriptor.id !in active) publish(
                PluginHandle(
                    descriptor,
                    PluginState.FAILED,
                    error.message
                )
            )
            PluginLoadResult.Failed(descriptor, error.message ?: "Invalid plugin", error)
        }

    fun load(id: String): PluginLoadResult = exclusive {
        val candidate = checkNotNull(candidates[id]) { "Plugin not discovered: $id" }
        val descriptor = candidate.descriptor
        if (id in duplicates) return@exclusive fail(
            descriptor,
            PluginLifecycleException(PluginFailureKind.DUPLICATE, "Duplicate plugin id: $id")
        )
        active[id]?.let {
            return@exclusive PluginLoadResult.Loaded(
                it.candidate.descriptor,
                it.plugin
            )
        }
        val plan = try {
            PluginDependencyResolver.plan(id, candidates.values.map { it.descriptor } +
                    duplicates.mapNotNull { candidates[it]?.descriptor })
        } catch (error: PluginLifecycleException) {
            return@exclusive fail(descriptor, error)
        }
        val started = mutableListOf<String>()
        val failures = mutableMapOf<String, Throwable>()
        for (item in plan) {
            if (item.id in active) continue
            val requiredFailure = item.dependencies.firstOrNull { !it.optional && it.id !in active }
            if (requiredFailure != null) {
                val error = PluginLifecycleException(
                    PluginFailureKind.DEPENDENCY_FAILED,
                    "${item.id} requires failed dependency ${requiredFailure.id}",
                    failures[requiredFailure.id]
                )
                failures[item.id] = error
                fail(item, error)
                continue
            }
            val source = candidates.getValue(item.id)
            var context: DefaultPluginContext? = null
            transition(item.id, PluginRuntimeTransition.ACTIVATING)
            try {
                source.failure?.let { throw it }
                val plugin = callback(source.create)
                context = callback(source.context)
                callback { plugin.activate(context) }
                val actions = callback { plugin.setupActions.toList() }
                active[item.id] = Active(source, plugin, context)
                started += item.id
                publish(PluginHandle(item, PluginState.ACTIVE, setupActions = actions))
            } catch (error: Throwable) {
                callback { context?.let(::closeContext) }
                extensions.unregisterOwner(item.id)
                // Fatal VM failures cannot safely be represented as a recoverable plugin failure.
                if (error is VirtualMachineError || error is ThreadDeath) throw error
                val failure = PluginLifecycleException(
                    PluginFailureKind.ACTIVATION,
                    "${item.id} activation failed: ${error.message}", error
                )
                failures[item.id] = failure
                fail(item, failure)
            }
        }
        // Protect every previously active plugin and the successful root. Only this transaction's
        // newly started, unreachable dependencies may be unwound (including failed optional trees).
        val needed = mutableSetOf<String>()
        fun retain(pluginId: String) {
            if (!needed.add(pluginId)) return
            active[pluginId]?.candidate?.descriptor?.dependencies?.forEach { dependency ->
                if (dependency.id in active) retain(dependency.id)
            }
        }
        active.keys.filter { it !in started || it == id }.forEach(::retain)
        started.asReversed().filter { it !in needed }.forEach(::stop)
        active[id]?.let { PluginLoadResult.Loaded(it.candidate.descriptor, it.plugin) }
            ?: PluginLoadResult.Failed(
                descriptor,
                failures[id]?.message ?: "Activation failed",
                failures[id]
            )
    }

    fun unload(id: String) = exclusive {
        val dependents = active.values.filter { entry ->
            entry.candidate.descriptor.dependencies.any { !it.optional && it.id == id }
        }.map { it.candidate.descriptor.id }.sorted()
        if (dependents.isNotEmpty()) throw PluginLifecycleException(
            PluginFailureKind.REQUIRED_DEPENDENTS,
            "Cannot unload $id; active required dependents: ${dependents.joinToString()}"
        )
        stop(id)
    }

    private fun stop(id: String) {
        val entry = active.remove(id) ?: return
        transition(id, PluginRuntimeTransition.DEACTIVATING)
        callback {
            try {
                entry.plugin.deactivate()
            } catch (error: Throwable) {
                entry.context.logger.warn("Plugin deactivation failed", error)
            } finally {
                closeContext(entry.context)
                extensions.unregisterOwner(id)
            }
        }
        publish(
            PluginHandle(
                entry.candidate.descriptor, PluginState.DISABLED,
                setupActions = handles[id]?.setupActions.orEmpty()
            )
        )
    }

    private fun closeContext(context: DefaultPluginContext) {
        asyncCleanup.get()?.add(context)
        context.disposeAll()
    }

    fun forget(id: String) = exclusive {
        check(id !in active) { "Plugin $id must be unloaded before removal" }
        candidates.remove(id)
        duplicates.remove(id)
        handles.remove(id)
        diagnostics.remove(id)
        snapshot = handles.values.toList()
        publishDiagnostics()
    }

    private fun fail(descriptor: PluginDescriptor, error: Throwable): PluginLoadResult.Failed {
        if (descriptor.id !in active) publish(
            PluginHandle(
                descriptor,
                if (!descriptor.enabledByDefault) PluginState.DISABLED else PluginState.FAILED,
                error.message
            ),
            (error as? PluginLifecycleException)?.kind
        )
        return PluginLoadResult.Failed(descriptor, error.message ?: "Plugin failed", error)
    }

    private fun publish(handle: PluginHandle, failureKind: PluginFailureKind? = null) {
        handles[handle.descriptor.id] = handle
        snapshot = handles.values.toList()
        diagnostics[handle.descriptor.id] = PluginRuntimeSnapshot(
            handle.descriptor.id,
            handle.descriptor.version, handle.state, handle.errorMessage, failureKind
        )
        publishDiagnostics()
    }

    private fun transition(id: String, transition: PluginRuntimeTransition) {
        diagnostics[id]?.let { diagnostics[id] = it.copy(transition = transition) }
        publishDiagnostics()
    }

    private fun publishDiagnostics() {
        observable.value = Collections.unmodifiableList(diagnostics.values.toList())
    }
}
