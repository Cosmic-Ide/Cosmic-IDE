package org.cosmic.ide.dependency.resolver.api

import org.cosmic.ide.dependency.resolver.resolvePOM
import java.io.File
import java.io.InputStream
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

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
            "${ repository!!.getURL() }/${ groupId.replace(".", "/") }/$artifactId/$version/$artifactId-$version.jar"
        val stream = URL(dependencyUrl).openConnection().inputStream
        output.outputStream().use { stream.copyTo(it) }
    }

    fun downloadArtifact(output: File) {
        val artifacts = resolve()

        val latestDeps =
            artifacts.groupBy { it.groupId to it.artifactId }.values.map { it.maxBy { it.version } }

        latestDeps.forEach { art ->
            if (art.version.isNotEmpty() && art.repository != null) {
                art.downloadTo(File(output, "${ art.artifactId }-${ art.version }.jar"))
            }
        }
    }

    fun resolve(): MutableList<Artifact> {
        val pom = getPOM()
        if (pom == null) return mutableListOf()
        val deps = pom.resolvePOM()
        val artifacts = mutableListOf()
        deps.forEach { dep ->
            if (dep.version.isEmpty()) {
                val meta = URL("${ dep.repository!!.getURL() }/${ dep.groupId.replace(".", "/") }/${ dep.artifactId }/maven-metadata.xml").openConnection().inputStream
                val factory = DocumentBuilderFactory.newInstance()
                val builder = factory.newDocumentBuilder()
                val doc = builder.parse(meta)
                val v = doc.getElementsByTagName("release").item(0)
                if (v != null) {
                    dep.version = v.textContent
                }
            }
            artifacts.add(dep)
            artifacts.addAll(dep.resolve())
        }
        return artifacts
    }

    fun getPOM(): InputStream? {
        val pomUrl =
            "${ repository!!.getURL() }/${ groupId.replace(".", "/") }/$artifactId/$version/$artifactId-$version.pom"
        return URL(pomUrl).openConnection().inputStream
    }
}
