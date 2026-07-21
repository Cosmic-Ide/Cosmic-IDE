/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.api

import java.util.concurrent.ConcurrentHashMap

data class ServiceKey<T : Any>(
    val name: String,
    val type: Class<T>
) {
    init {
        require(name.isNotBlank()) { "Service key name must not be blank" }
    }
}

interface ServiceRegistry {
    fun <T : Any> get(key: ServiceKey<T>): T?

    fun <T : Any> require(key: ServiceKey<T>): T {
        return get(key)
            ?: throw IllegalStateException("Required service '${key.name}' is not registered")
    }
}

interface MutableServiceRegistry : ServiceRegistry {
    fun <T : Any> register(key: ServiceKey<T>, instance: T): Disposable

    fun unregister(key: ServiceKey<*>)
}

class DefaultServiceRegistry : MutableServiceRegistry {

    private val services = ConcurrentHashMap<ServiceKey<*>, Any>()

    override fun <T : Any> register(key: ServiceKey<T>, instance: T): Disposable {
        require(key.type.isInstance(instance)) {
            "Service ${instance::class.java.name} does not implement ${key.type.name}"
        }
        services[key] = instance
        return Disposable {
            services.remove(key, instance)
        }
    }

    override fun unregister(key: ServiceKey<*>) {
        services.remove(key)
    }

    override fun <T : Any> get(key: ServiceKey<T>): T? {
        val service = services[key] ?: return null
        return key.type.cast(service)
    }
}
