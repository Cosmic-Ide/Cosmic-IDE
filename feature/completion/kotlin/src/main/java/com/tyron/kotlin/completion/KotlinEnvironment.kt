/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 *  This file is part of CodeAssist.
 *
 *  CodeAssist is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CodeAssist is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with CodeAssist.  If not, see <https://www.gnu.org/licenses/>.
 */
@file:OptIn(FrontendInternals::class)

package com.tyron.kotlin.completion

import android.util.Log
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.tyron.kotlin.completion.codeInsight.ReferenceVariantsHelper
import com.tyron.kotlin.completion.model.Analysis
import com.tyron.kotlin.completion.util.IdeDescriptorRenderersScripting
import com.tyron.kotlin.completion.util.getResolutionScope
import com.tyron.kotlin.completion.util.importableFqName
import com.tyron.kotlin.completion.util.isVisible
import com.tyron.kotlin.completion.util.logTime
import com.tyron.kotlin_completion.util.PsiUtils
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.project.Project
import org.cosmicide.common.Prefs
import org.cosmicide.editor.EditorCompletionItem
import org.cosmicide.rewrite.util.FileUtil
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
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
import org.jetbrains.kotlin.config.AnalysisFlags
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
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisContext
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.asFlexibleType
import org.jetbrains.kotlin.types.isFlexible
import java.io.File
import kotlin.collections.set

data class KotlinEnvironment(
    val kotlinEnvironment: KotlinCoreEnvironment
) {
    val kotlinFiles = mutableMapOf<String, KotlinFile>()

    fun updateKotlinFile(name: String, contents: String): KotlinFile {
        val kotlinFile = KotlinFile.from(kotlinEnvironment.project, name, contents)
        kotlinFiles[name] = kotlinFile
        return kotlinFile
    }

    private data class DescriptorInfo(
        val isTipsManagerCompletion: Boolean,
        val descriptors: List<DeclarationDescriptor>
    )

    private val renderer =
        IdeDescriptorRenderersScripting.SOURCE_CODE.withOptions {
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            typeNormalizer = IdeDescriptorRenderersScripting.APPROXIMATE_FLEXIBLE_TYPES
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
            typeNormalizer = { if (it.isFlexible()) it.asFlexibleType().upperBound else it }
        }


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
        override fun clear() {}

        override fun hasErrors() = hasError

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?
        ) {
            if (location == null) {
                println(message)
                return
            }
            if (severity.isError) {
                hasError = true
            }
            val kotlinFile = kotlinFiles[location.path.substring(1)]
            if (kotlinFile == null) {
                println("no kotlin file for ${location.path}")
                return
            }
            val issue = CodeIssue(
                kotlinFile.offsetFor(location.line - 1, location.column - 1),
                kotlinFile.offsetFor(location.lineEnd - 1, location.columnEnd - 1),
                message,
                severity
            )
            issueListener(issue)
        }
    }

    init {
        kotlinEnvironment.configuration.put(
            CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            messageCollector
        )
    }

    var analysis: TopDownAnalysisContext? = null

    private fun getPrefix(element: PsiElement): String {
        var text = (element as? KtSimpleNameExpression)?.text
        if (text == null) {
            val type = PsiUtils.findParent(element)
            if (type != null) {
                text = type.text
            }
        }
        if (text == null) {
            text = element.text
        }
        return (text ?: "").substringBefore(COMPLETION_SUFFIX).let {
            if (it.endsWith(".")) "" else it
        }
    }

    var currentItemCount = 0

    fun complete(file: KotlinFile, line: Int, character: Int): List<CompletionItem> {
        currentItemCount = 0
        var list: List<CompletionItem>
        val originalFile = file.kotlinFile
        CoroutineScope(Dispatchers.IO).launch {
            analysisOf(kotlinFiles.values.map { it.kotlinFile }, file.kotlinFile)
        }

        with(file.insert(COMPLETION_SUFFIX, line, character)) {
            kotlinFiles[originalFile.name] = this
            val position = elementAt(line, character)
            val prefix = position?.let { getPrefix(it) } ?: ""

            val reference = (position?.parent as? KtSimpleNameExpression)?.mainReference
            println("reference: $reference")

            list = position?.let { element ->
                val descriptorInfo = descriptorsFrom(element, file.kotlinFile)
                val items = descriptorInfo.descriptors
                    .sortedWith { a, b ->
                        val x = a.name.toString()
                        val y = b.name.toString()
                        x.compareTo(y)
                    }
                    .mapNotNull { descriptor ->
                        completionVariantFor(prefix, descriptor)
                    }
                if (items.size > 50) items.subList(0, 50) else items +
                        keywordsCompletionVariants(KtTokens.KEYWORDS, prefix)
            }
                ?: emptyList()
        }
        return list
    }

    private fun completionVariantFor(
        prefix: String,
        descriptor: DeclarationDescriptor
    ): CompletionItem? {
        val (name, tail) = descriptor.presentableName()

        var completionText = name
        val position = completionText.indexOf('(')
        if (position != -1) {
            completionText = StringBuilder(completionText).apply {
                replace(position, completionText.length, "()")
            }.toString()
        }

        val colonPosition = completionText.indexOf(":")
        if (colonPosition != -1) {
            completionText = completionText.substring(0, colonPosition - 1)
        }

        var tailName = tail
        val spacePosition = tail.lastIndexOf(" ")
        if (spacePosition != -1) {
            tailName = tail.substring(spacePosition + 1)
        }

        return if (name.startsWith(prefix)) {
            EditorCompletionItem(name, tailName, prefix.length, completionText).kind(
                when (descriptor) {
                    is ClassDescriptor -> CompletionItemKind.Class
                    is ConstructorDescriptor -> CompletionItemKind.Constructor
                    is FunctionDescriptor -> CompletionItemKind.Method
                    is PropertyDescriptor -> CompletionItemKind.Property
                    is VariableDescriptor -> CompletionItemKind.Variable
                    is PackageViewDescriptor -> CompletionItemKind.Module
                    else -> CompletionItemKind.Text
                }
            )
        } else {
            null
        }
    }

    private fun keywordsCompletionVariants(
        keywords: TokenSet,
        prefix: String
    ): List<CompletionItem> {
        // Return an empty list if the prefix is empty
        if (prefix.isEmpty()) return emptyList()

        val result = mutableListOf<CompletionItem>()

        // Iterate over the keywords and add the ones that match the prefix to the result
        for (token in keywords.types) {
            if (token is KtKeywordToken && token.value.startsWith(prefix)) {
                result.add(
                    EditorCompletionItem(
                        token.value,
                        "Keyword",
                        prefix.length,
                        token.value
                    ).kind(CompletionItemKind.Keyword)
                )
            }
        }

        return result
    }

    private fun descriptorsFrom(element: PsiElement, current: KtFile): DescriptorInfo {
        val files = kotlinFiles.values.map { it.kotlinFile }.toList()
        val analysis = analysisOf(files, current)
        return with(analysis) {
            logTime("referenceVariants") {
                (referenceVariantsFrom(element)
                    ?: referenceVariantsFrom(element.parent))?.let { descriptors ->
                    DescriptorInfo(true, descriptors)
                } ?: element.parent.let { parent ->
                    DescriptorInfo(
                        isTipsManagerCompletion = false,
                        descriptors = when (parent) {
                            is KtQualifiedExpression -> {
                                analysisResult.bindingContext.get(
                                    BindingContext.EXPRESSION_TYPE_INFO,
                                    parent.receiverExpression
                                )?.type?.let { expressionType ->
                                    analysisResult.bindingContext.get(
                                        BindingContext.LEXICAL_SCOPE,
                                        parent.receiverExpression
                                    )?.let {
                                        expressionType.memberScope.getContributedDescriptors(
                                            DescriptorKindFilter.ALL,
                                            MemberScope.ALL_NAME_FILTER
                                        )
                                    }
                                }?.toList() ?: emptyList()
                            }

                            else -> analysisResult.bindingContext.get(
                                BindingContext.LEXICAL_SCOPE,
                                element as KtExpression
                            )
                                ?.getContributedDescriptors(
                                    DescriptorKindFilter.ALL,
                                    MemberScope.ALL_NAME_FILTER
                                )
                                ?.toList() ?: emptyList()
                        }
                    )
                }

            }
        }
    }

    private val analyzerWithCompilerReport =
        AnalyzerWithCompilerReport(kotlinEnvironment.configuration)


    fun analysisOf(files: List<KtFile>, current: KtFile): Analysis {
        val bindingTrace = CliBindingTrace()
        val project = files.first().project
        var componentProvider: ComponentProvider? = null
        analyzerWithCompilerReport.analyzeAndReport(files) {
            componentProvider = logTime("componentProvider") {
                TopDownAnalyzerFacadeForJVM.createContainer(
                    kotlinEnvironment.project,
                    listOf(),
                    bindingTrace,
                    kotlinEnvironment.configuration,
                    kotlinEnvironment::createPackagePartProvider,
                    { storageManager, _ ->
                        FileBasedDeclarationProviderFactory(
                            storageManager,
                            files
                        )
                    }
                )
            }
            logTime("analyzeDeclarations") {
                analysis = componentProvider!!
                    .getService(LazyTopDownAnalyzer::class.java)
                    .analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)
            }

            val moduleDescriptor = componentProvider!!.getService(ModuleDescriptor::class.java)
            AnalysisHandlerExtension.getInstances(project).find {
                it.analysisCompleted(
                    project,
                    moduleDescriptor,
                    bindingTrace,
                    listOf(current)
                ) != null
            }

            return@analyzeAndReport AnalysisResult.success(
                bindingTrace.bindingContext,
                componentProvider!!.getService(ModuleDescriptor::class.java)
            )
        }
        return Analysis(
            componentProvider!!,
            analyzerWithCompilerReport.analysisResult
        )
    }

    @OptIn(FrontendInternals::class)
    private fun Analysis.referenceVariantsFrom(element: PsiElement): List<DeclarationDescriptor>? {
        val prefix = getPrefix(element)
        val elementKt = element as? KtElement ?: return emptyList()
        val bindingContext = analysisResult.bindingContext
        val resolutionFacade =
            KotlinResolutionFacade(
                project = element.project,
                componentProvider = componentProvider,
                moduleDescriptor = analysisResult.moduleDescriptor
            )
        val inDescriptor =
            elementKt.getResolutionScope(bindingContext, resolutionFacade).ownerDescriptor
        return when (element) {
            is KtSimpleNameExpression ->
                ReferenceVariantsHelper(
                    analysisResult.bindingContext,
                    resolutionFacade = resolutionFacade,
                    moduleDescriptor = analysisResult.moduleDescriptor,
                    visibilityFilter =
                    VisibilityFilter(
                        inDescriptor,
                        bindingContext,
                        element,
                        resolutionFacade
                    )
                )
                    .getReferenceVariants(
                        element,
                        DescriptorKindFilter.ALL,
                        nameFilter = {
                            if (prefix.isNotEmpty()) {
                                it.identifier.startsWith(prefix)
                            }
                            true
                        },
                        filterOutJavaGettersAndSetters = true,
                        filterOutShadowed = true,
                        excludeNonInitializedVariable = true,
                        useReceiverType = null
                    )
                    .toList()

            else -> null
        }
    }

    private fun DeclarationDescriptor.presentableName() =
        when (this) {
            is FunctionDescriptor ->
                name.asString() + renderer.renderFunctionParameters(this) to
                        when {
                            returnType != null -> renderer.renderType(returnType!!)
                            else ->
                                extensionReceiverParameter?.let { param ->
                                    " for ${renderer.renderType(param.type)} in ${
                                        DescriptorUtils.getFqName(
                                            containingDeclaration
                                        )
                                    }"
                                }
                                    ?: ""
                        }

            else ->
                name.asString() to
                        when (this) {
                            is VariableDescriptor -> renderer.renderType(type)
                            is ClassDescriptor ->
                                " (${DescriptorUtils.getFqName(containingDeclaration)})"

                            else -> renderer.render(this)
                        }
        }

    private inner class VisibilityFilter(
        private val inDescriptor: DeclarationDescriptor,
        private val bindingContext: BindingContext,
        private val element: KtElement,
        private val resolutionFacade: KotlinResolutionFacade
    ) : (DeclarationDescriptor) -> Boolean {
        @OptIn(FrontendInternals::class)
        override fun invoke(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor is TypeParameterDescriptor && !isTypeParameterVisible(descriptor))
                return false

            if (descriptor is DeclarationDescriptorWithVisibility) {
                return descriptor.isVisible(element, null, bindingContext, resolutionFacade)
            }

            return !descriptor.isInternalImplementationDetail()
        }

        private fun isTypeParameterVisible(typeParameter: TypeParameterDescriptor): Boolean {
            val owner = typeParameter.containingDeclaration
            var parent: DeclarationDescriptor? = inDescriptor
            while (parent != null) {
                if (parent == owner) return true
                if ((parent is ClassDescriptor) && !parent.isInner) return false
                parent = parent.containingDeclaration
            }
            return true
        }

        private fun DeclarationDescriptor.isInternalImplementationDetail(): Boolean =
            importableFqName?.asString() in excludedFromCompletion
    }

    companion object {
        private const val COMPLETION_SUFFIX = "æ"

        private val excludedFromCompletion: List<String> =
            listOf(
                "kotlin.jvm.internal",
                "kotlin.coroutines.experimental.intrinsics",
                "kotlin.coroutines.intrinsics",
                "kotlin.coroutines.experimental.jvm.internal",
                "kotlin.coroutines.jvm.internal",
                "kotlin.reflect.jvm.internal"
            )

        fun with(classpath: List<File>): KotlinEnvironment {
            setIdeaIoUseFallback()
            setupIdeaStandaloneExecution()
            return KotlinEnvironment(
                KotlinCoreEnvironment.createForProduction(
                    parentDisposable = {},
                    configFiles = EnvironmentConfigFiles.JVM_CONFIG_FILES,
                    configuration =
                    CompilerConfiguration().apply {
                        logTime("compilerConfig") {
                            put(JVMConfigurationKeys.NO_JDK, true)
                            put(JVMConfigurationKeys.NO_REFLECT, true)
                            put(
                                CLIConfigurationKeys.PERF_MANAGER,
                                object : CommonCompilerPerformanceManager("Profiling") {
                                    override fun notifyAnalysisStarted() {
                                        Log.i("Profiling", "Analysis started")
                                    }

                                    override fun notifyAnalysisFinished() {
                                        Log.i("Profiling", "Analysis started")
                                    }
                                })
                            put(
                                CommonConfigurationKeys.MODULE_NAME,
                                JvmProtoBufUtil.DEFAULT_MODULE_NAME
                            )
                            put(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING, false)
                            put(JVMConfigurationKeys.VALIDATE_IR, false)
                            put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, true)
                            put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, true)
                            put(JVMConfigurationKeys.DISABLE_RECEIVER_ASSERTIONS, true)
                            put(CommonConfigurationKeys.INCREMENTAL_COMPILATION, true)
                            put(JVMConfigurationKeys.USE_FAST_JAR_FILE_SYSTEM, Prefs.useFastJarFs)
                            put(CommonConfigurationKeys.USE_FIR, true)
                            put(CommonConfigurationKeys.USE_LIGHT_TREE, true)
                            put(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS, 10)
                            put(CommonConfigurationKeys.USE_FIR_EXTENDED_CHECKERS, false)

                            // enable all language features
                            val langFeatures =
                                mutableMapOf<LanguageFeature, LanguageFeature.State>()
                            for (langFeature in LanguageFeature.entries) {
                                langFeatures[langFeature] = LanguageFeature.State.ENABLED
                            }

                            val languageVersion =
                                LanguageVersion.fromVersionString(Prefs.kotlinVersion)!!
                            val languageVersionSettings = LanguageVersionSettingsImpl(
                                languageVersion,
                                ApiVersion.createByLanguageVersion(languageVersion),
                                mapOf(
                                    AnalysisFlags.extendedCompilerChecks to false,
                                    AnalysisFlags.ideMode to true,
                                    AnalysisFlags.skipMetadataVersionCheck to true,
                                    AnalysisFlags.skipPrereleaseCheck to true,
                                ),
                                langFeatures
                            )
                            put(
                                CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS,
                                languageVersionSettings
                            )
                            addJvmClasspathRoots(
                                classpath
                            )
                        }
                    }
                )
            )
        }

        fun get(module: Project): KotlinEnvironment {
            val jars = module.libDir.walk().filter { it.extension == "jar" }.toMutableList()
            jars.addAll(FileUtil.classpathDir.walk().filter { it.extension == "jar" })
            val environment = with(jars)
            environment.kotlinEnvironment.updateClasspath(
                jars.map { JvmClasspathRoot(it) }
            )
            module.srcDir.walk()
                .filter { it.extension == "kt" }
                .forEach {
                    environment.updateKotlinFile(it.absolutePath, it.readText())
                }
            return environment
        }
    }
}
