package org.cosmic.ide.dependency.resolver.api

import org.cosmic.ide.dependency.resolver.resolvePOM
import java.io.File
import java.io.InputStream
import java.net.URL

data class Artifact(
    val groupId: String,
    val artifactId: String,
    var version: String = "",
    var repository: Repository? = null
) {
    private fun downloadTo(output: File) {
        if (repository == null) {
            throw IllegalStateException("Repository is not declared.")
        }
        output.createNewFile()
        val dependencyUrl =
            "${ repository!!.getURL() }/${groupId.replace(".", "/")}/$artifactId/$version/$artifactId-$version.jar"
        val stream = URL(dependencyUrl).openConnection().inputStream
        output.outputStream().use { stream.copyTo(it) }
    }

    fun downloadArtifact(output: File) {
        val stream = getPOM()
        val artifacts = mutableListOf<Artifact>()
        for (dependency in stream.resolvePOM()) {
            artifacts.add(dependency)
            artifacts.addAll(dependency.getPOM().resolvePOM())
        }

        val latestDeps =
            artifacts.groupBy { it.groupId to it.artifactId }.values.map { it.maxBy { it.version } }

        for (art in latestDeps) {
            println("Downloading ${ art.artifactId }")
            art.downloadTo(File(output, "${ art.artifactId }-${ art.version }.jar"))
        }
    }

    fun getPOM(): InputStream {
        val pomUrl =
            "${ repository!!.getURL() }/${groupId.replace(".", "/")}/$artifactId/$version/$artifactId-$version.pom"
        return URL(pomUrl).openConnection().inputStream
    }
}
