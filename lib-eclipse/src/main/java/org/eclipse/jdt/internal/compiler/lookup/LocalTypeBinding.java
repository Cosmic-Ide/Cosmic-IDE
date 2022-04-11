/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *								bug 365662 - [compiler][null] warn on contradictory and redundant null annotations
 *								bug 401030 - [1.8][null] Null analysis support for lambda methods.
 *								Bug 429958 - [1.8][null] evaluate new DefaultLocation attribute of @NonNullByDefault
 *								Bug 435805 - [1.8][compiler][null] Java 8 compiler does not recognize declaration style null annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CaseStatement;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;

public final class LocalTypeBinding extends NestedTypeBinding {
	final static char[] LocalTypePrefix = { '$', 'L', 'o', 'c', 'a', 'l', '$' };

	private InnerEmulationDependency[] dependents;
	public CaseStatement enclosingCase; // from 1.4 on, local types should not be accessed across switch case blocks (52221)
	public int sourceStart; // used by computeUniqueKey to uniquely identify this binding
	public MethodBinding enclosingMethod;

public LocalTypeBinding(ClassScope scope, SourceTypeBinding enclosingType, CaseStatement switchCase) {
	super(
		new char[][] {CharOperation.concat(LocalTypeBinding.LocalTypePrefix, scope.referenceContext.name)},
		scope,
		enclosingType);
	TypeDeclaration typeDeclaration = scope.referenceContext;
	if ((typeDeclaration.bits & ASTNode.IsAnonymousType) != 0) {
		this.tagBits |= TagBits.AnonymousTypeMask;
	} else {
		this.tagBits |= TagBits.LocalTypeMask;
	}
	this.enclosingCase = switchCase;
	this.sourceStart = typeDeclaration.sourceStart;
	MethodScope methodScope = scope.enclosingMethodScope();
	MethodBinding methodBinding = methodScope.referenceMethodBinding();
	if (methodBinding != null) {
		this.enclosingMethod = methodBinding;
	}
	MethodScope lambdaScope = scope.enclosingLambdaScope();
	if (lambdaScope != null) {
		((LambdaExpression) lambdaScope.referenceContext).addLocalType(this);
	}
}

public LocalTypeBinding(LocalTypeBinding prototype) {
	super(prototype);
	this.dependents = prototype.dependents;
	this.enclosingCase = prototype.enclosingCase;
	this.sourceStart = prototype.sourceStart;
	this.enclosingMethod = prototype.enclosingMethod;
}

/* Record a dependency onto a source target type which may be altered
* by the end of the innerclass emulation. Later on, we will revisit
* all its dependents so as to update them (see updateInnerEmulationDependents()).
*/
public void addInnerEmulationDependent(BlockScope dependentScope, boolean wasEnclosingInstanceSupplied) {
	if (!isPrototype()) throw new IllegalStateException();
	int index;
	if (this.dependents == null) {
		index = 0;
		this.dependents = new InnerEmulationDependency[1];
	} else {
		index = this.dependents.length;
		for (int i = 0; i < index; i++)
			if (this.dependents[i].scope == dependentScope)
				return; // already stored
		System.arraycopy(this.dependents, 0, (this.dependents = new InnerEmulationDependency[index + 1]), 0, index);
	}
	this.dependents[index] = new InnerEmulationDependency(dependentScope, wasEnclosingInstanceSupplied);
	//  System.out.println("Adding dependency: "+ new String(scope.enclosingType().readableName()) + " --> " + new String(this.readableName()));
}

@Override
public MethodBinding enclosingMethod() {
	return this.enclosingMethod;
}

/*
 * Returns the anonymous original super type (in some error cases, superclass may get substituted with Object)
 */
public ReferenceBinding anonymousOriginalSuperType() {
	if (!isPrototype())
		return ((LocalTypeBinding) this.prototype).anonymousOriginalSuperType();
	if (this.superclass == null && this.scope != null)
		return this.scope.getJavaLangObject();

	if (this.superInterfaces != Binding.NO_SUPERINTERFACES) {
		return this.superInterfaces[0];
	}
	if ((this.tagBits & TagBits.HierarchyHasProblems) == 0) {
		return this.superclass;
	}
	if (this.scope != null) {
		TypeReference typeReference = this.scope.referenceContext.allocation.type;
		if (typeReference != null) {
			return (ReferenceBinding) typeReference.resolvedType;
		}
	}
	return this.superclass; // default answer
}

@Override
public char[] computeUniqueKey(boolean isLeaf) {
	if (!isPrototype())
		return this.prototype.computeUniqueKey(isLeaf);

	char[] outerKey = outermostEnclosingType().computeUniqueKey(isLeaf);
	int semicolon = CharOperation.lastIndexOf(';', outerKey);

	StringBuilder sig = new StringBuilder();
	sig.append(outerKey, 0, semicolon);

	// insert $sourceStart
	sig.append('$');
	sig.append(String.valueOf(this.sourceStart));

	// insert $LocalName if local
	if (!isAnonymousType()) {
		sig.append('$');
		sig.append(this.sourceName);
	}

	// insert remaining from outer key
	sig.append(outerKey, semicolon, outerKey.length-semicolon);

	int sigLength = sig.length();
	char[] uniqueKey = new char[sigLength];
	sig.getChars(0, sigLength, uniqueKey, 0);
	return uniqueKey;
}

@Override
public char[] constantPoolName() /* java/lang/Object */ {
	if (this.constantPoolName != null)
		return this.constantPoolName;
	if (!isPrototype())
		return this.constantPoolName = this.prototype.constantPoolName();
	if (this.constantPoolName == null && this.scope != null) {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=322154, we do have some
		// cases where the left hand does not know what the right is doing.
		this.constantPoolName = this.scope.compilationUnitScope().computeConstantPoolName(this);
	}
	return this.constantPoolName;
}

@Override
public TypeBinding clone(TypeBinding outerType) {
	LocalTypeBinding copy = new LocalTypeBinding(this);
	copy.enclosingType = (SourceTypeBinding) outerType;
	return copy;
}

@Override
public int hashCode() {
	return this.enclosingType.hashCode();
}
/*
 * Overriden for code assist. In this case, the constantPoolName() has not been computed yet.
 * Slam the source name so that the signature is syntactically correct.
 * (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=99686)
 */
@Override
public char[] genericTypeSignature() {

	if (!isPrototype())
		return this.prototype.genericTypeSignature();

	if (this.genericReferenceTypeSignature == null && this.constantPoolName == null) {
		if (isAnonymousType())
			setConstantPoolName(superclass().sourceName());
		else
			setConstantPoolName(sourceName());
	}
	return super.genericTypeSignature();
}

@Override
public char[] readableName() /*java.lang.Object,  p.X<T> */ {
    char[] readableName;
	if (isAnonymousType()) {
		readableName = CharOperation.concat(TypeConstants.ANONYM_PREFIX, anonymousOriginalSuperType().readableName(), TypeConstants.ANONYM_SUFFIX);
	} else if (isMemberType()) {
		readableName = CharOperation.concat(enclosingType().readableName(), this.sourceName, '.');
	} else {
		readableName = this.sourceName;
	}
	TypeVariableBinding[] typeVars;
	if ((typeVars = typeVariables()) != Binding.NO_TYPE_VARIABLES) {
	    StringBuilder nameBuffer = new StringBuilder(10);
	    nameBuffer.append(readableName).append('<');
	    for (int i = 0, length = typeVars.length; i < length; i++) {
	        if (i > 0) nameBuffer.append(',');
	        nameBuffer.append(typeVars[i].readableName());
	    }
	    nameBuffer.append('>');
	    int nameLength = nameBuffer.length();
		readableName = new char[nameLength];
		nameBuffer.getChars(0, nameLength, readableName, 0);
	}
	return readableName;
}

@Override
public char[] shortReadableName() /*Object*/ {
    char[] shortReadableName;
	if (isAnonymousType()) {
		shortReadableName = CharOperation.concat(TypeConstants.ANONYM_PREFIX, anonymousOriginalSuperType().shortReadableName(), TypeConstants.ANONYM_SUFFIX);
	} else if (isMemberType()) {
		shortReadableName = CharOperation.concat(enclosingType().shortReadableName(), this.sourceName, '.');
	} else {
		shortReadableName = this.sourceName;
	}
	TypeVariableBinding[] typeVars;
	if ((typeVars = typeVariables()) != Binding.NO_TYPE_VARIABLES) {
	    StringBuilder nameBuffer = new StringBuilder(10);
	    nameBuffer.append(shortReadableName).append('<');
	    for (int i = 0, length = typeVars.length; i < length; i++) {
	        if (i > 0) nameBuffer.append(',');
	        nameBuffer.append(typeVars[i].shortReadableName());
	    }
	    nameBuffer.append('>');
		int nameLength = nameBuffer.length();
		shortReadableName = new char[nameLength];
		nameBuffer.getChars(0, nameLength, shortReadableName, 0);
	}
	return shortReadableName;
}

// Record that the type is a local member type
public void setAsMemberType() {
	if (!isPrototype()) {
		this.tagBits |= TagBits.MemberTypeMask;
		((LocalTypeBinding) this.prototype).setAsMemberType();
		return;
	}
	this.tagBits |= TagBits.MemberTypeMask;
}

public void setConstantPoolName(char[] computedConstantPoolName) /* java/lang/Object */ {
	if (!isPrototype()) {
		this.constantPoolName = computedConstantPoolName;
		((LocalTypeBinding) this.prototype).setConstantPoolName(computedConstantPoolName);
		return;
	}
	this.constantPoolName = computedConstantPoolName;
}

public void transferConstantPoolNameTo(TypeBinding substType) {
	if (this.constantPoolName != null && substType instanceof LocalTypeBinding) {
		LocalTypeBinding substLocalType = (LocalTypeBinding) substType;
		if (substLocalType.constantPoolName == null) {
			substLocalType.setConstantPoolName(this.constantPoolName);
			this.scope.compilationUnitScope().constantPoolNameUsage.put(substLocalType.constantPoolName, substLocalType);
		}
	}
}

/*
 * Overriden for code assist. In this case, the constantPoolName() has not been computed yet.
 * Slam the source name so that the signature is syntactically correct.
 * (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=102284)
 */
@Override
public char[] signature() {

	if (!isPrototype())
		return this.prototype.signature();

	if (this.signature == null && this.constantPoolName == null) {
		if (isAnonymousType())
			setConstantPoolName(superclass().sourceName());
		else
			setConstantPoolName(sourceName());
	}
	return super.signature();
}

@Override
public char[] sourceName() {
	if (isAnonymousType()) {
		return CharOperation.concat(TypeConstants.ANONYM_PREFIX, anonymousOriginalSuperType().sourceName(), TypeConstants.ANONYM_SUFFIX);
	} else
		return this.sourceName;
}

@Override
public String toString() {
	if (this.hasTypeAnnotations())
		return annotatedDebugName() + " (local)"; //$NON-NLS-1$

	if (isAnonymousType())
		return "Anonymous type : " + super.toString(); //$NON-NLS-1$
	if (isMemberType())
		return "Local member type : " + new String(sourceName()) + " " + super.toString(); //$NON-NLS-2$ //$NON-NLS-1$
	return "Local type : " + new String(sourceName()) + " " + super.toString(); //$NON-NLS-2$ //$NON-NLS-1$
}

/* Trigger the dependency mechanism forcing the innerclass emulation
* to be propagated to all dependent source types.
*/
@Override
public void updateInnerEmulationDependents() {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.dependents != null) {
		for (int i = 0; i < this.dependents.length; i++) {
			InnerEmulationDependency dependency = this.dependents[i];
			// System.out.println("Updating " + new String(this.readableName()) + " --> " + new String(dependency.scope.enclosingType().readableName()));
			dependency.scope.propagateInnerEmulation(this, dependency.wasEnclosingInstanceSupplied);
		}
	}
}
}
