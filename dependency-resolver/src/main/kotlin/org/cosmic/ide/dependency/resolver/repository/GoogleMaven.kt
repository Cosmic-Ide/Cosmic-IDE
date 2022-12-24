package org.cosmic.ide.dependency.resolver.repository

import org.cosmic.ide.dependency.resolver.api.Repository

class GoogleMaven : Repository {

    override fun getName(): String {
        return "Google Maven"
    }

    override fun getURL(): String {
        return "https://dl.google.com/android/maven2"
    }
}
