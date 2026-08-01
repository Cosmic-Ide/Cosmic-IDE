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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class DefaultPluginContext(
    override val descriptor: PluginDescriptor,
    override val extensions: MutableExtensionRegistry,
    override val services: ServiceRegistry,
    override val logger: PluginLogger = AndroidPluginLogger(descriptor.id),
) : PluginContext {

    private val disposables = CopyOnWriteArrayList<Disposable>()

    override fun registerDisposable(disposable: Disposable): Disposable {
        val tracked = object : Disposable {
            private val disposed = AtomicBoolean(false)

            override fun dispose() {
                if (disposed.compareAndSet(false, true)) {
                    disposables.remove(this)
                    disposable.dispose()
                }
            }
        }
        disposables += tracked
        return tracked
    }

    fun disposeAll() {
        disposables.reversed().forEach { disposable ->
            runCatching {
                disposable.dispose()
            }.onFailure {
                logger.warn("Plugin disposable failed", it)
            }
        }
        disposables.clear()
    }
}
