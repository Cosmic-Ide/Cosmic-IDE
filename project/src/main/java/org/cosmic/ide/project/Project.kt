package org.cosmic.ide.project

import org.cosmic.ide.common.Indexer;

import java.io.File

interface Project {
    fun delete()

    fun getRootFile(): File

    fun getProjectName(): String

    fun getIndexer(): Indexer

    fun getProjectDirPath(): String

    fun getSrcDirPath(): String

    fun getBinDirPath(): String

    fun getLibDirPath(): String

    fun getBuildDirPath(): String

    fun getCacheDirPath(): String
}