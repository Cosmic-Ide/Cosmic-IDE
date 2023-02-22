package org.cosmic.ide.dependency.resolver

import org.cosmic.ide.dependency.resolver.api.Artifact
import org.cosmic.ide.dependency.resolver.repository.*
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

private val repositories by lazy { listOf(MavenCentral(), Jitpack(), GoogleMaven()) }

fun getArtifact(groupId: String, artifactId: String, version: String): Artifact? {
    return initHost(Artifact(groupId, artifactId, version))
}
/*
 * Finds the host repository of the artifact and initialises it.
 * Returns null if no repository hosts this artifact
 */
fun initHost(artifact: Artifact): Artifact? {
    for (repository in repositories) {
        if (repository.checkExists(artifact.groupId, artifact.artifactId, artifact.version)) {
            artifact.repository = repository
            return artifact
        }
    }
    println("No repository contains ${ artifact.artifactId }.")
    return null
}

/*
 * Resolves a POM file from InputStream and returns the list of artifacts it depends on.
 */
fun InputStream.resolvePOM(): List<Artifact> {
    val artifacts = mutableListOf<Artifact>()
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val doc = builder.parse(this)

    val dependencyElements = doc.getElementsByTagName("dependency")
    for (i in 0 until dependencyElements.length) {
        val dependencyElement = dependencyElements.item(i) as Element
        val scopeItem = dependencyElement.getElementsByTagName("scope").item(0)
        if (scopeItem != null) {
            val scope = scopeItem.textContent
            if (scope.isNotEmpty() && (scope == "test" || scope == "provided")) {
                continue
            }
        }
        val groupId = dependencyElement.getElementsByTagName("groupId").item(0).textContent
        val artifactId = dependencyElement.getElementsByTagName("artifactId").item(0).textContent
        if (artifactId.endsWith("bom")) {
            continue
        }
        val artifact = Artifact(groupId, artifactId)

        val item = dependencyElement.getElementsByTagName("version").item(0)
        if (item != null) {
            artifact.version = item.textContent
        }
        initHost(artifact)
        artifacts.add(artifact)
    }
    return artifacts
}
