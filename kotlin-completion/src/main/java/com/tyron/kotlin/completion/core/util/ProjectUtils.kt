package com.tyron.kotlin.completion.core.util

import org.cosmic.ide.project.KotlinProject
import com.tyron.kotlin.completion.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.psi.KtFile

object ProjectUtils {

    fun getSourceFiles(module: KotlinProject): List<KtFile> {
        val tempFiles = KotlinPsiManager.getFilesByProject(module);
        return tempFiles.map { KotlinPsiManager.getParsedFile(module, it) }
    }
}