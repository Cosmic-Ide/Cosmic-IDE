/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

data class ExtensionPoint<T : Any>(
    val id: String,
    val type: Class<T>
) {
    init {
        require(id.isNotBlank()) { "Extension point id must not be blank" }
    }
}

data class ExtensionRegistration<T : Any>(
    val point: ExtensionPoint<T>,
    val extension: T,
    val ownerPluginId: String,
    val priority: Int
)

interface ExtensionRegistry {
    fun <T : Any> registrations(point: ExtensionPoint<T>): List<ExtensionRegistration<T>>

    fun <T : Any> extensions(point: ExtensionPoint<T>): List<T> {
        return registrations(point).map { it.extension }
    }

    fun <T : Any> observeRegistrations(point: ExtensionPoint<T>): Flow<List<ExtensionRegistration<T>>> {
        return kotlinx.coroutines.flow.flow { emit(registrations(point)) }
    }

    fun <T : Any> observe(point: ExtensionPoint<T>): Flow<List<T>> {
        return observeRegistrations(point).map { list ->
            @Suppress("UNCHECKED_CAST")
            list.filter { point.type.isInstance(it.extension) }.map { it.extension } as List<T>
        }
    }
}

interface MutableExtensionRegistry : ExtensionRegistry {
    fun <T : Any> register(
        point: ExtensionPoint<T>,
        extension: T,
        ownerPluginId: String = PluginIds.CORE,
        priority: Int = 0
    ): Disposable

    fun unregisterOwner(ownerPluginId: String)
}

/** Optional mutation stamp; existing registry implementations need not implement it. */
interface ExtensionRegistryRevision {
    val revision: Long
}

class DefaultExtensionRegistry : MutableExtensionRegistry, ExtensionRegistryRevision {
    // Token equality is identity, never the contribution's user-defined equals implementation.
    private class Token(val registration: ExtensionRegistration<*>)
    private class Bucket {
        val entries = mutableListOf<Token>()
        var ordered: List<ExtensionRegistration<*>>? = null
        val snapshots = mutableMapOf<Class<*>, List<ExtensionRegistration<*>>>()
        val flow = MutableStateFlow<List<ExtensionRegistration<*>>>(emptyList())

        fun invalidate() {
            ordered = null
            snapshots.clear()
        }

        fun updateFlow() {
            val registrations = entries.map { it.registration }.sortedWith(
                compareByDescending<ExtensionRegistration<*>> { it.priority }.thenBy { it.ownerPluginId }
            )
            flow.value = java.util.Collections.unmodifiableList(registrations)
        }
    }

    private val lock = Any()
    private val points = mutableMapOf<String, Bucket>()
    @Volatile
    override var revision: Long = 0
        private set

    override fun <T : Any> register(
        point: ExtensionPoint<T>,
        extension: T,
        ownerPluginId: String,
        priority: Int
    ): Disposable {
        require(point.type.isInstance(extension)) {
            "Extension ${extension::class.java.name} does not implement ${point.type.name}"
        }
        require(ownerPluginId.isNotBlank()) { "Owner plugin id must not be blank" }
        val token = Token(ExtensionRegistration(point, extension, ownerPluginId, priority))
        synchronized(lock) {
            val bucket = points.getOrPut(point.id, ::Bucket)
            bucket.entries += token
            bucket.invalidate()
            bucket.updateFlow()
            revision++
        }
        return Disposable {
            synchronized(lock) {
                points[point.id]?.let { bucket ->
                    if (bucket.entries.remove(token)) {
                        bucket.invalidate()
                        bucket.updateFlow()
                        if (bucket.entries.isEmpty()) points.remove(point.id)
                        revision++
                    }
                }
            }
        }
    }

    override fun unregisterOwner(ownerPluginId: String) = synchronized(lock) {
        var changed = false
        val iterator = points.values.iterator()
        while (iterator.hasNext()) {
            val bucket = iterator.next()
            if (bucket.entries.removeAll { it.registration.ownerPluginId == ownerPluginId }) {
                changed = true
                bucket.invalidate()
                bucket.updateFlow()
                if (bucket.entries.isEmpty()) iterator.remove()
            }
        }
        if (changed) revision++
    }

    override fun <T : Any> registrations(point: ExtensionPoint<T>): List<ExtensionRegistration<T>> =
        synchronized(lock) {
            val bucket = points[point.id] ?: return@synchronized emptyList()
            // Index by id, filtering for the requested type.
            val snapshot = bucket.snapshots.getOrPut(point.type) {
                val ordered = bucket.ordered ?: bucket.entries.map { it.registration }.sortedWith(
                    compareByDescending<ExtensionRegistration<*>> { it.priority }.thenBy { it.ownerPluginId }
                ).also { bucket.ordered = it }
                java.util.Collections.unmodifiableList(ordered.filter { point.type.isInstance(it.extension) })
            }
            @Suppress("UNCHECKED_CAST")
            snapshot as List<ExtensionRegistration<T>>
        }

    override fun <T : Any> observeRegistrations(point: ExtensionPoint<T>): Flow<List<ExtensionRegistration<T>>> {
        val flow = synchronized(lock) {
            points.getOrPut(point.id, ::Bucket).flow
        }
        @Suppress("UNCHECKED_CAST")
        return flow.asStateFlow() as Flow<List<ExtensionRegistration<T>>>
    }

    override fun <T : Any> observe(point: ExtensionPoint<T>): Flow<List<T>> {
        return observeRegistrations(point).map { list ->
            @Suppress("UNCHECKED_CAST")
            list.filter { point.type.isInstance(it.extension) }.map { it.extension } as List<T>
        }
    }
}
