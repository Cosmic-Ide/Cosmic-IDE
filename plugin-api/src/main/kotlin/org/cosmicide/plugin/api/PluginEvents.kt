/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */
package org.cosmicide.plugin.api

import kotlinx.coroutines.flow.Flow

/**
 * Reactive, Kotlin Flow-based event bus for inter-plugin and IDE notifications.
 */
interface PluginEvents {
    fun <T : Any> emit(eventClass: Class<T>, event: T)
    fun <T : Any> observe(eventClass: Class<T>): Flow<T>

    companion object {
        @JvmField
        val KEY = ServiceKey("org.cosmicide.plugin.events", PluginEvents::class.java)
    }
}

inline fun <reified T : Any> PluginEvents.emit(event: T) = emit(T::class.java, event)
inline fun <reified T : Any> PluginEvents.observe(): Flow<T> = observe(T::class.java)
