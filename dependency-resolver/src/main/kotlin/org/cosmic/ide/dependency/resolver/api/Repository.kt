package org.cosmic.ide.dependency.resolver.api

import java.net.HttpURLConnection
import java.net.URL

interface Repository {

    fun checkExists(groupId: String, artifactId: String, version: String): Boolean {
        val repository = getURL()
        val dependencyUrl =
            "$repository/${groupId.replace(".", "/")}/$artifactId/$version/$artifactId-$version.jar"
        val url = URL(dependencyUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        if (connection.responseCode == 200) {
            return true
        } else {
            return false
        }
    }

    fun checkExists(artifact: Artifact): Boolean {
        val repository = getURL()
        val dependencyUrl =
            "$repository/${ artifact.groupId.replace(".", "/") }/${ artifact.artifactId }/${ artifact.version }/${ artifact.artifactId }-${ artifact.version }.jar"
        val url = URL(dependencyUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        if (connection.responseCode == 200) {
            return true
        } else {
            return false
        }
    }

    fun getName(): String

    fun getURL(): String
}
