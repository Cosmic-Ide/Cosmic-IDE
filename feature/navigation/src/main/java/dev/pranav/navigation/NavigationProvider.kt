/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.pranav.navigation

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList

object NavigationProvider {

    fun extractMethodsAndFields(javaFile: PsiClass, depth: Int = 0): List<NavigationItem> {
        var d = depth
        val navigationItems = mutableListOf<NavigationItem>()
        val item = ClassNavigationKind(
            javaFile.name!!,
            javaFile.modifierList!!,
            javaFile.textOffset,
            javaFile.textOffset + javaFile.textLength,
            d
        )
        if (d == 0) navigationItems.add(item)
        d++
        javaFile.children.forEach { child ->
            when (child) {
                is PsiMethod -> {
                    val methodName = child.name
                    val modifiers = child.modifierList
                    val startPosition = child.textOffset
                    val endPosition = startPosition + child.textLength

                    val methodItem =
                        MethodNavigationItem(methodName, modifiers, startPosition, endPosition, d)
                    navigationItems.add(methodItem)
                }

                is PsiField -> {
                    val fieldName = child.name
                    val modifiers = child.modifierList ?: return@forEach
                    val startPosition = child.textOffset

                    val fieldItem =
                        FieldNavigationItem(fieldName, modifiers, startPosition, depth = d)
                    navigationItems.add(fieldItem)
                }

                is PsiClass -> {
                    val innerClass = child.name
                    val modifiers = child.modifierList ?: return@forEach
                    val startPosition = child.textOffset
                    val endPosition = startPosition + child.textLength

                    val innerClassItem =
                        ClassNavigationKind(innerClass!!, modifiers, startPosition, endPosition, d)
                    navigationItems.add(innerClassItem)
                    navigationItems.addAll(extractMethodsAndFields(child, d + 1))
                }

                is PsiMember -> {
                    val fieldName = child.name
                    val modifiers = child.modifierList ?: return@forEach
                    val startPosition = child.textOffset

                    val fieldItem =
                        FieldNavigationItem(fieldName!!, modifiers, startPosition, depth = d)
                    navigationItems.add(fieldItem)
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
        val modifiers: PsiModifierList
        val startPosition: Int
        val endPosition: Int
        val kind: NavigationItemKind
        val depth: Int
            get() = 0
    }

    data class MethodNavigationItem(
        override val name: String,
        override val modifiers: PsiModifierList,
        override val startPosition: Int,
        override val endPosition: Int,
        override val depth: Int,
        override val kind: NavigationItemKind = NavigationItemKind.METHOD,
    ) : NavigationItem

    data class FieldNavigationItem(
        override val name: String,
        override val modifiers: PsiModifierList,
        override val startPosition: Int,
        override val endPosition: Int = startPosition + name.length,
        override val depth: Int,
        override val kind: NavigationItemKind = NavigationItemKind.FIELD,
    ) : NavigationItem

    data class ClassNavigationKind(
        override val name: String,
        override val modifiers: PsiModifierList,
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