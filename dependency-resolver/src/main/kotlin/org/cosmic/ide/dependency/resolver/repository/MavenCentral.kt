package org.cosmic.ide.dependency.resolver.repository

import org.cosmic.ide.dependency.resolver.api.Repository

class MavenCentral : Repository {

    override fun getName(): String {
        return "Maven Central"
    }

    override fun getURL(): String {
        return "https://repo1.maven.org/maven2"
    }
}
