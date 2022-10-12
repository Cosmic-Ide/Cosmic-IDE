package com.tyron.kotlin_completion.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

object PsiUtils {

    @JvmStatic
    fun findParent(element: PsiElement): KtSimpleNameExpression? {
        val parentWithSelf = element.parentsWithSelf
        val sequence = parentWithSelf.filterIsInstance(KtSimpleNameExpression::class.java)
        return sequence.firstOrNull()
    }
}