/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

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
@file:JvmName("FuzzyTypeUtils")

package com.tyron.kotlin.completion.util

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.CallHandle
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.types.DelegatedTypeSubstitution
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.error.ErrorUtils.containsErrorType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.nullability

fun CallableDescriptor.fuzzyExtensionReceiverType() =
    extensionReceiverParameter?.type?.toFuzzyType(typeParameters)

fun FuzzyType.nullability() = type.nullability()


fun KotlinType.toFuzzyType(freeParameters: Collection<TypeParameterDescriptor>) =
    FuzzyType(this, freeParameters)

class FuzzyType(
    val type: KotlinType,
    freeParameters: Collection<TypeParameterDescriptor>
) {
    val freeParameters: Set<TypeParameterDescriptor>

    init {
        if (freeParameters.isNotEmpty()) {
            // we allow to pass type parameters from another function with the same original in freeParameters
            val usedTypeParameters =
                HashSet<TypeParameterDescriptor>().apply { addUsedTypeParameters(type) }
            if (usedTypeParameters.isNotEmpty()) {
                val originalFreeParameters = freeParameters.map { it.toOriginal() }.toSet()
                this.freeParameters =
                    usedTypeParameters.filter { it.toOriginal() in originalFreeParameters }.toSet()
            } else {
                this.freeParameters = emptySet()
            }
        } else {
            this.freeParameters = emptySet()
        }
    }

    // Diagnostic for EA-109046
    @Suppress("USELESS_ELVIS")
    private fun TypeParameterDescriptor.toOriginal(): TypeParameterDescriptor {
        val callableDescriptor = containingDeclaration as? CallableMemberDescriptor ?: return this
        val original =
            callableDescriptor.original ?: error("original = null for $callableDescriptor")
        val typeParameters = original.typeParameters ?: error("typeParameters = null for $original")
        return typeParameters[index]
    }

    override fun equals(other: Any?) =
        other is FuzzyType && other.type == type && other.freeParameters == freeParameters

    override fun hashCode() = type.hashCode()

    private fun MutableSet<TypeParameterDescriptor>.addUsedTypeParameters(type: KotlinType) {
        val typeParameter = type.constructor.declarationDescriptor as? TypeParameterDescriptor
        if (typeParameter != null && add(typeParameter)) {
            typeParameter.upperBounds.forEach { addUsedTypeParameters(it) }
        }

        for (argument in type.arguments) {
            if (!argument.isStarProjection) { // otherwise we can fall into infinite recursion
                addUsedTypeParameters(argument.type)
            }
        }
    }

    private fun checkIsSuperTypeOf(otherType: FuzzyType): TypeSubstitutor? = matchedSubstitutor(
        otherType
    )

    fun checkIsSuperTypeOf(otherType: KotlinType): TypeSubstitutor? =
        checkIsSuperTypeOf(otherType.toFuzzyType(emptyList()))

    private fun matchedSubstitutor(otherType: FuzzyType): TypeSubstitutor? {
        if (type.isError) return null
        if (otherType.type.isError) return null

        fun KotlinType.checkInheritance(otherType: KotlinType): Boolean {
            return otherType.isSubtypeOf(this)
        }

        if (freeParameters.isEmpty() && otherType.freeParameters.isEmpty()) {
            return if (type.checkInheritance(otherType.type)) TypeSubstitutor.EMPTY else null
        }

        val builder = ConstraintSystemBuilderImpl()
        val typeVariableSubstitutor = builder.registerTypeVariables(
            CallHandle.NONE,
            freeParameters + otherType.freeParameters
        )

        val typeInSystem = typeVariableSubstitutor.substitute(type, Variance.INVARIANT)
        val otherTypeInSystem =
            typeVariableSubstitutor.substitute(otherType.type, Variance.INVARIANT)

        builder.addSubtypeConstraint(
            otherTypeInSystem,
            typeInSystem,
            ConstraintPositionKind.RECEIVER_POSITION.position()
        )

        builder.fixVariables()

        val constraintSystem = builder.build()

        if (constraintSystem.status.hasContradiction()) return null

        // currently ConstraintSystem return successful status in case there are problems with nullability
        // that's why we have to check subtyping manually
        val substitutor = constraintSystem.resultingSubstitutor
        val substitutedType = substitutor.substitute(type, Variance.INVARIANT) ?: return null
        if (substitutedType.isError) return TypeSubstitutor.EMPTY
        val otherSubstitutedType =
            substitutor.substitute(otherType.type, Variance.INVARIANT) ?: return null
        if (otherSubstitutedType.isError) return TypeSubstitutor.EMPTY
        if (!substitutedType.checkInheritance(otherSubstitutedType)) return null

        val substitutorToKeepCapturedTypes =
            object : DelegatedTypeSubstitution(substitutor.substitution) {
                override fun approximateCapturedTypes() = false
            }.buildSubstitutor()

        val substitutionMap: Map<TypeConstructor, TypeProjection> = constraintSystem.typeVariables
            .map { it.originalTypeParameter }
            .associateBy(
                keySelector = { it.typeConstructor },
                valueTransform = { descriptor ->
                    val typeProjection =
                        TypeProjectionImpl(Variance.INVARIANT, descriptor.defaultType)
                    val substitutedProjection =
                        substitutorToKeepCapturedTypes.substitute(typeProjection)
                    substitutedProjection?.takeUnless { containsErrorType(it.type) }
                        ?: typeProjection
                })
        return TypeConstructorSubstitution.createByConstructorsMap(
            substitutionMap,
            approximateCapturedTypes = true
        ).buildSubstitutor()
    }
}

