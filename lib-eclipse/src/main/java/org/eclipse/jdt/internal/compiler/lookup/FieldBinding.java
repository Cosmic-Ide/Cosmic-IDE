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
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - Contributions for
 *								bug 185682 - Increment/decrement operators mark local variables as read
 *								bug 331649 - [compiler][null] consider null annotations for fields
 *								Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *								Bug 447088 - [null] @Nullable on fully qualified field type is ignored
 *								Bug 435805 - [1.8][compiler][null] Java 8 compiler does not recognize declaration style null annotations
 *								Bug 458396 - NPE in CodeStream.invoke()
 *								Bug 446217 - [null] @NonNullByDefault in package-info.java causes bogus "null type safety" warning
 *     Till Brychcy - Contribution for
 *     						    bug 467094 - [1.8][null] TYPE_USE NullAnnotations of array contents are applied to field.
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;

public class FieldBinding extends VariableBinding {
	public ReferenceBinding declaringClass;
	public int compoundUseFlag = 0; // number or accesses via postIncrement or compoundAssignment

protected FieldBinding() {
	super(null, null, 0, null);
	// for creating problem field
}
public FieldBinding(char[] name, TypeBinding type, int modifiers, ReferenceBinding declaringClass, Constant constant) {
	super(name, type, modifiers, constant);
	this.declaringClass = declaringClass;
}
// special API used to change field declaring class for runtime visibility check
public FieldBinding(FieldBinding initialFieldBinding, ReferenceBinding declaringClass) {
	super(initialFieldBinding.name, initialFieldBinding.type, initialFieldBinding.modifiers, initialFieldBinding.constant());
	this.declaringClass = declaringClass;
	this.id = initialFieldBinding.id;
	setAnnotations(initialFieldBinding.getAnnotations(), false);
}
/* API
* Answer the receiver's binding type from Binding.BindingID.
*/
public FieldBinding(FieldDeclaration field, TypeBinding type, int modifiers, ReferenceBinding declaringClass) {
	this(field.name, type, modifiers, declaringClass, null);
	field.binding = this; // record binding in declaration
}

public final boolean canBeSeenBy(PackageBinding invocationPackage) {
	if (isPublic()) return true;
	if (isPrivate()) return false;

	// isProtected() or isDefault()
	return invocationPackage == this.declaringClass.getPackage();
}
/* Answer true if the receiver is visible to the type provided by the scope.
* InvocationSite implements isSuperAccess() to provide additional information
* if the receiver is protected.
*
* NOTE: Cannot invoke this method with a compilation unit scope.
*/

public final boolean canBeSeenBy(TypeBinding receiverType, InvocationSite invocationSite, Scope scope) {
	if (isPublic()) return true;

	SourceTypeBinding invocationType = scope.enclosingSourceType();
	if (TypeBinding.equalsEquals(invocationType, this.declaringClass) && TypeBinding.equalsEquals(invocationType, receiverType)) return true;

	if (invocationType == null) // static import call
		return !isPrivate() && scope.getCurrentPackage() == this.declaringClass.fPackage;

	if (isProtected()) {
		// answer true if the invocationType is the declaringClass or they are in the same package
		// OR the invocationType is a subclass of the declaringClass
		//    AND the receiverType is the invocationType or its subclass
		//    OR the method is a static method accessed directly through a type
		//    OR previous assertions are true for one of the enclosing type
		if (TypeBinding.equalsEquals(invocationType, this.declaringClass)) return true;
		if (invocationType.fPackage == this.declaringClass.fPackage) return true;

		ReferenceBinding currentType = invocationType;
		int depth = 0;
		ReferenceBinding receiverErasure = (ReferenceBinding)receiverType.erasure();
		ReferenceBinding declaringErasure = (ReferenceBinding) this.declaringClass.erasure();
		do {
			if (currentType.findSuperTypeOriginatingFrom(declaringErasure) != null) {
				if (invocationSite.isSuperAccess())
					return true;
				// receiverType can be an array binding in one case... see if you can change it
				if (receiverType instanceof ArrayBinding)
					return false;
				if (isStatic()) {
					if (depth > 0) invocationSite.setDepth(depth);
					return true; // see 1FMEPDL - return invocationSite.isTypeAccess();
				}
				if (TypeBinding.equalsEquals(currentType, receiverErasure) || receiverErasure.findSuperTypeOriginatingFrom(currentType) != null) {
					if (depth > 0) invocationSite.setDepth(depth);
					return true;
				}
			}
			depth++;
			currentType = currentType.enclosingType();
		} while (currentType != null);
		return false;
	}

	if (isPrivate()) {
		// answer true if the receiverType is the declaringClass
		// AND the invocationType and the declaringClass have a common enclosingType
		receiverCheck: {
			if (TypeBinding.notEquals(receiverType, this.declaringClass)) {
				// special tolerance for type variable direct bounds, but only if compliance <= 1.6, see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=334622
				if (scope.compilerOptions().complianceLevel <= ClassFileConstants.JDK1_6 && receiverType.isTypeVariable() && ((TypeVariableBinding) receiverType).isErasureBoundTo(this.declaringClass.erasure()))
					break receiverCheck;
				return false;
			}
		}

		if (TypeBinding.notEquals(invocationType, this.declaringClass)) {
			ReferenceBinding outerInvocationType = invocationType;
			ReferenceBinding temp = outerInvocationType.enclosingType();
			while (temp != null) {
				outerInvocationType = temp;
				temp = temp.enclosingType();
			}

			ReferenceBinding outerDeclaringClass = (ReferenceBinding) this.declaringClass.erasure();
			temp = outerDeclaringClass.enclosingType();
			while (temp != null) {
				outerDeclaringClass = temp;
				temp = temp.enclosingType();
			}
			if (TypeBinding.notEquals(outerInvocationType, outerDeclaringClass)) return false;
		}
		return true;
	}

	// isDefault()
	PackageBinding declaringPackage = this.declaringClass.fPackage;
	if (invocationType.fPackage != declaringPackage) return false;

	// receiverType can be an array binding in one case... see if you can change it
	if (receiverType instanceof ArrayBinding)
		return false;
	TypeBinding originalDeclaringClass = this.declaringClass.original();
	ReferenceBinding currentType = (ReferenceBinding) receiverType;
	do {
		if (currentType.isCapture()) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=285002
			if (TypeBinding.equalsEquals(originalDeclaringClass, currentType.erasure().original())) return true;
		} else {
			if (TypeBinding.equalsEquals(originalDeclaringClass, currentType.original())) return true;
		}
		PackageBinding currentPackage = currentType.fPackage;
		// package could be null for wildcards/intersection types, ignore and recurse in superclass
		if (currentPackage != null && currentPackage != declaringPackage) return false;
	} while ((currentType = currentType.superclass()) != null);
	return false;
}

/*
 * declaringUniqueKey dot fieldName ) returnTypeUniqueKey
 * p.X { X<T> x} --> Lp/X;.x)p/X<TT;>;
 */
@Override
public char[] computeUniqueKey(boolean isLeaf) {
	// declaring key
	char[] declaringKey =
		this.declaringClass == null /*case of length field for an array*/
			? CharOperation.NO_CHAR
			: this.declaringClass.computeUniqueKey(false/*not a leaf*/);
	int declaringLength = declaringKey.length;

	// name
	int nameLength = this.name.length;

	// return type
	char[] returnTypeKey = this.type == null ? new char[] {'V'} : this.type.computeUniqueKey(false/*not a leaf*/);
	int returnTypeLength = returnTypeKey.length;

	char[] uniqueKey = new char[declaringLength + 1 + nameLength + 1 + returnTypeLength];
	int index = 0;
	System.arraycopy(declaringKey, 0, uniqueKey, index, declaringLength);
	index += declaringLength;
	uniqueKey[index++] = '.';
	System.arraycopy(this.name, 0, uniqueKey, index, nameLength);
	index += nameLength;
	uniqueKey[index++] = ')';
	System.arraycopy(returnTypeKey, 0, uniqueKey, index, returnTypeLength);
	return uniqueKey;
}
@Override
public Constant constant() {
	Constant fieldConstant = this.constant;
	if (fieldConstant == null) {
		if (isFinal()) {
			//The field has not been yet type checked.
			//It also means that the field is not coming from a class that
			//has already been compiled. It can only be from a class within
			//compilation units to process. Thus the field is NOT from a BinaryTypeBinbing
			FieldBinding originalField = original();
			if (originalField.declaringClass instanceof SourceTypeBinding) {
				SourceTypeBinding sourceType = (SourceTypeBinding) originalField.declaringClass;
				if (sourceType.scope != null) {
					TypeDeclaration typeDecl = sourceType.scope.referenceContext;
					FieldDeclaration fieldDecl = typeDecl.declarationOf(originalField);
					MethodScope initScope = originalField.isStatic() ? typeDecl.staticInitializerScope : typeDecl.initializerScope;
					boolean old = initScope.insideTypeAnnotation;
					try {
						initScope.insideTypeAnnotation = false;
						fieldDecl.resolve(initScope); //side effect on binding
					} finally {
						initScope.insideTypeAnnotation = old;
					}
					fieldConstant = originalField.constant == null ? Constant.NotAConstant : originalField.constant;
				} else {
					fieldConstant = Constant.NotAConstant; // shouldn't occur per construction (paranoid null check)
				}
			} else {
				fieldConstant = Constant.NotAConstant; // shouldn't occur per construction (paranoid null check)
			}
		} else {
			fieldConstant = Constant.NotAConstant;
		}
		this.constant = fieldConstant;
	}
	return fieldConstant;
}

@Override
public Constant constant(Scope scope) {
	if (this.constant != null)
		return this.constant;
	ProblemReporter problemReporter = scope.problemReporter();
	IErrorHandlingPolicy suspendedPolicy = problemReporter.suspendTempErrorHandlingPolicy();
	try {
		return constant();
	} finally {
		problemReporter.resumeTempErrorHandlingPolicy(suspendedPolicy);
	}
}

public void fillInDefaultNonNullness(FieldDeclaration sourceField, Scope scope) {
	if (this.type == null || this.type.isBaseType())
		return;
	LookupEnvironment environment = scope.environment();
	if (environment.usesNullTypeAnnotations()) {
		if (!this.type.acceptsNonNullDefault())
			return;
		if ( (this.type.tagBits & TagBits.AnnotationNullMASK) == 0) {
			this.type = environment.createAnnotatedType(this.type, new AnnotationBinding[]{environment.getNonNullAnnotation()});
		} else if ((this.type.tagBits & TagBits.AnnotationNonNull) != 0) {
			scope.problemReporter().nullAnnotationIsRedundant(sourceField);
		}
	} else {
		if ( (this.tagBits & TagBits.AnnotationNullMASK) == 0 ) {
			this.tagBits |= TagBits.AnnotationNonNull;
		} else if ((this.tagBits & TagBits.AnnotationNonNull) != 0) {
			scope.problemReporter().nullAnnotationIsRedundant(sourceField);
		}
	}
}

/**
 * X<T> t   -->  LX<TT;>;
 */
public char[] genericSignature() {
    if ((this.modifiers & ExtraCompilerModifiers.AccGenericSignature) == 0) return null;
    return this.type.genericTypeSignature();
}
public final int getAccessFlags() {
	return this.modifiers & ExtraCompilerModifiers.AccJustFlag;
}

@Override
public AnnotationBinding[] getAnnotations() {
	FieldBinding originalField = original();
	ReferenceBinding declaringClassBinding = originalField.declaringClass;
	if (declaringClassBinding == null) {
		return Binding.NO_ANNOTATIONS;
	}
	return declaringClassBinding.retrieveAnnotations(originalField);
}

/**
 * Compute the tagbits for standard annotations. For source types, these could require
 * lazily resolving corresponding annotation nodes, in case of forward references.
 * @see org.eclipse.jdt.internal.compiler.lookup.Binding#getAnnotationTagBits()
 */
@Override
public long getAnnotationTagBits() {
	FieldBinding originalField = original();
	if ((originalField.tagBits & TagBits.AnnotationResolved) == 0 && originalField.declaringClass instanceof SourceTypeBinding) {
		ClassScope scope = ((SourceTypeBinding) originalField.declaringClass).scope;
		if (scope == null) { // synthetic fields do not have a scope nor any annotations
			this.tagBits |= (TagBits.AnnotationResolved | TagBits.DeprecatedAnnotationResolved);
			return 0;
		}
		TypeDeclaration typeDecl = scope.referenceContext;
		FieldDeclaration fieldDecl = typeDecl.declarationOf(originalField);
		if (fieldDecl != null) {
			MethodScope initializationScope = isStatic() ? typeDecl.staticInitializerScope : typeDecl.initializerScope;
			FieldBinding previousField = initializationScope.initializedField;
			int previousFieldID = initializationScope.lastVisibleFieldID;
			try {
				initializationScope.initializedField = originalField;
				initializationScope.lastVisibleFieldID = originalField.id;
				ASTNode.resolveAnnotations(initializationScope, fieldDecl.annotations, originalField);
			} finally {
				initializationScope.initializedField = previousField;
				initializationScope.lastVisibleFieldID = previousFieldID;
			}
		}
	}
	return originalField.tagBits;
}

public final boolean isDefault() {
	return !isPublic() && !isProtected() && !isPrivate();
}
/* Answer true if the receiver is a deprecated field
*/

/* Answer true if the receiver has default visibility
*/

public final boolean isDeprecated() {
	return (this.modifiers & ClassFileConstants.AccDeprecated) != 0;
}
/* Answer true if the receiver has private visibility
*/

public final boolean isPrivate() {
	return (this.modifiers & ClassFileConstants.AccPrivate) != 0;
}
/* Answer true if the receiver has private visibility or is enclosed by a class that does.
*/

public final boolean isOrEnclosedByPrivateType() {
	if ((this.modifiers & ClassFileConstants.AccPrivate) != 0)
		return true;
	return this.declaringClass != null && this.declaringClass.isOrEnclosedByPrivateType();
}
/* Answer true if the receiver has private visibility and is used locally
*/

public final boolean isProtected() {
	return (this.modifiers & ClassFileConstants.AccProtected) != 0;
}
/* Answer true if the receiver has public visibility
*/

public final boolean isPublic() {
	return (this.modifiers & ClassFileConstants.AccPublic) != 0;
}
/* Answer true if the receiver is a static field
*/

public final boolean isStatic() {
	return (this.modifiers & ClassFileConstants.AccStatic) != 0;
}
/* Answer true if the receiver is not defined in the source of the declaringClass
*/

public final boolean isSynthetic() {
	return (this.modifiers & ClassFileConstants.AccSynthetic) != 0;
}
/* Answer true if the receiver is a transient field
*/

public final boolean isTransient() {
	return (this.modifiers & ClassFileConstants.AccTransient) != 0;
}
/* Answer true if the receiver's declaring type is deprecated (or any of its enclosing types)
*/

public final boolean isUsed() {
	return (this.modifiers & ExtraCompilerModifiers.AccLocallyUsed) != 0 || this.compoundUseFlag > 0;
}
/* Answer true if the only use of this field is in compound assignment or post increment
 */

public final boolean isUsedOnlyInCompound() {
	return (this.modifiers & ExtraCompilerModifiers.AccLocallyUsed) == 0 && this.compoundUseFlag > 0;
}
/* Answer true if the receiver has protected visibility
*/

public final boolean isViewedAsDeprecated() {
	return (this.modifiers & (ClassFileConstants.AccDeprecated | ExtraCompilerModifiers.AccDeprecatedImplicitly)) != 0;
}
/* Answer true if the receiver is a volatile field
*/

@Override
public final boolean isVolatile() {
	return (this.modifiers & ClassFileConstants.AccVolatile) != 0;
}

@Override
public final int kind() {
	return FIELD;
}
public boolean isRecordComponent() {
	return this.declaringClass != null && this.declaringClass.isRecord() && !this.isStatic()
			&& (this.modifiers & ExtraCompilerModifiers.AccRecord) != 0;
}
/* Answer true if the receiver is visible to the invocationPackage.
*/
/**
 * Returns the original field (as opposed to parameterized instances)
 */
public FieldBinding original() {
	return this;
}
@Override
public void setAnnotations(AnnotationBinding[] annotations, boolean forceStore) {
	this.declaringClass.storeAnnotations(this, annotations, forceStore);
}
public FieldDeclaration sourceField() {
	SourceTypeBinding sourceType;
	try {
		sourceType = (SourceTypeBinding) this.declaringClass;
	} catch (ClassCastException e) {
		return null;
	}

	FieldDeclaration[] fields = sourceType.scope.referenceContext.fields;
	if (fields != null) {
		for (int i = fields.length; --i >= 0;)
			if (this == fields[i].binding)
				return fields[i];
	}
	return null;
}
}
