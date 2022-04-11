/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *								bug 328281 - visibility leaks not detected when analyzing unused field in private class
 *								bug 349326 - [1.7] new warning for missing try-with-resources
 *								bug 186342 - [compiler][null] Using annotations for null checking
 *								bug 365836 - [compiler][null] Incomplete propagation of null defaults.
 *								bug 365519 - editorial cleanup after bug 186342 and bug 365387
 *								bug 365662 - [compiler][null] warn on contradictory and redundant null annotations
 *								bug 365531 - [compiler][null] investigate alternative strategy for internally encoding nullness defaults
 *								bug 366063 - Compiler should not add synthetic @NonNull annotations
 *								bug 384663 - Package Based Annotation Compilation Error in JDT 3.8/4.2 (works in 3.7.2)
 *								bug 386356 - Type mismatch error with annotations and generics
 *								bug 388281 - [compiler][null] inheritance of null annotations as an option
 *								bug 331649 - [compiler][null] consider null annotations for fields
 *								bug 380896 - [compiler][null] Enum constants not recognised as being NonNull.
 *								bug 391376 - [1.8] check interaction of default methods with bridge methods and generics
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 415043 - [1.8][null] Follow-up re null type annotations after bug 392099
 *								Bug 392238 - [1.8][compiler][null] Detect semantically invalid null type annotations
 *								Bug 415850 - [1.8] Ensure RunJDTCoreTests can cope with null annotations enabled
 *								Bug 416172 - [1.8][compiler][null] null type annotation not evaluated on method return type
 *								Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *								Bug 426048 - [1.8] NPE in TypeVariableBinding.internalBoundCheck when parentheses are not balanced
 *								Bug 392238 - [1.8][compiler][null] Detect semantically invalid null type annotations
 *								Bug 429958 - [1.8][null] evaluate new DefaultLocation attribute of @NonNullByDefault
 *								Bug 432348 - [1.8] Internal compiler error (NPE) after upgrade to 1.8
 *								Bug 438458 - [1.8][null] clean up handling of null type annotations wrt type variables
 *								Bug 435570 - [1.8][null] @NonNullByDefault illegally tries to affect "throws E"
 *								Bug 441693 - [1.8][null] Bogus warning for type argument annotated with @NonNull
 *								Bug 435805 - [1.8][compiler][null] Java 8 compiler does not recognize declaration style null annotations
 *								Bug 457210 - [1.8][compiler][null] Wrong Nullness errors given on full build build but not on incremental build?
 *								Bug 461250 - ArrayIndexOutOfBoundsException in SourceTypeBinding.fields
 *								Bug 466713 - Null Annotations: NullPointerException using <int @Nullable []> as Type Param
 *      Jesper S Moller <jesper@selskabet.org> -  Contributions for
 *								Bug 412153 - [1.8][compiler] Check validity of annotations which may be repeatable
 *      Till Brychcy - Contributions for
 *     							bug 415269 - NonNullByDefault is not always inherited to nested classes
 *      Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          	Bug 405104 - [1.8][compiler][codegen] Implement support for serializeable lambdas
 *      Sebastian Zarnekow - Contributions for
 *								bug 544921 - [performance] Poor performance with large source files
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.RecordComponent;
import org.eclipse.jdt.internal.compiler.ast.ReferenceExpression;
import org.eclipse.jdt.internal.compiler.ast.SwitchStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference.AnnotationPosition;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ExternalAnnotationProvider;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.codegen.Opcodes;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.compiler.util.Util;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class SourceTypeBinding extends ReferenceBinding {
	public ReferenceBinding superclass;                    // MUST NOT be modified directly, use setter !
	public ReferenceBinding[] superInterfaces;             // MUST NOT be modified directly, use setter !
	private FieldBinding[] fields;                         // MUST NOT be modified directly, use setter !
	private MethodBinding[] methods;                       // MUST NOT be modified directly, use setter !
	public ReferenceBinding[] memberTypes;                 // MUST NOT be modified directly, use setter !
	public TypeVariableBinding[] typeVariables;            // MUST NOT be modified directly, use setter !
	public ReferenceBinding[] permittedTypes;              // MUST NOT be modified directly, use setter !

	public ClassScope scope;
	protected SourceTypeBinding prototype;
	LookupEnvironment environment;
	public ModuleBinding module;
	// Synthetics are separated into 4 categories: methods, super methods, fields, class literals and bridge methods
	// if a new category is added, also increment MAX_SYNTHETICS
	private final static int METHOD_EMUL = 0;
	private final static int FIELD_EMUL = 1;
	private final static int CLASS_LITERAL_EMUL = 2;

	private final static int MAX_SYNTHETICS = 3;

	Map[] synthetics;
	char[] genericReferenceTypeSignature;

	private SimpleLookupTable storedAnnotations = null; // keys are this ReferenceBinding & its fields and methods, value is an AnnotationHolder

	public int defaultNullness;
	boolean memberTypesSorted = false;
	private int nullnessDefaultInitialized = 0; // 0: nothing; 1: type; 2: package
	private ReferenceBinding containerAnnotationType = null;

	public ExternalAnnotationProvider externalAnnotationProvider;

	private SourceTypeBinding nestHost;

	private boolean isRecordDeclaration = false;
	private RecordComponentBinding[] components; // for Java 14 record declaration - preview
	public boolean isVarArgs =  false; // for record declaration
	private FieldBinding[] implicitComponentFields; // cache
	private MethodBinding[] recordComponentAccessors = null; // hash maybe an overkill

public SourceTypeBinding(char[][] compoundName, PackageBinding fPackage, ClassScope scope) {
	this.compoundName = compoundName;
	this.fPackage = fPackage;
	this.fileName = scope.referenceCompilationUnit().getFileName();
	this.modifiers = scope.referenceContext.modifiers;
	this.sourceName = scope.referenceContext.name;
	this.scope = scope;
	this.environment = scope.environment();

	// expect the fields & methods to be initialized correctly later
	this.components = Binding.UNINITIALIZED_COMPONENTS;
	this.fields = Binding.UNINITIALIZED_FIELDS;
	this.methods = Binding.UNINITIALIZED_METHODS;
	this.prototype = this;
	this.isRecordDeclaration = scope.referenceContext.isRecord();
	computeId();
}

public SourceTypeBinding(SourceTypeBinding prototype) {
	super(prototype);

	this.prototype = prototype.prototype;
	this.prototype.tagBits |= TagBits.HasAnnotatedVariants;
	this.tagBits &= ~TagBits.HasAnnotatedVariants;

	this.superclass = prototype.superclass;
	this.superInterfaces = prototype.superInterfaces;
	this.fields = prototype.fields;
	this.methods = prototype.methods;
	this.memberTypes = prototype.memberTypes;
	this.typeVariables = prototype.typeVariables;
	this.environment = prototype.environment;

	this.scope = prototype.scope; // compensated by TypeSystem.cleanUp(int)

	this.synthetics = prototype.synthetics;
	this.genericReferenceTypeSignature = prototype.genericReferenceTypeSignature;
	this.storedAnnotations = prototype.storedAnnotations;
	this.defaultNullness = prototype.defaultNullness;
	this.nullnessDefaultInitialized= prototype.nullnessDefaultInitialized;
	this.containerAnnotationType = prototype.containerAnnotationType;
	this.tagBits |= TagBits.HasUnresolvedMemberTypes; // see memberTypes()
	this.isRecordDeclaration = this.prototype.isRecordDeclaration;
}

private void addDefaultAbstractMethods() {

	if (!isPrototype()) throw new IllegalStateException();

	if ((this.tagBits & TagBits.KnowsDefaultAbstractMethods) != 0) return;

	this.tagBits |= TagBits.KnowsDefaultAbstractMethods;
	if (isClass() && isAbstract()) {
		if (this.scope.compilerOptions().targetJDK >= ClassFileConstants.JDK1_2)
			return; // no longer added for post 1.2 targets

		ReferenceBinding[] itsInterfaces = superInterfaces();
		if (itsInterfaces != Binding.NO_SUPERINTERFACES) {
			MethodBinding[] defaultAbstracts = null;
			int defaultAbstractsCount = 0;
			ReferenceBinding[] interfacesToVisit = itsInterfaces;
			int nextPosition = interfacesToVisit.length;
			for (int i = 0; i < nextPosition; i++) {
				ReferenceBinding superType = interfacesToVisit[i];
				if (superType.isValidBinding()) {
					MethodBinding[] superMethods = superType.methods();
					nextAbstractMethod: for (int m = superMethods.length; --m >= 0;) {
						MethodBinding method = superMethods[m];
						// explicitly implemented ?
						if (implementsMethod(method))
							continue nextAbstractMethod;
						if (defaultAbstractsCount == 0) {
							defaultAbstracts = new MethodBinding[5];
						} else {
							// already added as default abstract ?
							for (int k = 0; k < defaultAbstractsCount; k++) {
								MethodBinding alreadyAdded = defaultAbstracts[k];
								if (CharOperation.equals(alreadyAdded.selector, method.selector) && alreadyAdded.areParametersEqual(method))
									continue nextAbstractMethod;
							}
						}
						MethodBinding defaultAbstract = new MethodBinding(
								method.modifiers | ExtraCompilerModifiers.AccDefaultAbstract | ClassFileConstants.AccSynthetic,
								method.selector,
								method.returnType,
								method.parameters,
								method.thrownExceptions,
								this);
						if (defaultAbstractsCount == defaultAbstracts.length)
							System.arraycopy(defaultAbstracts, 0, defaultAbstracts = new MethodBinding[2 * defaultAbstractsCount], 0, defaultAbstractsCount);
						defaultAbstracts[defaultAbstractsCount++] = defaultAbstract;
					}

					if ((itsInterfaces = superType.superInterfaces()) != Binding.NO_SUPERINTERFACES) {
						int itsLength = itsInterfaces.length;
						if (nextPosition + itsLength >= interfacesToVisit.length)
							System.arraycopy(interfacesToVisit, 0, interfacesToVisit = new ReferenceBinding[nextPosition + itsLength + 5], 0, nextPosition);
						nextInterface : for (int a = 0; a < itsLength; a++) {
							ReferenceBinding next = itsInterfaces[a];
							for (int b = 0; b < nextPosition; b++)
								if (TypeBinding.equalsEquals(next, interfacesToVisit[b])) continue nextInterface;
							interfacesToVisit[nextPosition++] = next;
						}
					}
				}
			}
			if (defaultAbstractsCount > 0) {
				int length = this.methods.length;
				System.arraycopy(this.methods, 0, setMethods(new MethodBinding[length + defaultAbstractsCount]), 0, length);
				System.arraycopy(defaultAbstracts, 0, this.methods, length, defaultAbstractsCount);
				// re-sort methods
				length = length + defaultAbstractsCount;
				if (length > 1)
					ReferenceBinding.sortMethods(this.methods, 0, length);
				// this.tagBits |= TagBits.AreMethodsSorted; -- already set in #methods()
			}
		}
	}
}
/* Add a new synthetic field for <actualOuterLocalVariable>.
*	Answer the new field or the existing field if one already existed.
*/
public FieldBinding addSyntheticFieldForInnerclass(LocalVariableBinding actualOuterLocalVariable) {

	if (!isPrototype()) throw new IllegalStateException();

	if (this.synthetics == null)
		this.synthetics = new Map[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.FIELD_EMUL] == null)
		this.synthetics[SourceTypeBinding.FIELD_EMUL] = new LinkedHashMap(5);

	FieldBinding synthField = (FieldBinding) this.synthetics[SourceTypeBinding.FIELD_EMUL].get(actualOuterLocalVariable);
	if (synthField == null) {
		synthField = new SyntheticFieldBinding(
			CharOperation.concat(TypeConstants.SYNTHETIC_OUTER_LOCAL_PREFIX, actualOuterLocalVariable.name),
			actualOuterLocalVariable.type,
			ClassFileConstants.AccPrivate | ClassFileConstants.AccFinal | ClassFileConstants.AccSynthetic,
			this,
			Constant.NotAConstant,
			this.synthetics[SourceTypeBinding.FIELD_EMUL].size());
		this.synthetics[SourceTypeBinding.FIELD_EMUL].put(actualOuterLocalVariable, synthField);
	}

	// ensure there is not already such a field defined by the user
	boolean needRecheck;
	int index = 1;
	do {
		needRecheck = false;
		FieldBinding existingField;
		if ((existingField = getField(synthField.name, true /*resolve*/)) != null) {
			TypeDeclaration typeDecl = this.scope.referenceContext;
			FieldDeclaration[] fieldDeclarations = typeDecl.fields;
			int max = fieldDeclarations == null ? 0 : fieldDeclarations.length;
			for (int i = 0; i < max; i++) {
				FieldDeclaration fieldDecl = fieldDeclarations[i];
				if (fieldDecl.binding == existingField) {
					synthField.name = CharOperation.concat(
						TypeConstants.SYNTHETIC_OUTER_LOCAL_PREFIX,
						actualOuterLocalVariable.name,
						("$" + String.valueOf(index++)).toCharArray()); //$NON-NLS-1$
					needRecheck = true;
					break;
				}
			}
		}
	} while (needRecheck);
	return synthField;
}
/* Add a new synthetic field for <enclosingType>.
*	Answer the new field or the existing field if one already existed.
*/
public FieldBinding addSyntheticFieldForInnerclass(ReferenceBinding enclosingType) {

	if (!isPrototype()) throw new IllegalStateException();

	if (this.synthetics == null)
		this.synthetics = new Map[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.FIELD_EMUL] == null)
		this.synthetics[SourceTypeBinding.FIELD_EMUL] = new LinkedHashMap(5);

	FieldBinding synthField = (FieldBinding) this.synthetics[SourceTypeBinding.FIELD_EMUL].get(enclosingType);
	if (synthField == null) {
		synthField = new SyntheticFieldBinding(
			CharOperation.concat(
				TypeConstants.SYNTHETIC_ENCLOSING_INSTANCE_PREFIX,
				String.valueOf(enclosingType.depth()).toCharArray()),
			enclosingType,
			ClassFileConstants.AccDefault | ClassFileConstants.AccFinal | ClassFileConstants.AccSynthetic,
			this,
			Constant.NotAConstant,
			this.synthetics[SourceTypeBinding.FIELD_EMUL].size());
		this.synthetics[SourceTypeBinding.FIELD_EMUL].put(enclosingType, synthField);
	}
	// ensure there is not already such a field defined by the user
	boolean needRecheck;
	do {
		needRecheck = false;
		FieldBinding existingField;
		if ((existingField = getField(synthField.name, true /*resolve*/)) != null) {
			TypeDeclaration typeDecl = this.scope.referenceContext;
			FieldDeclaration[] fieldDeclarations = typeDecl.fields;
			int max = fieldDeclarations == null ? 0 : fieldDeclarations.length;
			for (int i = 0; i < max; i++) {
				FieldDeclaration fieldDecl = fieldDeclarations[i];
				if (fieldDecl.binding == existingField) {
					if (this.scope.compilerOptions().complianceLevel >= ClassFileConstants.JDK1_5) {
						synthField.name = CharOperation.concat(
							synthField.name,
							"$".toCharArray()); //$NON-NLS-1$
						needRecheck = true;
					} else {
						this.scope.problemReporter().duplicateFieldInType(this, fieldDecl);
					}
					break;
				}
			}
		}
	} while (needRecheck);
	return synthField;
}
/* Add a new synthetic field for a class literal access.
*	Answer the new field or the existing field if one already existed.
*/
public FieldBinding addSyntheticFieldForClassLiteral(TypeBinding targetType, BlockScope blockScope) {

	if (!isPrototype()) throw new IllegalStateException();

	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.CLASS_LITERAL_EMUL] == null)
		this.synthetics[SourceTypeBinding.CLASS_LITERAL_EMUL] = new LinkedHashMap(5);

	// use a different table than FIELDS, given there might be a collision between emulation of X.this$0 and X.class.
	FieldBinding synthField = (FieldBinding) this.synthetics[SourceTypeBinding.CLASS_LITERAL_EMUL].get(targetType);
	if (synthField == null) {
		synthField = new SyntheticFieldBinding(
			CharOperation.concat(
				TypeConstants.SYNTHETIC_CLASS,
				String.valueOf(this.synthetics[SourceTypeBinding.CLASS_LITERAL_EMUL].size()).toCharArray()),
			blockScope.getJavaLangClass(),
			ClassFileConstants.AccDefault | ClassFileConstants.AccStatic | ClassFileConstants.AccSynthetic,
			this,
			Constant.NotAConstant,
			this.synthetics[SourceTypeBinding.CLASS_LITERAL_EMUL].size());
		this.synthetics[SourceTypeBinding.CLASS_LITERAL_EMUL].put(targetType, synthField);
	}
	// ensure there is not already such a field defined by the user
	FieldBinding existingField;
	if ((existingField = getField(synthField.name, true /*resolve*/)) != null) {
		TypeDeclaration typeDecl = blockScope.referenceType();
		FieldDeclaration[] typeDeclarationFields = typeDecl.fields;
		int max = typeDeclarationFields == null ? 0 : typeDeclarationFields.length;
		for (int i = 0; i < max; i++) {
			FieldDeclaration fieldDecl = typeDeclarationFields[i];
			if (fieldDecl.binding == existingField) {
				blockScope.problemReporter().duplicateFieldInType(this, fieldDecl);
				break;
			}
		}
	}
	return synthField;
}
/* Add a new synthetic field for the emulation of the assert statement.
*	Answer the new field or the existing field if one already existed.
*/
public FieldBinding addSyntheticFieldForAssert(BlockScope blockScope) {

	if (!isPrototype()) throw new IllegalStateException();

	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.FIELD_EMUL] == null)
		this.synthetics[SourceTypeBinding.FIELD_EMUL] = new LinkedHashMap(5);

	FieldBinding synthField = (FieldBinding) this.synthetics[SourceTypeBinding.FIELD_EMUL].get("assertionEmulation"); //$NON-NLS-1$
	if (synthField == null) {
		synthField = new SyntheticFieldBinding(
			TypeConstants.SYNTHETIC_ASSERT_DISABLED,
			TypeBinding.BOOLEAN,
			(isInterface() ? ClassFileConstants.AccPublic : ClassFileConstants.AccDefault) | ClassFileConstants.AccStatic | ClassFileConstants.AccSynthetic | ClassFileConstants.AccFinal,
			this,
			Constant.NotAConstant,
			this.synthetics[SourceTypeBinding.FIELD_EMUL].size());
		this.synthetics[SourceTypeBinding.FIELD_EMUL].put("assertionEmulation", synthField); //$NON-NLS-1$
	}
	// ensure there is not already such a field defined by the user
	// ensure there is not already such a field defined by the user
	boolean needRecheck;
	int index = 0;
	do {
		needRecheck = false;
		FieldBinding existingField;
		if ((existingField = getField(synthField.name, true /*resolve*/)) != null) {
			TypeDeclaration typeDecl = this.scope.referenceContext;
			int max = (typeDecl.fields == null) ? 0 : typeDecl.fields.length;
			for (int i = 0; i < max; i++) {
				FieldDeclaration fieldDecl = typeDecl.fields[i];
				if (fieldDecl.binding == existingField) {
					synthField.name = CharOperation.concat(
						TypeConstants.SYNTHETIC_ASSERT_DISABLED,
						("_" + String.valueOf(index++)).toCharArray()); //$NON-NLS-1$
					needRecheck = true;
					break;
				}
			}
		}
	} while (needRecheck);
	return synthField;
}
/* Add a new synthetic field for recording all enum constant values
*	Answer the new field or the existing field if one already existed.
*/
public FieldBinding addSyntheticFieldForEnumValues() {

	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.FIELD_EMUL] == null)
		this.synthetics[SourceTypeBinding.FIELD_EMUL] = new LinkedHashMap(5);

	FieldBinding synthField = (FieldBinding) this.synthetics[SourceTypeBinding.FIELD_EMUL].get("enumConstantValues"); //$NON-NLS-1$
	if (synthField == null) {
		synthField = new SyntheticFieldBinding(
			TypeConstants.SYNTHETIC_ENUM_VALUES,
			this.scope.createArrayType(this,1),
			ClassFileConstants.AccPrivate | ClassFileConstants.AccStatic | ClassFileConstants.AccSynthetic | ClassFileConstants.AccFinal,
			this,
			Constant.NotAConstant,
			this.synthetics[SourceTypeBinding.FIELD_EMUL].size());
		this.synthetics[SourceTypeBinding.FIELD_EMUL].put("enumConstantValues", synthField); //$NON-NLS-1$
	}
	// ensure there is not already such a field defined by the user
	// ensure there is not already such a field defined by the user
	boolean needRecheck;
	int index = 0;
	do {
		needRecheck = false;
		FieldBinding existingField;
		if ((existingField = getField(synthField.name, true /*resolve*/)) != null) {
			TypeDeclaration typeDecl = this.scope.referenceContext;
			FieldDeclaration[] fieldDeclarations = typeDecl.fields;
			int max = fieldDeclarations == null ? 0 : fieldDeclarations.length;
			for (int i = 0; i < max; i++) {
				FieldDeclaration fieldDecl = fieldDeclarations[i];
				if (fieldDecl.binding == existingField) {
					synthField.name = CharOperation.concat(
						TypeConstants.SYNTHETIC_ENUM_VALUES,
						("_" + String.valueOf(index++)).toCharArray()); //$NON-NLS-1$
					needRecheck = true;
					break;
				}
			}
		}
	} while (needRecheck);
	return synthField;
}
/* Add a new synthetic access method for read/write access to <targetField>.
	Answer the new method or the existing method if one already existed.
*/
public SyntheticMethodBinding addSyntheticMethod(FieldBinding targetField, boolean isReadAccess, boolean isSuperAccess) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null)
		this.synthetics[SourceTypeBinding.METHOD_EMUL] = new LinkedHashMap(5);

	SyntheticMethodBinding accessMethod = null;
	SyntheticMethodBinding[] accessors = (SyntheticMethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(targetField);
	if (accessors == null) {
		accessMethod = new SyntheticMethodBinding(targetField, isReadAccess, isSuperAccess, this);
		this.synthetics[SourceTypeBinding.METHOD_EMUL].put(targetField, accessors = new SyntheticMethodBinding[2]);
		accessors[isReadAccess ? 0 : 1] = accessMethod;
	} else {
		if ((accessMethod = accessors[isReadAccess ? 0 : 1]) == null) {
			accessMethod = new SyntheticMethodBinding(targetField, isReadAccess, isSuperAccess, this);
			accessors[isReadAccess ? 0 : 1] = accessMethod;
		}
	}
	return accessMethod;
}
/* Add a new synthetic method the enum type. Selector can either be 'values' or 'valueOf'.
 * char[] constants from TypeConstants must be used: TypeConstants.VALUES/VALUEOF
*/
public SyntheticMethodBinding addSyntheticEnumMethod(char[] selector) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null)
		this.synthetics[SourceTypeBinding.METHOD_EMUL] = new LinkedHashMap(5);

	SyntheticMethodBinding accessMethod = null;
	SyntheticMethodBinding[] accessors = (SyntheticMethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(selector);
	if (accessors == null) {
		accessMethod = new SyntheticMethodBinding(this, selector);
		this.synthetics[SourceTypeBinding.METHOD_EMUL].put(selector, accessors = new SyntheticMethodBinding[2]);
		accessors[0] = accessMethod;
	} else {
		if ((accessMethod = accessors[0]) == null) {
			accessMethod = new SyntheticMethodBinding(this, selector);
			accessors[0] = accessMethod;
		}
	}
	return accessMethod;
}
/*
 * Add a synthetic field to handle the cache of the switch translation table for the corresponding enum type
 */
public SyntheticFieldBinding addSyntheticFieldForSwitchEnum(char[] fieldName, String key) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.FIELD_EMUL] == null)
		this.synthetics[SourceTypeBinding.FIELD_EMUL] = new LinkedHashMap(5);

	SyntheticFieldBinding synthField = (SyntheticFieldBinding) this.synthetics[SourceTypeBinding.FIELD_EMUL].get(key);
	if (synthField == null) {
		synthField = new SyntheticFieldBinding(
			fieldName,
			this.scope.createArrayType(TypeBinding.INT,1),
			(isInterface() ? (ClassFileConstants.AccPublic | ClassFileConstants.AccFinal) : ClassFileConstants.AccPrivate | ClassFileConstants.AccVolatile) | ClassFileConstants.AccStatic | ClassFileConstants.AccSynthetic,
			this,
			Constant.NotAConstant,
			this.synthetics[SourceTypeBinding.FIELD_EMUL].size());
		this.synthetics[SourceTypeBinding.FIELD_EMUL].put(key, synthField);
	}
	// ensure there is not already such a field defined by the user
	boolean needRecheck;
	int index = 0;
	do {
		needRecheck = false;
		FieldBinding existingField;
		if ((existingField = getField(synthField.name, true /*resolve*/)) != null) {
			TypeDeclaration typeDecl = this.scope.referenceContext;
			FieldDeclaration[] fieldDeclarations = typeDecl.fields;
			int max = fieldDeclarations == null ? 0 : fieldDeclarations.length;
			for (int i = 0; i < max; i++) {
				FieldDeclaration fieldDecl = fieldDeclarations[i];
				if (fieldDecl.binding == existingField) {
					synthField.name = CharOperation.concat(
						fieldName,
						("_" + String.valueOf(index++)).toCharArray()); //$NON-NLS-1$
					needRecheck = true;
					break;
				}
			}
		}
	} while (needRecheck);
	return synthField;
}
/* Add a new synthetic method the enum type. Selector can either be 'values' or 'valueOf'.
 * char[] constants from TypeConstants must be used: TypeConstants.VALUES/VALUEOF
*/
public SyntheticMethodBinding addSyntheticMethodForSwitchEnum(TypeBinding enumBinding, SwitchStatement switchStatement) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null)
		this.synthetics[SourceTypeBinding.METHOD_EMUL] = new LinkedHashMap(5);

	SyntheticMethodBinding accessMethod = null;
	char[] selector = CharOperation.concat(TypeConstants.SYNTHETIC_SWITCH_ENUM_TABLE, enumBinding.constantPoolName());
	CharOperation.replace(selector, '/', '$');
	final String key = new String(selector);
	SyntheticMethodBinding[] accessors = (SyntheticMethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(key);
	// first add the corresponding synthetic field
	if (accessors == null) {
		// then create the synthetic method
		final SyntheticFieldBinding fieldBinding = addSyntheticFieldForSwitchEnum(selector, key);
		accessMethod = new SyntheticMethodBinding(fieldBinding, this, enumBinding, selector, switchStatement);
		this.synthetics[SourceTypeBinding.METHOD_EMUL].put(key, accessors = new SyntheticMethodBinding[2]);
		accessors[0] = accessMethod;
	} else {
		if ((accessMethod = accessors[0]) == null) {
			final SyntheticFieldBinding fieldBinding = addSyntheticFieldForSwitchEnum(selector, key);
			accessMethod = new SyntheticMethodBinding(fieldBinding, this, enumBinding, selector, switchStatement);
			accessors[0] = accessMethod;
		}
	}
	return accessMethod;
}
public SyntheticMethodBinding addSyntheticMethodForEnumInitialization(int begin, int end) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null)
		this.synthetics[SourceTypeBinding.METHOD_EMUL] = new LinkedHashMap(5);

	SyntheticMethodBinding accessMethod = new SyntheticMethodBinding(this, begin, end);
	SyntheticMethodBinding[] accessors = new SyntheticMethodBinding[2];
	this.synthetics[SourceTypeBinding.METHOD_EMUL].put(accessMethod.selector, accessors);
	accessors[0] = accessMethod;
	return accessMethod;
}
public SyntheticMethodBinding addSyntheticMethod(LambdaExpression lambda) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null)
		this.synthetics[SourceTypeBinding.METHOD_EMUL] = new LinkedHashMap(5);

	SyntheticMethodBinding lambdaMethod = null;
	SyntheticMethodBinding[] lambdaMethods = (SyntheticMethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(lambda);
	if (lambdaMethods == null) {
		lambdaMethod = new SyntheticMethodBinding(lambda, CharOperation.concat(TypeConstants.ANONYMOUS_METHOD, Integer.toString(lambda.ordinal).toCharArray()), this);
		this.synthetics[SourceTypeBinding.METHOD_EMUL].put(lambda, lambdaMethods = new SyntheticMethodBinding[1]);
		lambdaMethods[0] = lambdaMethod;
	} else {
		lambdaMethod = lambdaMethods[0];
	}

	// Create a $deserializeLambda$ method if necessary, one is shared amongst all lambdas
	if (lambda.isSerializable) {
		addDeserializeLambdaMethod();
	}

	return lambdaMethod;
}
/*
 * Add a synthetic method for the reference expression as a place holder for code generation
 * only if the reference expression's target is serializable
 *
 */
public SyntheticMethodBinding addSyntheticMethod(ReferenceExpression ref) {
	if (!isPrototype()) throw new IllegalStateException();
	if (!ref.isSerializable)
		return null;
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null)
		this.synthetics[SourceTypeBinding.METHOD_EMUL] = new LinkedHashMap(5);

	SyntheticMethodBinding lambdaMethod = null;
	SyntheticMethodBinding[] lambdaMethods = (SyntheticMethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(ref);
	if (lambdaMethods == null) {
		lambdaMethod = new SyntheticMethodBinding(ref, this);
		this.synthetics[SourceTypeBinding.METHOD_EMUL].put(ref, lambdaMethods = new SyntheticMethodBinding[1]);
		lambdaMethods[0] = lambdaMethod;
	} else {
		lambdaMethod = lambdaMethods[0];
	}

	// Create a $deserializeLambda$ method, one is shared amongst all lambdas
	addDeserializeLambdaMethod();
	return lambdaMethod;
}
private void addDeserializeLambdaMethod() {
	SyntheticMethodBinding[] deserializeLambdaMethods = (SyntheticMethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(TypeConstants.DESERIALIZE_LAMBDA);
	if (deserializeLambdaMethods == null) {
		SyntheticMethodBinding deserializeLambdaMethod = new SyntheticMethodBinding(this);
		this.synthetics[SourceTypeBinding.METHOD_EMUL].put(TypeConstants.DESERIALIZE_LAMBDA,deserializeLambdaMethods = new SyntheticMethodBinding[1]);
		deserializeLambdaMethods[0] = deserializeLambdaMethod;
	}
}
/* Add a new synthetic access method for access to <targetMethod>.
 * Must distinguish access method used for super access from others (need to use invokespecial bytecode)
	Answer the new method or the existing method if one already existed.
*/
public SyntheticMethodBinding addSyntheticMethod(MethodBinding targetMethod, boolean isSuperAccess) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null)
		this.synthetics[SourceTypeBinding.METHOD_EMUL] = new LinkedHashMap(5);

	SyntheticMethodBinding accessMethod = null;
	SyntheticMethodBinding[] accessors = (SyntheticMethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(targetMethod);
	if (accessors == null) {
		accessMethod = new SyntheticMethodBinding(targetMethod, isSuperAccess, this);
		this.synthetics[SourceTypeBinding.METHOD_EMUL].put(targetMethod, accessors = new SyntheticMethodBinding[2]);
		accessors[isSuperAccess ? 0 : 1] = accessMethod;
	} else {
		if ((accessMethod = accessors[isSuperAccess ? 0 : 1]) == null) {
			accessMethod = new SyntheticMethodBinding(targetMethod, isSuperAccess, this);
			accessors[isSuperAccess ? 0 : 1] = accessMethod;
		}
	}
	if (targetMethod.declaringClass.isStatic()) {
		if ((targetMethod.isConstructor() && targetMethod.parameters.length >= 0xFE)
				|| targetMethod.parameters.length >= 0xFF) {
			this.scope.problemReporter().tooManyParametersForSyntheticMethod(targetMethod.sourceMethod());
		}
	} else if ((targetMethod.isConstructor() && targetMethod.parameters.length >= 0xFD)
			|| targetMethod.parameters.length >= 0xFE) {
		this.scope.problemReporter().tooManyParametersForSyntheticMethod(targetMethod.sourceMethod());
	}
	return accessMethod;
}
public SyntheticMethodBinding addSyntheticArrayMethod(ArrayBinding arrayType, int purpose, char[] selector) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null)
		this.synthetics[SourceTypeBinding.METHOD_EMUL] = new LinkedHashMap(5);

	SyntheticMethodBinding arrayMethod = null;
	SyntheticMethodBinding[] arrayMethods = (SyntheticMethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(arrayType);
	if (arrayMethods == null) {
		arrayMethod = new SyntheticMethodBinding(purpose, arrayType, selector, this);
		this.synthetics[SourceTypeBinding.METHOD_EMUL].put(arrayType, arrayMethods = new SyntheticMethodBinding[2]);
		arrayMethods[purpose == SyntheticMethodBinding.ArrayConstructor ? 0 : 1] = arrayMethod;
	} else {
		if ((arrayMethod = arrayMethods[purpose == SyntheticMethodBinding.ArrayConstructor ? 0 : 1]) == null) {
			arrayMethod = new SyntheticMethodBinding(purpose, arrayType, selector, this);
			arrayMethods[purpose == SyntheticMethodBinding.ArrayConstructor ? 0 : 1] = arrayMethod;
		}
	}
	return arrayMethod;
}
public SyntheticMethodBinding addSyntheticFactoryMethod(MethodBinding privateConstructor, MethodBinding publicConstructor, TypeBinding [] enclosingInstances, char[] selector) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null)
		this.synthetics[SourceTypeBinding.METHOD_EMUL] = new LinkedHashMap(5);

	SyntheticMethodBinding factory = new SyntheticMethodBinding(privateConstructor, publicConstructor, selector, enclosingInstances, this);
	this.synthetics[SourceTypeBinding.METHOD_EMUL].put(selector, new SyntheticMethodBinding[] { factory });
	return factory;
}
/*
 * Record the fact that bridge methods need to be generated to override certain inherited methods
 */
public SyntheticMethodBinding addSyntheticBridgeMethod(MethodBinding inheritedMethodToBridge, MethodBinding targetMethod) {
	if (!isPrototype()) throw new IllegalStateException();
	if (isInterface() && this.scope.compilerOptions().sourceLevel <= ClassFileConstants.JDK1_7) return null; // only classes & enums get bridge methods, interfaces too at 1.8+
	// targetMethod may be inherited
	if (TypeBinding.equalsEquals(inheritedMethodToBridge.returnType.erasure(), targetMethod.returnType.erasure())
		&& inheritedMethodToBridge.areParameterErasuresEqual(targetMethod)) {
			return null; // do not need bridge method
	}
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null) {
		this.synthetics[SourceTypeBinding.METHOD_EMUL] = new LinkedHashMap(5);
	} else {
		// check to see if there is another equivalent inheritedMethod already added
		Iterator synthMethods = this.synthetics[SourceTypeBinding.METHOD_EMUL].keySet().iterator();
		while (synthMethods.hasNext()) {
			Object synthetic = synthMethods.next();
			if (synthetic instanceof MethodBinding) {
				MethodBinding method = (MethodBinding) synthetic;
				if (CharOperation.equals(inheritedMethodToBridge.selector, method.selector)
					&& TypeBinding.equalsEquals(inheritedMethodToBridge.returnType.erasure(), method.returnType.erasure())
					&& inheritedMethodToBridge.areParameterErasuresEqual(method)) {
						return null;
				}
			}
		}
	}

	SyntheticMethodBinding accessMethod = null;
	SyntheticMethodBinding[] accessors = (SyntheticMethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(inheritedMethodToBridge);
	if (accessors == null) {
		accessMethod = new SyntheticMethodBinding(inheritedMethodToBridge, targetMethod, this);
		this.synthetics[SourceTypeBinding.METHOD_EMUL].put(inheritedMethodToBridge, accessors = new SyntheticMethodBinding[2]);
		accessors[1] = accessMethod;
	} else {
		if ((accessMethod = accessors[1]) == null) {
			accessMethod = new SyntheticMethodBinding(inheritedMethodToBridge, targetMethod, this);
			accessors[1] = accessMethod;
		}
	}
	return accessMethod;
}
/*
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=288658. Generate a bridge method if a public method is inherited
 * from a non-public class into a public class (only in 1.6 or greater)
 * https://bugs.eclipse.org/404690 : this doesn't apply to inherited interface methods (i.e., default methods)
 */
public SyntheticMethodBinding addSyntheticBridgeMethod(MethodBinding inheritedMethodToBridge) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.scope.compilerOptions().complianceLevel <= ClassFileConstants.JDK1_5) {
		return null;
	}
	if (isInterface() && !inheritedMethodToBridge.isDefaultMethod()) return null;
	if (inheritedMethodToBridge.isAbstract() || inheritedMethodToBridge.isFinal() || inheritedMethodToBridge.isStatic()) {
		return null;
	}
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null) {
		this.synthetics[SourceTypeBinding.METHOD_EMUL] = new LinkedHashMap(5);
	} else {
		// check to see if there is another equivalent inheritedMethod already added
		Iterator synthMethods = this.synthetics[SourceTypeBinding.METHOD_EMUL].keySet().iterator();
		while (synthMethods.hasNext()) {
			Object synthetic = synthMethods.next();
			if (synthetic instanceof MethodBinding) {
				MethodBinding method = (MethodBinding) synthetic;
				if (CharOperation.equals(inheritedMethodToBridge.selector, method.selector)
					&& TypeBinding.equalsEquals(inheritedMethodToBridge.returnType.erasure(), method.returnType.erasure())
					&& inheritedMethodToBridge.areParameterErasuresEqual(method)) {
						return null;
				}
			}
		}
	}

	SyntheticMethodBinding accessMethod = null;
	SyntheticMethodBinding[] accessors = (SyntheticMethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(inheritedMethodToBridge);
	if (accessors == null) {
		accessMethod = new SyntheticMethodBinding(inheritedMethodToBridge, this);
		this.synthetics[SourceTypeBinding.METHOD_EMUL].put(inheritedMethodToBridge, accessors = new SyntheticMethodBinding[2]);
		accessors[1] = accessMethod;
	} else {
		if ((accessMethod = accessors[1]) == null) {
			accessMethod = new SyntheticMethodBinding(inheritedMethodToBridge, this);
			accessors[1] = accessMethod;
		}
	}
	return accessMethod;
}
/* JLS 14 Record - Preview - begin */
public MethodBinding[] checkAndAddSyntheticRecordMethods(MethodBinding[] methodBindings, int count) {
	if (!this.isRecordDeclaration)
		return methodBindings;
	List<MethodBinding> implicitMethods = checkAndAddSyntheticRecordComponentAccessors(methodBindings);
	implicitMethods = checkAndAddSyntheticRecordOverrideMethods(methodBindings, implicitMethods);
	for (int i = 0; i < count; ++i)
		implicitMethods.add(methodBindings[i]);
	return implicitMethods.toArray(new MethodBinding[0]);
}
public List<MethodBinding> checkAndAddSyntheticRecordOverrideMethods(MethodBinding[] methodBindings, List<MethodBinding> implicitMethods) {
	if (!hasMethodWithNumArgs(TypeConstants.TOSTRING, 0)) {
		MethodBinding m = addSyntheticRecordOverrideMethod(TypeConstants.TOSTRING, implicitMethods.size());
		implicitMethods.add(m);
	}
	if (!hasMethodWithNumArgs(TypeConstants.HASHCODE, 0)) {
		MethodBinding m = addSyntheticRecordOverrideMethod(TypeConstants.HASHCODE, implicitMethods.size());
		implicitMethods.add(m);
	}
	boolean isEqualsPresent = Arrays.stream(methodBindings)
			.filter(m -> CharOperation.equals(TypeConstants.EQUALS, m.selector))
			.anyMatch(m -> m.parameters != null && m.parameters.length == 1 &&
				m.parameters[0].equals(this.scope.getJavaLangObject()));
	if (!isEqualsPresent) {
		MethodBinding m = addSyntheticRecordOverrideMethod(TypeConstants.EQUALS, implicitMethods.size());
		implicitMethods.add(m);
	}
	if (this.isRecordDeclaration &&  getImplicitCanonicalConstructor() == -1) {
		MethodBinding explicitCanon = null;
		for (MethodBinding m : methodBindings) {
			if (m.isCompactConstructor() || m.isCanonicalConstructor()) {
				explicitCanon = m;
				break;
			}
		}
		if (explicitCanon == null) {
			implicitMethods.add(addSyntheticRecordCanonicalConstructor());
		}
	}
	return implicitMethods;
}
public List<MethodBinding> checkAndAddSyntheticRecordComponentAccessors(MethodBinding[] methodBindings) {
	List<MethodBinding> implicitMethods = new ArrayList<>(0);
	if (this.fields == null)
		return implicitMethods;
	// JLS 14 8.10.3 Item 2 create the accessors for the fields if required
	/*
	 * An implicitly declared public accessor method with the same name as the record component,
	 * whose return type is the declared type of the record component,
	 * unless a public method with the same signature is explicitly declared in the body of the declaration of R.
	 */

	// Note: filteredComponents implies that only those components which are successful in having field - for eg
	// if the component name is not correct (say one of finalize, clone etc) then the compilation not successful
	// and no accessor should be created (essentially in a recovered code if there are errors) - if there are no
	// errors then filteredComponents equals components.
	List<String> filteredComponents = Arrays.stream(this.fields) // initialize with all the record components
			.filter(f -> f.isRecordComponent())
			.map(f -> new String(f.name))
			.collect(Collectors.toList());

	List<MethodBinding> accessors = new ArrayList<>();
	if (this.methods != null) {
		accessors = Arrays.stream(methodBindings)
				.filter(m -> m.selector != null && m.selector.length > 0)
				.filter(m -> filteredComponents.contains(new String(m.selector)))
				.filter(m -> m.parameterNames == null || m.parameterNames.length == 0)
				.collect(Collectors.toList());

		List<String> candidates = accessors.stream()
			.map(m -> new String(m.selector))
			.collect(Collectors.toList());
		filteredComponents.removeAll(candidates);
	}
	int missingCount = filteredComponents.size();
	for (int i = 0; i < missingCount; ++i) {
		RecordComponentBinding rcb = this.getRecordComponent(filteredComponents.get(i).toCharArray());
		if (rcb != null)
			implicitMethods.add(addSyntheticRecordComponentAccessor(rcb, i));
	}
	accessors.addAll(implicitMethods);
	this.recordComponentAccessors = accessors.toArray(new MethodBinding[0]);
	return implicitMethods;
}
public SyntheticMethodBinding addSyntheticRecordCanonicalConstructor() {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null)
		this.synthetics[SourceTypeBinding.METHOD_EMUL] = new LinkedHashMap(5);

	SyntheticMethodBinding canonicalConstructor = new SyntheticMethodBinding(this, this.components);
	SyntheticMethodBinding[] accessors = new SyntheticMethodBinding[2];
	this.synthetics[SourceTypeBinding.METHOD_EMUL].put(TypeConstants.INIT, accessors);
	accessors[0] = canonicalConstructor;
	return canonicalConstructor;
}
public void removeSyntheticRecordCanonicalConstructor(SyntheticMethodBinding implicitCanonicalConstructor) {
	if (this.synthetics == null || this.synthetics[SourceTypeBinding.METHOD_EMUL] == null) return;
	SyntheticMethodBinding[] accessors = (SyntheticMethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(TypeConstants.INIT);
	if (accessors == null || accessors.length < 1) return;
	if (accessors[0] == implicitCanonicalConstructor)
		this.synthetics[SourceTypeBinding.METHOD_EMUL].remove(TypeConstants.INIT);
}
/* Add a new synthetic component accessor for the recordtype. Selector should be identical to component name.
 * char[] component name of the record
*/
public SyntheticMethodBinding addSyntheticRecordComponentAccessor(RecordComponentBinding rcb, int index) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null)
		this.synthetics = new LinkedHashMap[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null)
		this.synthetics[SourceTypeBinding.METHOD_EMUL] = new LinkedHashMap(5);

	SyntheticMethodBinding accessMethod = new SyntheticMethodBinding(this, rcb, index);
	SyntheticMethodBinding[] accessors = (SyntheticMethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(rcb.name);
//	TODO: Annotation propagation to be done later.
//	FieldBinding field = getField(selector, true);
//	accessMethod = new SyntheticMethodBinding(this, field, index);
//	AnnotationBinding[] annotations = field.getAnnotations();
//	if (annotations.length > 0) {
//		List<AnnotationBinding> list = new ArrayList<>();
//		for (AnnotationBinding binding : annotations) {
//			long bits = binding.getAnnotationType().getAnnotationTagBits();
//			if ((bits & TagBits.AnnotationForMethod) != 0
//					|| (bits & TagBits.AnnotationTargetMASK) == 0) {
//				list.add(binding);
//			}
//		}
//		if (list.size() > 0) {
//			AnnotationBinding[] annots = new AnnotationBinding[list.size()];
//			annotations = list.toArray(annots);
//			accessMethod.setAnnotations(annotations, true);
//		}
//	}
	if (accessors == null) {
		this.synthetics[SourceTypeBinding.METHOD_EMUL].put(rcb.name, accessors = new SyntheticMethodBinding[2]);
		accessors[0] = accessMethod;
	} else {
		if ((accessMethod = accessors[0]) == null) {
			accessors[0] = accessMethod;
		}
	}
	return accessMethod;
}
public SyntheticMethodBinding addSyntheticRecordOverrideMethod(char[] selector, int index) {
	if (this.synthetics == null)
		this.synthetics = new Map[MAX_SYNTHETICS];
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null)
		this.synthetics[SourceTypeBinding.METHOD_EMUL] = new LinkedHashMap(5);

	SyntheticMethodBinding accessMethod = null;
	SyntheticMethodBinding[] accessors = (SyntheticMethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(selector);
	accessMethod = new SyntheticMethodBinding(this, selector, index);
	if (accessors == null) {
		this.synthetics[SourceTypeBinding.METHOD_EMUL].put(selector, accessors = new SyntheticMethodBinding[2]);
		accessors[0] = accessMethod;
	} else {
		if ((accessMethod = accessors[0]) == null) {
			accessors[0] = accessMethod;
		}
	}
	return accessMethod;
}
private void removeSyntheticRecordOverrideMethod(MethodBinding smb) {
	if (this.synthetics == null)
		return;
	Map syntheticMethods = this.synthetics[SourceTypeBinding.METHOD_EMUL];
	if (syntheticMethods == null)
		return;
	syntheticMethods.remove(smb.selector);
}
boolean areComponentsInitialized() {
	if (!isPrototype())
		return this.prototype.areComponentsInitialized();
	return this.components != Binding.UNINITIALIZED_COMPONENTS;
}
/* JLS 14 Record - Preview - end */
boolean areFieldsInitialized() {
	if (!isPrototype())
		return this.prototype.areFieldsInitialized();
	return this.fields != Binding.UNINITIALIZED_FIELDS;
}
boolean areMethodsInitialized() {
	if (!isPrototype())
		return this.prototype.areMethodsInitialized();
	return this.methods != Binding.UNINITIALIZED_METHODS;
}
@Override
public int kind() {
	if (!isPrototype())
		return this.prototype.kind();
	if (this.typeVariables != Binding.NO_TYPE_VARIABLES) return Binding.GENERIC_TYPE;
	return Binding.TYPE;
}

@Override
public TypeBinding clone(TypeBinding immaterial) {
	return new SourceTypeBinding(this);
}

@Override
public char[] computeUniqueKey(boolean isLeaf) {
	if (!isPrototype())
		return this.prototype.computeUniqueKey();
	char[] uniqueKey = super.computeUniqueKey(isLeaf);
	if (uniqueKey.length == 2) return uniqueKey; // problem type's unique key is "L;"
	if (Util.isClassFileName(this.fileName)) return uniqueKey; // no need to insert compilation unit name for a .class file

	// insert compilation unit name if the type name is not the main type name
	int end = CharOperation.lastIndexOf('.', this.fileName);
	if (end != -1) {
		int start = CharOperation.lastIndexOf('/', this.fileName) + 1;
		char[] mainTypeName = CharOperation.subarray(this.fileName, start, end);
		start = CharOperation.lastIndexOf('/', uniqueKey) + 1;
		if (start == 0)
			start = 1; // start after L
		if (this.isMemberType()) {
			end = CharOperation.indexOf('$', uniqueKey, start);
		} else {
			// '$' is part of the type name
			end = -1;
		}
		if (end == -1)
			end = CharOperation.indexOf('<', uniqueKey, start);
		if (end == -1)
			end = CharOperation.indexOf(';', uniqueKey, start);
		char[] topLevelType = CharOperation.subarray(uniqueKey, start, end);
		if (!CharOperation.equals(topLevelType, mainTypeName)) {
			StringBuilder buffer = new StringBuilder();
			buffer.append(uniqueKey, 0, start);
			buffer.append(mainTypeName);
			buffer.append('~');
			buffer.append(topLevelType);
			buffer.append(uniqueKey, end, uniqueKey.length - end);
			int length = buffer.length();
			uniqueKey = new char[length];
			buffer.getChars(0, length, uniqueKey, 0);
			return uniqueKey;
		}
	}
	return uniqueKey;
}

private void checkAnnotationsInType() {
	// check @Deprecated annotation
	getAnnotationTagBits(); // marks as deprecated by side effect
	ReferenceBinding enclosingType = enclosingType();
	if (enclosingType != null && enclosingType.isViewedAsDeprecated() && !isDeprecated()) {
		this.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
		this.tagBits |= (enclosingType.tagBits & TagBits.AnnotationTerminallyDeprecated);
	}

	for (int i = 0, length = this.memberTypes.length; i < length; i++)
		((SourceTypeBinding) this.memberTypes[i]).checkAnnotationsInType();
}

void faultInTypesForFieldsAndMethods() {
	if (!isPrototype()) throw new IllegalStateException();
	checkPermitsInType();
	checkAnnotationsInType();
	internalFaultInTypeForFieldsAndMethods();
}

private Map.Entry<TypeReference, ReferenceBinding> getFirstSealedSuperTypeOrInterface(TypeDeclaration typeDecl) {
	boolean isAnySuperTypeSealed = typeDecl.superclass != null && this.superclass != null ? this.superclass.isSealed() : false;
	if (isAnySuperTypeSealed)
		return new AbstractMap.SimpleEntry<>(typeDecl.superclass, this.superclass);

	ReferenceBinding[] superInterfaces1 = this.superInterfaces();
	int l = superInterfaces1 != null ? superInterfaces1.length : 0;
	for (int i = 0; i < l; ++i) {
		ReferenceBinding superInterface = superInterfaces1[i];
		if (superInterface.isSealed()) {
			return new AbstractMap.SimpleEntry<>(typeDecl.superInterfaces[i], superInterface);
		}
	}
	return null;
}
// TODO: Optimize the multiple loops - defer until the feature becomes standard.
private void checkPermitsInType() {
//	if (/* this.isRecordDeclaration || */this.isEnum())
//		return; // handled separately
	TypeDeclaration typeDecl = this.scope.referenceContext;
	if (this.isInterface()) {
		if (isSealed() && isNonSealed()) {
			this.scope.problemReporter().sealedInterfaceIsSealedAndNonSealed(this, typeDecl);
			return;
		}
	}
	boolean hasPermittedTypes = this.permittedTypes != null && this.permittedTypes.length > 0;
	if (hasPermittedTypes) {
		if (!this.isSealed())
			this.scope.problemReporter().sealedMissingSealedModifier(this, typeDecl);
		ModuleBinding sourceModuleBinding = this.module();
		boolean isUnnamedModule = sourceModuleBinding.isUnnamed();
		if (isUnnamedModule) {
			PackageBinding sourceTypePackage = this.getPackage();
			for (int i =0, l = this.permittedTypes.length; i < l; i++) {
				ReferenceBinding permType = this.permittedTypes[i];
				if (!permType.isValidBinding()) continue;
				if (sourceTypePackage != permType.getPackage()) {
					TypeReference permittedTypeRef = typeDecl.permittedTypes[i];
					this.scope.problemReporter().sealedPermittedTypeOutsideOfPackage(permType, this, permittedTypeRef, sourceTypePackage);
				}
			}
		} else {
			for (int i = 0, l = this.permittedTypes.length; i < l; i++) {
				ReferenceBinding permType = this.permittedTypes[i];
				if (!permType.isValidBinding()) continue;
				ModuleBinding permTypeModule = permType.module();
				if (permTypeModule != null && sourceModuleBinding != permTypeModule) {
					TypeReference permittedTypeRef = typeDecl.permittedTypes[i];
					this.scope.problemReporter().sealedPermittedTypeOutsideOfModule(permType, this, permittedTypeRef, sourceModuleBinding);
				}
			}
		}
	}

//	ReferenceBinding superType = this.superclass();
	Map.Entry<TypeReference, ReferenceBinding> sealedEntry = getFirstSealedSuperTypeOrInterface(typeDecl);
	boolean foundSealedSuperTypeOrInterface = sealedEntry != null;
	if (this.isLocalType()) {
		if (this.isSealed() || this.isNonSealed())
			return; // already handled elsewhere
		if (foundSealedSuperTypeOrInterface) {
			this.scope.problemReporter().sealedLocalDirectSuperTypeSealed(this, sealedEntry.getKey(), sealedEntry.getValue());
			return;
		}
	} else if (this.isNonSealed()) {
		if (!foundSealedSuperTypeOrInterface) {
			if (this.isClass() && !this.isRecord()) // record to give only illegal modifier error.
				this.scope.problemReporter().sealedDisAllowedNonSealedModifierInClass(this, typeDecl);
			else if (this.isInterface())
				this.scope.problemReporter().sealedDisAllowedNonSealedModifierInInterface(this, typeDecl);
		}
	}
	if (foundSealedSuperTypeOrInterface) {
		if (!(this.isFinal() || this.isSealed() || this.isNonSealed())) {
			if (this.isClass())
				this.scope.problemReporter().sealedMissingClassModifier(this, typeDecl, sealedEntry.getValue());
			else if (this.isInterface())
				this.scope.problemReporter().sealedMissingInterfaceModifier(this, typeDecl, sealedEntry.getValue());
		}
		List<SourceTypeBinding> typesInCU = collectAllTypeBindings(typeDecl, this.scope.compilationUnitScope());
		if (!typeDecl.isRecord() && typeDecl.superclass != null && !checkPermitsAndAdd(this.superclass, typesInCU)) {
			reportSealedSuperTypeDoesNotPermitProblem(typeDecl.superclass, this.superclass);
		}
		for (int i = 0, l = this.superInterfaces.length; i < l; ++i) {
			ReferenceBinding superInterface = this.superInterfaces[i];
			if (superInterface != null && !checkPermitsAndAdd(superInterface, typesInCU)) {
				TypeReference superInterfaceRef = typeDecl.superInterfaces[i];
				reportSealedSuperTypeDoesNotPermitProblem(superInterfaceRef, superInterface);
			}
		}
	}
	for (int i = 0, length = this.memberTypes.length; i < length; i++)
		((SourceTypeBinding) this.memberTypes[i]).checkPermitsInType();

	if (this.scope.referenceContext.permittedTypes == null) {
		// Ignore implicitly permitted case
		return;
	}
	// In case of errors, be safe.
	int l = this.permittedTypes.length <= this.scope.referenceContext.permittedTypes.length ?
			this.permittedTypes.length : this.scope.referenceContext.permittedTypes.length;
	for (int i = 0; i < l; i++) {
	    TypeReference permittedTypeRef = this.scope.referenceContext.permittedTypes[i];
		ReferenceBinding permittedType = this.permittedTypes[i];
		if (permittedType == null || !permittedType.isValidBinding())
			continue;
		if (this.isClass()) {
			ReferenceBinding permSuperType = permittedType.superclass();
			permSuperType = getActualType(permSuperType);
			if (!TypeBinding.equalsEquals(this, permSuperType)) {
				this.scope.problemReporter().sealedNotDirectSuperClass(permittedType, permittedTypeRef, this);
				continue;
			}
		} else if (this.isInterface()) {
			ReferenceBinding[] permSuperInterfaces = permittedType.superInterfaces();
			boolean foundSuperInterface = false;
			if (permSuperInterfaces != null) {
				for (ReferenceBinding psi : permSuperInterfaces) {
					psi = getActualType(psi);
					if (TypeBinding.equalsEquals(this, psi)) {
						foundSuperInterface = true;
						break;
					}
				}
				if (!foundSuperInterface) {
					this.scope.problemReporter().sealedNotDirectSuperInterface(permittedType, permittedTypeRef, this);
					continue;
				}
			}
		}
	}
	return;
}

private void reportSealedSuperTypeDoesNotPermitProblem(TypeReference superTypeRef, TypeBinding superType) {
	ModuleBinding sourceModuleBinding = this.module();
	boolean isUnnamedModule = sourceModuleBinding.isUnnamed();
	boolean isClass =  false;
	if (superType.isClass()) {
		isClass =  true;
	}
	boolean sealedSuperTypeDoesNotPermit = false;
	ReferenceBinding superReferenceBinding = null;
	if (superType instanceof ReferenceBinding) {
		superReferenceBinding = (ReferenceBinding) superType;
		if (isUnnamedModule) {
			PackageBinding superTypePackage = superReferenceBinding.getPackage();
			PackageBinding pkg = this.getPackage();
			sealedSuperTypeDoesNotPermit = pkg!= null && pkg.equals(superTypePackage);
		} else {
			ModuleBinding superTypeModule = superReferenceBinding.module();
			ModuleBinding mod = this.module();
			sealedSuperTypeDoesNotPermit  = mod!= null && mod.equals(superTypeModule);
		}
	}
	if (sealedSuperTypeDoesNotPermit) {
		if (isClass) {
			this.scope.problemReporter().sealedSuperClassDoesNotPermit(this, superTypeRef, superType);
		} else {
			this.scope.problemReporter().sealedSuperInterfaceDoesNotPermit(this, superTypeRef, superType);
		}
	} else {
		if (superReferenceBinding instanceof SourceTypeBinding && isUnnamedModule) {
			PackageBinding superTypePackage = superReferenceBinding.getPackage();
			if (isClass) {
				this.scope.problemReporter().sealedSuperClassInDifferentPackage(this, superTypeRef, superType, superTypePackage);
			} else {
				this.scope.problemReporter().sealedSuperInterfaceInDifferentPackage(this, superTypeRef, superType, superTypePackage);
			}
		} else {
			if (isClass) {
				this.scope.problemReporter().sealedSuperClassDisallowed(this, superTypeRef, superType);
			} else {
				this.scope.problemReporter().sealedSuperInterfaceDisallowed(this, superTypeRef, superType);
			}
		}
	}
}

private ReferenceBinding getActualType(ReferenceBinding ref) {
	return ref.isParameterizedType() || ref.isRawType() ? ref.actualType(): ref;
}
public List<SourceTypeBinding> collectAllTypeBindings(TypeDeclaration typeDecl, CompilationUnitScope unitScope) {
	class TypeBindingsCollector extends ASTVisitor {
		List<SourceTypeBinding> types = new ArrayList<>();
		@Override
		public boolean visit(
				TypeDeclaration localTypeDeclaration,
				BlockScope scope1) {
				checkAndAddBinding(localTypeDeclaration.binding);
				return true;
			}
			@Override
			public boolean visit(
				TypeDeclaration memberTypeDeclaration,
				ClassScope scope1) {
				checkAndAddBinding(memberTypeDeclaration.binding);
				return true;
			}
			@Override
			public boolean visit(
				TypeDeclaration typeDeclaration,
				CompilationUnitScope scope1) {
				checkAndAddBinding(typeDeclaration.binding);
				return true; // do nothing by default, keep traversing
			}
			private void checkAndAddBinding(SourceTypeBinding stb) {
				if (stb != null)
					this.types.add(stb);
			}
	}
	TypeBindingsCollector typeCollector = new TypeBindingsCollector();
	typeDecl.traverse(typeCollector, unitScope);
	return typeCollector.types;
}

private boolean checkPermitsAndAdd(ReferenceBinding superType, List<SourceTypeBinding> types) {
	if (superType == null
			|| superType.equals(this.scope.getJavaLangObject())
			|| !superType.isSealed())
		return true;
	if (superType.isSealed()) {
		superType = getActualType(superType);
		ReferenceBinding[] superPermittedTypes = superType.permittedTypes();
		for (ReferenceBinding permittedType : superPermittedTypes) {
			permittedType = getActualType(permittedType);
			if (permittedType.isValidBinding() && TypeBinding.equalsEquals(this, permittedType))
				return true;
		}
	}
	return false;
}

@Override
public RecordComponentBinding[] components() {

	if (!this.isRecordDeclaration)
		return null;
	if (!isPrototype()) {
		if ((this.extendedTagBits & ExtendedTagBits.AreRecordComponentsComplete) != 0)
			return this.components;
		this.extendedTagBits |= ExtendedTagBits.AreRecordComponentsComplete;
		return this.components = this.prototype.components();
	}

	if ((this.extendedTagBits & ExtendedTagBits.AreRecordComponentsComplete) != 0)
		return this.components;

	if (!areComponentsInitialized()) {
		this.scope.buildComponents();
	}
	int failed = 0;
	RecordComponentBinding[] resolvedComponents = this.components;
	try {
		// Note: do not sort the components
		RecordComponentBinding[] componentsSnapshot = this.components;
		for (int i = 0, length = componentsSnapshot.length; i < length; i++) {
			if (resolveTypeFor(componentsSnapshot[i]) == null) {
				// do not alter original component array until resolution is over, due to reentrance (143259)
				// TODO: to check for relevance
				if (resolvedComponents == componentsSnapshot) {
					System.arraycopy(componentsSnapshot, 0, resolvedComponents = new RecordComponentBinding[length], 0, length);
				}
				resolvedComponents[i] = null;
				failed++;
			} else {
				// we need to complete some unfinished work here - find the synthetic accessor method
				// and fill in the blanks
				RecordComponentBinding rcb = resolvedComponents[i];
				MethodBinding accessor = getRecordComponentAccessor(rcb.name);
				if (accessor instanceof SyntheticMethodBinding) { // double checking
					SyntheticMethodBinding smb = (SyntheticMethodBinding) accessor;
					TypeBinding leafType = rcb.type.leafComponentType();
					if (leafType instanceof ReferenceBinding && (((ReferenceBinding) leafType).modifiers & ExtraCompilerModifiers.AccGenericSignature) != 0)
						smb.modifiers |= ExtraCompilerModifiers.AccGenericSignature;
					// Don't copy the annotations to the accessor method's return type from record component
					smb.returnType = rcb.type.unannotated();
					// add code for implicit canonical constructor argument annotations also
					for (FieldBinding f : this.fields) {
						if (f.isRecordComponent() && CharOperation.equals(f.name, rcb.name)) {
							smb.targetReadField = f;
							ASTNode.copyRecordComponentAnnotations(this.scope, smb,
									rcb.sourceRecordComponent().annotations);
							// Note: a) type use bit set above and b) reusing rcb type, so
							// copySE8Annot already done for rcb.type, hence not
							// required here (tricky on an SMB without ast)
							break;
						}
					}
				}
			}
		}
	} finally {
		if (failed > 0) {
			int newSize = resolvedComponents.length - failed;
			if (newSize == 0)
				return setComponents(Binding.NO_COMPONENTS);

			RecordComponentBinding[] newComponents = new RecordComponentBinding[newSize];
			for (int i = 0, j = 0, length = resolvedComponents.length; i < length; i++) {
				if (resolvedComponents[i] != null)
					newComponents[j++] = resolvedComponents[i];
			}
			setComponents(newComponents);
		}
		//fill in the type for SMB Constructor
		for (MethodBinding method : this.methods) {
			if (method instanceof SyntheticMethodBinding) {
				SyntheticMethodBinding smb = (SyntheticMethodBinding) method;
				if (smb.purpose == SyntheticMethodBinding.RecordCanonicalConstructor) {
					for (int i = 0, l = smb.parameters.length; i < l; ++i) {
						smb.parameters[i] = this.components[i].type;
					}
					if (this.isVarArgs == true) {
						smb.modifiers |= ClassFileConstants.AccVarargs;
					}
				}
			}
		}
	}
	this.extendedTagBits |= ExtendedTagBits.AreRecordComponentsComplete;
	return this.components;
}

public RecordComponentBinding resolveTypeFor(RecordComponentBinding component) {
	if (!isPrototype())
		return this.prototype.resolveTypeFor(component);

	if ((component.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0)
		return component;

	component.getAnnotationTagBits();
	if ((component.getAnnotationTagBits() & TagBits.AnnotationDeprecated) != 0)  // TODO: Watch out the spec changes
		component.modifiers |= ClassFileConstants.AccDeprecated;  // expected to be available soon.

	if (isViewedAsDeprecated() && !component.isDeprecated()) {
		component.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
		component.tagBits |= this.tagBits & TagBits.AnnotationTerminallyDeprecated;
	}
	if (hasRestrictedAccess())
		component.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;
	RecordComponent[] componentDecls = this.scope.referenceContext.recordComponents;
	int length = componentDecls == null ? 0 : componentDecls.length;
	for (int f = 0; f < length; f++) {
		if (componentDecls[f].binding != component)
			continue;

		// component cannot be static, hence no static initializer scope
		MethodScope initializationScope = this.scope.referenceContext.initializerScope;
		RecordComponent componentDecl = componentDecls[f];
		TypeBinding componentType = componentDecl.type.resolveType(initializationScope, true /* check bounds*/);
		component.type = componentType;
		component.modifiers &= ~ExtraCompilerModifiers.AccUnresolved;
		if (componentType == null) {
			componentDecl.binding = null;
			return null;
		}
		if (componentType == TypeBinding.VOID) {
			this.scope.problemReporter().recordComponentCannotBeVoid(componentDecl);
			componentDecl.binding = null;
			return null;
		}
		if (componentType.isArrayType() && ((ArrayBinding) componentType).leafComponentType == TypeBinding.VOID) {
			this.scope.problemReporter().variableTypeCannotBeVoidArray(componentDecl);
			componentDecl.binding = null;
			return null;
		}
		if ((componentType.tagBits & TagBits.HasMissingType) != 0) {
			component.tagBits |= TagBits.HasMissingType;
		}
		TypeBinding leafType = componentType.leafComponentType();
		if (leafType instanceof ReferenceBinding && (((ReferenceBinding)leafType).modifiers & ExtraCompilerModifiers.AccGenericSignature) != 0) {
			component.modifiers |= ExtraCompilerModifiers.AccGenericSignature;
		}
		Annotation [] annotations = componentDecl.annotations;
		ASTNode.copyRecordComponentAnnotations(initializationScope, component, annotations);

		long sourceLevel = this.scope.compilerOptions().sourceLevel;
		if (sourceLevel >= ClassFileConstants.JDK1_8) {
			if (annotations != null && annotations.length != 0) {
				// piggybacking on an existing method to move type_use annotations to type in record component
				ASTNode.copySE8AnnotationsToType(initializationScope, component, annotations, false);
			}
			Annotation.isTypeUseCompatible(componentDecl.type, this.scope, annotations);
		}
		// TODO Bug 562478: apply null default: - to check anything to be done? - SH
//		if (this.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {}

		if (initializationScope.shouldCheckAPILeaks(this, component.isPublic()) && componentDecl.type != null) // fieldDecl.type is null for enum constants
			initializationScope.detectAPILeaks(componentDecl.type, componentType);

		if (this.externalAnnotationProvider != null) {
			ExternalAnnotationSuperimposer.annotateComponentBinding(component, this.externalAnnotationProvider, this.environment);
		}
		return component;
	}
	return null; // should never reach this point
}

private void internalFaultInTypeForFieldsAndMethods() {
	fields();
	methods();

	for (int i = 0, length = this.memberTypes.length; i < length; i++)
		((SourceTypeBinding) this.memberTypes[i]).internalFaultInTypeForFieldsAndMethods();
}
// NOTE: the type of each field of a source type is resolved when needed
@Override
public FieldBinding[] fields() {

	components(); // In a record declaration, the components should be complete prior to fields and probably for methods
	if (!isPrototype()) {
		if ((this.tagBits & TagBits.AreFieldsComplete) != 0)
			return this.fields;
		this.tagBits |= TagBits.AreFieldsComplete;
		return this.fields = this.prototype.fields();
	}

	if ((this.tagBits & TagBits.AreFieldsComplete) != 0)
		return this.fields;

	int failed = 0;
	FieldBinding[] resolvedFields = this.fields;
	try {
		// lazily sort fields
		if ((this.tagBits & TagBits.AreFieldsSorted) == 0) {
			int length = this.fields.length;
			if (length > 1)
				ReferenceBinding.sortFields(this.fields, 0, length);
			this.tagBits |= TagBits.AreFieldsSorted;
		}
		FieldBinding[] fieldsSnapshot = this.fields;
		for (int i = 0, length = fieldsSnapshot.length; i < length; i++) {
			if (resolveTypeFor(fieldsSnapshot[i]) == null) {
				// do not alter original field array until resolution is over, due to reentrance (143259)
				if (resolvedFields == fieldsSnapshot) {
					System.arraycopy(fieldsSnapshot, 0, resolvedFields = new FieldBinding[length], 0, length);
				}
				resolvedFields[i] = null;
				failed++;
			}
		}
	} finally {
		if (failed > 0) {
			// ensure fields are consistent reqardless of the error
			int newSize = resolvedFields.length - failed;
			if (newSize == 0)
				return setFields(Binding.NO_FIELDS);

			FieldBinding[] newFields = new FieldBinding[newSize];
			for (int i = 0, j = 0, length = resolvedFields.length; i < length; i++) {
				if (resolvedFields[i] != null)
					newFields[j++] = resolvedFields[i];
			}
			setFields(newFields);
		}
	}
	this.tagBits |= TagBits.AreFieldsComplete;
	computeRecordComponents();
	return this.fields;
}
/**
 * @see org.eclipse.jdt.internal.compiler.lookup.TypeBinding#genericTypeSignature()
 */
@Override
public char[] genericTypeSignature() {
	if (!isPrototype())
		return this.prototype.genericTypeSignature();

    if (this.genericReferenceTypeSignature == null)
    	this.genericReferenceTypeSignature = computeGenericTypeSignature(this.typeVariables);
    return this.genericReferenceTypeSignature;
}
/**
 * <param1 ... paramN>superclass superinterface1 ... superinterfaceN
 * <T:LY<TT;>;U:Ljava/lang/Object;V::Ljava/lang/Runnable;:Ljava/lang/Cloneable;:Ljava/util/Map;>Ljava/lang/Exception;Ljava/lang/Runnable;
 */
public char[] genericSignature() {
	if (!isPrototype())
		return this.prototype.genericSignature();

    StringBuilder sig = null;
	if (this.typeVariables != Binding.NO_TYPE_VARIABLES) {
	    sig = new StringBuilder(10);
	    sig.append('<');
	    for (int i = 0, length = this.typeVariables.length; i < length; i++)
	        sig.append(this.typeVariables[i].genericSignature());
	    sig.append('>');
	} else {
	    // could still need a signature if any of supertypes is parameterized
	    noSignature: if (this.superclass == null || !this.superclass.isParameterizedType()) {
		    for (int i = 0, length = this.superInterfaces.length; i < length; i++)
		        if (this.superInterfaces[i].isParameterizedType())
					break noSignature;
	        return null;
	    }
	    sig = new StringBuilder(10);
	}
	if (this.superclass != null)
		sig.append(this.superclass.genericTypeSignature());
	else // interface scenario only (as Object cannot be generic) - 65953
		sig.append(this.scope.getJavaLangObject().genericTypeSignature());
    for (int i = 0, length = this.superInterfaces.length; i < length; i++)
        sig.append(this.superInterfaces[i].genericTypeSignature());
	return sig.toString().toCharArray();
}

/**
 * Compute the tagbits for standard annotations. For source types, these could require
 * lazily resolving corresponding annotation nodes, in case of forward references.
 * For type use bindings, this method still returns the tagbits corresponding to the type
 * declaration binding.
 * @see org.eclipse.jdt.internal.compiler.lookup.Binding#getAnnotationTagBits()
 */
@Override
public long getAnnotationTagBits() {
	if (!isPrototype())
		return this.prototype.getAnnotationTagBits();

	if ((this.tagBits & TagBits.AnnotationResolved) == 0 && this.scope != null) {
		if ((this.tagBits & TagBits.EndHierarchyCheck) == 0) {
			CompilationUnitScope pkgCUS = this.scope.compilationUnitScope();
			boolean current = pkgCUS.connectingHierarchy;
			pkgCUS.connectingHierarchy = true;
			try {
				initAnnotationTagBits();
			} finally {
				pkgCUS.connectingHierarchy = current;
			}
		} else {
			initAnnotationTagBits();
		}
	}
	return this.tagBits;
}
private void initAnnotationTagBits() {
	TypeDeclaration typeDecl = this.scope.referenceContext;
	boolean old = typeDecl.staticInitializerScope.insideTypeAnnotation;
	try {
		typeDecl.staticInitializerScope.insideTypeAnnotation = true;
		ASTNode.resolveAnnotations(typeDecl.staticInitializerScope, typeDecl.annotations, this);
	} finally {
		typeDecl.staticInitializerScope.insideTypeAnnotation = old;
	}
	if ((this.tagBits & TagBits.AnnotationDeprecated) != 0)
		this.modifiers |= ClassFileConstants.AccDeprecated;
}
public MethodBinding[] getDefaultAbstractMethods() {
	if (!isPrototype())
		return this.prototype.getDefaultAbstractMethods();

	int count = 0;
	for (int i = this.methods.length; --i >= 0;)
		if (this.methods[i].isDefaultAbstract())
			count++;
	if (count == 0) return Binding.NO_METHODS;

	MethodBinding[] result = new MethodBinding[count];
	count = 0;
	for (int i = this.methods.length; --i >= 0;)
		if (this.methods[i].isDefaultAbstract())
			result[count++] = this.methods[i];
	return result;
}
// NOTE: the return type, arg & exception types of each method of a source type are resolved when needed
@Override
public MethodBinding getExactConstructor(TypeBinding[] argumentTypes) {
	if (!isPrototype())
		return this.prototype.getExactConstructor(argumentTypes);

	int argCount = argumentTypes.length;
	if (this.isRecordDeclaration && argCount > 0)
		methods();
	if ((this.tagBits & TagBits.AreMethodsComplete) != 0) { // have resolved all arg types & return type of the methods
		long range;
		if ((range = ReferenceBinding.binarySearch(TypeConstants.INIT, this.methods)) >= 0) {
			nextMethod: for (int imethod = (int)range, end = (int)(range >> 32); imethod <= end; imethod++) {
				MethodBinding method = this.methods[imethod];
				if (method.parameters.length == argCount) {
					TypeBinding[] toMatch = method.parameters;
					for (int iarg = 0; iarg < argCount; iarg++)
						if (TypeBinding.notEquals(toMatch[iarg], argumentTypes[iarg]))
							continue nextMethod;
					return method;
				}
			}
		}
	} else {
		// lazily sort methods
		if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
			int length = this.methods.length;
			if (length > 1)
				ReferenceBinding.sortMethods(this.methods, 0, length);
			this.tagBits |= TagBits.AreMethodsSorted;
		}
		long range;
		if ((range = ReferenceBinding.binarySearch(TypeConstants.INIT, this.methods)) >= 0) {
			nextMethod: for (int imethod = (int)range, end = (int)(range >> 32); imethod <= end; imethod++) {
				MethodBinding method = this.methods[imethod];
				if (resolveTypesFor(method) == null || method.returnType == null) {
					methods();
					return getExactConstructor(argumentTypes);  // try again since the problem methods have been removed
				}
				if (method.parameters.length == argCount) {
					TypeBinding[] toMatch = method.parameters;
					for (int iarg = 0; iarg < argCount; iarg++)
						if (TypeBinding.notEquals(toMatch[iarg], argumentTypes[iarg]))
							continue nextMethod;
					return method;
				}
			}
		}
	}
	return null;
}

/* package */ MethodBinding getSyntheticCanon() {
	if (this.isRecordDeclaration) {
		SyntheticMethodBinding[] smbs = this.syntheticMethods();
		int len = smbs != null ? smbs.length : 0;
		if (len > 0) {
			for (MethodBinding method : smbs) {
				if ((CharOperation.equals(TypeConstants.INIT, method.selector)))
					return method;
			}
		}
	}
	return null;
}
//NOTE: the return type, arg & exception types of each method of a source type are resolved when needed
//searches up the hierarchy as long as no potential (but not exact) match was found.
@Override
public MethodBinding getExactMethod(char[] selector, TypeBinding[] argumentTypes, CompilationUnitScope refScope) {
	if (!isPrototype())
		return this.prototype.getExactMethod(selector, argumentTypes, refScope);

	// sender from refScope calls recordTypeReference(this)
	int argCount = argumentTypes.length;
	boolean foundNothing = true;

	if ((this.tagBits & TagBits.AreMethodsComplete) != 0) { // have resolved all arg types & return type of the methods
		long range;
		if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
			nextMethod: for (int imethod = (int)range, end = (int)(range >> 32); imethod <= end; imethod++) {
				MethodBinding method = this.methods[imethod];
				foundNothing = false; // inner type lookups must know that a method with this name exists
				if (method.parameters.length == argCount) {
					TypeBinding[] toMatch = method.parameters;
					for (int iarg = 0; iarg < argCount; iarg++)
						if (TypeBinding.notEquals(toMatch[iarg], argumentTypes[iarg]))
							continue nextMethod;
					return method;
				}
			}
		}
	} else {
		// lazily sort methods
		if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
			int length = this.methods.length;
			if (length > 1)
				ReferenceBinding.sortMethods(this.methods, 0, length);
			this.tagBits |= TagBits.AreMethodsSorted;
		}

		long range;
		if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
			// check unresolved method
			int start = (int) range, end = (int) (range >> 32);
			for (int imethod = start; imethod <= end; imethod++) {
				MethodBinding method = this.methods[imethod];
				if (resolveTypesFor(method) == null || method.returnType == null) {
					methods();
					return getExactMethod(selector, argumentTypes, refScope); // try again since the problem methods have been removed
				}
			}
			// check dup collisions
			boolean isSource15 = this.scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
			for (int i = start; i <= end; i++) {
				MethodBinding method1 = this.methods[i];
				for (int j = end; j > i; j--) {
					MethodBinding method2 = this.methods[j];
					boolean paramsMatch = isSource15
						? method1.areParameterErasuresEqual(method2)
						: method1.areParametersEqual(method2);
					if (paramsMatch) {
						methods();
						return getExactMethod(selector, argumentTypes, refScope); // try again since the problem methods have been removed
					}
				}
			}
			nextMethod: for (int imethod = start; imethod <= end; imethod++) {
				MethodBinding method = this.methods[imethod];
				TypeBinding[] toMatch = method.parameters;
				if (toMatch.length == argCount) {
					for (int iarg = 0; iarg < argCount; iarg++)
						if (TypeBinding.notEquals(toMatch[iarg], argumentTypes[iarg]))
							continue nextMethod;
					return method;
				}
			}
		}
	}

	if (foundNothing) {
		if (isInterface()) {
			 if (this.superInterfaces.length == 1) {
				if (refScope != null)
					refScope.recordTypeReference(this.superInterfaces[0]);
				return this.superInterfaces[0].getExactMethod(selector, argumentTypes, refScope);
			 }
		} else if (this.superclass != null) {
			if (refScope != null)
				refScope.recordTypeReference(this.superclass);
			return this.superclass.getExactMethod(selector, argumentTypes, refScope);
		}
	}
	return null;
}

//NOTE: the type of a field of a source type is resolved when needed
@Override
public FieldBinding getField(char[] fieldName, boolean needResolve) {

	if (!isPrototype())
		return this.prototype.getField(fieldName, needResolve);

	if ((this.tagBits & TagBits.AreFieldsComplete) != 0)
		return ReferenceBinding.binarySearch(fieldName, this.fields);

	// lazily sort fields
	if ((this.tagBits & TagBits.AreFieldsSorted) == 0) {
		int length = this.fields.length;
		if (length > 1)
			ReferenceBinding.sortFields(this.fields, 0, length);
		this.tagBits |= TagBits.AreFieldsSorted;
	}
	// always resolve anyway on source types
	FieldBinding field = ReferenceBinding.binarySearch(fieldName, this.fields);
	if (field != null) {
		FieldBinding result = null;
		try {
			result = resolveTypeFor(field);
			return result;
		} finally {
			if (result == null) {
				// ensure fields are consistent reqardless of the error
				int newSize = this.fields.length - 1;
				if (newSize == 0) {
					setFields(Binding.NO_FIELDS);
				} else {
					FieldBinding[] newFields = new FieldBinding[newSize];
					int index = 0;
					for (int i = 0, length = this.fields.length; i < length; i++) {
						FieldBinding f = this.fields[i];
						if (f == field) continue;
						newFields[index++] = f;
					}
					setFields(newFields);
				}
			}
		}
	}
	return null;
}

// NOTE: the return type, arg & exception types of each method of a source type are resolved when needed
@Override
public MethodBinding[] getMethods(char[] selector) {
	if (!isPrototype())
		return this.prototype.getMethods(selector);

	if ((this.tagBits & TagBits.AreMethodsComplete) != 0) {
		long range;
		if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
			int start = (int) range, end = (int) (range >> 32);
			int length = end - start + 1;
			MethodBinding[] result;
			System.arraycopy(this.methods, start, result = new MethodBinding[length], 0, length);
			return result;
		} else {
			return Binding.NO_METHODS;
		}
	}
	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}
	MethodBinding[] result;
	long range;
	if ((range = ReferenceBinding.binarySearch(selector, this.methods)) >= 0) {
		int start = (int) range, end = (int) (range >> 32);
		for (int i = start; i <= end; i++) {
			MethodBinding method = this.methods[i];
			if (resolveTypesFor(method) == null || method.returnType == null) {
				methods();
				return getMethods(selector); // try again since the problem methods have been removed
			}
		}
		int length = end - start + 1;
		System.arraycopy(this.methods, start, result = new MethodBinding[length], 0, length);
	} else {
		return Binding.NO_METHODS;
	}
	boolean isSource15 = this.scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
	for (int i = 0, length = result.length - 1; i < length; i++) {
		MethodBinding method = result[i];
		for (int j = length; j > i; j--) {
			boolean paramsMatch = isSource15
				? method.areParameterErasuresEqual(result[j])
				: method.areParametersEqual(result[j]);
			if (paramsMatch) {
				methods();
				return getMethods(selector); // try again since the duplicate methods have been removed
			}
		}
	}
	return result;
}
public void generateSyntheticFinalFieldInitialization(CodeStream codeStream) {
	if (this.synthetics == null || this.synthetics[SourceTypeBinding.FIELD_EMUL] == null)
		return;
	Collection<FieldBinding> syntheticFields = this.synthetics[SourceTypeBinding.FIELD_EMUL].values();
	for (FieldBinding field : syntheticFields) {
		if (CharOperation.prefixEquals(TypeConstants.SYNTHETIC_SWITCH_ENUM_TABLE, field.name) && field.isFinal()) {
			MethodBinding[] accessors = (MethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(new String(field.name));
			if (accessors == null || accessors[0] == null) // not a field for switch enum
				continue;
			codeStream.invoke(Opcodes.OPC_invokestatic, accessors[0], null /* default declaringClass */);
			codeStream.fieldAccess(Opcodes.OPC_putstatic, field, null /* default declaringClass */);
		}
	}
}
/* Answer the synthetic field for <actualOuterLocalVariable>
*	or null if one does not exist.
*/
public FieldBinding getSyntheticField(LocalVariableBinding actualOuterLocalVariable) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null || this.synthetics[SourceTypeBinding.FIELD_EMUL] == null) return null;
	return (FieldBinding) this.synthetics[SourceTypeBinding.FIELD_EMUL].get(actualOuterLocalVariable);
}
/* Answer the synthetic field for <targetEnclosingType>
*	or null if one does not exist.
*/
public FieldBinding getSyntheticField(ReferenceBinding targetEnclosingType, boolean onlyExactMatch) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null || this.synthetics[SourceTypeBinding.FIELD_EMUL] == null) return null;
	FieldBinding field = (FieldBinding) this.synthetics[SourceTypeBinding.FIELD_EMUL].get(targetEnclosingType);
	if (field != null) return field;

	// type compatibility : to handle cases such as
	// class T { class M{}}
	// class S extends T { class N extends M {}} --> need to use S as a default enclosing instance for the super constructor call in N().
	if (!onlyExactMatch){
		Iterator accessFields = this.synthetics[SourceTypeBinding.FIELD_EMUL].values().iterator();
		while (accessFields.hasNext()) {
			field = (FieldBinding) accessFields.next();
			if (CharOperation.prefixEquals(TypeConstants.SYNTHETIC_ENCLOSING_INSTANCE_PREFIX, field.name)
				&& field.type.findSuperTypeOriginatingFrom(targetEnclosingType) != null)
					return field;
		}
	}
	return null;
}
/*
 * Answer the bridge method associated for an  inherited methods or null if one does not exist
 */
public SyntheticMethodBinding getSyntheticBridgeMethod(MethodBinding inheritedMethodToBridge) {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.synthetics == null) return null;
	if (this.synthetics[SourceTypeBinding.METHOD_EMUL] == null) return null;
	SyntheticMethodBinding[] accessors = (SyntheticMethodBinding[]) this.synthetics[SourceTypeBinding.METHOD_EMUL].get(inheritedMethodToBridge);
	if (accessors == null) return null;
	return accessors[1];
}

@Override
public boolean hasTypeBit(int bit) {
	if (!isPrototype()) {
		return this.prototype.hasTypeBit(bit);
	}
	// source types initialize type bits during connectSuperclass/interfaces()
	return (this.typeBits & bit) != 0;
}

/**
 * @see org.eclipse.jdt.internal.compiler.lookup.Binding#initializeDeprecatedAnnotationTagBits()
 */
@Override
public void initializeDeprecatedAnnotationTagBits() {
	if (!isPrototype()) {
		this.prototype.initializeDeprecatedAnnotationTagBits();
		return;
	}
	if ((this.tagBits & TagBits.DeprecatedAnnotationResolved) == 0) {
		TypeDeclaration typeDecl = this.scope.referenceContext;
		boolean old = typeDecl.staticInitializerScope.insideTypeAnnotation;
		try {
			typeDecl.staticInitializerScope.insideTypeAnnotation = true;
			ASTNode.resolveDeprecatedAnnotations(typeDecl.staticInitializerScope, typeDecl.annotations, this);
			this.tagBits |= TagBits.DeprecatedAnnotationResolved;
		} finally {
			typeDecl.staticInitializerScope.insideTypeAnnotation = old;
		}
		if ((this.tagBits & TagBits.AnnotationDeprecated) != 0) {
			this.modifiers |= ClassFileConstants.AccDeprecated;
		}
	}
}

// ensure the receiver knows its hierarchy & fields/methods so static imports can be resolved correctly
// see bug 230026
@Override
void initializeForStaticImports() {
	if (!isPrototype()) {
		this.prototype.initializeForStaticImports();
		return;
	}
	if (this.scope == null) return; // already initialized

	if (this.superInterfaces == null)
		this.scope.connectTypeHierarchy();
	this.scope.buildFields();
	this.scope.buildMethods();
}

@Override
int getNullDefault() {

	if (!isPrototype()) {
		return this.prototype.getNullDefault();
	}
	// ensure nullness defaults are initialized at all enclosing levels:
	switch (this.nullnessDefaultInitialized) {
	case 0:
		getAnnotationTagBits(); // initialize
		//$FALL-THROUGH$
	case 1:
		getPackage().isViewedAsDeprecated(); // initialize annotations
		this.nullnessDefaultInitialized = 2;
	}
	return this.defaultNullness;
}

/**
 * Returns true if a type is identical to another one,
 * or for generic types, true if compared to its raw type.
 */
@Override
public boolean isEquivalentTo(TypeBinding otherType) {
	if (!isPrototype())
		return this.prototype.isEquivalentTo(otherType);

	if (TypeBinding.equalsEquals(this, otherType)) return true;
	if (otherType == null) return false;
	switch(otherType.kind()) {

		case Binding.WILDCARD_TYPE :
		case Binding.INTERSECTION_TYPE:
			return ((WildcardBinding) otherType).boundCheck(this);

		case Binding.PARAMETERIZED_TYPE :
			if ((otherType.tagBits & TagBits.HasDirectWildcard) == 0 && (!isMemberType() || !otherType.isMemberType()))
				return false; // should have been identical
			ParameterizedTypeBinding otherParamType = (ParameterizedTypeBinding) otherType;
			if (TypeBinding.notEquals(this, otherParamType.genericType()))
				return false;
			if (!isStatic()) { // static member types do not compare their enclosing
            	ReferenceBinding enclosing = enclosingType();
            	if (enclosing != null) {
            		ReferenceBinding otherEnclosing = otherParamType.enclosingType();
            		if (otherEnclosing == null) return false;
            		if ((otherEnclosing.tagBits & TagBits.HasDirectWildcard) == 0) {
						if (TypeBinding.notEquals(enclosing, otherEnclosing)) return false;
            		} else {
            			if (!enclosing.isEquivalentTo(otherParamType.enclosingType())) return false;
            		}
            	}
			}
			int length = this.typeVariables == null ? 0 : this.typeVariables.length;
			TypeBinding[] otherArguments = otherParamType.arguments;
			int otherLength = otherArguments == null ? 0 : otherArguments.length;
			if (otherLength != length)
				return false;
			for (int i = 0; i < length; i++)
				if (!this.typeVariables[i].isTypeArgumentContainedBy(otherArguments[i]))
					return false;
			return true;

		case Binding.RAW_TYPE :
	        return TypeBinding.equalsEquals(otherType.erasure(), this);
	}
	return false;
}
@Override
public boolean isGenericType() {
	if (!isPrototype())
		return this.prototype.isGenericType();
    return this.typeVariables != Binding.NO_TYPE_VARIABLES;
}
@Override
public boolean isHierarchyConnected() {
	if (!isPrototype())
		return this.prototype.isHierarchyConnected();
	return (this.tagBits & TagBits.EndHierarchyCheck) != 0;
}
@Override
public boolean isRepeatableAnnotationType() {
	if (!isPrototype()) throw new IllegalStateException();
	return this.containerAnnotationType != null;
}

@Override
public boolean isTaggedRepeatable() {  // tagged but not necessarily repeatable. see isRepeatableAnnotationType.
	return (this.tagBits & TagBits.AnnotationRepeatable) != 0;
}
@Override
public boolean canBeSeenBy(Scope sco) {
	SourceTypeBinding invocationType = sco.enclosingSourceType();
	if (TypeBinding.equalsEquals(invocationType, this))
		return true;
	return ((this.environment.canTypeBeAccessed(this, sco)) &&
			super.canBeSeenBy(sco));
}
@Override
public ReferenceBinding[] memberTypes() {
	if (!isPrototype()) {
		if ((this.tagBits & TagBits.HasUnresolvedMemberTypes) == 0)
			return sortedMemberTypes();
		// members obtained from the prototype are already sorted so it is safe
		// to set the sorted flag here immediately.
		ReferenceBinding [] members = this.memberTypes = this.prototype.memberTypes();
		int membersLength = members == null ? 0 : members.length;
		this.memberTypes = new ReferenceBinding[membersLength];
		for (int i = 0; i < membersLength; i++) {
			this.memberTypes[i] = this.environment.createMemberType(members[i], this);
		}
		this.tagBits &= ~TagBits.HasUnresolvedMemberTypes;
		this.memberTypesSorted = true;
	}
	return sortedMemberTypes();
}

private ReferenceBinding[] sortedMemberTypes() {
	if (!this.memberTypesSorted) {
		// lazily sort member types
		int length = this.memberTypes.length;
		if (length > 1)
			sortMemberTypes(this.memberTypes, 0, length);
		this.memberTypesSorted = true;
	}
	return this.memberTypes;
}

@Override
public boolean hasMemberTypes() {
	if (!isPrototype())
		return this.prototype.hasMemberTypes();
    return this.memberTypes.length > 0;
}

private int getImplicitCanonicalConstructor() {
	if (this.methods != null && this.scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK14) {
		for (int i = 0, l = this.methods.length; i < l; ++i) {
			MethodBinding method = this.methods[i];
			if (method.isCanonicalConstructor() && method.isImplicit())
				return i;
		}
	}
	return -1;
}
private MethodBinding checkAndGetExplicitCanonicalConstructors() {
	RecordComponentBinding[] recComps = this.components;
	int nRecordComponents = recComps.length;
	MethodBinding explictCanConstr = null;
	for (MethodBinding method : this.methods) {
		if (!method.isConstructor())
			continue;
		if (method.isImplicit()) {
			continue;
		}
		if (method.parameters.length != nRecordComponents)
			continue;
		boolean isEC = true;
		int firstErasureOnlyEqualsPosition = -1;
		for (int j = 0; j < nRecordComponents; ++j) {
			TypeBinding methodParam = method.parameters[j];
			TypeBinding recComp = recComps[j].type;
			if (TypeBinding.notEquals(methodParam, recComp)) {
				if (TypeBinding.notEquals(methodParam.erasure(), recComp.erasure())) {
					isEC = false;
					break;
				} else {
					firstErasureOnlyEqualsPosition = firstErasureOnlyEqualsPosition < 0 ? j : firstErasureOnlyEqualsPosition;
				}
			}
		}
		if (isEC) {
			explictCanConstr = checkRecordCanonicalConstructor(method, firstErasureOnlyEqualsPosition);
			// Just exit after sighting the first explicit canonical constructor,
			// because there can only be one.
			if (explictCanConstr != null)
				break;
			isEC = false; //error
		}
	}
	return explictCanConstr;
}
private int getImplicitMethod(MethodBinding[] resolvedMethods, char[] name) {
	if (resolvedMethods != null && this.scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK16) {
		for (int i = 0, l = resolvedMethods.length; i < l; ++i) {
			MethodBinding method = resolvedMethods[i];
			if (method == null || !CharOperation.equals(method.selector, name))
				continue;
			if (method.isImplicit() || method instanceof SyntheticMethodBinding)
				return i;
		}
	}
	return -1;
}
// NOTE: the return type, arg & exception types of each method of a source type are resolved when needed
@Override
public MethodBinding[] methods() {

	components(); // In a record declaration, the components should be complete prior to fields and probably for methods

	if (!isPrototype()) {
		if ((this.tagBits & TagBits.AreMethodsComplete) != 0)
			return this.methods;
		this.tagBits |= TagBits.AreMethodsComplete;
		return this.methods = this.prototype.methods();
	}

	if ((this.tagBits & TagBits.AreMethodsComplete) != 0)
		return this.methods;

	if (!areMethodsInitialized()) { // https://bugs.eclipse.org/384663
		this.scope.buildMethods();
	}

	// lazily sort methods
	if ((this.tagBits & TagBits.AreMethodsSorted) == 0) {
		int length = this.methods.length;
		if (length > 1)
			ReferenceBinding.sortMethods(this.methods, 0, length);
		this.tagBits |= TagBits.AreMethodsSorted;
	}

	int failed = 0;
	MethodBinding[] resolvedMethods = this.methods;
	try {
		for (int i = 0, length = this.methods.length; i < length; i++) {
			if ((this.tagBits & TagBits.AreMethodsComplete) != 0) {
				// recursive c-all to methods() from resolveTypesFor(..) resolved the methods
				return this.methods;
			}

			if (resolveTypesFor(this.methods[i]) == null) {
				// do not alter original method array until resolution is over, due to reentrance (143259)
				if (resolvedMethods == this.methods) {
					System.arraycopy(this.methods, 0, resolvedMethods = new MethodBinding[length], 0, length);
				}
				resolvedMethods[i] = null; // unable to resolve parameters
				failed++;
			}
		}

		// find & report collision cases
		boolean complyTo15OrAbove = this.scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
		boolean compliance16 = this.scope.compilerOptions().complianceLevel == ClassFileConstants.JDK1_6;
		int recordCanonIndex = -1;
		if (this.isRecordDeclaration) {
			recordCanonIndex = getImplicitCanonicalConstructor();
			computeRecordComponents();
			MethodBinding recordExplicitCanon = checkAndGetExplicitCanonicalConstructors();
			if (recordExplicitCanon != null) {
				if (recordCanonIndex != -1 ) {
					removeSyntheticRecordCanonicalConstructor((SyntheticMethodBinding) resolvedMethods[recordCanonIndex]);
					resolvedMethods[recordCanonIndex] = null;
					failed++;
				}
			} else if (recordCanonIndex != -1 && this.isVarArgs)  {
				// cannot say that implicit constructor is present - if there are errors
				checkAndFlagHeapPollutionForRecordImplicit(resolvedMethods[recordCanonIndex], this.scope.referenceContext);
			}
		}
		int recordEqualsIndex = getImplicitMethod(resolvedMethods, TypeConstants.EQUALS);

		for (int i = 0, length = this.methods.length; i < length; i++) {
			int severity = ProblemSeverities.Error;
			MethodBinding method = resolvedMethods[i];
			if (method == null)
				continue;
			char[] selector = method.selector;
			AbstractMethodDeclaration methodDecl = null;
			nextSibling: for (int j = i + 1; j < length; j++) {
				MethodBinding method2 = resolvedMethods[j];
				if (method2 == null)
					continue nextSibling;
				if (!CharOperation.equals(selector, method2.selector))
					break nextSibling; // methods with same selector are contiguous

				if (complyTo15OrAbove) {
					if (method.areParameterErasuresEqual(method2)) {
						// we now ignore return types in 1.7 when detecting duplicates, just as we did before 1.5
						// Only in 1.6, we have to make sure even return types are different
						// https://bugs.eclipse.org/bugs/show_bug.cgi?id=317719
						if (compliance16 && method.returnType != null && method2.returnType != null) {
							if (TypeBinding.notEquals(method.returnType.erasure(), method2.returnType.erasure())) {
								// check to see if the erasure of either method is equal to the other
								// if not, then change severity to WARNING
								TypeBinding[] params1 = method.parameters;
								TypeBinding[] params2 = method2.parameters;
								int pLength = params1.length;
								TypeVariableBinding[] vars = method.typeVariables;
								TypeVariableBinding[] vars2 = method2.typeVariables;
								boolean equalTypeVars = vars == vars2;
								MethodBinding subMethod = method2;
								if (!equalTypeVars) {
									MethodBinding temp = method.computeSubstitutedMethod(method2, this.scope.environment());
									if (temp != null) {
										equalTypeVars = true;
										subMethod = temp;
									}
								}
								boolean equalParams = method.areParametersEqual(subMethod);
								if (equalParams && equalTypeVars) {
									// duplicates regardless of return types
								} else if (vars != Binding.NO_TYPE_VARIABLES && vars2 != Binding.NO_TYPE_VARIABLES) {
									// both have type arguments. Erasure of signature of one cannot be equal to signature of other
									severity = ProblemSeverities.Warning;
								} else if (pLength > 0) {
									int index = pLength;
									// is erasure of signature of m2 same as signature of m1?
									for (; --index >= 0;) {
										if (TypeBinding.notEquals(params1[index], params2[index].erasure())) {
											// If one of them is a raw type
											if (params1[index] instanceof RawTypeBinding) {
												if (TypeBinding.notEquals(params2[index].erasure(), ((RawTypeBinding)params1[index]).actualType())) {
													break;
												}
											} else  {
												break;
											}
										}
										if (TypeBinding.equalsEquals(params1[index], params2[index])) {
											TypeBinding type = params1[index].leafComponentType();
											if (type instanceof SourceTypeBinding && type.typeVariables() != Binding.NO_TYPE_VARIABLES) {
												index = pLength; // handle comparing identical source types like X<T>... its erasure is itself BUT we need to answer false
												break;
											}
										}
									}
									if (index >= 0 && index < pLength) {
										// is erasure of signature of m1 same as signature of m2?
										for (index = pLength; --index >= 0;)
											if (TypeBinding.notEquals(params1[index].erasure(), params2[index])) {
												// If one of them is a raw type
												if (params2[index] instanceof RawTypeBinding) {
													if (TypeBinding.notEquals(params1[index].erasure(), ((RawTypeBinding)params2[index]).actualType())) {
														break;
													}
												} else  {
													break;
												}
											}

									}
									if (index >= 0) {
										// erasure of neither is equal to signature of other
										severity = ProblemSeverities.Warning;
									}
								} else if (pLength != 0){
									severity = ProblemSeverities.Warning;
								} // pLength = 0 automatically makes erasure of arguments one equal to arguments of other.
							}
							// else return types also equal. All conditions satisfied
							// to give error in 1.6 compliance as well.
						}
					} else {
						continue nextSibling;
					}
				} else if (!method.areParametersEqual(method2)) {
					// prior to 1.5, parameters identical meant a collision case
					continue nextSibling;
				}
				if (recordCanonIndex == i || recordCanonIndex == j) {
					methodDecl = this.methods[recordCanonIndex].sourceMethod();
					assert methodDecl != null;
					methodDecl.binding = null;
					// do not alter original method array until resolution is over, due to reentrance (143259)
					if (resolvedMethods == this.methods)
						System.arraycopy(this.methods, 0, resolvedMethods = new MethodBinding[length], 0, length);
					resolvedMethods[recordCanonIndex] = null;
					failed++;
					MethodBinding explicitCanonicalConstructor = recordCanonIndex == i ? this.methods[j] : this.methods[i];
					methodDecl = explicitCanonicalConstructor.sourceMethod();
					recordCanonIndex = -1; // reset;
					continue;
				}
				if (recordEqualsIndex == i || recordEqualsIndex == j) {
					methodDecl = this.methods[recordEqualsIndex].sourceMethod();
					if (methodDecl != null) {
						methodDecl.binding = null;
					}
					// do not alter original method array until resolution is over, due to reentrance (143259)
					if (resolvedMethods == this.methods)
						System.arraycopy(this.methods, 0, resolvedMethods = new MethodBinding[length], 0, length);
					removeSyntheticRecordOverrideMethod(resolvedMethods[recordEqualsIndex]);
					resolvedMethods[recordEqualsIndex] = null;
					failed++;
					continue;
				}
				// otherwise duplicates / name clash
				boolean isEnumSpecialMethod = isEnum() && (CharOperation.equals(selector,TypeConstants.VALUEOF) || CharOperation.equals(selector,TypeConstants.VALUES));
				// report duplicate
				boolean removeMethod2 = (severity == ProblemSeverities.Error) ? true : false; // do not remove if in 1.6 and just a warning given
				if (methodDecl == null) {
					methodDecl = method.sourceMethod(); // cannot be retrieved after binding is lost & may still be null if method is special
					if (methodDecl != null && methodDecl.binding != null) { // ensure its a valid user defined method
						boolean removeMethod = method.returnType == null && method2.returnType != null;
						if (isEnumSpecialMethod) {
							this.scope.problemReporter().duplicateEnumSpecialMethod(this, methodDecl);
							// remove user defined methods & keep the synthetic
							removeMethod = true;
						} else {
							this.scope.problemReporter().duplicateMethodInType(methodDecl, method.areParametersEqual(method2), severity);
						}
						if (removeMethod) {
							removeMethod2 = false;
							methodDecl.binding = null;
							// do not alter original method array until resolution is over, due to reentrance (143259)
							if (resolvedMethods == this.methods)
								System.arraycopy(this.methods, 0, resolvedMethods = new MethodBinding[length], 0, length);
							resolvedMethods[i] = null;
							failed++;
						}
					}
				}
				AbstractMethodDeclaration method2Decl = method2.sourceMethod();
				if (method2Decl != null && method2Decl.binding != null) { // ensure its a valid user defined method
					if (isEnumSpecialMethod) {
						this.scope.problemReporter().duplicateEnumSpecialMethod(this, method2Decl);
						removeMethod2 = true;
					} else {
						this.scope.problemReporter().duplicateMethodInType(method2Decl, method.areParametersEqual(method2), severity);
					}
					if (removeMethod2) {
						method2Decl.binding = null;
						// do not alter original method array until resolution is over, due to reentrance (143259)
						if (resolvedMethods == this.methods)
							System.arraycopy(this.methods, 0, resolvedMethods = new MethodBinding[length], 0, length);
						resolvedMethods[j] = null;
						failed++;
					}
				}
			}
			if (method.returnType == null && resolvedMethods[i] != null) { // forget method with invalid return type... was kept to detect possible collisions
				methodDecl = method.sourceMethod();
				if (methodDecl != null)
					methodDecl.binding = null;
				// do not alter original method array until resolution is over, due to reentrance (143259)
				if (resolvedMethods == this.methods)
					System.arraycopy(this.methods, 0, resolvedMethods = new MethodBinding[length], 0, length);
				resolvedMethods[i] = null;
				failed++;
			}
		}
	} finally {
		if ((this.tagBits & TagBits.AreMethodsComplete) != 0) {
			// recursive call to methods() from resolveTypesFor(..) resolved the methods
			return this.methods;
		}
		if (failed > 0) {
			int newSize = resolvedMethods.length - failed;
			if (newSize == 0) {
				setMethods(Binding.NO_METHODS);
			} else {
				MethodBinding[] newMethods = new MethodBinding[newSize];
				for (int i = 0, j = 0, length = resolvedMethods.length; i < length; i++)
					if (resolvedMethods[i] != null)
						newMethods[j++] = resolvedMethods[i];
				setMethods(newMethods);
			}
		}
		// handle forward references to potential default abstract methods
		addDefaultAbstractMethods();
		this.tagBits |= TagBits.AreMethodsComplete;
	}
	return this.methods;
}

static boolean isAtleastAsAccessibleAsRecord(MethodBinding canonicalConstructor) {
	ReferenceBinding enclosingRecord = canonicalConstructor.declaringClass;
	if (enclosingRecord.isPublic())
		return canonicalConstructor.isPublic();

	if (enclosingRecord.isProtected())
		return canonicalConstructor.isPublic() || canonicalConstructor.isProtected();

	if (enclosingRecord.isPrivate())
		return true;

	/* package visibility */
	return !canonicalConstructor.isPrivate();
}

private void checkCanonicalConstructorParameterNames(MethodBinding explicitCanonicalConstructor,
		AbstractMethodDeclaration methodDecl) {
	int l = explicitCanonicalConstructor.parameters != null ? explicitCanonicalConstructor.parameters.length : 0;
	if (l == 0) return;
	ReferenceBinding enclosingRecord = explicitCanonicalConstructor.declaringClass;
	assert enclosingRecord.isRecord();
	assert enclosingRecord instanceof SourceTypeBinding;
	SourceTypeBinding recordBinding = (SourceTypeBinding) enclosingRecord;
	RecordComponentBinding[] comps = recordBinding.components();
	Argument[] args = methodDecl.arguments;
	for (int i = 0; i < l; ++i) {
		if (!CharOperation.equals(args[i].name, comps[i].name))
			this.scope.problemReporter().recordIllegalParameterNameInCanonicalConstructor(comps[i], args[i]);
	}
}

private MethodBinding checkRecordCanonicalConstructor(MethodBinding explicitCanonicalConstructor, int firstErasureOnlyEqualsPosition) {

	AbstractMethodDeclaration methodDecl = explicitCanonicalConstructor.sourceMethod();
	if (methodDecl == null)
		return null;
	if (firstErasureOnlyEqualsPosition >= 0)
		this.scope.problemReporter().recordErasureIncompatibilityInCanonicalConstructor(methodDecl.arguments[firstErasureOnlyEqualsPosition].type);
	if (!SourceTypeBinding.isAtleastAsAccessibleAsRecord(explicitCanonicalConstructor))
		this.scope.problemReporter().recordCanonicalConstructorVisibilityReduced(methodDecl);
	TypeParameter[] typeParameters = methodDecl.typeParameters();
	if (typeParameters != null && typeParameters.length > 0)
		this.scope.problemReporter().recordCanonicalConstructorShouldNotBeGeneric(methodDecl);
	if (explicitCanonicalConstructor.thrownExceptions != null && explicitCanonicalConstructor.thrownExceptions.length > 0)
		this.scope.problemReporter().recordCanonicalConstructorHasThrowsClause(methodDecl);
	checkCanonicalConstructorParameterNames(explicitCanonicalConstructor, methodDecl);
	explicitCanonicalConstructor.extendedTagBits |= ExtendedTagBits.IsCanonicalConstructor;
//	checkAndFlagExplicitConstructorCallInCanonicalConstructor(methodDecl);
	return explicitCanonicalConstructor;
}

@Override
public ReferenceBinding[] permittedTypes() {
	return this.permittedTypes;
}

@Override
public TypeBinding prototype() {
	return this.prototype;
}

public boolean isPrototype() {
	return this == this.prototype;  //$IDENTITY-COMPARISON$
}

@Override
public boolean isRecord() {
	return this.isRecordDeclaration;
}

@Override
public ReferenceBinding containerAnnotationType() {

	if (!isPrototype()) throw new IllegalStateException();

	if (this.containerAnnotationType instanceof UnresolvedReferenceBinding) {
		this.containerAnnotationType = (ReferenceBinding)BinaryTypeBinding.resolveType(this.containerAnnotationType, this.scope.environment(), false);
	}
	return this.containerAnnotationType;
}

public FieldBinding resolveTypeFor(FieldBinding field) {

	if (!isPrototype())
		return this.prototype.resolveTypeFor(field);

	if ((field.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0)
		return field;

	long sourceLevel = this.scope.compilerOptions().sourceLevel;
	if (sourceLevel >= ClassFileConstants.JDK1_5) {
		if ((field.getAnnotationTagBits() & TagBits.AnnotationDeprecated) != 0)
			field.modifiers |= ClassFileConstants.AccDeprecated;
	}
	if (isViewedAsDeprecated() && !field.isDeprecated()) {
		field.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
		field.tagBits |= this.tagBits & TagBits.AnnotationTerminallyDeprecated;
	}
	if (hasRestrictedAccess())
		field.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;
	FieldDeclaration[] fieldDecls = this.scope.referenceContext.fields;
	int length = fieldDecls == null ? 0 : fieldDecls.length;
	for (int f = 0; f < length; f++) {
		if (fieldDecls[f].binding != field)
			continue;

		MethodScope initializationScope = field.isStatic()
			? this.scope.referenceContext.staticInitializerScope
			: this.scope.referenceContext.initializerScope;
		FieldBinding previousField = initializationScope.initializedField;
		try {
			initializationScope.initializedField = field;
			FieldDeclaration fieldDecl = fieldDecls[f];
			TypeBinding fieldType =
				fieldDecl.getKind() == AbstractVariableDeclaration.ENUM_CONSTANT
					? initializationScope.environment().convertToRawType(this, false /*do not force conversion of enclosing types*/) // enum constant is implicitly of declaring enum type
					: fieldDecl.type.resolveType(initializationScope, true /* check bounds*/);
			field.type = fieldType;
			field.modifiers &= ~ExtraCompilerModifiers.AccUnresolved;
			if (fieldType == null) {
				fieldDecl.binding = null;
				return null;
			}
			if (fieldType == TypeBinding.VOID) {
				this.scope.problemReporter().variableTypeCannotBeVoid(fieldDecl);
				fieldDecl.binding = null;
				return null;
			}
			if (fieldType.isArrayType() && ((ArrayBinding) fieldType).leafComponentType == TypeBinding.VOID) {
				this.scope.problemReporter().variableTypeCannotBeVoidArray(fieldDecl);
				fieldDecl.binding = null;
				return null;
			}
			if ((fieldType.tagBits & TagBits.HasMissingType) != 0) {
				field.tagBits |= TagBits.HasMissingType;
			}
			TypeBinding leafType = fieldType.leafComponentType();
			if (leafType instanceof ReferenceBinding && (((ReferenceBinding)leafType).modifiers & ExtraCompilerModifiers.AccGenericSignature) != 0) {
				field.modifiers |= ExtraCompilerModifiers.AccGenericSignature;
			}

			Annotation[] relevantRecordComponentAnnotations = null;
			if (sourceLevel >= ClassFileConstants.JDK14) {
				// copy annotations from record component if applicable
				if (field.isRecordComponent()) {
					RecordComponentBinding rcb = getRecordComponent(field.name);
					if (rcb != null)
						relevantRecordComponentAnnotations = ASTNode.copyRecordComponentAnnotations(initializationScope,
								field, rcb.sourceRecordComponent().annotations);
				}
			}
			if (sourceLevel >= ClassFileConstants.JDK1_8) {
				Annotation [] annotations = fieldDecl.annotations;
				if (annotations == null && relevantRecordComponentAnnotations != null) // field represents a record component.
					annotations = relevantRecordComponentAnnotations;

				if (annotations != null && annotations.length != 0) {
					ASTNode.copySE8AnnotationsToType(initializationScope, field, annotations,
							fieldDecl.getKind() == AbstractVariableDeclaration.ENUM_CONSTANT); // type annotation is illegal on enum constant
				}
				Annotation.isTypeUseCompatible(fieldDecl.type, this.scope, annotations);
			}
			// apply null default:
			if (this.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
				// TODO(SH): different strategy for 1.8, or is "repair" below enough?
				if (fieldDecl.getKind() == AbstractVariableDeclaration.ENUM_CONSTANT) {
					// enum constants neither have a type declaration nor can they be null
					field.tagBits |= TagBits.AnnotationNonNull;
				} else {
					if (hasNonNullDefaultFor(DefaultLocationField, fieldDecl.sourceStart)) {
						field.fillInDefaultNonNullness(fieldDecl, initializationScope);
					}
					// validate null annotation:
					if (!this.scope.validateNullAnnotation(field.tagBits, fieldDecl.type, fieldDecl.annotations))
						field.tagBits &= ~TagBits.AnnotationNullMASK;
				}
			}
			if (initializationScope.shouldCheckAPILeaks(this, field.isPublic()) && fieldDecl.type != null) // fieldDecl.type is null for enum constants
				initializationScope.detectAPILeaks(fieldDecl.type, fieldType);
		} finally {
		    initializationScope.initializedField = previousField;
		}
		if (this.externalAnnotationProvider != null) {
			ExternalAnnotationSuperimposer.annotateFieldBinding(field, this.externalAnnotationProvider, this.environment);
		}
		return field;
	}
	return null; // should never reach this point
}

public MethodBinding resolveTypesFor(MethodBinding method) {
	ProblemReporter problemReporter = this.scope.problemReporter();
	IErrorHandlingPolicy suspendedPolicy = problemReporter.suspendTempErrorHandlingPolicy();
	try {
		return resolveTypesWithSuspendedTempErrorHandlingPolicy(method);
	} finally {
		problemReporter.resumeTempErrorHandlingPolicy(suspendedPolicy);
	}
}

private MethodBinding resolveTypesWithSuspendedTempErrorHandlingPolicy(MethodBinding method) {
	if (!isPrototype())
		return this.prototype.resolveTypesFor(method);

	if ((method.modifiers & ExtraCompilerModifiers.AccUnresolved) == 0)
		return method;

	final long sourceLevel = this.scope.compilerOptions().sourceLevel;
	if (sourceLevel >= ClassFileConstants.JDK1_5) {
		ReferenceBinding object = this.scope.getJavaLangObject();
		TypeVariableBinding[] tvb = method.typeVariables;
		for (int i = 0; i < tvb.length; i++)
			tvb[i].superclass = object;		// avoid null (see https://bugs.eclipse.org/426048)

		if ((method.getAnnotationTagBits() & TagBits.AnnotationDeprecated) != 0)
			method.modifiers |= ClassFileConstants.AccDeprecated;
	}
	if (isViewedAsDeprecated() && !method.isDeprecated()) {
		method.modifiers |= ExtraCompilerModifiers.AccDeprecatedImplicitly;
		method.tagBits |= this.tagBits & TagBits.AnnotationTerminallyDeprecated;
	}
	if (hasRestrictedAccess())
		method.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;

	AbstractMethodDeclaration methodDecl = method.sourceMethod();
	if (methodDecl == null) return null; // method could not be resolved in previous iteration


	TypeParameter[] typeParameters = methodDecl.typeParameters();
	if (typeParameters != null) {
		methodDecl.scope.connectTypeVariables(typeParameters, true);
		// Perform deferred bound checks for type variables (only done after type variable hierarchy is connected)
		for (int i = 0, paramLength = typeParameters.length; i < paramLength; i++)
			typeParameters[i].checkBounds(methodDecl.scope);
	}
	TypeReference[] exceptionTypes = methodDecl.thrownExceptions;
	if (exceptionTypes != null) {
		int size = exceptionTypes.length;
		method.thrownExceptions = new ReferenceBinding[size];
		int count = 0;
		ReferenceBinding resolvedExceptionType;
		for (int i = 0; i < size; i++) {
			resolvedExceptionType = (ReferenceBinding) exceptionTypes[i].resolveType(methodDecl.scope, true /* check bounds*/);
			if (resolvedExceptionType == null)
				continue;
			if (resolvedExceptionType.isBoundParameterizedType()) {
				methodDecl.scope.problemReporter().invalidParameterizedExceptionType(resolvedExceptionType, exceptionTypes[i]);
				continue;
			}
			if (resolvedExceptionType.findSuperTypeOriginatingFrom(TypeIds.T_JavaLangThrowable, true) == null) {
				if (resolvedExceptionType.isValidBinding()) {
					methodDecl.scope.problemReporter().cannotThrowType(exceptionTypes[i], resolvedExceptionType);
					continue;
				}
			}
			if ((resolvedExceptionType.tagBits & TagBits.HasMissingType) != 0) {
				method.tagBits |= TagBits.HasMissingType;
			}
			if (exceptionTypes[i].hasNullTypeAnnotation(AnnotationPosition.ANY)) {
				methodDecl.scope.problemReporter().nullAnnotationUnsupportedLocation(exceptionTypes[i]);
			}
			method.modifiers |= (resolvedExceptionType.modifiers & ExtraCompilerModifiers.AccGenericSignature);
			method.thrownExceptions[count++] = resolvedExceptionType;
		}
		if (count < size)
			System.arraycopy(method.thrownExceptions, 0, method.thrownExceptions = new ReferenceBinding[count], 0, count);
	}

	if (methodDecl.receiver != null) {
		method.receiver = methodDecl.receiver.type.resolveType(methodDecl.scope, true /* check bounds*/);
	}
	final boolean reportUnavoidableGenericTypeProblems = this.scope.compilerOptions().reportUnavoidableGenericTypeProblems;
	boolean foundArgProblem = false;
	boolean checkAPIleak = methodDecl.scope.shouldCheckAPILeaks(this, method.isPublic());
	Argument[] arguments = methodDecl.arguments;
	if (arguments != null) {
		int size = arguments.length;
		method.parameters = Binding.NO_PARAMETERS;
		TypeBinding[] newParameters = new TypeBinding[size];
		for (int i = 0; i < size; i++) {
			Argument arg = arguments[i];
			if (arg.annotations != null) {
				method.tagBits |= TagBits.HasParameterAnnotations;
			}
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=322817
			boolean deferRawTypeCheck = !reportUnavoidableGenericTypeProblems && !method.isConstructor() && (arg.type.bits & ASTNode.IgnoreRawTypeCheck) == 0;
			TypeBinding parameterType;
			if (deferRawTypeCheck) {
				arg.type.bits |= ASTNode.IgnoreRawTypeCheck;
			}
			try {
				ASTNode.handleNonNullByDefault(methodDecl.scope, arg.annotations, arg);
				parameterType = arg.type.resolveType(methodDecl.scope, true /* check bounds*/);
			} finally {
				if (deferRawTypeCheck) {
					arg.type.bits &= ~ASTNode.IgnoreRawTypeCheck;
				}
			}

			if (parameterType == null) {
				foundArgProblem = true;
			} else if (parameterType == TypeBinding.VOID) {
				if (this.isRecordDeclaration &&
						methodDecl instanceof ConstructorDeclaration &&
						((methodDecl.bits & ASTNode.IsImplicit) != 0)) {
					// do nothing - already raised for record component.
				} else
					methodDecl.scope.problemReporter().argumentTypeCannotBeVoid(methodDecl, arg);
				foundArgProblem = true;
			} else {
				if ((parameterType.tagBits & TagBits.HasMissingType) != 0) {
					method.tagBits |= TagBits.HasMissingType;
				}
				TypeBinding leafType = parameterType.leafComponentType();
				if (leafType instanceof ReferenceBinding && (((ReferenceBinding) leafType).modifiers & ExtraCompilerModifiers.AccGenericSignature) != 0)
					method.modifiers |= ExtraCompilerModifiers.AccGenericSignature;
				newParameters[i] = parameterType;
				if (checkAPIleak)
					methodDecl.scope.detectAPILeaks(arg.type, parameterType);
				arg.binding = new LocalVariableBinding(arg, parameterType, arg.modifiers, methodDecl.scope);
			}
		}
		// only assign parameters if no problems are found
		if (!foundArgProblem) {
			method.parameters = newParameters;
		}
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=337799
	if (sourceLevel >= ClassFileConstants.JDK1_7) {
		if ((method.tagBits & TagBits.AnnotationSafeVarargs) != 0) {
			if (!method.isVarargs()) {
				methodDecl.scope.problemReporter().safeVarargsOnFixedArityMethod(method);
			} else if (!method.isStatic() && !method.isFinal() && !method.isConstructor()
					&& !(sourceLevel >= ClassFileConstants.JDK9 && method.isPrivate())) {
				methodDecl.scope.problemReporter().safeVarargsOnNonFinalInstanceMethod(method);
			}
		} else {
			checkAndFlagHeapPollution(method, methodDecl);
		}
	}

	boolean foundReturnTypeProblem = false;
	if (!method.isConstructor()) {
		TypeReference returnType = methodDecl instanceof MethodDeclaration
			? ((MethodDeclaration) methodDecl).returnType
			: null;
		if (returnType == null) {
			methodDecl.scope.problemReporter().missingReturnType(methodDecl);
			method.returnType = null;
			foundReturnTypeProblem = true;
		} else {
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=322817
			boolean deferRawTypeCheck = !reportUnavoidableGenericTypeProblems && (returnType.bits & ASTNode.IgnoreRawTypeCheck) == 0;
			TypeBinding methodType;
			if (deferRawTypeCheck) {
				returnType.bits |= ASTNode.IgnoreRawTypeCheck;
			}
			try {
				methodType = returnType.resolveType(methodDecl.scope, true /* check bounds*/);
			} finally {
				if (deferRawTypeCheck) {
					returnType.bits &= ~ASTNode.IgnoreRawTypeCheck;
				}
			}
			if (methodType == null) {
				foundReturnTypeProblem = true;
			} else {
				if ((methodType.tagBits & TagBits.HasMissingType) != 0) {
					method.tagBits |= TagBits.HasMissingType;
				}
				method.returnType = methodType;
				if (sourceLevel >= ClassFileConstants.JDK1_8 && !method.isVoidMethod()) {
					Annotation [] annotations = methodDecl.annotations;
					if (annotations != null && annotations.length != 0) {
						ASTNode.copySE8AnnotationsToType(methodDecl.scope, method, methodDecl.annotations, false);
					}
					Annotation.isTypeUseCompatible(returnType, this.scope, methodDecl.annotations);
				}
				TypeBinding leafType = methodType.leafComponentType();
				if (leafType instanceof ReferenceBinding && (((ReferenceBinding) leafType).modifiers & ExtraCompilerModifiers.AccGenericSignature) != 0)
					method.modifiers |= ExtraCompilerModifiers.AccGenericSignature;
				else if (leafType == TypeBinding.VOID && methodDecl.annotations != null)
					rejectTypeAnnotatedVoidMethod(methodDecl);
				if (checkAPIleak)
					methodDecl.scope.detectAPILeaks(returnType, methodType);
			}
		}
	} else {
		if (sourceLevel >= ClassFileConstants.JDK1_8) {
			Annotation [] annotations = methodDecl.annotations;
			if (annotations != null && annotations.length != 0) {
				ASTNode.copySE8AnnotationsToType(methodDecl.scope, method, methodDecl.annotations, false);
			}
		}
	}
	if (foundArgProblem) {
		methodDecl.binding = null;
		method.parameters = Binding.NO_PARAMETERS; // see 107004
		// nullify type parameter bindings as well as they have a backpointer to the method binding
		// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=81134)
		if (typeParameters != null)
			for (int i = 0, length = typeParameters.length; i < length; i++)
				typeParameters[i].binding = null;
		return null;
	}
	CompilerOptions compilerOptions = this.scope.compilerOptions();
	if (compilerOptions.isAnnotationBasedNullAnalysisEnabled) {
		if (!method.isConstructor() && method.returnType != null) {
			long nullTagBits = method.tagBits & TagBits.AnnotationNullMASK;
			if (nullTagBits != 0) {
				TypeReference returnTypeRef = ((MethodDeclaration)methodDecl).returnType;
				if (this.scope.environment().usesNullTypeAnnotations()) {
					if (!this.scope.validateNullAnnotation(nullTagBits, returnTypeRef, methodDecl.annotations))
						method.returnType.tagBits &= ~TagBits.AnnotationNullMASK;
					method.tagBits &= ~TagBits.AnnotationNullMASK;
				} else {
					if (!this.scope.validateNullAnnotation(nullTagBits, returnTypeRef, methodDecl.annotations))
						method.tagBits &= ~TagBits.AnnotationNullMASK;
				}
			}
		}
	}
	if (this.externalAnnotationProvider != null)
		ExternalAnnotationSuperimposer.annotateMethodBinding(method, arguments, this.externalAnnotationProvider, this.environment);
	if (compilerOptions.storeAnnotations)
		createArgumentBindings(method, compilerOptions); // need annotations resolved already at this point
	if (foundReturnTypeProblem)
		return method; // but its still unresolved with a null return type & is still connected to its method declaration

	method.modifiers &= ~ExtraCompilerModifiers.AccUnresolved;
	return method;
}

private void checkAndFlagHeapPollution(MethodBinding method, AbstractMethodDeclaration methodDecl) {
	if (method.parameters != null && method.parameters.length > 0 && method.isVarargs()) { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=337795
		if (!method.parameters[method.parameters.length - 1].isReifiable()) {
				methodDecl.scope.problemReporter().possibleHeapPollutionFromVararg(methodDecl.arguments[methodDecl.arguments.length - 1]);
		}
	}
}
private void checkAndFlagHeapPollutionForRecordImplicit(MethodBinding method, TypeDeclaration recordDecl) {

	if (this.isRecordDeclaration && this.isVarArgs
			&& method.parameters != null && method.parameters.length > 0) {
		int lastParamIndex = method.parameters.length - 1;
		if (!method.parameters[lastParamIndex].isReifiable()) {
			this.scope.problemReporter().possibleHeapPollutionFromVararg(recordDecl.recordComponents[lastParamIndex]);
		}
	}
}
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=391108
private static void rejectTypeAnnotatedVoidMethod(AbstractMethodDeclaration methodDecl) {
	Annotation[] annotations = methodDecl.annotations;
	int length = annotations == null ? 0 : annotations.length;
	for (int i = 0; i < length; i++) {
		ReferenceBinding binding = (ReferenceBinding) annotations[i].resolvedType;
		if (binding != null
				&& (binding.tagBits & TagBits.AnnotationForTypeUse) != 0
				&& (binding.tagBits & TagBits.AnnotationForMethod) == 0) {
			methodDecl.scope.problemReporter().illegalUsageOfTypeAnnotations(annotations[i]);
		}
	}
}

private void createArgumentBindings(MethodBinding method, CompilerOptions compilerOptions) {

	if (!isPrototype()) throw new IllegalStateException();
	if (compilerOptions.isAnnotationBasedNullAnalysisEnabled)
		getNullDefault(); // ensure initialized
	AbstractMethodDeclaration methodDecl = method.sourceMethod();
	if (methodDecl != null) {
		// while creating argument bindings we also collect explicit null annotations:
		if (method.parameters != Binding.NO_PARAMETERS)
			methodDecl.createArgumentBindings();
		// add implicit annotations (inherited(?) & default):
		if (compilerOptions.isAnnotationBasedNullAnalysisEnabled) {
			new ImplicitNullAnnotationVerifier(this.scope.environment()).checkImplicitNullAnnotations(method, methodDecl, true, this.scope);
		}
	}
}

public void evaluateNullAnnotations() {

	if (!isPrototype()) throw new IllegalStateException();

	if (this.nullnessDefaultInitialized > 0 || !this.scope.compilerOptions().isAnnotationBasedNullAnalysisEnabled)
		return;

	if ((this.tagBits & TagBits.AnnotationNullMASK) != 0) {
		Annotation[] annotations = this.scope.referenceContext.annotations;
		for (int i = 0; i < annotations.length; i++) {
			ReferenceBinding annotationType = annotations[i].getCompilerAnnotation().getAnnotationType();
			if (annotationType != null) {
				if (annotationType.hasNullBit(TypeIds.BitNonNullAnnotation|TypeIds.BitNullableAnnotation)) {
					this.scope.problemReporter().nullAnnotationUnsupportedLocation(annotations[i]);
					this.tagBits &= ~TagBits.AnnotationNullMASK;
				}
			}
		}
	}

	boolean isPackageInfo = CharOperation.equals(this.sourceName, TypeConstants.PACKAGE_INFO_NAME);
	PackageBinding pkg = getPackage();
	boolean isInDefaultPkg = (pkg.compoundName == CharOperation.NO_CHAR_CHAR);
	if (!isPackageInfo) {
		boolean isInNullnessAnnotationPackage = this.scope.environment().isNullnessAnnotationPackage(pkg);
		if (pkg.getDefaultNullness() == NO_NULL_DEFAULT && !isInDefaultPkg && !isInNullnessAnnotationPackage && !(this instanceof NestedTypeBinding)) {
			ReferenceBinding packageInfo = pkg.getType(TypeConstants.PACKAGE_INFO_NAME, this.module);
			if (packageInfo == null) {
				// no pkgInfo - complain
				this.scope.problemReporter().missingNonNullByDefaultAnnotation(this.scope.referenceContext);
				pkg.setDefaultNullness(NULL_UNSPECIFIED_BY_DEFAULT);
			} else {
				// if pkgInfo has no default annot. - complain
				packageInfo.getAnnotationTagBits();
			}
		}
	}
	this.nullnessDefaultInitialized = 1;
	if (this.defaultNullness != 0) {
		TypeDeclaration typeDecl = this.scope.referenceContext;
		if (isPackageInfo) {
			if (pkg.enclosingModule.getDefaultNullness() == this.defaultNullness) {
				this.scope.problemReporter().nullDefaultAnnotationIsRedundant(typeDecl, typeDecl.annotations, pkg.enclosingModule);
			} else {
				pkg.setDefaultNullness(this.defaultNullness);
			}
		} else {
			Binding target = this.scope.parent.checkRedundantDefaultNullness(this.defaultNullness, typeDecl.declarationSourceStart);
			if(target != null) {
				this.scope.problemReporter().nullDefaultAnnotationIsRedundant(typeDecl, typeDecl.annotations, target);
			}
		}
	} else if (isPackageInfo || (isInDefaultPkg && !(this instanceof NestedTypeBinding))) {
		this.scope.problemReporter().missingNonNullByDefaultAnnotation(this.scope.referenceContext);
		if (!isInDefaultPkg)
			pkg.setDefaultNullness(NULL_UNSPECIFIED_BY_DEFAULT);
	}
	maybeMarkTypeParametersNonNull();
}

private void maybeMarkTypeParametersNonNull() {
	if (this.typeVariables != null && this.typeVariables.length > 0) {
	// when creating type variables we didn't yet have the defaultNullness, fill it in now:
		if (this.scope == null || !this.scope.hasDefaultNullnessFor(DefaultLocationTypeParameter, this.sourceStart()))
		return;
		AnnotationBinding[] annots = new AnnotationBinding[]{ this.environment.getNonNullAnnotation() };
		for (int i = 0; i < this.typeVariables.length; i++) {
			TypeVariableBinding tvb = this.typeVariables[i];
			if ((tvb.tagBits & TagBits.AnnotationNullMASK) == 0)
				this.typeVariables[i] = (TypeVariableBinding) this.environment.createAnnotatedType(tvb, annots);
		}
	}
}

@Override
boolean hasNonNullDefaultFor(int location, int sourceStart) {

	if (!isPrototype()) throw new IllegalStateException();

	if (this.scope == null) {
		return (this.defaultNullness & location) != 0;
	}
	Scope skope = this.scope.referenceContext.initializerScope; // for @NNBD on a field
	if (skope == null)
		skope = this.scope;
	return skope.hasDefaultNullnessFor(location, sourceStart);
}

@Override
protected boolean hasMethodWithNumArgs(char[] selector, int numArgs) {
	if ((this.tagBits & TagBits.AreMethodsComplete) != 0)
		return super.hasMethodWithNumArgs(selector, numArgs);
	// otherwise don't trigger unResolvedMethods() which would actually resolve!
	if (this.scope != null && this.scope.referenceContext.methods != null) {
		for (AbstractMethodDeclaration method : this.scope.referenceContext.methods) {
			if (CharOperation.equals(method.selector, selector)) {
				if (numArgs == 0) {
					if (method.arguments == null)
						return true;
				} else {
					if (method.arguments != null && method.arguments.length == numArgs)
						return true;
				}
			}
		}
	}
	return false;
}

@Override
public AnnotationHolder retrieveAnnotationHolder(Binding binding, boolean forceInitialization) {
	if (!isPrototype())
		return this.prototype.retrieveAnnotationHolder(binding, forceInitialization);
	if (forceInitialization)
		binding.getAnnotationTagBits(); // ensure annotations are up to date
	return super.retrieveAnnotationHolder(binding, false);
}

@Override
public void setContainerAnnotationType(ReferenceBinding value) {
	if (!isPrototype()) throw new IllegalStateException();
	this.containerAnnotationType  = value;
}

@Override
public void tagAsHavingDefectiveContainerType() {
	if (!isPrototype()) throw new IllegalStateException();
	if (this.containerAnnotationType != null && this.containerAnnotationType.isValidBinding())
		this.containerAnnotationType = new ProblemReferenceBinding(this.containerAnnotationType.compoundName, this.containerAnnotationType, ProblemReasons.DefectiveContainerAnnotationType);
}

// Record Declaration - Java 14 - preview
//Propagate writes to all annotated variants so the clones evolve along.
public RecordComponentBinding[] setComponents(RecordComponentBinding[] comps) {

	if (!isPrototype())
		return this.prototype.setComponents(comps);

	if ((this.tagBits & TagBits.HasAnnotatedVariants) != 0) {
		TypeBinding [] annotatedTypes = this.scope.environment().getAnnotatedTypes(this);
		for (int i = 0, length = annotatedTypes == null ? 0 : annotatedTypes.length; i < length; i++) {
			SourceTypeBinding annotatedType = (SourceTypeBinding) annotatedTypes[i];
			annotatedType.components = comps;
		}
	}
	return this.components = comps;
}

// Propagate writes to all annotated variants so the clones evolve along.
public FieldBinding [] setFields(FieldBinding[] fields) {

	if (!isPrototype())
		return this.prototype.setFields(fields);

	if ((this.tagBits & TagBits.HasAnnotatedVariants) != 0) {
		TypeBinding [] annotatedTypes = this.scope.environment().getAnnotatedTypes(this);
		for (int i = 0, length = annotatedTypes == null ? 0 : annotatedTypes.length; i < length; i++) {
			SourceTypeBinding annotatedType = (SourceTypeBinding) annotatedTypes[i];
			annotatedType.fields = fields;
		}
	}
	return this.fields = fields;
}

// We need to specialize member types, can't just propagate. Can't specialize here, clones could created post setMemberTypes()
public ReferenceBinding [] setMemberTypes(ReferenceBinding[] memberTypes) {

	if (!isPrototype())
		return this.prototype.setMemberTypes(memberTypes);

	this.memberTypes = memberTypes;
	if ((this.tagBits & TagBits.HasAnnotatedVariants) != 0) {
		TypeBinding [] annotatedTypes = this.scope.environment().getAnnotatedTypes(this);
		for (int i = 0, length = annotatedTypes == null ? 0 : annotatedTypes.length; i < length; i++) {
			SourceTypeBinding annotatedType = (SourceTypeBinding) annotatedTypes[i];
			annotatedType.tagBits |= TagBits.HasUnresolvedMemberTypes;
			annotatedType.memberTypes(); // recompute.
		}
	}
	sortedMemberTypes();
	return this.memberTypes;
}

// Propagate writes to all annotated variants so the clones evolve along.
public MethodBinding [] setMethods(MethodBinding[] methods) {

	if (!isPrototype())
		return this.prototype.setMethods(methods);

	if ((this.tagBits & TagBits.HasAnnotatedVariants) != 0) {
		TypeBinding [] annotatedTypes = this.scope.environment().getAnnotatedTypes(this);
		for (int i = 0, length = annotatedTypes == null ? 0 : annotatedTypes.length; i < length; i++) {
			SourceTypeBinding annotatedType = (SourceTypeBinding) annotatedTypes[i];
			annotatedType.methods = methods;
		}
	}
	return this.methods = methods;
}

//Propagate writes to all annotated variants so the clones evolve along.
public ReferenceBinding [] setPermittedTypes(ReferenceBinding [] permittedTypes) {

	if (!isPrototype())
		return this.prototype.setPermittedTypes(permittedTypes);

	if ((this.tagBits & TagBits.HasAnnotatedVariants) != 0) {
		TypeBinding [] annotatedTypes = this.scope.environment().getAnnotatedTypes(this);
		for (int i = 0, length = annotatedTypes == null ? 0 : annotatedTypes.length; i < length; i++) {
			SourceTypeBinding annotatedType = (SourceTypeBinding) annotatedTypes[i];
			annotatedType.permittedTypes = permittedTypes;
		}
	}
	return this.permittedTypes = permittedTypes;
}

// Propagate writes to all annotated variants so the clones evolve along.
public ReferenceBinding setSuperClass(ReferenceBinding superClass) {

	if (!isPrototype())
		return this.prototype.setSuperClass(superClass);

	if ((this.tagBits & TagBits.HasAnnotatedVariants) != 0) {
		TypeBinding [] annotatedTypes = this.scope.environment().getAnnotatedTypes(this);
		for (int i = 0, length = annotatedTypes == null ? 0 : annotatedTypes.length; i < length; i++) {
			SourceTypeBinding annotatedType = (SourceTypeBinding) annotatedTypes[i];
			annotatedType.superclass = superClass;
		}
	}
	return this.superclass = superClass;
}

// Propagate writes to all annotated variants so the clones evolve along.
public ReferenceBinding [] setSuperInterfaces(ReferenceBinding [] superInterfaces) {

	if (!isPrototype())
		return this.prototype.setSuperInterfaces(superInterfaces);

	if ((this.tagBits & TagBits.HasAnnotatedVariants) != 0) {
		TypeBinding [] annotatedTypes = this.scope.environment().getAnnotatedTypes(this);
		for (int i = 0, length = annotatedTypes == null ? 0 : annotatedTypes.length; i < length; i++) {
			SourceTypeBinding annotatedType = (SourceTypeBinding) annotatedTypes[i];
			annotatedType.superInterfaces = superInterfaces;
		}
	}
	return this.superInterfaces = superInterfaces;
}

// Propagate writes to all annotated variants so the clones evolve along.
public TypeVariableBinding [] setTypeVariables(TypeVariableBinding [] typeVariables) {

	if (!isPrototype())
		return this.prototype.setTypeVariables(typeVariables);

	if ((this.tagBits & TagBits.HasAnnotatedVariants) != 0) {
		TypeBinding [] annotatedTypes = this.scope.environment().getAnnotatedTypes(this);
		for (int i = 0, length = annotatedTypes == null ? 0 : annotatedTypes.length; i < length; i++) {
			SourceTypeBinding annotatedType = (SourceTypeBinding) annotatedTypes[i];
			annotatedType.typeVariables = typeVariables;
		}
	}
	return this.typeVariables = typeVariables;
}

public final int sourceEnd() {
	if (!isPrototype())
		return this.prototype.sourceEnd();

	return this.scope.referenceContext.sourceEnd;
}
public final int sourceStart() {
	if (!isPrototype())
		return this.prototype.sourceStart();

	return this.scope.referenceContext.sourceStart;
}
@Override
SimpleLookupTable storedAnnotations(boolean forceInitialize, boolean forceStore) {
	if (!isPrototype())
		return this.prototype.storedAnnotations(forceInitialize, forceStore);

	if (forceInitialize && this.storedAnnotations == null && this.scope != null) { // scope null when no annotation cached, and type got processed fully (159631)
		this.scope.referenceCompilationUnit().compilationResult.hasAnnotations = true;
		final CompilerOptions globalOptions = this.scope.environment().globalOptions;
		if (!globalOptions.storeAnnotations && !forceStore)
			return null; // not supported during this compile
		this.storedAnnotations = new SimpleLookupTable(3);
	}
	return this.storedAnnotations;
}

@Override
public ReferenceBinding superclass() {
	if (!isPrototype())
		return this.superclass = this.prototype.superclass();
	return this.superclass;
}

@Override
public ReferenceBinding[] superInterfaces() {
	if (!isPrototype())
		return this.superInterfaces = this.prototype.superInterfaces();
	return this.superInterfaces != null ? this.superInterfaces : isAnnotationType() ? this.superInterfaces = new ReferenceBinding [] { this.scope.getJavaLangAnnotationAnnotation() } : null;
}

public SyntheticMethodBinding[] syntheticMethods() {

	if (!isPrototype()) throw new IllegalStateException();

	if (this.synthetics == null
			|| this.synthetics[SourceTypeBinding.METHOD_EMUL] == null
			|| this.synthetics[SourceTypeBinding.METHOD_EMUL].size() == 0) {
		return null;
	}
	// difficult to compute size up front because of the embedded arrays so assume there is only 1
	int index = 0;
	SyntheticMethodBinding[] bindings = new SyntheticMethodBinding[1];
	Iterator methodArrayIterator = this.synthetics[SourceTypeBinding.METHOD_EMUL].values().iterator();
	while (methodArrayIterator.hasNext()) {
		SyntheticMethodBinding[] methodAccessors = (SyntheticMethodBinding[]) methodArrayIterator.next();
		for (int i = 0, max = methodAccessors.length; i < max; i++) {
			if (methodAccessors[i] != null) {
				if (index+1 > bindings.length) {
					System.arraycopy(bindings, 0, (bindings = new SyntheticMethodBinding[index + 1]), 0, index);
				}
				bindings[index++] = methodAccessors[i];
			}
		}
	}
	// sort them in according to their own indexes
	Arrays.sort(bindings, new Comparator<>() {
		@Override
		public int compare(SyntheticMethodBinding o1, SyntheticMethodBinding o2) {
			return o1.index - o2.index;
		}
	});


	return bindings;
}
/**
 * Answer the collection of synthetic fields to append into the classfile
 */
public FieldBinding[] syntheticFields() {

	if (!isPrototype()) throw new IllegalStateException();

	if (this.synthetics == null) return null;
	int fieldSize = this.synthetics[SourceTypeBinding.FIELD_EMUL] == null ? 0 : this.synthetics[SourceTypeBinding.FIELD_EMUL].size();
	int literalSize = this.synthetics[SourceTypeBinding.CLASS_LITERAL_EMUL] == null ? 0 :this.synthetics[SourceTypeBinding.CLASS_LITERAL_EMUL].size();
	int totalSize = fieldSize + literalSize;
	if (totalSize == 0) return null;
	FieldBinding[] bindings = new FieldBinding[totalSize];

	// add innerclass synthetics
	if (this.synthetics[SourceTypeBinding.FIELD_EMUL] != null){
		Iterator elements = this.synthetics[SourceTypeBinding.FIELD_EMUL].values().iterator();
		for (int i = 0; i < fieldSize; i++) {
			SyntheticFieldBinding synthBinding = (SyntheticFieldBinding) elements.next();
			bindings[synthBinding.index] = synthBinding;
		}
	}
	// add class literal synthetics
	if (this.synthetics[SourceTypeBinding.CLASS_LITERAL_EMUL] != null){
		Iterator elements = this.synthetics[SourceTypeBinding.CLASS_LITERAL_EMUL].values().iterator();
		for (int i = 0; i < literalSize; i++) {
			SyntheticFieldBinding synthBinding = (SyntheticFieldBinding) elements.next();
			bindings[fieldSize+synthBinding.index] = synthBinding;
		}
	}
	return bindings;
}
@Override
public String toString() {
	if (this.hasTypeAnnotations()) {
		return annotatedDebugName();
    }

	StringBuilder buffer = new StringBuilder(30);
    buffer.append("(id="); //$NON-NLS-1$
    if (this.id == TypeIds.NoId)
        buffer.append("NoId"); //$NON-NLS-1$
    else
        buffer.append(this.id);
    buffer.append(")\n"); //$NON-NLS-1$
	if (isDeprecated()) buffer.append("deprecated "); //$NON-NLS-1$
	if (isPublic()) buffer.append("public "); //$NON-NLS-1$
	if (isProtected()) buffer.append("protected "); //$NON-NLS-1$
	if (isPrivate()) buffer.append("private "); //$NON-NLS-1$
	if (isAbstract() && isClass()) buffer.append("abstract "); //$NON-NLS-1$
	if (isStatic() && isNestedType()) buffer.append("static "); //$NON-NLS-1$
	if (isFinal()) buffer.append("final "); //$NON-NLS-1$

	if (isRecord()) buffer.append("record "); //$NON-NLS-1$
	else if (isEnum()) buffer.append("enum "); //$NON-NLS-1$
	else if (isAnnotationType()) buffer.append("@interface "); //$NON-NLS-1$
	else if (isClass()) buffer.append("class "); //$NON-NLS-1$
	else buffer.append("interface "); //$NON-NLS-1$
	buffer.append((this.compoundName != null) ? CharOperation.toString(this.compoundName) : "UNNAMED TYPE"); //$NON-NLS-1$

	if (this.typeVariables == null) {
		buffer.append("<NULL TYPE VARIABLES>"); //$NON-NLS-1$
	} else if (this.typeVariables != Binding.NO_TYPE_VARIABLES) {
		buffer.append("<"); //$NON-NLS-1$
		for (int i = 0, length = this.typeVariables.length; i < length; i++) {
			if (i  > 0) buffer.append(", "); //$NON-NLS-1$
			if (this.typeVariables[i] == null) {
				buffer.append("NULL TYPE VARIABLE"); //$NON-NLS-1$
				continue;
			}
			char[] varChars = this.typeVariables[i].toString().toCharArray();
			buffer.append(varChars, 1, varChars.length - 2);
		}
		buffer.append(">"); //$NON-NLS-1$
	}
	buffer.append("\n\textends "); //$NON-NLS-1$
	buffer.append((this.superclass != null) ? this.superclass.debugName() : "NULL TYPE"); //$NON-NLS-1$

	if (this.superInterfaces != null) {
		if (this.superInterfaces != Binding.NO_SUPERINTERFACES) {
			buffer.append("\n\timplements : "); //$NON-NLS-1$
			for (int i = 0, length = this.superInterfaces.length; i < length; i++) {
				if (i  > 0)
					buffer.append(", "); //$NON-NLS-1$
				buffer.append((this.superInterfaces[i] != null) ? this.superInterfaces[i].debugName() : "NULL TYPE"); //$NON-NLS-1$
			}
		}
	} else {
		buffer.append("NULL SUPERINTERFACES"); //$NON-NLS-1$
	}

	if (enclosingType() != null) {
		buffer.append("\n\tenclosing type : "); //$NON-NLS-1$
		buffer.append(enclosingType().debugName());
	}

	if (this.fields != null) {
		if (this.fields != Binding.NO_FIELDS) {
			buffer.append("\n/*   fields   */"); //$NON-NLS-1$
			for (int i = 0, length = this.fields.length; i < length; i++)
			    buffer.append('\n').append((this.fields[i] != null) ? this.fields[i].toString() : "NULL FIELD"); //$NON-NLS-1$
		}
	} else {
		buffer.append("NULL FIELDS"); //$NON-NLS-1$
	}

	if (this.methods != null) {
		if (this.methods != Binding.NO_METHODS) {
			buffer.append("\n/*   methods   */"); //$NON-NLS-1$
			for (int i = 0, length = this.methods.length; i < length; i++)
				buffer.append('\n').append((this.methods[i] != null) ? this.methods[i].toString() : "NULL METHOD"); //$NON-NLS-1$
		}
	} else {
		buffer.append("NULL METHODS"); //$NON-NLS-1$
	}

	if (this.memberTypes != null) {
		if (this.memberTypes != Binding.NO_MEMBER_TYPES) {
			buffer.append("\n/*   members   */"); //$NON-NLS-1$
			for (int i = 0, length = this.memberTypes.length; i < length; i++)
				buffer.append('\n').append((this.memberTypes[i] != null) ? this.memberTypes[i].toString() : "NULL TYPE"); //$NON-NLS-1$
		}
	} else {
		buffer.append("NULL MEMBER TYPES"); //$NON-NLS-1$
	}

	buffer.append("\n\n"); //$NON-NLS-1$
	return buffer.toString();
}
@Override
public TypeVariableBinding[] typeVariables() {
	if (!isPrototype())
		return this.typeVariables = this.prototype.typeVariables();
	return this.typeVariables != null ? this.typeVariables : Binding.NO_TYPE_VARIABLES;
}
void verifyMethods(MethodVerifier verifier) {

	if (!isPrototype()) throw new IllegalStateException();

	verifier.verify(this);

	for (int i = this.memberTypes.length; --i >= 0;)
		 ((SourceTypeBinding) this.memberTypes[i]).verifyMethods(verifier);
}

@Override
public TypeBinding unannotated() {
	return this.prototype;
}

@Override
public TypeBinding withoutToplevelNullAnnotation() {
	if (!hasNullTypeAnnotations())
		return this;
	AnnotationBinding[] newAnnotations = this.environment.filterNullTypeAnnotations(this.typeAnnotations);
	if (newAnnotations.length > 0)
		return this.environment.createAnnotatedType(this.prototype, newAnnotations);
	return this.prototype;
}

@Override
public FieldBinding[] unResolvedFields() {
	if (!isPrototype())
		return this.prototype.unResolvedFields();
	return this.fields;
}

public void tagIndirectlyAccessibleMembers() {
	if (!isPrototype()) {
		this.prototype.tagIndirectlyAccessibleMembers();
		return;
	}
	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=328281
	for (int i = 0; i < this.fields.length; i++) {
		if (!this.fields[i].isPrivate())
			this.fields[i].modifiers |= ExtraCompilerModifiers.AccLocallyUsed;
	}
	for (int i = 0; i < this.memberTypes.length; i++) {
		if (!this.memberTypes[i].isPrivate())
			this.memberTypes[i].modifiers |= ExtraCompilerModifiers.AccLocallyUsed;
	}
	if (this.superclass.isPrivate())
		if (this.superclass instanceof SourceTypeBinding)  // should always be true because private super type can only be accessed in same CU
			((SourceTypeBinding) this.superclass).tagIndirectlyAccessibleMembers();
}
@Override
public ModuleBinding module() {
	if (!isPrototype())
		return this.prototype.module;
	return this.module;
}

public SourceTypeBinding getNestHost() {
	return this.nestHost;
}

public void setNestHost(SourceTypeBinding nestHost) {
	this.nestHost = nestHost;
}

public boolean isNestmateOf(SourceTypeBinding other) {

	CompilerOptions options = this.scope.compilerOptions();
	if (options.targetJDK < ClassFileConstants.JDK11 ||
		options.complianceLevel < ClassFileConstants.JDK11)
		return false; // default false if level less than 11

	SourceTypeBinding otherHost = other.getNestHost();
	return TypeBinding.equalsEquals(this, other) ||
			TypeBinding.equalsEquals(this.nestHost == null ? this : this.nestHost,
					otherHost == null ? other : otherHost);
}
/* Get the field bindings in the order of record component declaration
 * should be called only after a called to fields() */
public FieldBinding[] getImplicitComponentFields() {
	return this.implicitComponentFields;
}
public RecordComponentBinding getRecordComponent(char[] name) {
	if (this.isRecordDeclaration && this.components != null) {
		for (RecordComponentBinding rcb : this.components) {
			if (CharOperation.equals(name, rcb.name))
				return rcb;
		}
	}
	return null;
}
/**
 * Get the accessor method given the record component name
 * @param name name of the record component
 * @return the method binding of the accessor if found, else null
 */
public MethodBinding getRecordComponentAccessor(char[] name) {
	MethodBinding accessor = null;
	if (this.recordComponentAccessors != null) {
		for (MethodBinding m : this.recordComponentAccessors) {
			if (CharOperation.equals(m.selector, name)) {
				accessor = m;
				break;
			}
		}
	}
	return accessor;
}
public void computeRecordComponents() {
	if (!this.isRecord() || this.implicitComponentFields != null)
		return;
	List<String> recordComponentNames = Stream.of(this.components)
			.map(arg -> new String(arg.name))
			.collect(Collectors.toList());
	List<FieldBinding> list = new ArrayList<>();
	if (recordComponentNames != null && recordComponentNames.size() > 0 && this.fields != null) {
		for (String rc : recordComponentNames) {
			for (FieldBinding f : this.fields) {
				if (rc.equals(new String(f.name))) {
					list.add(f);
				}
			}
		}
	}
	this.implicitComponentFields = list.toArray(new FieldBinding[0]);
}

public void cleanUp() {
	if (this.environment != null) {
		// delegate so as to clean all variants of this prototype:
		this.environment.typeSystem.cleanUp(this.id);
	}
	this.scope = null; // for types that are not registered in typeSystem.
}

}