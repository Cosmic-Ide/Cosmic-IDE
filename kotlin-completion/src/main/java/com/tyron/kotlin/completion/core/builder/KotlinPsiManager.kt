package com.tyron.kotlin.completion.core.builder

import org.cosmic.ide.project.KotlinProject
import com.tyron.kotlin.completion.core.model.KotlinLightVirtualFile
import com.tyron.kotlin.completion.core.model.getEnvironment
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.HashSet

interface PsiFilesStorage {
    fun getPsiFile(module: KotlinProject, eclipseFile: File): KtFile

    fun getPsiFile(file: File, expectedSourceCode: String): KtFile

    fun isApplicable(file: File): Boolean

    fun removeFile(file: File)
}

private class ProjectSourceFiles : PsiFilesStorage {
    companion object {
        @JvmStatic
        fun isKotlinFile(file: File): Boolean = KotlinFileType.EXTENSION == file.extension
    }

    private val projectFiles = hashMapOf<KotlinProject, HashSet<File>>()
    private val cachedKtFiles = hashMapOf<File, KtFile>()
    private val mapOperationLock = Any()

    override fun getPsiFile(module: KotlinProject, file: File): KtFile {
        synchronized(mapOperationLock) {
            updateProjectPsiSourcesIfNeeded(module)

            return cachedKtFiles.getOrPut(file) {
                KotlinPsiManager.parseFile(file, module) ?: throw IllegalStateException("Cant parse file $file")
            }
        }
    }

    fun getFilesByProject(module: KotlinProject): Set<File> {
        synchronized(mapOperationLock) {
            updateProjectPsiSourcesIfNeeded(module)

            if (projectFiles.containsKey(module)) {
                return Collections.unmodifiableSet(projectFiles[module])
            }

            return emptySet()
        }
    }

    override fun getPsiFile(file: File, expectedSourceCode: String): KtFile {
        TODO("Not yet implemented")
    }

    override fun isApplicable(file: File): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeFile(file: File) {
        TODO("Not yet implemented")
    }

    fun updateProjectPsiSourcesIfNeeded(module: KotlinProject) {
        if (projectFiles.containsKey(module)) {
            return
        }

        addProject(module)
    }

    fun addProject(module: KotlinProject) {
        synchronized(mapOperationLock) {
            addFilesToParse(module)
        }
    }

    fun addFilesToParse(module: KotlinProject) {
        projectFiles[module] = HashSet()

        val kotlinFiles = File(module.getSrcDirPath()).walkBottomUp().filter {
            it.extension == "kt"
        }

    }

    fun addFile(module: KotlinProject, file: File) {
        synchronized(mapOperationLock) {
            projectFiles.getOrPut(module) { hashSetOf() }
                .add(file)
        }
    }
}

object KotlinPsiManager {

    private val projectSourceFiles = ProjectSourceFiles()

    fun getParsedFile(kotlinModule: KotlinProject, file: File): KtFile {
        return projectSourceFiles.getPsiFile(kotlinModule, file)
    }

    fun parseFile(file: File, kotlinModule: KotlinProject): KtFile? {
        if (!file.exists()) {
            return null
        }

        return parseText(FileUtil.loadFile(file, null, true), file, kotlinModule)
    }

    fun parseText(text: String, file: File, kotlinModule: KotlinProject): KtFile? {
        val project = getEnvironment(kotlinModule).project

        val virtualFile = KotlinLightVirtualFile(file, text)
        virtualFile.charset = CharsetToolkit.UTF8_CHARSET

        val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl

        return psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as? KtFile;
    }

    fun getFilesByProject(module: KotlinProject): Set<File> {
        return projectSourceFiles.getFilesByProject(module)
    }
}