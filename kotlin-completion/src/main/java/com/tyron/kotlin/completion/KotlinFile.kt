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
package com.tyron.kotlin.completion

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile

class KotlinFile(val name: String, val kotlinFile: KtFile) {

    fun elementAt(line: Int, character: Int): PsiElement? = kotlinFile.findElementAt(offsetFor(line, character))?.let { expressionFor(it) }

    fun insert(content: String, atLine: Int, atCharacter: Int): KotlinFile {
        val caretPositionOffset = offsetFor(atLine, atCharacter)
        return if (caretPositionOffset != 0) {
            from(kotlinFile.project, kotlinFile.name,
                content = StringBuilder(kotlinFile.text.substring(0, caretPositionOffset))
                    .append(content)
                    .append(kotlinFile.text.substring(caretPositionOffset)).toString()
            )
        } else this
    }

    private fun offsetFor(line: Int, character: Int) = (kotlinFile.viewProvider.document?.getLineStartOffset(line) ?: 0) + character

    private tailrec fun expressionFor(element: PsiElement): PsiElement =
        if (element is KtExpression) element else expressionFor(element.parent)

    companion object {
        fun from(project: Project, name: String, content: String) =
            KotlinFile(name, (PsiFileFactory.getInstance(project) as PsiFileFactoryImpl)
                .trySetupPsiForFile(
                    LightVirtualFile(
                        if (name.endsWith(".kt")) name else "$name.kt",
                        KotlinLanguage.INSTANCE,
                        content
                    ).apply { charset = CharsetToolkit.UTF8_CHARSET },
                    KotlinLanguage.INSTANCE, true, false
                ) as KtFile
            )
    }
}