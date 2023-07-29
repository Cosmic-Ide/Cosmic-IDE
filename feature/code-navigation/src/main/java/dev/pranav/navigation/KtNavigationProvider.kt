package dev.pranav.navigation

import dev.pranav.navigation.NavigationProvider.ClassNavigationKind
import dev.pranav.navigation.NavigationProvider.FieldNavigationItem
import dev.pranav.navigation.NavigationProvider.MethodNavigationItem
import dev.pranav.navigation.NavigationProvider.NavigationItem
import org.jetbrains.kotlin.descriptors.explicitParameters
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSuperTypeEntry
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.TopDownAnalysisContext

object KtNavigationProvider {

    fun parseAnalysisContext(context: TopDownAnalysisContext): List<NavigationItem> {
        val navigationItems = mutableListOf<NavigationItem>()

        for (file in context.allClasses) {
            println("Parsing ${file.name}")
            file.declaredCallableMembers.forEach { callableMember ->
                println("Parsing ${callableMember.name}, ${callableMember::class.java.name}")
                when (callableMember) {
                    is SimpleFunctionDescriptorImpl -> {
                        val returnType = callableMember.returnType.toString()

                        val name = buildString {
                            append(
                                "fun ${callableMember.name}(${
                                    callableMember.explicitParameters.toMutableList()
                                        .joinToString(", ") {
                                            it.name.asString() + ": " + it.type.toString()
                                        }
                                }"
                            )
                            append(": $returnType")
                        }
                        navigationItems.add(
                            MethodNavigationItem(
                                name,
                                callableMember.visibility.name,
                                0,
                                0,
                                0
                            )
                        )
                    }

                    is PropertyDescriptorImpl -> {
                        val type = callableMember.type.toString()
                        val name = callableMember.name.asString()
                        navigationItems.add(
                            FieldNavigationItem(
                                "$name: $type",
                                callableMember.visibility.name,
                                0,
                                0,
                                0
                            )
                        )
                    }
                }
            }
        }
        return navigationItems
    }

    fun parseKtFile(psiFile: KtFile, depth: Int = 0): List<NavigationItem> {
        var d = depth
        val navigationItems = mutableListOf<NavigationItem>()
        d++

        for (declaration in psiFile.declarations) {
            when (declaration) {
                is KtClass -> {
                    val superClass = declaration.superTypeListEntries
                        .filterIsInstance<KtSuperTypeEntry>()
                        .firstOrNull()
                        ?.typeAsUserType?.referencedName

                    val interfaces = declaration.superTypeListEntries
                        .filterIsInstance<KtSuperTypeEntry>()
                        .drop(1)
                        .mapNotNull { it.typeAsUserType?.referencedName }
                        .joinToString(", ")

                    val name = buildString {
                        append(declaration.name ?: "")
                        if (superClass != null) {
                            append(" : $superClass")
                        }
                        if (interfaces.isNotEmpty()) {
                            append(" -> $interfaces")
                        }
                    }

                    navigationItems.add(
                        ClassNavigationKind(
                            name,
                            declaration.modifierList?.text ?: "",
                            declaration.startOffset,
                            declaration.endOffset,
                            d
                        )
                    )
                    navigationItems.addAll(parseKotlinClass(declaration, d + 1))
                }

                is KtNamedFunction -> {
                    val returnType = declaration.typeReference?.text
                    val parameters = declaration.valueParameters.joinToString(", ") { parameter ->
                        "${parameter.name}: ${parameter.typeReference?.text ?: ""}"
                    }

                    val name = buildString {
                        append(declaration.name ?: return@buildString)
                        append("($parameters)")
                        if (returnType != null) {
                            append(": $returnType")
                        }
                    }
                    navigationItems.add(
                        MethodNavigationItem(
                            name,
                            declaration.modifierList?.text ?: "",
                            declaration.startOffset,
                            declaration.endOffset,
                            d
                        )
                    )
                }

                is KtProperty -> {
                    val type = declaration.typeReference?.text
                    val name = declaration.name
                    navigationItems.add(
                        FieldNavigationItem(
                            name + (if (type != null) ": $type" else ""),
                            declaration.modifierList?.text ?: "",
                            declaration.startOffset,
                            declaration.endOffset,
                            d
                        )
                    )
                }

                else -> {
                    /*
                    navigationItems.add(
                        object : NavigationItem {
                            override val name: String
                                get() = declaration.text
                            override val modifiers: String
                                get() = ""
                            override val startPosition: Int
                                get() = declaration.startOffset
                            override val endPosition: Int
                                get() = declaration.endOffset
                            override val kind: NavigationProvider.NavigationItemKind
                                get() = NavigationProvider.NavigationItemKind.METHOD
                            override val depth: Int
                                get() = d
                        }
                    )
                     */
                }
            }
        }

        return navigationItems
    }

    fun parseKotlinClass(psiFile: KtClass, depth: Int = 0): List<NavigationItem> {
        var d = depth
        val navigationItems = mutableListOf<NavigationItem>()
        d++

        for (declaration in psiFile.declarations) {
            when (declaration) {
                is KtClass -> {
                    val superClass = declaration.superTypeListEntries
                        .filterIsInstance<KtSuperTypeEntry>()
                        .firstOrNull()
                        ?.typeAsUserType?.referencedName

                    val interfaces = declaration.superTypeListEntries
                        .filterIsInstance<KtSuperTypeEntry>()
                        .drop(1)
                        .mapNotNull { it.typeAsUserType?.referencedName }
                        .joinToString(", ")

                    val name = buildString {
                        append(declaration.name ?: "")
                        if (superClass != null) {
                            append(" : $superClass")
                        }
                        if (interfaces.isNotEmpty()) {
                            append(" -> $interfaces")
                        }
                    }

                    navigationItems.add(
                        ClassNavigationKind(
                            name,
                            declaration.modifierList?.text ?: "",
                            declaration.startOffset,
                            declaration.endOffset,
                            d
                        )
                    )
                    navigationItems.addAll(parseKotlinClass(declaration, d + 1))
                }

                is KtNamedFunction -> {
                    val returnType = declaration.typeReference?.text
                    val parameters = declaration.valueParameters.joinToString(", ") { parameter ->
                        "${parameter.name}: ${parameter.typeReference?.text ?: ""}"
                    }

                    val name = buildString {
                        append(declaration.name ?: return@buildString)
                        append("($parameters)")
                        if (returnType != null) {
                            append(": $returnType")
                        }
                    }
                    navigationItems.add(
                        MethodNavigationItem(
                            name,
                            declaration.modifierList?.text ?: "",
                            declaration.startOffset,
                            declaration.endOffset,
                            d
                        )
                    )
                }

                is KtProperty -> {
                    val type = declaration.typeReference?.text
                    val name = declaration.name
                    navigationItems.add(
                        FieldNavigationItem(
                            name + (if (type != null) ": $type" else ""),
                            declaration.modifierList?.text ?: "",
                            declaration.startOffset,
                            declaration.endOffset,
                            d
                        )
                    )
                }

                else -> {
                    navigationItems.add(
                        object : NavigationItem {
                            override val name: String
                                get() = declaration.text
                            override val modifiers: String
                                get() = ""
                            override val startPosition: Int
                                get() = declaration.startOffset
                            override val endPosition: Int
                                get() = declaration.endOffset
                            override val kind: NavigationProvider.NavigationItemKind
                                get() = NavigationProvider.NavigationItemKind.METHOD
                            override val depth: Int
                                get() = d
                        }
                    )
                }
            }
        }

        return navigationItems
    }
}