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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.nullability

fun <TCallable : CallableDescriptor> TCallable.substituteExtensionIfCallable(
    receiverTypes: Collection<KotlinType>,
    callType: CallType<*>
): Collection<TCallable> {
    if (!callType.descriptorKindFilter.accepts(this)) return emptyList()

    var types = receiverTypes.asSequence()
    if (callType == CallType.SAFE) {
        types = types.map { it.makeNotNullable() }
    }

    val extensionReceiverType = fuzzyExtensionReceiverType()!!
    val substitutors = types.mapNotNull {
        var substitutor = extensionReceiverType.checkIsSuperTypeOf(it)
        // check if we may fail due to receiver expression being nullable
        if (substitutor == null && it.nullability() == TypeNullability.NULLABLE && extensionReceiverType.nullability() == TypeNullability.NOT_NULL) {
            substitutor = extensionReceiverType.checkIsSuperTypeOf(it.makeNotNullable())
        }
        substitutor
    }
    return if (typeParameters.isEmpty()) { // optimization for non-generic callables
        if (substitutors.any()) listOf(this) else emptyList()
    } else {
        substitutors
            .mapNotNull { @Suppress("UNCHECKED_CAST") (substitute(it) as TCallable?) }
            .toList()
    }
}

fun ReceiverValue?.getThisReceiverOwner(bindingContext: BindingContext): DeclarationDescriptor? {
    return when (this) {
        is ExpressionReceiver -> {
            val thisRef = (KtPsiUtil.deparenthesize(this.expression) as? KtThisExpression)?.instanceReference ?: return null
            bindingContext[BindingContext.REFERENCE_TARGET, thisRef]
        }

        is ImplicitReceiver -> this.declarationDescriptor

        else -> null
    }
}

fun ReceiverValue?.getReceiverTargetDescriptor(bindingContext: BindingContext): DeclarationDescriptor? = when (this) {
    is ExpressionReceiver -> when (val expression = KtPsiUtil.deparenthesize(this.expression)) {
        is KtThisExpression -> expression.instanceReference
        is KtReferenceExpression -> expression
        else -> null
    }?.let { referenceExpression ->
        bindingContext[BindingContext.REFERENCE_TARGET, referenceExpression]
    }

    is ImplicitReceiver -> this.declarationDescriptor

    else -> null
}
