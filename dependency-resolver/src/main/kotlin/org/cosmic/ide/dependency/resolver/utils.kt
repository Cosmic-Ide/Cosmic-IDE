package org.cosmic.ide.dependency.resolver

import org.cosmic.ide.dependency.resolver.api.Repository
import org.cosmic.ide.dependency.resolver.repository.*

private val repositories by lazy {
    listOf(MavenCentral(), Jitpack(), GoogleMaven())
}

fun checkArtifact(groupId: String, artifactId: String, version: String): Repository? {
    for (repository in repositories) {
        if (repository.checkExists(groupId, artifactId, version)) {
            return repository
        }
    }
    return null
}
