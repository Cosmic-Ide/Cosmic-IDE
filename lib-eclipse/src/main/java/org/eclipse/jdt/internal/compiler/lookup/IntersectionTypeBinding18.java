/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *							Bug 423504 - [1.8] Implement "18.5.3 Functional Interface Parameterization Inference"
 *							Bug 426676 - [1.8][compiler] Wrong generic method type inferred from lambda expression
 *							Bug 426542 - [1.8] Most specific method not picked when one method has intersection type as type parameter
 *							Bug 428019 - [1.8][compiler] Type inference failure with nested generic invocation.
 *     Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 405104 - [1.8][compiler][codegen] Implement support for serializeable lambdas
 *     Jesper S Møller - Contributions for bug 381345 : [1.8] Take care of the Java 8 major version
 *                          Bug 527554 - [18.3] Compiler support for JEP 286 Local-Variable Type
 *******************************************************************************/

package org.eclipse.jdt.internal.compiler.lookup;

import java.util.Set;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;

/**
 * Abstraction used for intersection casts in Java 8 + and inferred types:
 * <ul>
 * <li>type inference at 1.8+</li>
 * <li>lub at 1.8+</li>
 * <li>projections for 'var' at 10+</li>
 * </ul>
 */
public class IntersectionTypeBinding18 extends ReferenceBinding {

	public ReferenceBinding [] intersectingTypes;
	private ReferenceBinding javaLangObject;
	int length;

	public IntersectionTypeBinding18(ReferenceBinding[] intersectingTypes, LookupEnvironment environment) {
		this.intersectingTypes = intersectingTypes;
		this.length = intersectingTypes.length;
		if (!intersectingTypes[0].isClass()) {
			this.javaLangObject = environment.getResolvedJavaBaseType(TypeConstants.JAVA_LANG_OBJECT, null);
			this.modifiers |= ClassFileConstants.AccInterface;
		}
	}

	private IntersectionTypeBinding18(IntersectionTypeBinding18 prototype) {
		this.intersectingTypes = prototype.intersectingTypes;
		this.length = prototype.length;
		if (!this.intersectingTypes[0].isClass()) {
			this.javaLangObject = prototype.javaLangObject;
			this.modifiers |= ClassFileConstants.AccInterface;
		}
	}

	@Override
	public TypeBinding clone(TypeBinding enclosingType) {
		return new IntersectionTypeBinding18(this);
	}

	@Override
	protected MethodBinding[] getInterfaceAbstractContracts(Scope scope, boolean replaceWildcards, boolean filterDefaultMethods) throws InvalidInputException {
		int typesLength = this.intersectingTypes.length;
		MethodBinding[][] methods = new MethodBinding[typesLength][];
		int contractsLength = 0;
		for (int i = 0; i < typesLength; i++) {
			methods[i] = this.intersectingTypes[i].getInterfaceAbstractContracts(scope, replaceWildcards, true);
			contractsLength += methods[i].length;
		}
		MethodBinding[] contracts = new MethodBinding[contractsLength];
		int idx = 0;
		for (int i = 0; i < typesLength; i++) {
			int len = methods[i].length;
			System.arraycopy(methods[i], 0, contracts, idx, len);
			idx += len;
		}
		return contracts;
	}

	@Override
	public boolean hasTypeBit(int bit) { // Stephan ??
		for (int i = 0; i < this.length; i++) {
			if (this.intersectingTypes[i].hasTypeBit(bit))
				return true;
		}
		return false;
	}

	@Override
	public boolean canBeInstantiated() {
		return false;
	}

	@Override
	public boolean canBeSeenBy(PackageBinding invocationPackage) {
		for (int i = 0; i < this.length; i++) {
			if (!this.intersectingTypes[i].canBeSeenBy(invocationPackage))
				return false;
		}
		return true;
	}

	@Override
	public boolean canBeSeenBy(Scope scope) {
		for (int i = 0; i < this.length; i++) {
			if (!this.intersectingTypes[i].canBeSeenBy(scope))
				return false;
		}
		return true;
	}

	@Override
	public boolean canBeSeenBy(ReferenceBinding receiverType, ReferenceBinding invocationType) {
		for (int i = 0; i < this.length; i++) {
			if (!this.intersectingTypes[i].canBeSeenBy(receiverType, invocationType))
				return false;
		}
		return true;
	}


	@Override
	public char[] constantPoolName() {
		TypeBinding erasure = erasure();
		if (erasure != this) //$IDENTITY-COMPARISON$
			return erasure.constantPoolName();
		if (this.intersectingTypes[0].id == TypeIds.T_JavaLangObject && this.intersectingTypes.length > 1)
			return this.intersectingTypes[1].constantPoolName(); // improve stack map
		return this.intersectingTypes[0].constantPoolName();
	}

	@Override
	public PackageBinding getPackage() {
		throw new UnsupportedOperationException(); // cannot be referred to
	}

	@Override
	public ReferenceBinding[] getIntersectingTypes() {
		return this.intersectingTypes;
	}

	@Override
	public ReferenceBinding superclass() {
		return this.intersectingTypes[0].isClass() ? this.intersectingTypes[0] : this.javaLangObject;
	}

	@Override
	public ReferenceBinding [] superInterfaces() {
		if (this.intersectingTypes[0].isClass()) {
			ReferenceBinding [] superInterfaces = new ReferenceBinding[this.length - 1];
			System.arraycopy(this.intersectingTypes, 1, superInterfaces, 0, this.length - 1);
			return superInterfaces;
		}
		return this.intersectingTypes;
	}

	@Override
	public boolean isBoxedPrimitiveType() {
		return this.intersectingTypes[0].isBoxedPrimitiveType();
	}

	/* Answer true if the receiver type can be assigned to the argument type (right)
	 */
	@Override
	public boolean isCompatibleWith(TypeBinding right, Scope scope) {

		// easy way out?
		if (TypeBinding.equalsEquals(this, right))
			return true;

		// need to compare two intersection types?
		int rightKind = right.kind();
		TypeBinding[] rightIntersectingTypes = null;
		if (rightKind == INTERSECTION_TYPE && right.boundKind() == Wildcard.EXTENDS) {
			TypeBinding allRightBounds = ((WildcardBinding) right).allBounds();
			if (allRightBounds instanceof IntersectionTypeBinding18)
				rightIntersectingTypes = ((IntersectionTypeBinding18) allRightBounds).intersectingTypes;
		} else if (rightKind == INTERSECTION_TYPE18) {
			rightIntersectingTypes = ((IntersectionTypeBinding18) right).intersectingTypes;
		}
		if (rightIntersectingTypes != null) {
			nextRequired:
			for (TypeBinding required : rightIntersectingTypes) {
				for (TypeBinding provided : this.intersectingTypes) {
					if (provided.isCompatibleWith(required, scope))
						continue nextRequired;
				}
				return false;
			}
			return true;
		}

		// normal case:
		for (int i = 0; i < this.length; i++) {
			if (this.intersectingTypes[i].isCompatibleWith(right, scope))
				return true;
		}
		return false;
	}

	@Override
	public boolean isSubtypeOf(TypeBinding other, boolean simulatingBugJDK8026527) {
		if (TypeBinding.equalsEquals(this, other))
			return true;
		if (other instanceof ReferenceBinding) {
			TypeBinding[] rightIntersectingTypes = ((ReferenceBinding) other).getIntersectingTypes();
			if (rightIntersectingTypes != null && rightIntersectingTypes.length > 1) {
				int numRequired = rightIntersectingTypes.length;
				TypeBinding[] required = new TypeBinding[numRequired];
				System.arraycopy(rightIntersectingTypes, 0, required, 0, numRequired);
				for (int i = 0; i < this.length; i++) {
					TypeBinding provided = this.intersectingTypes[i];
					for (int j = 0; j < required.length; j++) {
						if (required[j] == null) continue;
						if (provided.isSubtypeOf(required[j], simulatingBugJDK8026527)) {
							required[j] = null;
							if (--numRequired == 0)
								return true;
						}
					}
				}
				return false;
			}
		}
		for (int i = 0; i < this.intersectingTypes.length; i++) {
			if (this.intersectingTypes[i].isSubtypeOf(other, false))
				return true;
		}
		return false;
	}

	@Override
	public TypeBinding erasure() {
		int classIdx = -1;
		for (int i = 0; i < this.intersectingTypes.length; i++) {
			if (this.intersectingTypes[i].isClass() && this.intersectingTypes[i].id != TypeIds.T_JavaLangObject) { // ignore j.l.Object to improve stack map
				if (classIdx == -1) {
					classIdx = i;
				} else {
					classIdx = Integer.MAX_VALUE;
					break;
				}
			}
		}
		if (classIdx > -1 && classIdx < Integer.MAX_VALUE)
			return this.intersectingTypes[classIdx].erasure();
		return this;
	}

	@Override
	public char[] qualifiedSourceName() {
		StringBuilder qualifiedSourceName = new StringBuilder(16);
		for (int i = 0; i < this.length; i++) {
				qualifiedSourceName.append(this.intersectingTypes[i].qualifiedSourceName());
				if (i != this.length - 1)
					qualifiedSourceName.append(" & "); //$NON-NLS-1$
		}
		return qualifiedSourceName.toString().toCharArray();
	}

	@Override
	public char[] sourceName() {
		StringBuilder srcName = new StringBuilder(16);
		for (int i = 0; i < this.length; i++) {
				srcName.append(this.intersectingTypes[i].sourceName());
				if (i != this.length - 1)
					srcName.append(" & "); //$NON-NLS-1$
		}
		return srcName.toString().toCharArray();
	}

	@Override
	public char[] readableName() {
		StringBuilder readableName = new StringBuilder(16);
		for (int i = 0; i < this.length; i++) {
				readableName.append(this.intersectingTypes[i].readableName());
				if (i != this.length - 1)
					readableName.append(" & "); //$NON-NLS-1$
		}
		return readableName.toString().toCharArray();
	}
	@Override
	public char[] shortReadableName() {
		StringBuilder shortReadableName = new StringBuilder(16);
		for (int i = 0; i < this.length; i++) {
				shortReadableName.append(this.intersectingTypes[i].shortReadableName());
				if (i != this.length - 1)
					shortReadableName.append(" & "); //$NON-NLS-1$
		}
		return shortReadableName.toString().toCharArray();
	}
	@Override
	public boolean isIntersectionType18() {
		return true;
	}
	@Override
	public int kind() {
		return Binding.INTERSECTION_TYPE18;
	}
	@Override
	public String debugName() {
		StringBuilder debugName = new StringBuilder(16);
		for (int i = 0; i < this.length; i++) {
				debugName.append(this.intersectingTypes[i].debugName());
				if (i != this.length - 1)
					debugName.append(" & "); //$NON-NLS-1$
		}
		return debugName.toString();
	}
	@Override
	public String toString() {
	    return debugName();
	}

	public TypeBinding getSAMType(Scope scope) {
		for (int i = 0, max = this.intersectingTypes.length; i < max; i++) {
			TypeBinding typeBinding = this.intersectingTypes[i];
			MethodBinding methodBinding = typeBinding.getSingleAbstractMethod(scope, true);
			// Why doesn't getSingleAbstractMethod do as the javadoc says, and return null
			// when it is not a SAM type
			if (methodBinding != null && methodBinding.problemId() != ProblemReasons.NoSuchSingleAbstractMethod) {
				return typeBinding; // answer the first SAM we find
			}
		}
		return null;
	}

	@Override
	void collectInferenceVariables(Set<InferenceVariable> variables) {
		for (int i = 0; i < this.intersectingTypes.length; i++)
			this.intersectingTypes[i].collectInferenceVariables(variables);
	}

	@Override
	public ReferenceBinding upwardsProjection(Scope scope, TypeBinding[] mentionedTypeVariables) {
		ReferenceBinding[] projectedTypes = new ReferenceBinding[this.intersectingTypes.length];
		for (int i = 0; i < this.intersectingTypes.length; ++i) {
			projectedTypes[i] =  this.intersectingTypes[i].upwardsProjection(scope, mentionedTypeVariables);
		}
		return (ReferenceBinding) scope.environment().createIntersectionType18(projectedTypes);
	}

	@Override
	public ReferenceBinding downwardsProjection(Scope scope, TypeBinding[] mentionedTypeVariables) {
		ReferenceBinding[] projectedTypes = new ReferenceBinding[this.intersectingTypes.length];
		for (int i = 0; i < this.intersectingTypes.length; ++i) {
			projectedTypes[i] = this.intersectingTypes[i].downwardsProjection(scope, mentionedTypeVariables);
		}
		return (ReferenceBinding) scope.environment().createIntersectionType18(projectedTypes);
	}

	@Override
	public boolean mentionsAny(TypeBinding[] parameters, int idx) {
		if (super.mentionsAny(parameters, idx))
			return true;
		for (int i = 0; i < this.intersectingTypes.length; i++) {
			if (this.intersectingTypes[i].mentionsAny(parameters, -1))
				return true;
		}
		return false;
	}
	@Override
	public long updateTagBits() {
		for (TypeBinding intersectingType : this.intersectingTypes)
			this.tagBits |= intersectingType.updateTagBits();
		return super.updateTagBits();
	}
}
