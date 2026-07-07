package org.cosmicide.sdk.manager

import org.cosmicide.sdk.manager.jdk.FoojayClient


suspend fun main() {
    val targetOs = "linux"
    val targetArch = "aarch64"
    val targetLibC = "glibc"

    FoojayClient().apply {
        fetchMaintainedDistributions().onSuccess { distros ->
            println("Maintained distributions:")
            distros.forEach { println("- ${it.name} (versions: ${it.versions.joinToString(", ")})") }
        }.onFailure { println("Failed to fetch distributions: ${it.message}") }

        resolveLatestArtifact(
            "temurin",
            "27-ea+29",
            FoojayClient.OS.resolve(targetOs),
            FoojayClient.Arch.resolve(targetArch),
            FoojayClient.LibCType.resolve(targetLibC)
        ).onSuccess { artifact ->
            println("Resolved artifact: ${artifact.vendor} ${artifact.exactVersion} (${artifact.filename})")
            println("Download URL: ${artifact.binaryUrl}")
            println("Checksum: ${artifact.checksum}")
        }.onFailure { println("Failed to resolve artifact: ${it.message}") }

        fetchLatestVersions("semeru", true).onSuccess { latest ->
            println("Latest versions for ${latest.vendor}: GA=${latest.latestGa}, EA=${latest.latestEa}")
        }.onFailure { println("Failed to fetch latest versions: ${it.message}") }
    }
}