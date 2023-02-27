package org.cosmic.ide.dependency.resolver.api

import java.net.HttpURLConnection
import java.net.URL

interface Repository {

    fun checkExists(groupId: String, artifactId: String, version: String): Boolean {
        val repository = getURL()
        val dependencyUrl = if (!version.isEmpty())

            "$repository/${groupId.replace(".", "/")}/$artifactId/$version/$artifactId-$version.jar"
            else
            "$repository/${groupId.replace(".", "/")}/$artifactId/maven-metadata.xml"
        val url = URL(dependencyUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        return connection.responseCode == 200
    }

    fun checkExists(artifact: Artifact): Boolean {
        val repository = getURL()
        val dependencyUrl =
            "$repository/${ artifact.groupId.replace(".", "/") }/${ artifact.artifactId }/${ artifact.version }/${ artifact.artifactId }-${ artifact.version }.jar"
        val url = URL(dependencyUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        return connection.responseCode == 200
    }

    fun getName(): String

    fun getURL(): String
}
