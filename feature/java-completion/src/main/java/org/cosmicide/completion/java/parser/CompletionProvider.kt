/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */
package org.cosmicide.completion.java.parser

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl
import com.intellij.openapi.vfs.impl.VirtualFileManagerImpl
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiImportList
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiImportStaticReferenceElement
import com.intellij.psi.PsiImportStaticStatement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiJavaToken
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.tree.TreeCopyHandler
import com.intellij.psi.util.PsiTreeUtil
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import javassist.CtClass
import javassist.Modifier
import org.cosmicide.common.Prefs
import org.cosmicide.completion.java.parser.cache.SymbolCacher
import org.cosmicide.completion.java.parser.cache.qualifiedName
import org.cosmicide.editor.EditorCompletionItem
import org.cosmicide.rewrite.util.FileUtil
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import java.util.logging.Logger

class CompletionProvider {

    companion object {
        val logger: Logger = Logger.getLogger(CompletionProvider::javaClass.name)

        @OptIn(K1Deprecation::class)
        val environment = KotlinCoreEnvironment.createProjectEnvironmentForTests(
            {},
            CompilerConfiguration().apply {
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
            })
//            JavaCoreProjectEnvironment({ logger.info("JavaCoreProjectEnvironment disposed") },
//                JavaCoreApplicationEnvironment { logger.info("JavaCoreApplicationEnvironment disposed") })

        val symbolCacher: SymbolCacher by lazy {
            SymbolCacher(FileUtil.classpathDir.resolve("android.jar")).apply {
                loadClassesFromJar()
            }
        }

        val fileFactory: PsiFileFactory by lazy {
            PsiFileFactory.getInstance(environment.project)
        }

        val javaKeywords: Array<String> = arrayOf(
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while",
            "true",
            "false",
            "null"
        )

        @Suppress("DEPRECATION")
        fun registerExtensions(extensionArea: ExtensionsAreaImpl) {
            if (!extensionArea.hasExtensionPoint("com.intellij.virtualFileManagerListener")) {
                extensionArea.registerExtensionPoint(
                    "com.intellij.virtualFileManagerListener",
                    VirtualFileManagerImpl::class.java.name,
                    ExtensionPoint.Kind.INTERFACE
                )
            }
            if (extensionArea.hasExtensionPoint("com.intellij.java.elementFinder").not()) {
                extensionArea.registerExtensionPoint(
                    "com.intellij.java.elementFinder",
                    PsiElementFinder::class.java.name,
                    ExtensionPoint.Kind.INTERFACE
                )
            }
            val rootArea = Extensions.getRootArea()
            if (rootArea.hasExtensionPoint("com.intellij.treeCopyHandler").not()) {
                rootArea.registerExtensionPoint(
                    "com.intellij.treeCopyHandler",
                    TreeCopyHandler::class.java.name,
                    ExtensionPoint.Kind.INTERFACE
                )
            }
            if (rootArea.hasExtensionPoint("com.intellij.codeStyleManager").not()) {
                rootArea.registerExtensionPoint(
                    "com.intellij.codeStyleManager",
                    CodeStyleManager::class.java.name,
                    ExtensionPoint.Kind.INTERFACE
                )
            }
            if (rootArea.hasExtensionPoint("com.intellij.psiElementFactory").not()) {
                rootArea.registerExtensionPoint(
                    "com.intellij.psiElementFactory",
                    PsiElementFactory::class.java.name,
                    ExtensionPoint.Kind.INTERFACE
                )
            }
            if (rootArea.hasExtensionPoint("com.intellij.lang.psiAugmentProvider").not()) {
                rootArea.registerExtensionPoint(
                    "com.intellij.lang.psiAugmentProvider",
                    PsiAugmentProvider::class.java.name,
                    ExtensionPoint.Kind.INTERFACE
                )
            }
            if (rootArea.hasExtensionPoint("com.intellij.psiElementFinder").not()) {
                rootArea.registerExtensionPoint(
                    "com.intellij.psiElementFinder",
                    PsiElementFinder::class.java.name,
                    ExtensionPoint.Kind.INTERFACE
                )
            }
        }
    }

    init {
        setIdeaIoUseFallback()
        setupIdeaStandaloneExecution()
//        registerExtensions(environment.project.extensionArea)
    }

    fun complete(source: String?, fileName: String?, index: Int): List<EditorCompletionItem> {
        environment.addJarToClassPath(FileUtil.classpathDir.resolve("android.jar"))
        val psiFile = fileFactory.createFileFromText(
            fileName!!,
            JavaLanguage.INSTANCE,
            source!!
        ) as PsiJavaFile

        val element = findElementAtOffset(psiFile, index) ?: return emptyList()
        val completionItems = mutableListOf<EditorCompletionItem>()

        // 1. ROBUST IMPORT STATEMENT CONTEXT DETECTION (Zero manual string slicing)
        var current: PsiElement? = element
        var importStatement: PsiImportStatement? = null
        while (current != null && current !is PsiFile) {
            if (current is PsiImportStatement) {
                importStatement = current
                break
            }
            current = current.parent
        }

        if (importStatement != null) {
            val reference = importStatement.importReference
            if (reference != null) {
                // Get the clean text straight from the PSI node itself
                var importText = reference.text.trim()
                if (importText.endsWith(".")) {
                    importText = importText.dropLast(1).trim()
                }

                if (importText.isNotEmpty()) {
                    // Find sub-packages (e.g., typing 'import java.' shows 'util', 'io', etc.)
                    symbolCacher.getPackages()
                        .filter { it.key.substringBeforeLast('.') == importText }
                        .forEach {
                            val toAdd = it.key.substringAfterLast('.')
                            completionItems.add(
                                EditorCompletionItem(toAdd, it.key, 0, toAdd).kind(
                                    CompletionItemKind.Module
                                )
                            )
                        }

                    // Find classes inside this package (e.g., typing 'import java.lang.' shows 'System', 'String')
                    symbolCacher.getClasses()
                        .filter {
                            it.key.startsWith("$importText.") && !it.key.substringAfter("$importText.")
                                .contains('.')
                        }
                        .forEach {
                            completionItems.add(
                                EditorCompletionItem(
                                    it.value.qualifiedName(),
                                    it.key,
                                    0,
                                    it.value.qualifiedName()
                                ).kind(CompletionItemKind.Class)
                            )
                        }
                    return completionItems
                }
            }
        }

        // 2. Regular Code Context (System. , Math. , target.)
        val referenceExpr = PsiTreeUtil.getParentOfType(element, PsiReferenceExpression::class.java)
            ?: (element.parent as? PsiReferenceExpression)

        if (referenceExpr != null) {
            val qualifier = referenceExpr.qualifierExpression
            if (qualifier != null) {
                val resolvedType = qualifier.type
                if (resolvedType != null && resolvedType != PsiType.NULL && resolvedType.getCanonicalText() != "null") {
                    // Instance variable completion (e.g., target.)
                    val ctClass = symbolCacher.getClass(resolvedType.getCanonicalText())
                    if (ctClass != null) {
                        addAllFieldAndMethods(ctClass, completionItems, isStatic = false)
                        return completionItems
                    }
                } else {
                    // Static class completion (e.g., System. , Math.)
                    val resolvedTarget = qualifier.reference?.resolve()
                    if (resolvedTarget is PsiClass) {
                        val qualifiedClassName = resolvedTarget.qualifiedName
                        if (qualifiedClassName != null) {
                            val ctClass = symbolCacher.getClass(qualifiedClassName)
                            if (ctClass != null) {
                                addAllFieldAndMethods(ctClass, completionItems, isStatic = true)
                                return completionItems
                            }
                        }
                    }
                }
            }
        }

        // 3. Global Fallback (Keywords and raw prefixes)
        val prefix = element.text.filter { it.isLetterOrDigit() || it == '_' }
        if (prefix.isNotEmpty()) {
            symbolCacher.filterClassNames(prefix).forEach { clazz ->
                completionItems.add(
                    EditorCompletionItem(
                        clazz.value,
                        clazz.key,
                        element.textLength,
                        clazz.value
                    ).kind(CompletionItemKind.Class)
                )
            }
            javaKeywords.filter { it.startsWith(prefix) }.forEach { keyword ->
                completionItems.add(
                    EditorCompletionItem(
                        keyword,
                        "Keyword",
                        element.textLength,
                        keyword
                    ).kind(CompletionItemKind.Keyword)
                )
            }
        }

        return completionItems
    }

    private fun addAllFieldAndMethods(
        ctClass: CtClass,
        completionItems: MutableList<EditorCompletionItem>,
        isStatic: Boolean = false
    ) {
        val fields = ctClass.fields
        for (field in fields) {
            if ((isStatic && Modifier.isStatic(field.modifiers)) || (!isStatic && Modifier.isPublic(
                    field.modifiers
                ) && !Modifier.isStatic(field.modifiers))
            ) {
                completionItems.add(
                    EditorCompletionItem(
                        field.name,
                        field.type.name.substringAfterLast('.'),
                        0,
                        field.name
                    )
                        .kind(CompletionItemKind.Field)
                )
            }
        }
        val methods = ctClass.methods
        for (method in methods) {
            if ((isStatic && Modifier.isStatic(method.modifiers)) || (!isStatic && Modifier.isPublic(
                    method.modifiers
                ) && !Modifier.isStatic(method.modifiers))
            ) {
                completionItems.add(
                    EditorCompletionItem(
                        method.name + method.parameterTypes.joinToString(
                            ", ",
                            "(",
                            ")"
                        ) { it.simpleName },
                        method.returnType.name.substringAfterLast('.'),
                        0,
                        method.name
                    ).kind(CompletionItemKind.Method)
                )
            }
        }
    }

    fun isImportStatementContext(element: PsiElement): Boolean {
        return element is PsiImportStatement ||
                element.parent is PsiImportList ||
                element.parent is PsiImportStaticStatement ||
                element.parent is PsiImportStaticReferenceElement ||
                element.parent is PsiImportStatement
    }

    fun formatCode(content: String): String {
        val psiFile = fileFactory.createFileFromText("temp.java", JavaLanguage.INSTANCE, content)
        formatCode(psiFile)
        return psiFile.text
    }

    fun formatCode(psiFile: PsiFile) {
        val codeStyleManager = CodeStyleManager.getInstance(environment.project)
        codeStyleManager.reformat(psiFile)
    }

    private fun findElementAtOffset(file: PsiFile, offset: Int): PsiElement? {
        var element = file.findElementAt(offset)
        // Secure bounds adjustment to ensure we catch the true leaf reference
        if (element is PsiWhiteSpace || element is PsiComment || (element is PsiJavaToken && element.tokenType == JavaTokenType.DOT)) {
            val safeOffset = if (offset - 1 >= 0) offset - 1 else 0
            element = file.findElementAt(safeOffset)
        }
        if (element is PsiJavaToken && element.tokenType == JavaTokenType.DOT) {
            element = element.parent
        }
        return element
    }

    private fun getFullyQualifiedPackageName(element: PsiElement): String? {
        if (element is PsiImportStatement) {
            val importReference = element.importReference
            if (importReference != null) {
                return importReference.qualifiedName
            }
        }
        return null
    }

    fun addImport(psiFile: PsiFile, importStatement: String): String {
        val typeSolver = CombinedTypeSolver()
        val config = ParserConfiguration().setSymbolResolver(JavaSymbolSolver(typeSolver))
        val parser = JavaParser(config)
        val parsed = parser.parse(psiFile.text)
        if (parsed.result.isPresent) {
            val cu = parsed.result.get()
            val imports = cu.imports
            imports.add(ImportDeclaration(importStatement, false, false))
            cu.setImports(imports)
            val printerConfiguration = DefaultPrinterConfiguration()
            return cu.toString(printerConfiguration)
        }
        return psiFile.text
    }
}
