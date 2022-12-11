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

    fun elementAt(line: Int, character: Int): PsiElement? {
        val offset = offsetFor(line, character)
        return kotlinFile.findElementAt(offset)?.let { expressionFor(it) }
    }

    fun insert(content: String, atLine: Int, atCharacter: Int): KotlinFile {
        val caretPositionOffset = offsetFor(atLine, atCharacter)
        val fileContent = kotlinFile.text.substring(0, caretPositionOffset) + content + kotlinFile.text.substring(caretPositionOffset)
        return if (caretPositionOffset != 0) {
            from(kotlinFile.project, kotlinFile.name, fileContent)
        } else this
    }

    private fun offsetFor(line: Int, character: Int): Int {
        val lineStartOffset = kotlinFile.viewProvider.document?.getLineStartOffset(line) ?: 0
        return lineStartOffset + character
    }

    private tailrec fun expressionFor(element: PsiElement): PsiElement =
        if (element is KtExpression) element else expressionFor(element.parent)

    companion object {
        private var instance: KotlinFile? = null

        fun from(project: Project, name: String, content: String): KotlinFile {
            if (instance == null) {
                instance = KotlinFile(name, (PsiFileFactory.getInstance(project) as PsiFileFactoryImpl)
                    .trySetupPsiForFile(
                        LightVirtualFile(
                            if (name.endsWith(".kt")) name else "$name.kt",
                            KotlinLanguage.INSTANCE,
                            content
                        ).apply { charset = CharsetToolkit.UTF8_CHARSET },
                        KotlinLanguage.INSTANCE, true, false
                    ) as KtFile
                )
            } else {
                instance!!.kotlinFile.viewProvider.document?.setText(content)
            }
            return instance!!
        }
    }
}
