/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.pranav.navigation

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod

object NavigationProvider {

    fun extractMethodsAndFields(javaFile: PsiClass, depth: Int = 0): List<NavigationItem> {
        var d = depth
        val navigationItems = mutableListOf<NavigationItem>()
        val name = buildString {
            append(javaFile.name)
            if (javaFile.superClass != null) {
                append(" : ")
                append(javaFile.superClass?.name)
            }
            if (javaFile.implementsList != null && javaFile.implementsList!!.referenceElements.isNotEmpty()) {
                append(" implements ")
                append(javaFile.implementsList?.referenceElements?.joinToString(", ") { it.text })
            }
        }
        val item = ClassNavigationKind(
            name,
            javaFile.modifierList!!.text,
            javaFile.textOffset,
            javaFile.textOffset + javaFile.textLength,
            d
        )
        if (d == 0) navigationItems.add(item)
        d++
        javaFile.children.forEach { child ->
            when (child) {
                is PsiMethod -> {
                    val modifiers = child.modifierList
                    val parameters = child.parameterList
                    val returnType = child.returnTypeElement?.text ?: "void"

                    val methodName = child.name + "(" + parameters.parameters.joinToString(", ") {
                        it.typeElement?.text ?: "void"
                    } + ") : $returnType"

                    val startPosition = child.textOffset
                    val endPosition = startPosition + child.textLength

                    val methodItem =
                        MethodNavigationItem(
                            methodName,
                            modifiers.text,
                            startPosition,
                            endPosition,
                            d
                        )
                    navigationItems.add(methodItem)
                }

                is PsiField -> {
                    val modifiers = child.modifierList ?: return@forEach
                    val startPosition = child.textOffset
                    val type = child.typeElement?.text ?: "void"
                    val fieldName = child.name + " : $type"

                    val fieldItem =
                        FieldNavigationItem(fieldName, modifiers.text, startPosition, depth = d)
                    navigationItems.add(fieldItem)
                }

                is PsiClass -> {
                    val modifiers = child.modifierList
                    val innerClassName = buildString {
                        append(javaFile.name)
                        if (javaFile.superClass != null) {
                            append(" : ")
                            append(javaFile.superClass?.name)
                        }
                        if (javaFile.implementsList != null && javaFile.implementsList!!.referenceElements.isNotEmpty()) {
                            append(" implements ")
                            append(javaFile.implementsList?.referenceElements?.joinToString(", ") { it.text })
                        }
                    }
                    val startPosition = child.textOffset
                    val endPosition = startPosition + child.textLength

                    val innerClassItem =
                        ClassNavigationKind(
                            innerClassName,
                            modifiers!!.text,
                            startPosition,
                            endPosition,
                            d
                        )
                    navigationItems.add(innerClassItem)
                    navigationItems.addAll(extractMethodsAndFields(child, d + 1))
                }
            }
        }

        """
            class Main {
                public void main() {}
            	public static void k(String[] args) {}
            	private class Test {
            	private String h = "hi";
            	public void j() {}
            	}
            }
        """.trimIndent()

        return navigationItems
    }

    interface NavigationItem {
        val name: String
        val modifiers: String
        val startPosition: Int
        val endPosition: Int
        val kind: NavigationItemKind
        val depth: Int
            get() = 0
    }

    data class MethodNavigationItem(
        override val name: String,
        override val modifiers: String,
        override val startPosition: Int,
        override val endPosition: Int,
        override val depth: Int,
        override val kind: NavigationItemKind = NavigationItemKind.METHOD,
    ) : NavigationItem

    data class FieldNavigationItem(
        override val name: String,
        override val modifiers: String,
        override val startPosition: Int,
        override val endPosition: Int = startPosition + name.length,
        override val depth: Int,
        override val kind: NavigationItemKind = NavigationItemKind.FIELD,
    ) : NavigationItem

    data class ClassNavigationKind(
        override val name: String,
        override val modifiers: String,
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
