package dev.pranav.navigation

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

object KtNavigationProvider {
    fun extractMethodsAndFields(psiElement: PsiElement, depth: Int = 0): List<NavigationItem> {
        val navigationItems = mutableListOf<NavigationItem>()

        when (psiElement) {
            is KtFile -> {
                psiElement.declarations.forEach { declaration ->
                    if (declaration is KtClass) {
                        navigationItems.addAll(extractMethodsAndFields(declaration, depth))
                    }
                }
            }

            is KtClass -> {
                val item = ClassNavigationKind(
                    psiElement.name ?: "",
                    psiElement.modifierList ?: return navigationItems,
                    psiElement.textOffset,
                    psiElement.textOffset + psiElement.textLength,
                    depth
                )
                navigationItems.add(item)

                psiElement.declarations.forEach { declaration ->
                    println(declaration.name)
                    when (declaration) {
                        is KtNamedFunction -> {
                            val methodName = declaration.name ?: ""
                            val modifiers = declaration.modifierList ?: return@forEach

                            val methodItem = MethodNavigationItem(
                                methodName,
                                modifiers,
                                declaration.textOffset,
                                declaration.textOffset + declaration.textLength,
                                depth
                            )
                            navigationItems.add(methodItem)
                        }

                        is KtProperty -> {
                            val propertyName = declaration.name ?: ""
                            val modifiers = declaration.modifierList ?: return@forEach

                            val propertyItem = FieldNavigationItem(
                                propertyName,
                                modifiers,
                                declaration.textOffset,
                                declaration.textOffset + declaration.textLength,
                                depth
                            )
                            navigationItems.add(propertyItem)
                        }

                        is KtClass -> {
                            navigationItems.addAll(extractMethodsAndFields(declaration, depth + 1))
                        }
                    }
                }
            }
        }

        return navigationItems
    }

    interface NavigationItem {
        val name: String
        val modifiers: KtModifierList
        val startPosition: Int
        val endPosition: Int
        val kind: NavigationItemKind
        val depth: Int
    }

    data class MethodNavigationItem(
        override val name: String,
        override val modifiers: KtModifierList,
        override val startPosition: Int,
        override val endPosition: Int,
        override val depth: Int,
        override val kind: NavigationItemKind = NavigationItemKind.METHOD,
    ) : NavigationItem

    data class FieldNavigationItem(
        override val name: String,
        override val modifiers: KtModifierList,
        override val startPosition: Int,
        override val endPosition: Int,
        override val depth: Int,
        override val kind: NavigationItemKind = NavigationItemKind.FIELD,
    ) : NavigationItem

    data class ClassNavigationKind(
        override val name: String,
        override val modifiers: KtModifierList,
        override val startPosition: Int,
        override val endPosition: Int,
        override val depth: Int,
        override val kind: NavigationItemKind = NavigationItemKind.CLASS,
    ) : NavigationItem

    enum class NavigationItemKind {
        METHOD,
        FIELD,
        CLASS,
    }

}
