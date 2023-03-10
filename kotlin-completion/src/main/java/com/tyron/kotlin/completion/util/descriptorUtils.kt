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
package com.tyron.kotlin.completion.util

import com.intellij.psi.PsiElement
import com.tyron.kotlin.completion.resolve.ResolutionFacade
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy

fun DeclarationDescriptorWithVisibility.isVisible(
    context: PsiElement,
    receiverExpression: KtExpression?,
    bindingContext: BindingContext,
    resolutionFacade: ResolutionFacade
): Boolean {
    val resolutionScope = context.getResolutionScope(bindingContext, resolutionFacade)
    val from = resolutionScope.ownerDescriptor
    return isVisible(from, receiverExpression, bindingContext, resolutionScope)
}

private fun DeclarationDescriptorWithVisibility.isVisible(
    from: DeclarationDescriptor,
    receiverExpression: KtExpression?,
    bindingContext: BindingContext? = null,
    resolutionScope: LexicalScope? = null
): Boolean {
    if (DescriptorVisibilities.isVisibleWithAnyReceiver(this, from, false)) return true

    if (bindingContext == null || resolutionScope == null) return false

    // for extension it makes no sense to check explicit receiver because we need dispatch receiver which is implicit in this case
    if (receiverExpression != null && !isExtension) {
        val receiverType = bindingContext.getType(receiverExpression) ?: return false
        val explicitReceiver =
            ExpressionReceiver.create(receiverExpression, receiverType, bindingContext)
        return DescriptorVisibilities.isVisible(explicitReceiver, this, from, false)
    } else {
        return resolutionScope.getImplicitReceiversHierarchy().any {
            DescriptorVisibilities.isVisible(it.value, this, from, false)
        }
    }
}

