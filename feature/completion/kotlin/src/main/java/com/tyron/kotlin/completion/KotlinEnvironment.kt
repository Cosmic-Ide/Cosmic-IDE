/*
 * This file is part of CodeAssist.
 *
 * CodeAssist is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CodeAssist is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CodeAssist.  If not, see <https://www.gnu.org/licenses/>.
 */
@file:OptIn(FrontendInternals::class)

package com.tyron.kotlin.completion

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.tyron.kotlin.completion.codeInsight.ReferenceVariantsHelper
import com.tyron.kotlin.completion.model.Analysis
import com.tyron.kotlin.completion.util.IdeDescriptorRenderersScripting
import com.tyron.kotlin.completion.util.getResolutionScope
import com.tyron.kotlin.completion.util.importableFqName
import com.tyron.kotlin.completion.util.isVisible
import dev.pranav.navigation.NavigationProvider.ClassNavigationKind
import dev.pranav.navigation.NavigationProvider.FieldNavigationItem
import dev.pranav.navigation.NavigationProvider.MethodNavigationItem
import dev.pranav.navigation.NavigationProvider.NavigationItem
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.cosmicide.common.Prefs
import org.cosmicide.editor.EditorCompletionItem
import org.cosmicide.project.Project
import org.cosmicide.rewrite.util.FileUtil
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.asFlexibleType
import org.jetbrains.kotlin.types.isFlexible
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class KotlinEnvironment(val kotlinEnvironment: KotlinCoreEnvironment) {
    val kotlinFiles = mutableMapOf<String, KotlinFile>()
    private val analysisMutex = Mutex()
    var cachedAnalysis: Analysis? = null
    private var isDirty = true
    private var lastAnalyzedFiles: List<KtFile> = emptyList()

    private val navigationCache = ConcurrentHashMap<String, CachedNavigation>()

    private data class CachedNavigation(val items: List<NavigationItem>, val timestamp: Long)

    fun updateKotlinFile(name: String, contents: String): KotlinFile {
        val kotlinFile = KotlinFile.from(kotlinEnvironment.project, name, contents)
        kotlinFiles[name] = kotlinFile
        isDirty = true
        navigationCache.remove(name)
        return kotlinFile
    }

    private val renderer = IdeDescriptorRenderersScripting.SOURCE_CODE.withOptions {
        classifierNamePolicy = ClassifierNamePolicy.SHORT
        typeNormalizer = IdeDescriptorRenderersScripting.APPROXIMATE_FLEXIBLE_TYPES
        parameterNameRenderingPolicy = ParameterNameRenderingPolicy.ALL
        typeNormalizer = { if (it.isFlexible()) it.asFlexibleType().upperBound else it }
    }

    private data class DescriptorInfo(
        val isTipsManagerCompletion: Boolean,
        val descriptors: List<DeclarationDescriptor>
    )

    data class CodeIssue(
        val startOffset: Int,
        val endOffset: Int,
        val message: String,
        val severity: CompilerMessageSeverity
    )

    private var issueListener = { _: CodeIssue -> }

    fun addIssueListener(listener: (issue: CodeIssue) -> Unit) {
        issueListener = listener
    }

    private val messageCollector = object : MessageCollector {
        private var hasError = false
        override fun clear() {
            hasError = false
        }
        override fun hasErrors() = hasError
        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?
        ) {
            if (location == null) {
                println(message); return
            }
            if (severity.isError) hasError = true
            val kotlinFile = kotlinFiles[location.path.substring(1)] ?: return
            issueListener(
                CodeIssue(
                kotlinFile.offsetFor(location.line - 1, location.column - 1),
                kotlinFile.offsetFor(location.lineEnd - 1, location.columnEnd - 1),
                message,
                severity
                )
            )
        }
    }

    init {
        kotlinEnvironment.configuration.put(
            CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            messageCollector
        )
    }

    fun complete(file: KotlinFile, line: Int, character: Int): List<CompletionItem> {
        if (isDirty || cachedAnalysis == null) {
            runBlocking(Dispatchers.Default) {
                analysisMutex.withLock {
                    if (isDirty || cachedAnalysis == null) {
                        cachedAnalysis =
                            analysisOf(kotlinFiles.values.map { it.kotlinFile }, file.kotlinFile)
                        isDirty = false
                    }
                }
            }
        }

        val cleanFile = file.kotlinFile
        val offset = try {
            file.offsetFor(line, character)
        } catch (e: Exception) {
            return emptyList()
        }
        val prefix = getPrefix(cleanFile, offset)

        var element = cleanFile.findElementAt(offset)
        if (element == null && offset > 0) element = cleanFile.findElementAt(offset - 1)
        if (element != null && element.findParentOrSelf<KtSimpleNameExpression>() == null && offset > 0) {
            val prev = cleanFile.findElementAt(offset - 1)
            if (prev != null && prev.findParentOrSelf<KtSimpleNameExpression>() != null) element =
                prev
        }
        if (element == null) return emptyList()

        val analysis = cachedAnalysis ?: return emptyList()
        val descriptorInfo = descriptorsFrom(element, cleanFile, analysis, prefix)
        val comparator = RelevanceComparator(prefix)

        return descriptorInfo.descriptors
            .sortedWith(comparator)
            .mapNotNull { completionVariantFor(prefix, it) }
            .distinctBy { it.label }
            .take(50) + keywordsCompletionVariants(KtTokens.KEYWORDS, prefix)
    }

    private inline fun <reified T : PsiElement> PsiElement.findParentOrSelf(): T? {
        var curr: PsiElement? = this
        while (curr != null) {
            if (curr is T) return curr; curr = curr.parent
        }
        return null
    }

    private fun getPrefix(file: KtFile, offset: Int): String {
        val text = file.text
        if (offset > text.length) return ""
        val textBefore = text.substring(0, offset)
        val match = Regex("[a-zA-Z0-9_]+$").find(textBefore)
        return match?.value ?: ""
    }

    private class RelevanceComparator(private val prefix: String) :
        Comparator<DeclarationDescriptor> {
        override fun compare(a: DeclarationDescriptor, b: DeclarationDescriptor): Int {
            val nameA = a.name.asString();
            val nameB = b.name.asString()
            val aStartsExact = nameA.startsWith(prefix);
            val bStartsExact = nameB.startsWith(prefix)
            if (aStartsExact != bStartsExact) return if (aStartsExact) -1 else 1
            val scoreA = getDescriptorTypeScore(a);
            val scoreB = getDescriptorTypeScore(b)
            if (scoreA != scoreB) return scoreA.compareTo(scoreB)
            if (nameA.length != nameB.length) return nameA.length.compareTo(nameB.length)
            return nameA.compareTo(nameB)
        }

        private fun getDescriptorTypeScore(d: DeclarationDescriptor): Int = when (d) {
            is ValueParameterDescriptor -> 0; is PropertyDescriptor -> 1; is VariableDescriptor -> 2
            is FunctionDescriptor -> 3; is ClassDescriptor -> 4; is PackageViewDescriptor -> 5; else -> 6
        }
    }

    private fun completionVariantFor(
        prefix: String,
        descriptor: DeclarationDescriptor
    ): CompletionItem? {
        val simpleName = descriptor.name.asString()
        if (!simpleName.startsWith(prefix, ignoreCase = true)) return null
        val (label, tail) = descriptor.presentableName()
        val kind = when (descriptor) {
            is ClassDescriptor -> CompletionItemKind.Class; is ConstructorDescriptor -> CompletionItemKind.Constructor
            is FunctionDescriptor -> CompletionItemKind.Method; is PropertyDescriptor -> CompletionItemKind.Property
            is VariableDescriptor -> CompletionItemKind.Variable; is PackageViewDescriptor -> CompletionItemKind.Module
            else -> CompletionItemKind.Text
        }
        return EditorCompletionItem(label, tail, prefix.length, simpleName).kind(kind)
    }

    private fun keywordsCompletionVariants(
        keywords: TokenSet,
        prefix: String
    ): List<CompletionItem> {
        if (prefix.isEmpty()) return emptyList()
        return keywords.types.filterIsInstance<KtKeywordToken>()
            .filter { it.value.startsWith(prefix, ignoreCase = true) }
            .map {
                EditorCompletionItem(it.value, "Keyword", prefix.length, it.value).kind(
                    CompletionItemKind.Keyword
                )
            }
    }

    private fun descriptorsFrom(
        element: PsiElement,
        current: KtFile,
        analysis: Analysis,
        prefix: String
    ): DescriptorInfo {
        return with(analysis) {
            val simpleName = element.findParentOrSelf<KtSimpleNameExpression>()
            if (simpleName != null) {
                referenceVariantsFrom(simpleName, prefix)?.let {
                    return@with DescriptorInfo(
                        true,
                        it
                    )
                }
            }
            DescriptorInfo(
                false, when (val parent = element.parent) {
                    is KtQualifiedExpression ->
                        analysisResult.bindingContext.get(
                            BindingContext.EXPRESSION_TYPE_INFO,
                            parent.receiverExpression
                        )
                            ?.type?.memberScope?.getContributedDescriptors(
                                DescriptorKindFilter.ALL,
                                MemberScope.ALL_NAME_FILTER
                            )?.toList() ?: emptyList()

                    else -> {
                        val expr = element.findParentOrSelf<KtExpression>()
                        if (expr != null) {
                            analysisResult.bindingContext.get(BindingContext.LEXICAL_SCOPE, expr)
                                ?.getContributedDescriptors(
                                    DescriptorKindFilter.ALL,
                                    MemberScope.ALL_NAME_FILTER
                                )?.toList() ?: emptyList()
                        } else emptyList()
                    }
                }
            )
        }
    }

    private val analyzerWithCompilerReport =
        AnalyzerWithCompilerReport(kotlinEnvironment.configuration)

    @Suppress("DEPRECATION_ERROR")
    fun analysisOf(files: List<KtFile>, current: KtFile): Analysis {
        if (files.isEmpty()) throw IllegalStateException("No files to analyze")
        val project = files.first().project
        val bindingTrace = CliBindingTrace(project)
        var componentProvider: ComponentProvider? = null

        analyzerWithCompilerReport.analyzeAndReport(files) {
            componentProvider = TopDownAnalyzerFacadeForJVM.createContainer(
                kotlinEnvironment.project,
                emptyList(),
                bindingTrace,
                kotlinEnvironment.configuration,
                kotlinEnvironment::createPackagePartProvider,
                { sm, _ -> FileBasedDeclarationProviderFactory(sm, files) }
            )
            componentProvider.getService(LazyTopDownAnalyzer::class.java)
                .analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)
            val moduleDescriptor = componentProvider!!.getService(ModuleDescriptor::class.java)
            AnalysisHandlerExtension.getInstances(project).find {
                it.analysisCompleted(
                    project,
                    moduleDescriptor,
                    bindingTrace,
                    listOf(current)
                ) != null
            }
            AnalysisResult.success(bindingTrace.bindingContext, moduleDescriptor)
        }
        lastAnalyzedFiles = files
        return Analysis(componentProvider!!, analyzerWithCompilerReport.analysisResult)
    }

    private fun Analysis.referenceVariantsFrom(
        element: PsiElement,
        prefix: String
    ): List<DeclarationDescriptor>? {
        val elementKt = element as? KtElement ?: return null
        val resolutionFacade = KotlinResolutionFacade(
            project = element.project,
            componentProvider = componentProvider,
            moduleDescriptor = analysisResult.moduleDescriptor
        )
        val inDescriptor = elementKt.getResolutionScope(
            analysisResult.bindingContext,
            resolutionFacade
        ).ownerDescriptor
        return when (element) {
            is KtSimpleNameExpression -> ReferenceVariantsHelper(
                analysisResult.bindingContext, resolutionFacade, analysisResult.moduleDescriptor,
                VisibilityFilter(
                    inDescriptor,
                    analysisResult.bindingContext,
                    element,
                    resolutionFacade
                )
            ).getReferenceVariants(
                element,
                DescriptorKindFilter.ALL,
                { it.identifier.startsWith(prefix, ignoreCase = true) },
                true,
                true,
                true,
                null
            ).toList()
            else -> null
        }
    }

    private fun DeclarationDescriptor.presentableName(): Pair<String, String> = when (this) {
        is FunctionDescriptor -> {
            val params = renderer.renderFunctionParameters(this)
            name.asString() + params to (returnType?.let { renderer.renderType(it) } ?: "")
        }

        else -> name.asString() to (when (this) {
            is VariableDescriptor -> renderer.renderType(type)
            is ClassDescriptor -> " (${DescriptorUtils.getFqName(containingDeclaration)})"
            else -> renderer.render(this)
        })
    }

    private inner class VisibilityFilter(
        private val inDescriptor: DeclarationDescriptor,
        private val bindingContext: BindingContext,
        private val element: KtElement,
        private val resolutionFacade: KotlinResolutionFacade
    ) : (DeclarationDescriptor) -> Boolean {
        override fun invoke(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor is TypeParameterDescriptor && !isTypeParameterVisible(descriptor)) return false
            if (descriptor is DeclarationDescriptorWithVisibility) return descriptor.isVisible(
                element,
                null,
                bindingContext,
                resolutionFacade
            )
            return (descriptor.importableFqName?.asString() !in excludedFromCompletion)
        }
        private fun isTypeParameterVisible(typeParameter: TypeParameterDescriptor): Boolean {
            var parent: DeclarationDescriptor? = inDescriptor
            while (parent != null) {
                if (parent == typeParameter.containingDeclaration) return true
                if (parent is ClassDescriptor && !parent.isInner) return false
                parent = parent.containingDeclaration
            }
            return true
        }
    }

    // ==================== OPTIMIZED NAVIGATION PARSING ====================

    /**
     * Returns navigation items from all analyzed files with caching.
     * Cache TTL: 5 seconds to balance freshness vs performance.
     */
    fun getNavigationItems(): List<NavigationItem> {
        // Ensure analysis is fresh
        if (isDirty || cachedAnalysis == null) {
            runBlocking(Dispatchers.Default) {
                analysisMutex.withLock {
                    if (isDirty || cachedAnalysis == null) {
                        cachedAnalysis = analysisOf(
                            kotlinFiles.values.map { it.kotlinFile },
                            kotlinFiles.values.firstOrNull()?.kotlinFile ?: return@withLock
                        )
                        isDirty = false
                    }
                }
            }
        }

        val now = System.currentTimeMillis()
        val allItems = mutableListOf<NavigationItem>()

        for (file in lastAnalyzedFiles) {
            val cacheKey =
                "${file.virtualFile?.path ?: file.name}:${file.text.length}:${file.modificationStamp}"
            val cached = navigationCache[cacheKey]

            if (cached != null && (now - cached.timestamp) < 5000L) {
                allItems.addAll(cached.items)
            } else {
                val items = parseDeclarations(file)
                navigationCache[cacheKey] = CachedNavigation(items, now)
                allItems.addAll(items)
            }
        }

        return allItems
    }


    /**
     * Returns navigation items for a specific file only.
     * @param fileName Optional: the file path/name to parse. If null, returns items for ALL files.
     * @param useCache Whether to use cached results (default: true). Disable for forced refresh.
     */
    fun getNavigationItems(
        fileName: String? = null,
        useCache: Boolean = true
    ): MutableList<NavigationItem> {
        // Ensure analysis is fresh if requested file isn't cached
        if (isDirty || cachedAnalysis == null) {
            runBlocking(Dispatchers.Default) {
                analysisMutex.withLock {
                    if (isDirty || cachedAnalysis == null) {
                        cachedAnalysis = analysisOf(
                            kotlinFiles.values.map { it.kotlinFile },
                            kotlinFiles.values.firstOrNull()?.kotlinFile ?: return@withLock
                        )
                        isDirty = false
                    }
                }
            }
        }

        val now = System.currentTimeMillis()

        // If fileName is specified, only parse that file
        return if (fileName != null) {
            val targetFile = kotlinFiles[fileName]?.kotlinFile ?: lastAnalyzedFiles.find {
                it.name == fileName || it.virtualFile?.path?.endsWith(
                    fileName
                ) == true
            }
            targetFile?.let { file ->
                val cacheKey =
                    "file:${file.virtualFile?.path ?: file.name}:${file.text.length}:${file.modificationStamp}"

                if (useCache) {
                    val cached = navigationCache[cacheKey]
                    if (cached != null && (now - cached.timestamp) < 5000L) {
                        return cached.items.toMutableList()
                    }
                }

                val items = parseDeclarations(file).toMutableList()
                navigationCache[cacheKey] = CachedNavigation(items, now)
                items
            } ?: mutableListOf()
        } else {
            // Original behavior: return items for ALL analyzed files
            val allItems = mutableListOf<NavigationItem>()
            for (file in lastAnalyzedFiles) {
                val cacheKey =
                    "file:${file.virtualFile?.path ?: file.name}:${file.text.length}:${file.modificationStamp}"
                val cached = navigationCache[cacheKey]

                if (useCache && cached != null && (now - cached.timestamp) < 5000L) {
                    allItems.addAll(cached.items)
                } else {
                    val items = parseDeclarations(file).toList()
                    navigationCache[cacheKey] = CachedNavigation(items, now)
                    allItems.addAll(items)
                }
            }
            allItems
        }
    }

    /**
     * Convenience overload: get navigation items for a KotlinFile object directly.
     */
    fun getNavigationItems(file: KtFile, useCache: Boolean = true): MutableList<NavigationItem> {
        val fileName = file.virtualFile?.path ?: file.name
        return getNavigationItems(fileName, useCache)
    }

    /**
     * Parse navigation items for a file WITHOUT triggering full project analysis.
     * Useful for lightweight operations like outline/breadcrumb views.
     * Note: May miss cross-file references, but much faster for single-file views.
     */
    fun getNavigationItemsLazy(file: KotlinFile, depth: Int = 0): List<NavigationItem> {
        // Skip analysis entirely — just parse PSI structure
        return parseDeclarations(file.kotlinFile, depth).toList()
    }

    /**
     * Clear navigation cache for a specific file, or all files if fileName is null.
     */
    fun clearNavigationCache(fileName: String? = null) {
        if (fileName != null) {
            // Remove only entries matching this file
            val keysToRemove =
                navigationCache.keys.filter { it.startsWith("file:$fileName") || it.contains(":$fileName:") }
            keysToRemove.forEach { navigationCache.remove(it) }
        } else {
            navigationCache.clear()
        }
    }

    /**
     * Single optimized recursive parser for all declaration types.
     * Avoids code duplication between file/class parsing.
     */
    private fun parseDeclarations(
        element: KtElement,
        depth: Int = 0,
        accumulator: MutableList<NavigationItem> = mutableListOf()
    ): List<NavigationItem> {
        when (element) {
            is KtClass -> {
                val classItem = buildClassItem(element, depth)
                accumulator.add(classItem)
                // Recurse into class body
                for (declaration in element.declarations) {
                    parseDeclarations(declaration, depth + 1, accumulator)
                }
            }

            is KtNamedFunction -> {
                accumulator.add(buildFunctionItem(element, depth))
            }

            is KtProperty -> {
                accumulator.add(buildPropertyItem(element, depth))
            }

            is KtFile -> {
                // Entry point: parse all top-level declarations
                for (declaration in element.declarations) {
                    parseDeclarations(declaration, depth, accumulator)
                }
            }
        }
        return accumulator
    }

    private fun buildClassItem(klass: KtClass, depth: Int): NavigationItem {
        val name = buildString {
            append(klass.name ?: "anonymous")
            val superTypes = klass.superTypeListEntries
            if (superTypes.isNotEmpty()) {
                val superClass = superTypes.firstOrNull()?.typeAsUserType?.referencedName
                if (superClass != null) append(" : $superClass")
                val interfaces = superTypes.drop(1).mapNotNull { it.typeAsUserType?.referencedName }
                if (interfaces.isNotEmpty()) append(" -> ${interfaces.joinToString(", ")}")
            }
        }
        return ClassNavigationKind(
            name = name,
            modifiers = klass.modifierList?.text ?: "",
            startPosition = klass.startOffset,
            endPosition = klass.endOffset,
            depth = depth
        )
    }

    private fun buildFunctionItem(func: KtNamedFunction, depth: Int): NavigationItem {
        val name = buildString {
            append(func.name ?: "anonymous")
            append("(")
            append(func.valueParameters.joinToString(", ") { param ->
                buildString {
                    append(param.name ?: "_")
                    val type = param.typeReference?.text
                    if (type != null) append(": $type")
                }
            })
            append(")")
            val returnType = func.typeReference?.text
            if (returnType != null) append(": $returnType")
        }
        return MethodNavigationItem(
            name = name,
            modifiers = func.modifierList?.text ?: "",
            startPosition = func.startOffset,
            endPosition = func.endOffset,
            depth = depth
        )
    }

    private fun buildPropertyItem(prop: KtProperty, depth: Int): NavigationItem {
        val name = buildString {
            append(prop.name ?: "anonymous")
            val type = prop.typeReference?.text
            if (type != null) append(": $type")
        }
        return FieldNavigationItem(
            name = name,
            modifiers = prop.modifierList?.text ?: "",
            startPosition = prop.startOffset,
            endPosition = prop.endOffset,
            depth = depth
        )
    }

    /**
     * Clear navigation cache (use when project structure changes significantly).
     */
    fun clearNavigationCache() {
        navigationCache.clear()
    }

    // ==================== ADVANCED APIS ====================

    fun getAnalyzedFiles(): List<KtFile> = lastAnalyzedFiles
    fun getBindingContext(): BindingContext? = cachedAnalysis?.analysisResult?.bindingContext
    fun getModuleDescriptor(): ModuleDescriptor? = cachedAnalysis?.analysisResult?.moduleDescriptor
    fun getComponentProvider(): ComponentProvider? = cachedAnalysis?.componentProvider

    companion object {
        private val excludedFromCompletion = listOf(
            "kotlin.jvm.internal",
            "kotlin.coroutines.experimental.intrinsics",
            "kotlin.coroutines.intrinsics",
            "kotlin.coroutines.experimental.jvm.internal",
            "kotlin.coroutines.jvm.internal",
            "kotlin.reflect.jvm.internal"
        )

        @OptIn(K1Deprecation::class)
        fun with(classpath: List<File>): KotlinEnvironment {
            setIdeaIoUseFallback()
            setupIdeaStandaloneExecution()

            val configuration = CompilerConfiguration().apply {
                put(JVMConfigurationKeys.NO_JDK, true)
                put(JVMConfigurationKeys.NO_REFLECT, true)
                put(CommonConfigurationKeys.MODULE_NAME, JvmProtoBufUtil.DEFAULT_MODULE_NAME)
                put(JVMConfigurationKeys.USE_FAST_JAR_FILE_SYSTEM, Prefs.useFastJarFs)
                put(CommonConfigurationKeys.USE_FIR, true)
                put(CommonConfigurationKeys.USE_LIGHT_TREE, true)
                put(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS, 2)

                val langFeatures =
                    LanguageFeature.entries.associateWith { LanguageFeature.State.ENABLED }
                val languageVersion = LanguageVersion.fromVersionString(Prefs.kotlinVersion)
                    ?: LanguageVersion.LATEST_STABLE
                put(
                    CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS,
                    LanguageVersionSettingsImpl(
                        languageVersion,
                        ApiVersion.createByLanguageVersion(languageVersion),
                        emptyMap(),
                        langFeatures
                    )
                )
                addJvmClasspathRoots(classpath)
            }

            return KotlinEnvironment(
                KotlinCoreEnvironment.createForProduction(
                    {},
                    configuration,
                    EnvironmentConfigFiles.JVM_CONFIG_FILES
                )
            )
        }

        fun get(module: Project): KotlinEnvironment {
            val jars =
                (module.libDir.walk() + FileUtil.classpathDir.walk()).filter { it.extension == "jar" }
                    .toList()
            val environment = with(jars)
            environment.kotlinEnvironment.updateClasspath(jars.map { JvmClasspathRoot(it) })
            module.srcDir.walk().filter { it.extension == "kt" }
                .forEach { environment.updateKotlinFile(it.absolutePath, it.readText()) }
            return environment
        }
    }
}
