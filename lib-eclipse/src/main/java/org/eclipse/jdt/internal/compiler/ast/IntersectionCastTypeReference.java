/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contribution for
 *							Bug 429958 - [1.8][null] evaluate new DefaultLocation attribute of @NonNullByDefault
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 409236 - [1.8][compiler] Type annotations on intersection cast types dropped by code generator
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.IntersectionTypeBinding18;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

@SuppressWarnings({"rawtypes"})
public class IntersectionCastTypeReference extends TypeReference {
	public TypeReference[] typeReferences;

	public IntersectionCastTypeReference(TypeReference[] typeReferences) {
		this.typeReferences = typeReferences;
		this.sourceStart = typeReferences[0].sourceStart;
		int length = typeReferences.length;
		this.sourceEnd = typeReferences[length - 1].sourceEnd;
		for (int i = 0, max = typeReferences.length; i < max; i++) {
			if ((typeReferences[i].bits & ASTNode.HasTypeAnnotations) != 0) {
				this.bits |= ASTNode.HasTypeAnnotations;
				break;
			}
		}
	}

	@Override
	public TypeReference augmentTypeWithAdditionalDimensions(int additionalDimensions, Annotation[][] additionalAnnotations, boolean isVarargs) {
		throw new UnsupportedOperationException(); // no syntax for this.
	}

	@Override
	public char[] getLastToken() {
		return null;
	}

	/**
	 * @see org.eclipse.jdt.internal.compiler.ast.ArrayQualifiedTypeReference#getTypeBinding(org.eclipse.jdt.internal.compiler.lookup.Scope)
	 */
	@Override
	protected TypeBinding getTypeBinding(Scope scope) {
		return null; // not supported here - combined with resolveType(...)
	}

	@Override
	public TypeReference[] getTypeReferences() {
		return this.typeReferences;
	}

	@Override
	public TypeBinding resolveType(BlockScope scope, boolean checkBounds, int location) {

		int length = this.typeReferences.length;
		ReferenceBinding[] intersectingTypes = new ReferenceBinding[length];
		boolean hasError = false;

		int typeCount = 0;
		nextType:
		for (int i = 0; i < length; i++) {
			final TypeReference typeReference = this.typeReferences[i];
			TypeBinding type = typeReference.resolveType(scope, checkBounds, location);
			if (type == null || ((type.tagBits & TagBits.HasMissingType) != 0)) {
				hasError = true;
				continue;
			}
			if (i == 0) {
				if (type.isBaseType()) { // rejected in grammar for i > 0
					scope.problemReporter().onlyReferenceTypesInIntersectionCast(typeReference);
					hasError = true;
					continue;
				}
				if (type.isArrayType()) { // javac rejects the pedantic cast: (X[] & Serializable & Cloneable) new X[0], what is good for the goose ...
					scope.problemReporter().illegalArrayTypeInIntersectionCast(typeReference);
					hasError = true;
					continue;
				}
			} else if (!type.isInterface()) {
				scope.problemReporter().boundMustBeAnInterface(typeReference, type);
				hasError = true;
				continue;
			}
			for (int j = 0; j < typeCount; j++) {
				final ReferenceBinding priorType = intersectingTypes[j];
				if (TypeBinding.equalsEquals(priorType, type)) {
					scope.problemReporter().duplicateBoundInIntersectionCast(typeReference);
					hasError = true;
					continue;
				}
				if (!priorType.isInterface())
					continue;
				if (TypeBinding.equalsEquals(type.findSuperTypeOriginatingFrom(priorType), priorType)) {
					intersectingTypes[j] = (ReferenceBinding) type;
					continue nextType;
				}
				if (TypeBinding.equalsEquals(priorType.findSuperTypeOriginatingFrom(type), type))
					continue nextType;
			}
			intersectingTypes[typeCount++] = (ReferenceBinding) type;
		}

		if (hasError) {
			return null;
		}
		if (typeCount != length) {
			if (typeCount == 1) {
				return this.resolvedType = intersectingTypes[0];
			}
			System.arraycopy(intersectingTypes, 0, intersectingTypes = new ReferenceBinding[typeCount], 0, typeCount);
		}
		IntersectionTypeBinding18 intersectionType = (IntersectionTypeBinding18) scope.environment().createIntersectionType18(intersectingTypes);
		// check for parameterized interface collisions (when different parameterizations occur)
		ReferenceBinding itsSuperclass = null;
		ReferenceBinding[] interfaces = intersectingTypes;
		ReferenceBinding firstType = intersectingTypes[0];
		if (firstType.isClass()) {
			itsSuperclass = firstType.superclass();
			System.arraycopy(intersectingTypes, 1, interfaces = new ReferenceBinding[typeCount - 1], 0, typeCount - 1);
		}

		Map invocations = new HashMap(2);
		nextInterface: for (int i = 0, interfaceCount = interfaces.length; i < interfaceCount; i++) {
			ReferenceBinding one = interfaces[i];
			if (one == null) continue nextInterface;
			if (itsSuperclass != null && scope.hasErasedCandidatesCollisions(itsSuperclass, one, invocations, intersectionType, this))
				continue nextInterface;
			nextOtherInterface: for (int j = 0; j < i; j++) {
				ReferenceBinding two = interfaces[j];
				if (two == null) continue nextOtherInterface;
				if (scope.hasErasedCandidatesCollisions(one, two, invocations, intersectionType, this))
					continue nextInterface;
			}
		}
		if ((intersectionType.tagBits & TagBits.HierarchyHasProblems) != 0)
			return null;

		return (this.resolvedType = intersectionType);
	}

	@Override
	public char[][] getTypeName() {
		// we need to keep a return value that is a char[][]
		return this.typeReferences[0].getTypeName();
	}

	@Override
	public void traverse(ASTVisitor visitor, BlockScope scope) {
		if (visitor.visit(this, scope)) {
			int length = this.typeReferences == null ? 0 : this.typeReferences.length;
			for (int i = 0; i < length; i++) {
				this.typeReferences[i].traverse(visitor, scope);
			}
		}
		visitor.endVisit(this, scope);
	}

	@Override
	public void traverse(ASTVisitor visitor, ClassScope scope) {
		throw new UnsupportedOperationException("Unexpected traversal request: IntersectionTypeReference in class scope"); //$NON-NLS-1$
	}

	@Override
	public StringBuffer printExpression(int indent, StringBuffer output) {
		int length = this.typeReferences == null ? 0 : this.typeReferences.length;
		printIndent(indent, output);
		for (int i = 0; i < length; i++) {
			this.typeReferences[i].printExpression(0, output);
			if (i != length - 1) {
				output.append(" & "); //$NON-NLS-1$
			}
		}
		return output;
	}
}
