package org.cosmicide.rewrite.plugin.api

interface Plugin {
    fun getName(): String

    fun getVersion(): String

    fun getAuthor(): String
    fun getDescription(): String

    fun getSource(): String
}
