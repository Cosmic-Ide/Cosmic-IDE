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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.findLabelAndCall
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.DslMarkerUtils
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

fun LexicalScope.getImplicitReceiversWithInstance(excludeShadowedByDslMarkers: Boolean = false): Collection<ReceiverParameterDescriptor> =
    getImplicitReceiversWithInstanceToExpression(excludeShadowedByDslMarkers).keys

interface ReceiverExpressionFactory {
    val isImmediate: Boolean
    val expressionText: String
    fun createExpression(psiFactory: KtPsiFactory, shortThis: Boolean = true): KtExpression
}

fun LexicalScope.getFactoryForImplicitReceiverWithSubtypeOf(receiverType: KotlinType): ReceiverExpressionFactory? =
    getImplicitReceiversWithInstanceToExpression().entries.firstOrNull { (receiverDescriptor, _) ->
        receiverDescriptor.type.isSubtypeOf(receiverType)
    }?.value

fun LexicalScope.getImplicitReceiversWithInstanceToExpression(
    excludeShadowedByDslMarkers: Boolean = false
): Map<ReceiverParameterDescriptor, ReceiverExpressionFactory?> {
    val allReceivers = getImplicitReceiversHierarchy()
    // we use a set to workaround a bug with receiver for companion object present twice in the result of getImplicitReceiversHierarchy()
    val receivers = LinkedHashSet(
        if (excludeShadowedByDslMarkers) {
            allReceivers - allReceivers.shadowedByDslMarkers()
        } else {
            allReceivers
        }
    )

    val outerDeclarationsWithInstance = LinkedHashSet<DeclarationDescriptor>()
    var current: DeclarationDescriptor? = ownerDescriptor
    while (current != null) {
        if (current is PropertyAccessorDescriptor) {
            current = current.correspondingProperty
        }
        outerDeclarationsWithInstance.add(current)

        val classDescriptor = current as? ClassDescriptor
        if (classDescriptor != null && !classDescriptor.isInner && !DescriptorUtils.isLocal(classDescriptor)) break

        current = current.containingDeclaration
    }

    val result = LinkedHashMap<ReceiverParameterDescriptor, ReceiverExpressionFactory?>()
    for ((index, receiver) in receivers.withIndex()) {
        val owner = receiver.containingDeclaration
        if (owner is ScriptDescriptor) {
            result[receiver] = null
            outerDeclarationsWithInstance.addAll(owner.implicitReceivers)
            continue
        }
        val (expressionText, isImmediateThis) = if (owner in outerDeclarationsWithInstance) {
            val thisWithLabel = thisQualifierName(receiver)?.let { "this@${it.render()}" }
            if (index == 0)
                (thisWithLabel ?: "this") to true
            else
                thisWithLabel to false
        } else if (owner is ClassDescriptor && owner.kind.isSingleton) {
            IdeDescriptorRenderersScripting.SOURCE_CODE.renderClassifierName(owner) to false
        } else {
            continue
        }
        val factory = if (expressionText != null)
            object : ReceiverExpressionFactory {
                override val isImmediate = isImmediateThis
                override val expressionText: String get() = expressionText
                override fun createExpression(psiFactory: KtPsiFactory, shortThis: Boolean): KtExpression {
                    return psiFactory.createExpression(if (shortThis && isImmediateThis) "this" else expressionText)
                }
            }
        else
            null
        result[receiver] = factory
    }
    return result
}

private fun thisQualifierName(receiver: ReceiverParameterDescriptor): Name? {
    val descriptor = receiver.containingDeclaration
    val name = descriptor.name
    if (!name.isSpecial) return name

    val functionLiteral = DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? KtFunctionLiteral
    return functionLiteral?.findLabelAndCall()?.first
}

private fun List<ReceiverParameterDescriptor>.shadowedByDslMarkers(): Set<ReceiverParameterDescriptor> {
    val typesByDslScopes = mutableMapOf<FqName, MutableList<ReceiverParameterDescriptor>>()

    for (receiver in this) {
        val dslMarkers = DslMarkerUtils.extractDslMarkerFqNames(receiver.value).all()
        for (marker in dslMarkers) {
            typesByDslScopes.getOrPut(marker) { mutableListOf() } += receiver
        }
    }

    // for each DSL marker, all receivers except the closest one are shadowed by it; that is why we drop it
    return typesByDslScopes.values.flatMapTo(hashSetOf()) { it.drop(1) }
}
