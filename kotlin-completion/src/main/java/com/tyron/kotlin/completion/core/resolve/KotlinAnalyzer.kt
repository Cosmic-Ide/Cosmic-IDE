package com.tyron.kotlin.completion.core.resolve

import org.cosmic.ide.project.KotlinProject
import com.tyron.kotlin.completion.core.model.KotlinAnalysisFileCache
import com.tyron.kotlin.completion.core.model.KotlinEnvironment
import com.tyron.kotlin.completion.core.model.getEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.psi.KtFile


object KotlinAnalyzer {
    fun analyzeFile(jetFile: KtFile): AnalysisResultWithProvider {
        return KotlinAnalysisFileCache.getAnalysisResult(jetFile)
    }

    fun analyzeFiles(allFiles: Collection<KtFile>, files: Collection<KtFile>): AnalysisResultWithProvider {
        return when {
            files.isEmpty() -> throw IllegalStateException("There should be at least one file to analyze")

            files.size == 1 -> analyzeFile(files.single())

            else -> {
                val environment = getEnvironment(files.first().project)
                if (environment == null) {
                    throw IllegalStateException("There is no environment for project: ${files.first().project}")
                }

                if (false) {
                    throw IllegalStateException("Only KotlinEnvironment can be used to analyze several files")
                }

                analyzeFiles(environment, allFiles, files)
            }
        }
    }

    fun analyzeProject(module: KotlinProject): AnalysisResultWithProvider {
        val environment = KotlinEnvironment.getEnvironment(module)
        return analyzeFiles(environment, emptyList(), emptyList())
    }

    private fun analyzeFiles(kotlinEnvironment: KotlinCoreEnvironment,
                             allFiles: Collection<KtFile>,
                             filesToAnalyze: Collection<KtFile>): AnalysisResultWithProvider {
        return CodeAssistAnalyzerFacadeForJVM.analyzeSources(kotlinEnvironment, allFiles, filesToAnalyze)
    }
}