package org.cosmic.ide.project

import java.io.File

interface Project {
    fun delete()

    fun getRootFile(): File

    fun getProjectName(): String

    fun getProjectDirPath(): String

    fun getSrcDirPath(): String

    fun getBinDirPath(): String

    fun getLibDirPath(): String

    fun getBuildDirPath(): String

    fun getCacheDirPath(): String
}