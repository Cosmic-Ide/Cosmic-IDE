package org.cosmic.ide.project

import org.cosmic.ide.common.Indexer

import java.io.File

interface Project {

    fun delete()

    val rootFile: File

    val projectName: String

    val projectDirPath: String

    val srcDirPath: String

    val binDirPath: String

    val libDirPath: String

    val buildDirPath: String
}