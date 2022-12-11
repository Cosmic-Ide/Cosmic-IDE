@file:OptIn(FrontendInternals::class)

package com.tyron.kotlin.completion

import org.cosmic.ide.project.Project
import org.cosmic.ide.common.util.FileUtil
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItem
import com.tyron.kotlin.completion.codeInsight.ReferenceVariantsHelper
import com.tyron.kotlin.completion.model.Analysis
import com.tyron.kotlin.completion.util.*
import com.tyron.kotlin_completion.util.PsiUtils
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.asFlexibleType
import org.jetbrains.kotlin.types.isFlexible
import java.io.File
import java.util.*
import kotlin.collections.set

data class KotlinEnvironment(
    val classpath: List<File>,
    val kotlinEnvironment: KotlinCoreEnvironment
) {
    private val kotlinFiles = mutableMapOf<String, KotlinFile>()

    fun updateKotlinFile(name: String, contents: String): KotlinFile {
        val kotlinFile = KotlinFile.from(kotlinEnvironment.project, name, contents)
        kotlinFiles[name] = kotlinFile
        return kotlinFile
    }

    fun removeKotlinFile(name: String) {
        kotlinFiles.remove(name)
    }

    private data class DescriptorInfo(
        val isTipsManagerCompletion: Boolean,
        val descriptors: List<DeclarationDescriptor>
    )

    private val renderer = IdeDescriptorRenderersScripting.SOURCE_CODE.withOptions {
        classifierNamePolicy = ClassifierNamePolicy.SHORT
        typeNormalizer = IdeDescriptorRenderersScripting.APPROXIMATE_FLEXIBLE_TYPES
        parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
        typeNormalizer = {
            if (it.isFlexible()) it.asFlexibleType().upperBound
            else it
        }
    }

    fun getPrefix(element: PsiElement): String {
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
        return (text ?: "").substringBefore(COMPLETION_SUFFIX)
            .let {
                if (it.endsWith(".")) "" else it
            }
    }


    fun complete(file: KotlinFile, line: Int, character: Int) =
        with(file.insert("$COMPLETION_SUFFIX", line, character)) {
            kotlinFiles[file.name] = this

            elementAt(line, character)?.let { element ->
                val descriptorInfo = descriptorsFrom(element)
                val prefix = getPrefix(element)
                descriptorInfo.descriptors.toMutableList().apply {
                    sortWith { a, b ->
                        a.presentableName().compareTo(b.presentableName(), true)
                    }
                }.mapNotNull { descriptor ->
                    completionVariantFor(
                        prefix,
                        descriptor
                    )
                } + keywordsCompletionVariants(
                    KtTokens.KEYWORDS, prefix
                ) + keywordsCompletionVariants(KtTokens.SOFT_KEYWORDS, prefix)
            } ?: emptyList()
        }

    private fun completionVariantFor(
        prefix: String,
        descriptor: DeclarationDescriptor
    ): CompletionItem? {
        val (name, tail) = descriptor.presentableName()
        val fullName: String = formatName(name, 40)
        var completionText = fullName
        var position = completionText.indexOf('(')
        if (position != -1) {
            if (completionText[position - 1] == ' ') position -= 2
            if (completionText[position + 1] == ')') position++
            completionText = completionText.substring(0, position + 1)
        }
        position = completionText.indexOf(":")
        if (position != -1) completionText = completionText.substring(0, position - 1)
        return if (fullName.startsWith(prefix)) {
            SimpleCompletionItem(fullName, tail, prefix.length, completionText)
        } else null
    }

    private fun formatName(builder: String, symbols: Int) =
        if (builder.length > symbols) builder.substring(0, symbols) + "..." else builder


    private fun keywordsCompletionVariants(keywords: TokenSet, prefix: String): SimpleCompletionItem? {
        if (prefix == "") return null
        keywords.types.mapNotNull {
            if (&& it.value.startsWith(prefix)) {
                return SimpleCompletionItem(it.value, "Keyword", prefix.length, it.value)
            } else {
                return null
            }
        }
    }

    private fun descriptorsFrom(element: PsiElement): DescriptorInfo {
        val files = kotlinFiles.values.map { it.kotlinFile }.toList()
        val analysis = analysisOf(files)
        return with(analysis) {
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

    private fun analysisOf(files: List<KtFile>): Analysis {
        val trace = CliBindingTrace()
        val project = files.first().project
        val componentProvider = TopDownAnalyzerFacadeForJVM.createContainer(
            kotlinEnvironment.project,
            files,
            trace,
            kotlinEnvironment.configuration,
            { globalSearchScope -> kotlinEnvironment.createPackagePartProvider(globalSearchScope) },
            { storageManager, ktFiles ->
                FileBasedDeclarationProviderFactory(
                    storageManager,
                    ktFiles
                )
            },
            sourceModuleSearchScope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(
                project,
                files
            )
        )
        return logTime("analysis") {
            componentProvider.getService(LazyTopDownAnalyzer::class.java)
                .analyzeDeclarations(
                    TopDownAnalysisMode.TopLevelDeclarations,
                    files
                )
            val moduleDescriptor = componentProvider.getService(ModuleDescriptor::class.java)
            AnalysisHandlerExtension.getInstances(project)
                .find { it.analysisCompleted(project, moduleDescriptor, trace, files) != null }
            return@logTime Analysis(
                componentProvider,
                AnalysisResult.success(trace.bindingContext, moduleDescriptor)
            )
        }
    }

    private fun Analysis.referenceVariantsFrom(element: PsiElement): List<DeclarationDescriptor>? {
        val prefix = getPrefix(element)
        val elementKt = element as? KtElement ?: return emptyList()
        val bindingContext = analysisResult.bindingContext
        val resolutionFacade = KotlinResolutionFacade(
            project = element.project,
            componentProvider = componentProvider,
            moduleDescriptor = analysisResult.moduleDescriptor
        )
        val inDescriptor: DeclarationDescriptor =
            elementKt.getResolutionScope(bindingContext, resolutionFacade).ownerDescriptor
        return when (element) {
            is KtSimpleNameExpression -> ReferenceVariantsHelper(
                analysisResult.bindingContext,
                resolutionFacade = resolutionFacade,
                moduleDescriptor = analysisResult.moduleDescriptor,
                visibilityFilter = VisibilityFilter(
                    inDescriptor,
                    bindingContext,
                    element,
                    resolutionFacade
                )
            ).getReferenceVariants(
                element,
                DescriptorKindFilter.ALL,
                nameFilter = {
                    if (prefix.isNotEmpty()) {
                        it.identifier.startsWith(prefix)
                    }
                    false
                }
            ).toList()
            else -> null
        }
    }

    private fun DeclarationDescriptor.presentableName() = when (this) {
        is FunctionDescriptor -> name.asString() + renderer.renderFunctionParameters(this) to when {
            returnType != null -> renderer.renderType(returnType!!)
            else -> extensionReceiverParameter?.let { param ->
                " for ${renderer.renderType(param.type)} in ${
                    DescriptorUtils.getFqName(
                        containingDeclaration
                    )
                }"
            } ?: ""
        }
        else -> name.asString() to when (this) {
            is VariableDescriptor -> renderer.renderType(type)
            is ClassDescriptor -> " (${DescriptorUtils.getFqName(containingDeclaration)})"
            else -> renderer.render(this)
        }
    }


    // This code is a fragment of org.jetbrains.kotlin.idea.completion.CompletionSession from Kotlin IDE Plugin
    // with a few simplifications which were possible because webdemo has very restricted environment (and well,
    // because requirements on compeltion' quality in web-demo are lower)
    private inner class VisibilityFilter(
        private val inDescriptor: DeclarationDescriptor,
        private val bindingContext: BindingContext,
        private val element: KtElement,
        private val resolutionFacade: KotlinResolutionFacade
    ) : (DeclarationDescriptor) -> Boolean {
        override fun invoke(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor is TypeParameterDescriptor && !isTypeParameterVisible(descriptor)) return false

            if (descriptor is DeclarationDescriptorWithVisibility) {
                return descriptor.isVisible(element, null, bindingContext, resolutionFacade)
            }

            if (descriptor.isInternalImplementationDetail()) return false

            return true
        }

        private fun isTypeParameterVisible(typeParameter: TypeParameterDescriptor): Boolean {
            val owner = typeParameter.containingDeclaration
            var parent: DeclarationDescriptor? = inDescriptor
            while (parent != null) {
                if (parent == owner) return true
                if (parent is ClassDescriptor && !parent.isInner) return false
                parent = parent.containingDeclaration
            }
            return true
        }

        private fun DeclarationDescriptor.isInternalImplementationDetail(): Boolean =
            importableFqName?.asString() in excludedFromCompletion
    }

    companion object {
        private const val COMPLETION_SUFFIX = ";"

        private val excludedFromCompletion: List<String> = listOf(
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
            return KotlinEnvironment(classpath, KotlinCoreEnvironment.createForProduction(
                parentDisposable = Disposer.newDisposable(),
                configFiles = EnvironmentConfigFiles.JVM_CONFIG_FILES,
                configuration = CompilerConfiguration().apply {
                    logTime("compilerConfig") {
                        addJvmClasspathRoots(classpath.filter { it.exists() && it.isFile && it.extension == "jar" })
                        put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, LoggingMessageCollector)
                        put(CommonConfigurationKeys.MODULE_NAME, "completion")
                        put(CommonConfigurationKeys.USE_FIR, true)
                        put(JVMConfigurationKeys.NO_JDK, true)
                        put(JVMConfigurationKeys.NO_REFLECT, true)
                        put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)
                        put(JVMConfigurationKeys.IGNORE_CONST_OPTIMIZATION_ERRORS, true)
                        put(JVMConfigurationKeys.VALIDATE_BYTECODE, false)
                        put(JVMConfigurationKeys.USE_FAST_JAR_FILE_SYSTEM, true)
                    }
                }
            ))
        }

        fun get(module: Project): KotlinEnvironment {
            val jars = File(module.libDirPath).walk().filter {
                it.extension == "jar"
            }.toMutableList()
            jars.add(File(FileUtil.getClasspathDir(), "android.jar"))
            jars.add(File(FileUtil.getClasspathDir(), "kotlin-stdlib-1.7.20.jar"))
            jars.add(File(FileUtil.getClasspathDir(), "kotlin-stdlib-common-1.7.20.jar"))
            val environment = with(jars)
            File(module.srcDirPath).walk().forEach {
                if (it.extension == "kt") {
                    environment.updateKotlinFile(it.absolutePath, it.readText())
                }
            }
            return environment
        }
    }
}