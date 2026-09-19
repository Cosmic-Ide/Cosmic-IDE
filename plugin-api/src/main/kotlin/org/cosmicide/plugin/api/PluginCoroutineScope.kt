/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */
package org.cosmicide.plugin.api

import kotlinx.coroutines.CoroutineScope

/** Optional context-local service for cooperative plugin work.
 *
 * Children are supervised: one failed child does not cancel its siblings. The host cancels this
 * scope before disposing registered resources. Use cancellable APIs and finally blocks; this is
 * not forced termination of blocking code. Do not replace the scope's Job or launch detached work
 * if host-owned cancellation is required. Older hosts may not provide this service.
 */
interface PluginCoroutineScope {
    val scope: CoroutineScope

    companion object {
        @JvmField
        val KEY =
            ServiceKey("org.cosmicide.plugin.coroutineScope", PluginCoroutineScope::class.java)
    }
}
