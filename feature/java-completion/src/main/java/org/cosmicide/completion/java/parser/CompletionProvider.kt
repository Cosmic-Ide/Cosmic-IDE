/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

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
import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl
import com.intellij.openapi.vfs.impl.VirtualFileManagerImpl
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiExpressionStatement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiImportList
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiImportStaticReferenceElement
import com.intellij.psi.PsiImportStaticStatement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiJavaToken
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.tree.TreeCopyHandler
import com.intellij.psi.util.PsiTreeUtil
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import javassist.CtClass
import javassist.Modifier
import org.cosmicide.completion.java.parser.cache.SymbolCacher
import org.cosmicide.completion.java.parser.cache.qualifiedName
import org.cosmicide.rewrite.editor.EditorCompletionItem
import org.cosmicide.rewrite.util.FileUtil
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.jvm.compiler.setupIdeaStandaloneExecution
import java.util.logging.Logger

// TODO: cleanup, pretty ugly for now
class CompletionProvider {


    companion object {
        val logger = Logger.getLogger(CompletionProvider::javaClass.name)

        val environment =
            JavaCoreProjectEnvironment({ logger.info("JavaCoreProjectEnvironment disposed") },
                JavaCoreApplicationEnvironment { logger.info("JavaCoreApplicationEnvironment disposed") })
        val symbolCacher by lazy {
            SymbolCacher(FileUtil.classpathDir.resolve("android.jar")).apply {
                loadClassesFromJar()
            }
        }

        val fileFactory by lazy {
            PsiFileFactory.getInstance(environment.project)
        }

        val javaKeywords = arrayOf(
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
        registerExtensions(environment.project.extensionArea)
    }


    private fun getElementType(psiElement: PsiElement): String {
        return psiElement.javaClass.simpleName
    }

    private fun getFullyQualifiedName(referenceExpression: PsiReferenceExpression): String? {
        val referenceNameElement = referenceExpression.referenceNameElement
        if (referenceNameElement != null) {
            val resolvedElement = referenceNameElement.reference!!.resolve()
            if (resolvedElement is PsiField) {
                val containingClass = resolvedElement.containingClass
                if (containingClass != null) {
                    return containingClass.qualifiedName
                }
            }
        }
        return null
    }

    fun complete(source: String?, fileName: String?, index: Int): List<EditorCompletionItem> {
        environment.addJarToClassPath(FileUtil.classpathDir.resolve("android.jar"))
        val psiFile = fileFactory.createFileFromText(fileName!!, JavaLanguage.INSTANCE, source!!)

        // Find the element at the specified position
        val element = findElementAtOffset(psiFile, index)
        println("Element: $element")
        if (element == null) {
            return emptyList()
        }

        val completionItems = mutableListOf<EditorCompletionItem>()
        println("Prefix: ${element.text}")

        println("Element is ${getElementType(element)}")
        val isImported = isImportedClass(element)
        println("Imported already ${isImported.first}")
        if (isImportStatementContext(element)) {
            println("Import statement context")
            val packageName =
                getFullyQualifiedPackageName(element)!! + if (element.text.endsWith(".")) "." else ""
            println("Package name: $packageName")
            if (packageName.endsWith('.')) {
                val mPackage = packageName.substringBeforeLast('.')
                symbolCacher.getPackages()
                    .filter {
                        val parentPkgName = it.key.substringBeforeLast('.')
                        println("match : $parentPkgName")
                        val matches = parentPkgName == mPackage
                        println("Matches: $matches, package: ${it.key}")
                        matches
                    }
                    .map {
                        val toAdd = it.key.substringAfterLast('.')
                        completionItems.add(
                            EditorCompletionItem(
                                toAdd,
                                it.key.substringBeforeLast('.'),
                                0,
                                toAdd
                            ).kind(CompletionItemKind.Module)
                        )
                    }
            }
            symbolCacher.getClasses()
                .filter {
                    val bool = it.key.startsWith(packageName) && it.key.substringAfter(packageName)
                        .contains('.').not()
                    println("condition: $bool, class: ${it.key}")
                    bool
                }.map {
                    completionItems.add(
                        completionItems.lastIndex + 1,
                        EditorCompletionItem(
                            it.value.qualifiedName(),
                            it.key.substringBeforeLast('.'),
                            0,
                            it.value.qualifiedName()
                        ).kind(CompletionItemKind.Class)
                    )
                }
            return completionItems
        }

        if (element.text.endsWith('.')) {
            if (element.text.first().isUpperCase()) {
                if (element.text.count { it == '.' } > 1) {
                    // probably something like System.out.
                    if (element is PsiReferenceExpression) {
                        val className = element.text.substringBefore('.')
                        println("Class name: $className")
                        // check if it is imported
                        val qualified =
                            if (isImported.first) isImported.second else "java.lang.$className"
                        println("Qualified: $qualified")

                        val ctClass = symbolCacher.getClass(qualified)
                        if (ctClass == null) {
                            println("Class not found '$qualified' with element ${element.text}")
                            return completionItems
                        }

                        val field = element.text.substringAfter("$className.").substringBefore('.')
                        if (element.text.endsWith(").")) {
                            val methodName =
                                element.text.substringAfter("$className.").substringBefore('(')
                            val params =
                                element.text.substringAfter("$className.").substringAfter('(')
                                    .substringBefore(')')
                            ctClass.methods.find { it.name == methodName }?.let {
                                println("Method found: $it")
                                // for now, we don't check the parameters
                                addAllFieldAndMethods(it.returnType, completionItems)
                            }
                        }
                        ctClass.fields.find { it.name == field }?.let {
                            println("Field found: $it")
                            addAllFieldAndMethods(it.type, completionItems)
                        }
                    }
                }
                val className = element.text.substring(0, element.text.length - 1)
                // check if it is imported
                val qualified = if (isImported.first) isImported.second else "java.lang.$className"
                println("prob static, qualified: $qualified")

                if (qualified.isEmpty()) {
                    val ctClass = symbolCacher.getClass("java.lang.$className")
                    if (ctClass == null) {
                        println("Class not found '$className' with element ${element.text}")
                        return completionItems
                    }

                    addAllFieldAndMethods(ctClass, completionItems, true)
                    return completionItems
                }
                val clazz = symbolCacher.getClass(qualified)
                if (clazz == null) {
                    println("Class not found '$className' with element ${element.text}")
                    return completionItems
                }
                addAllFieldAndMethods(clazz, completionItems, true)
            } else {
                val packageName = element.text.substringBeforeLast('.')
                if (packageName.isEmpty()) {
                    return completionItems
                }
                if (packageName.lowercase() == packageName) {
                    // probably something like java.lang.System
                    val clazz = symbolCacher.getClass(packageName)
                    if (clazz == null) {
                        println("Class not found '$packageName' with element ${element.text}")
                        return completionItems
                    }
                    addAllFieldAndMethods(clazz, completionItems)
                }
            }
            return completionItems
        }

        val items = symbolCacher.filterClassNames(element.text)
        println("Items: $items")
        for (clazz in items) {
            val item = EditorCompletionItem(
                clazz.value,
                clazz.key,
                element.textLength,
                clazz.value
            ).kind(CompletionItemKind.Class)
            val qualifiedName = clazz.key + "." + clazz.value
            println("Qualified name: $qualifiedName")
            if (!isImportedClass(psiFile, qualifiedName)) {
                println("Not imported")
                /*item.setOnComplete {
                    it.replace(0, it.length, addImport(psiFile, qualifiedName))
                    println("Added import statement")
                }*/
            }
            completionItems.add(item)
        }

        for (keyword in javaKeywords) {
            if (keyword.startsWith(element.text)) {
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
            if (Modifier.isStatic(field.modifiers) || (isStatic.not() && Modifier.isPublic(field.modifiers))) {
                completionItems.add(
                    EditorCompletionItem(
                        field.name,
                        field.type.name.substringAfterLast('.'),
                        0,
                        field.name
                    ).kind(CompletionItemKind.Field)
                )
            }
        }
        val methods = ctClass.methods
        for (method in methods) {
            // if modifier is public/static, then add it
            if ((isStatic.not() && Modifier.isPublic(method.modifiers)) || Modifier.isStatic(method.modifiers)) {
                completionItems.add(
                    EditorCompletionItem(
                        method.name + method.parameterTypes.joinToString(", ", "(", ")") {
                            it.simpleName
                        },
                        method.returnType.name.substringAfterLast('.'),
                        0,
                        method.name
                    ).kind(CompletionItemKind.Method)
                )
            }
        }
    }


    private fun isImportedClass(element: PsiElement): Pair<Boolean, String> {
        val psiFile = element.containingFile
        if (psiFile is PsiJavaFile) {
            val importList = psiFile.importList
            if (importList != null) {
                val importStatements = importList.importStatements
                val className = element.text
                for (importStatement in importStatements) {
                    val importedClassName = importStatement.qualifiedName
                    println("Imported class: $importedClassName")
                    if (importedClassName?.substringAfterLast('.') == className.substringBefore('.')) {
                        return Pair(true, importedClassName)
                    }
                }
            }
        }
        return Pair(false, "")
    }

    private fun isImportedClass(psiFile: PsiFile, className: String): Boolean {
        if (psiFile is PsiJavaFile) {
            val importList = psiFile.importList
            if (importList != null) {
                val importStatements = importList.importStatements
                for (importStatement in importStatements) {
                    val importedClassName = importStatement.qualifiedName
                    if (importedClassName?.substringAfterLast('.') == className) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun getQualifiedNameIfImported(psiFile: PsiFile, clazz: String): String {
        if (psiFile is PsiJavaFile) {
            val importList = psiFile.importList
            if (importList != null) {
                val importStatements = importList.importStatements
                for (importStatement in importStatements) {
                    val importedClassName = importStatement.qualifiedName
                    if (importedClassName?.substringAfterLast('.') == clazz) {
                        return importedClassName
                    }
                }
            }
        }
        return ""
    }

    fun isImportStatementContext(element: PsiElement): Boolean {
        return element is PsiImportStatement || element.parent is PsiImportList || element.parent is PsiImportStaticStatement || element.parent is PsiImportStaticReferenceElement || element.parent is PsiImportStatement
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

    private fun getTypeOfExpression(element: PsiElement): PsiType? {
        var type: PsiType? = null

        // Find the parent expression statement or variable declaration
        val parentExpression = PsiTreeUtil.getParentOfType(
            element,
            PsiExpressionStatement::class.java,
            PsiVariable::class.java
        )
        if (parentExpression != null) {
            if (parentExpression is PsiExpressionStatement) {
                val expression = parentExpression.expression
                if (expression is PsiAssignmentExpression) {
                    // Handle assignment expressions, e.g., "variable = value;"
                    val leftOperand = expression.lExpression
                    if (leftOperand is PsiReferenceExpression) {
                        val resolvedElement = leftOperand.resolve()
                        if (resolvedElement is PsiField) {
                            type = resolvedElement.type
                        } else if (resolvedElement is PsiVariable) {
                            type = resolvedElement.type
                        }
                    }
                } else if (expression is PsiMethodCallExpression) {
                    // Handle method call expressions, e.g., "method().field"
                    val referenceExpression = expression.methodExpression
                    val resolvedElement = referenceExpression.resolve()
                    if (resolvedElement is PsiField) {
                        type = resolvedElement.type
                    }
                }
            } else if (parentExpression is PsiVariable) {
                // Handle variable declarations, e.g., "Type variable = value;"
                type = parentExpression.type
            }
        }
        return type
    }

    private fun findElementAtOffset(file: PsiFile, offset: Int): PsiElement? {
        var element = file.findElementAt(offset)
        if (element is PsiWhiteSpace || element is PsiComment) {
            element = file.findElementAt(offset - 1)
        }
        if (element is PsiJavaToken && element.tokenType == JavaTokenType.DOT) {
            element = element.getParent()
        }
        return element
    }

    private fun getFullyQualifiedPackageName(element: PsiElement): String? {
        if (element is PsiImportStatement) {
            val importReference = element.importReference
            if (importReference != null) {
                importReference.qualifiedName?.let {
                    println("Imported package: $it")
                    return it
                }
            }
        }
        println("Not an import statement")
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
        } else {
            println("Failed to parse")
            println(parsed.problems)
        }
        return psiFile.text

    }
}
