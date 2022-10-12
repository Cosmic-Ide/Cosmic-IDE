package com.tyron.kotlin_completion.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

object PsiUtils {

    @JvmStatic
    fun findParent(element: PsiElement, find: Class<Find>): Find {
        val parentWithSelf = element.parentsWithSelf
        val sequence = parentWithSelf.filterIsInstance(find)
        return sequence.firstOrNull()
    }
}