/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 */
package org.cosmicide.plugin.api

/** Initial host contract baselines, independent of app and Maven artifact release versions. */
object PluginCompatibility {
    const val PLUGIN_API_VERSION = "1.0.0"
    const val IDE_API_VERSION = "1.0.0"

    /** Strict numeric major.minor.patch; no prerelease, build suffix, operators or wildcards. */
    fun parseApiVersion(value: String): ApiVersion {
        require(value.length <= 32 && value.matches(Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"))) {
            "Invalid API version '$value': expected major.minor.patch"
        }
        val parts = value.split('.').map {
            requireNotNull(it.toIntOrNull()) { "API version component is too large: '$value'" }
        }
        return ApiVersion(parts[0], parts[1], parts[2])
    }

    /** Checks the actual host version, not an assumed window of future host APIs. */
    fun requireCompatible(requirements: PluginApiRequirements) {
        requirements.pluginApi?.requireContains("plugin", PLUGIN_API_VERSION)
        requirements.ideApi?.requireContains("IDE", IDE_API_VERSION)
    }
}

data class ApiVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<ApiVersion> {
    init { require(major >= 0 && minor >= 0 && patch >= 0) }
    override fun compareTo(other: ApiVersion): Int =
        compareValuesBy(this, other, ApiVersion::major, ApiVersion::minor, ApiVersion::patch)
    override fun toString(): String = "$major.$minor.$patch"
}

/** Explicit half-open range [minInclusive, maxExclusive). Both bounds are required. */
data class HostApiRange(val minInclusive: String, val maxExclusive: String) {
    private val minimum = PluginCompatibility.parseApiVersion(minInclusive)
    private val maximum = PluginCompatibility.parseApiVersion(maxExclusive)
    init { require(minimum < maximum) { "API range must have minInclusive < maxExclusive" } }

    fun contains(version: String): Boolean = PluginCompatibility.parseApiVersion(version).let {
        it >= minimum && it < maximum
    }

    internal fun requireContains(label: String, hostVersion: String) {
        if (!contains(hostVersion)) throw PluginCompatibilityException(
            "Incompatible $label API: host $hostVersion is outside [$minInclusive, $maxExclusive)"
        )
    }
}

/** Omitted APIs are unchecked, including when only the other API has a declared range. */
data class PluginApiRequirements(
    val pluginApi: HostApiRange? = null,
    val ideApi: HostApiRange? = null
) {
    val hasUncheckedApi: Boolean get() = pluginApi == null || ideApi == null
}

/** Specific pre-load failure for a well-formed but incompatible API declaration. */
class PluginCompatibilityException(message: String) : IllegalArgumentException(message)
