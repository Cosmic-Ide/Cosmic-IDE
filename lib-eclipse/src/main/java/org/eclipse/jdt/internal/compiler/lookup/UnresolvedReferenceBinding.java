/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Stephan Herrmann - Contributions for
 *								bug 349326 - [1.7] new warning for missing try-with-resources
 *								bug 392384 - [1.8][compiler][null] Restore nullness info from type annotations in class files
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 467032 - TYPE_USE Null Annotations: IllegalStateException with annotated arrays of Enum when accessed via BinaryTypeBinding
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;

public class UnresolvedReferenceBinding extends ReferenceBinding {

ReferenceBinding resolvedType;
TypeBinding[] wrappers;
UnresolvedReferenceBinding prototype;

UnresolvedReferenceBinding(char[][] compoundName, PackageBinding packageBinding) {
	this.compoundName = compoundName;
	this.sourceName = compoundName[compoundName.length - 1]; // reasonable guess
	this.fPackage = packageBinding;
	this.wrappers = null;
	this.prototype = this;
	computeId();
}

public UnresolvedReferenceBinding(UnresolvedReferenceBinding prototype) {
	super(prototype);
	this.resolvedType = prototype.resolvedType;
	this.wrappers = null;
	this.prototype = prototype.prototype;
}

@Override
public TypeBinding clone(TypeBinding outerType) {
	if (this.resolvedType != null)
		return this.resolvedType.clone(outerType);
	UnresolvedReferenceBinding copy = new UnresolvedReferenceBinding(this);
	this.addWrapper(copy, null);
	return copy;
}

void addWrapper(TypeBinding wrapper, LookupEnvironment environment) {
	if (this.resolvedType != null) {
		// the type reference B<B<T>.M> means a signature of <T:Ljava/lang/Object;>LB<LB<TT;>.M;>;
		// when the ParameterizedType for Unresolved B is created with args B<T>.M, the Unresolved B is resolved before the wrapper is added
		wrapper.swapUnresolved(this, this.resolvedType, environment);
		return;
	}
	if (this.wrappers == null) {
		this.wrappers = new TypeBinding[] {wrapper};
	} else {
		int length = this.wrappers.length;
		System.arraycopy(this.wrappers, 0, this.wrappers = new TypeBinding[length + 1], 0, length);
		this.wrappers[length] = wrapper;
	}
}
@Override
public boolean isUnresolvedType() {
	return true;
}
@Override
public String debugName() {
	return toString();
}
@Override
public int depth() {
	// we don't yet have our enclosing types wired, but we know the nesting depth from our compoundName:
	// (NOTE: this an upper bound, because class names may contain '$')
	int last = this.compoundName.length-1;
	return CharOperation.occurencesOf('$', this.compoundName[last], 1); // leading '$' must be part of the class name, so start at 1.
}
@Override
public boolean hasTypeBit(int bit) {
	// shouldn't happen since we are not called before analyseCode(), but play safe:
	return false;
}

@Override
public TypeBinding prototype() {
	return this.prototype;
}

ReferenceBinding resolve(LookupEnvironment environment, boolean convertGenericToRawType) {
	ReferenceBinding targetType;
	if (this != this.prototype) { //$IDENTITY-COMPARISON$
		targetType = this.prototype.resolve(environment, convertGenericToRawType);
		if (convertGenericToRawType && targetType != null && targetType.isRawType()) {
			targetType = (ReferenceBinding) environment.createAnnotatedType(targetType, this.typeAnnotations);
		} else {
			targetType = this.resolvedType;
		}
		return targetType;
	}
	targetType = this.resolvedType;
	if (targetType == null) {
		char[] typeName = this.compoundName[this.compoundName.length - 1];
		targetType = this.fPackage.getType0(typeName);
		if (targetType == this || targetType == null) { //$IDENTITY-COMPARISON$
			if (this.fPackage instanceof SplitPackageBinding) // leverage SplitPackageBinding to avoid duplicate creation of BinaryTypeBinding
				targetType = environment.askForType(this.fPackage, typeName, this.fPackage.enclosingModule);
			else if (targetType == this) //$IDENTITY-COMPARISON$
				targetType = environment.askForType(this.compoundName, this.fPackage.enclosingModule);
		}
		if ((targetType == null || targetType == this) && CharOperation.contains('.', typeName)) { //$IDENTITY-COMPARISON$
			// bug 491354: this complements the NameLookup#seekTypes(..), which performs the same adaptation
			targetType = environment.askForType(this.fPackage, CharOperation.replaceOnCopy(typeName, '.', '$'), this.fPackage.enclosingModule);
		}
		if (targetType == null || targetType == this) { // could not resolve any better, error was already reported against it //$IDENTITY-COMPARISON$
			// report the missing class file first - only if not resolving a previously missing type
			if ((this.tagBits & TagBits.HasMissingType) == 0 && !environment.mayTolerateMissingType) {
				environment.problemReporter.isClassPathCorrect(
					this.compoundName,
					environment.root.unitBeingCompleted,
					environment.missingClassFileLocation, false);
			}
			// create a proxy for the missing BinaryType
			targetType = environment.createMissingType(null, this.compoundName);
		}
		setResolvedType(targetType, environment);
	}
	if (convertGenericToRawType) {
		targetType = (ReferenceBinding) environment.convertUnresolvedBinaryToRawType(targetType);
	}
	return targetType;
}
void setResolvedType(ReferenceBinding targetType, LookupEnvironment environment) {
	if (this.resolvedType == targetType) return; // already resolved //$IDENTITY-COMPARISON$

	// targetType may be a source or binary type
	this.resolvedType = targetType;
	environment.updateCaches(this, targetType);
	// must ensure to update any other type bindings that can contain the resolved type
	// otherwise we could create 2 : 1 for this unresolved type & 1 for the resolved type
	if (this.wrappers != null)
		for (int i = 0, l = this.wrappers.length; i < l; i++)
			this.wrappers[i].swapUnresolved(this, targetType, environment);
}

@Override
public void swapUnresolved(UnresolvedReferenceBinding unresolvedType, ReferenceBinding unannotatedType, LookupEnvironment environment) {
	if (this.resolvedType != null) return;
	ReferenceBinding annotatedType = (ReferenceBinding) unannotatedType.clone(null);
	this.resolvedType = annotatedType;
	annotatedType.setTypeAnnotations(getTypeAnnotations(), environment.globalOptions.isAnnotationBasedNullAnalysisEnabled);

	environment.updateCaches(this, annotatedType);
	if (this.wrappers != null)
		for (int i = 0, l = this.wrappers.length; i < l; i++)
			this.wrappers[i].swapUnresolved(this, annotatedType, environment);
}

@Override
public String toString() {
	if (this.hasTypeAnnotations())
		return super.annotatedDebugName() + "(unresolved)"; //$NON-NLS-1$
	return "Unresolved type " + ((this.compoundName != null) ? CharOperation.toString(this.compoundName) : "UNNAMED"); //$NON-NLS-1$ //$NON-NLS-2$
}
}
