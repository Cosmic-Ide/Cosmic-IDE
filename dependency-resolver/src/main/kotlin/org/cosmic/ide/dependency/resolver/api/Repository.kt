package org.cosmic.ide.dependency.resolver.api

import java.net.HttpURLConnection
import java.net.URL
import java.io.File

interface Repository {

    fun checkExists(groupId: String, artifactId: String, version: String): Boolean {
        val repository = getURL()
        val dependencyUrl = "$repository/${groupId.replace(".", "/")}/$artifactId/$version/$artifactId-$version.jar"
        val url = URL(dependencyUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        if (connection.responseCode == 200) {
            return true
        } else {
            return false
        }
    }
    
    fun downloadArtifact(groupId: String, artifactId: String, version: String, output: File) {
        val repository = getURL()
        val dependencyUrl = "$repository/${groupId.replace(".", "/")}/$artifactId/$version/$artifactId-$version.jar"
        val stream = URL(dependencyUrl).openConnection().inputStream
        output.outputStream().use { stream.copyTo(it) }
    }
    fun getName(): String

    fun getURL(): String
}