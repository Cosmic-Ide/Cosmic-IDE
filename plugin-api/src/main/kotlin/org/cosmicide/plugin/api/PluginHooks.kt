/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */
package org.cosmicide.plugin.api

import org.cosmicide.plugin.api.hook.Hook
import org.cosmicide.plugin.api.hook.HookManager

/**
 * Disposable-aware hook helper for plugin authors.
 * Registers a bytecode hook using [HookManager] and automatically registers its disposable
 * on the [PluginContext] for clean unhooking on plugin unload.
 */
fun PluginContext.registerHook(hook: Hook): Disposable {
    val disposable = HookManager.registerHook(hook)
    return registerDisposable(disposable)
}
