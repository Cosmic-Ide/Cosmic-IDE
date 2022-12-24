package org.cosmic.ide.dependency.resolver.repository

import org.cosmic.ide.dependency.resolver.api.Repository

class Jitpack : Repository {

    override fun getName(): String {
        return "Jitpack"
    }

    override fun getURL(): String {
        return "https://jitpack.io"
    }
}
