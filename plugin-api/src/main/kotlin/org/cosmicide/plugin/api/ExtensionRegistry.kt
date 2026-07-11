/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.api

import java.util.concurrent.CopyOnWriteArrayList

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

class DefaultExtensionRegistry : MutableExtensionRegistry {

    private val entries = CopyOnWriteArrayList<ExtensionRegistration<*>>()

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

        val registration = ExtensionRegistration(point, extension, ownerPluginId, priority)
        entries += registration
        return Disposable {
            entries.remove(registration)
        }
    }

    override fun unregisterOwner(ownerPluginId: String) {
        entries.removeIf { it.ownerPluginId == ownerPluginId }
    }

    override fun <T : Any> registrations(point: ExtensionPoint<T>): List<ExtensionRegistration<T>> {
        return entries
            .asSequence()
            .filter { it.point.id == point.id && point.type.isInstance(it.extension) }
            .sortedWith(
                compareByDescending<ExtensionRegistration<*>> { it.priority }
                    .thenBy { it.ownerPluginId }
            )
            .map {
                @Suppress("UNCHECKED_CAST")
                it as ExtensionRegistration<T>
            }
            .toList()
    }
}
