/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Registers a contribution under this plugin's id and tracks its disposable for cleanup. */
fun <T : Any> PluginContext.register(
    point: ExtensionPoint<T>,
    extension: T,
    priority: Int = 0
): Disposable = registerDisposable(extensions.register(point, extension, descriptor.id, priority))

/** Tracks synchronous cleanup using the context's disposal policy. Returns a handle for early cleanup. */
fun PluginContext.onDispose(cleanup: () -> Unit): Disposable =
    registerDisposable(Disposable(cleanup))

/**
 * Launches cooperative work in the host-provided lifecycle scope, inheriting its dispatcher and Job.
 * No detached fallback is created: throws [IllegalStateException] if the service is unavailable
 * (for example, on an older host or after context close). Cancellation does not interrupt blocking
 * code. Coroutine finalizers must tolerate registered resources already being disposed.
 */
fun PluginContext.launch(block: suspend CoroutineScope.() -> Unit): Job =
    services.require(PluginCoroutineScope.KEY).scope.launch(block = block)

/** Event bus for this plugin context. */
val PluginContext.events: PluginEvents
    get() = services.require(PluginEvents.KEY)

/** Namespaced key-value settings for this plugin context. */
val PluginContext.settings: PluginSettings
    get() = services.require(PluginSettings.KEY)

/** Isolated data and cache directories for this plugin context. */
val PluginContext.storage: PluginStorage
    get() = services.require(PluginStorage.KEY)
