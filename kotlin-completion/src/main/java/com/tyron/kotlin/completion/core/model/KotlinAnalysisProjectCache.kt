package com.tyron.kotlin.completion.core.model

import org.cosmic.ide.project.KotlinProject
import com.tyron.kotlin.completion.core.resolve.CodeAssistAnalyzerFacadeForJVM
import com.tyron.kotlin.completion.core.util.ProjectUtils
import org.jetbrains.kotlin.analyzer.AnalysisResult
import java.util.concurrent.ConcurrentHashMap

object KotlinAnalysisProjectCache {
    private val cachedAnalysisResults = ConcurrentHashMap<KotlinProject, AnalysisResult>()

    fun resetCache(module: KotlinProject) {
        synchronized(module) {
            cachedAnalysisResults.remove(module)
        }
    }

    fun resetAllCaches() {
        cachedAnalysisResults.keys.toList().forEach {
            resetCache(it)
        }
    }

    fun getAnalysisResult(module: KotlinProject): AnalysisResult {
        return synchronized(module) {
            val analysisResult = cachedAnalysisResults[module] ?: run {
                CodeAssistAnalyzerFacadeForJVM.analyzeSources(
                    KotlinEnvironment.getEnvironment(module),
                    ProjectUtils.getSourceFiles(module)
                ).analysisResult
            }

            cachedAnalysisResults.putIfAbsent(module, analysisResult) ?: analysisResult
        }
    }
}