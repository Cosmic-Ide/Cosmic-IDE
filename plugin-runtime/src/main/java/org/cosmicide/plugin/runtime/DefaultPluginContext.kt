/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.runtime

import org.cosmicide.plugin.api.Disposable
import org.cosmicide.plugin.api.MutableExtensionRegistry
import org.cosmicide.plugin.api.PluginContext
import org.cosmicide.plugin.api.PluginDescriptor
import org.cosmicide.plugin.api.PluginLogger
import org.cosmicide.plugin.api.ServiceRegistry
import org.cosmicide.plugin.api.ExtensionPoint
import org.cosmicide.plugin.api.ExtensionRegistration
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.cosmicide.plugin.api.PluginCoroutineScope
import org.cosmicide.plugin.api.ServiceKey
import org.cosmicide.plugin.api.PluginIds
import org.cosmicide.plugin.api.PluginEvents
import org.cosmicide.plugin.api.PluginSettings
import org.cosmicide.plugin.api.PluginStorage
import org.cosmicide.plugins.AndroidPluginServices

class DefaultPluginContext(
    override val descriptor: PluginDescriptor,
    extensions: MutableExtensionRegistry,
    services: ServiceRegistry,
    override val logger: PluginLogger = AndroidPluginLogger(descriptor.id),
) : PluginContext {

    private val lock = Any()
    private val disposables = mutableListOf<Disposable>()
    private var closed = false
    private val lifecycleJob = SupervisorJob()
    private val coroutineService = object : PluginCoroutineScope {
        override val scope = CoroutineScope(
            lifecycleJob + Dispatchers.Default +
                    CoroutineExceptionHandler { _, error ->
                        logger.warn(
                            "Plugin coroutine failed",
                            error
                        )
                    })
    }

    private var lazyEvents: PluginEvents? = null
    private var lazySettings: PluginSettings? = null
    private var lazyStorage: PluginStorage? = null

    override val services: ServiceRegistry = object : ServiceRegistry {
        override fun <T : Any> get(key: ServiceKey<T>): T? = synchronized(lock) {
            if (closed) return null
            if (key == PluginCoroutineScope.KEY) {
                key.type.cast(coroutineService)
            } else if (key == PluginEvents.KEY) {
                val existing = services.get(PluginEvents.KEY)
                if (existing != null) return key.type.cast(existing)
                if (lazyEvents == null) lazyEvents = DefaultPluginEvents()
                key.type.cast(lazyEvents)
            } else if (key == PluginSettings.KEY) {
                val existing = services.get(PluginSettings.KEY)
                if (existing != null) return key.type.cast(existing)
                val context = services.get(AndroidPluginServices.APPLICATION_CONTEXT)
                if (context != null) {
                    if (lazySettings == null) lazySettings =
                        DefaultPluginSettings(context, descriptor.id)
                    key.type.cast(lazySettings)
                } else null
            } else if (key == PluginStorage.KEY) {
                val existing = services.get(PluginStorage.KEY)
                if (existing != null) return key.type.cast(existing)
                val context = services.get(AndroidPluginServices.APPLICATION_CONTEXT)
                if (context != null) {
                    if (lazyStorage == null) lazyStorage =
                        DefaultPluginStorage(context, descriptor.id)
                    key.type.cast(lazyStorage)
                } else null
            } else {
                services.get(key)
            }
        }
    }

    /** Called outside the transition gate by async operations, after cancellation and disposal. */
    internal suspend fun awaitCancellation() = lifecycleJob.join()

    private fun scopedOwner(ownerPluginId: String): String {
        require(ownerPluginId == PluginIds.CORE || ownerPluginId == descriptor.id) {
            "Plugin ${descriptor.id} cannot mutate owner $ownerPluginId"
        }
        return descriptor.id
    }

    override val extensions: MutableExtensionRegistry = object : MutableExtensionRegistry {
        override fun <T : Any> registrations(point: ExtensionPoint<T>): List<ExtensionRegistration<T>> =
            extensions.registrations(point)

        override fun <T : Any> register(
            point: ExtensionPoint<T>,
            extension: T,
            ownerPluginId: String,
            priority: Int
        ): Disposable =
            synchronized(lock) {
                check(!closed) { "Plugin context is closed: ${descriptor.id}" }
                registerDisposable(
                    extensions.register(
                        point,
                        extension,
                        scopedOwner(ownerPluginId),
                        priority
                    )
                )
            }

        override fun unregisterOwner(ownerPluginId: String) = synchronized(lock) {
            check(!closed) { "Plugin context is closed: ${descriptor.id}" }
            extensions.unregisterOwner(scopedOwner(ownerPluginId))
        }
    }

    override fun registerDisposable(disposable: Disposable): Disposable {
        val tracked = object : Disposable {
            private val disposed = AtomicBoolean(false)

            override fun dispose() {
                if (disposed.compareAndSet(false, true)) {
                    synchronized(lock) { disposables.remove(this) }
                    disposable.dispose()
                }
            }
        }
        val disposeNow = synchronized(lock) {
            if (closed) true else {
                disposables += tracked
                false
            }
        }
        if (disposeNow) disposeSafely(tracked)
        return tracked
    }

    fun disposeAll() {
        val pending = synchronized(lock) {
            if (closed) return
            closed = true
            disposables.reversed().also { disposables.clear() }
        }
        lifecycleJob.cancel()
        pending.forEach(::disposeSafely)
    }

    private fun disposeSafely(disposable: Disposable) {
        runCatching { disposable.dispose() }.onFailure {
            logger.warn("Plugin disposable failed", it)
        }
    }
}
