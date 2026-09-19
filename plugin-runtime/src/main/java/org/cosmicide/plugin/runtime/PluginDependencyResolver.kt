/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */
package org.cosmicide.plugin.runtime

import org.cosmicide.plugin.api.PluginDescriptor

/** Error types for plugin lifecycle operations. */
enum class PluginFailureKind { MISSING, DISABLED, VERSION, DUPLICATE, CYCLE, DEPENDENCY_FAILED, ACTIVATION, REENTRANT, REQUIRED_DEPENDENTS }

class PluginLifecycleException(
    val kind: PluginFailureKind,
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

/** One installed version per id; no downloads, range solving, or implementation-class sharing. */
internal object PluginDependencyResolver {
    fun plan(root: String, descriptors: List<PluginDescriptor>): List<PluginDescriptor> {
        val byId = descriptors.groupBy { it.id }
        val ordered = linkedMapOf<String, PluginDescriptor>()
        val visiting = linkedSetOf<String>()

        fun visit(id: String) {
            if (id in visiting) throw PluginLifecycleException(
                PluginFailureKind.CYCLE,
                "Required dependency cycle: ${(visiting + id).joinToString(" -> ")} -> $id"
            )
            if (id in ordered) return
            val matches = byId[id].orEmpty()
            if (matches.isEmpty()) throw PluginLifecycleException(
                PluginFailureKind.MISSING,
                "Missing dependency: $id"
            )
            if (matches.size != 1) throw PluginLifecycleException(
                PluginFailureKind.DUPLICATE,
                "Duplicate plugin id: $id"
            )
            val descriptor = matches.single()
            if (!descriptor.enabledByDefault) throw PluginLifecycleException(
                PluginFailureKind.DISABLED,
                "Disabled plugin: $id"
            )
            visiting += id
            try {
                // Required edges always win over optional edges, regardless of manifest order.
                descriptor.dependencies.sortedWith(
                    compareBy(
                        { it.optional },
                        { it.id },
                        { it.minVersion.orEmpty() })
                ).forEach { dependency ->
                    val before = ordered.keys.toSet()
                    try {
                        val provider = byId[dependency.id]?.singleOrNull()
                        val minimum = dependency.minVersion
                        if (minimum != null && provider != null &&
                            !SemVer.atLeast(provider.version, minimum)
                        ) {
                            throw PluginLifecycleException(
                                PluginFailureKind.VERSION,
                                "$id requires ${dependency.id} >= ${dependency.minVersion}; found ${provider.version} (strict SemVer required)"
                            )
                        }
                        visit(dependency.id)
                    } catch (failure: PluginLifecycleException) {
                        if (!dependency.optional) throw failure
                        // Drop an unusable optional subtree, including optional back-edges.
                        ordered.keys.retainAll(before)
                    }
                }
                ordered[id] = descriptor
            } finally {
                visiting -= id
            }
        }
        visit(root)
        return ordered.values.toList()
    }
}

/** Strict SemVer 2.0 precedence. Numeric identifiers are unbounded; build metadata is ignored. */
internal data class SemVer(val core: List<String>, val prerelease: List<String>) :
    Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        core.zip(other.core).forEach { (a, b) -> numeric(a, b).let { if (it != 0) return it } }
        if (prerelease.isEmpty() || other.prerelease.isEmpty()) {
            return when {
                prerelease == other.prerelease -> 0; prerelease.isEmpty() -> 1; else -> -1
            }
        }
        prerelease.zip(other.prerelease).forEach { (a, b) ->
            val an = a.all { it in '0'..'9' }
            val bn = b.all { it in '0'..'9' }
            val comparison = when {
                an && bn -> numeric(a, b); an -> -1; bn -> 1; else -> a.compareTo(b)
            }
            if (comparison != 0) return comparison
        }
        return prerelease.size.compareTo(other.prerelease.size)
    }

    companion object {
        private val syntax =
            Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?")

        private fun numeric(a: String, b: String) =
            a.length.compareTo(b.length).takeIf { it != 0 } ?: a.compareTo(b)

        fun parse(value: String): SemVer? {
            val match = syntax.matchEntire(value) ?: return null
            val pre = match.groupValues[4].takeIf { it.isNotEmpty() }?.split('.').orEmpty()
            if (pre.any { it.length > 1 && it[0] == '0' && it.all { c -> c in '0'..'9' } }) return null
            return SemVer(match.groupValues.subList(1, 4), pre)
        }

        fun atLeast(actual: String, minimum: String): Boolean {
            val a = parse(actual) ?: return false
            val b = parse(minimum) ?: return false
            return a >= b
        }
    }
}
