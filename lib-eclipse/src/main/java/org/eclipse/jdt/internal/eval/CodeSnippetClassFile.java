/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
 *        Andy Clement - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *******************************************************************************/
package org.eclipse.jdt.internal.eval;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.codegen.CodeStream;
import org.eclipse.jdt.internal.compiler.codegen.ConstantPool;
import org.eclipse.jdt.internal.compiler.codegen.StackMapFrameCodeStream;
import org.eclipse.jdt.internal.compiler.codegen.TypeAnnotationCodeStream;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.eclipse.jdt.internal.compiler.util.Util;

public class CodeSnippetClassFile extends ClassFile {
/**
 * CodeSnippetClassFile constructor comment.
 * @param aType org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding
 * @param enclosingClassFile org.eclipse.jdt.internal.compiler.ClassFile
 * @param creatingProblemType boolean
 */
public CodeSnippetClassFile(
	org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding aType,
	org.eclipse.jdt.internal.compiler.ClassFile enclosingClassFile,
	boolean creatingProblemType) {
	/**
	 * INTERNAL USE-ONLY
	 * This methods creates a new instance of the receiver.
	 *
	 * @param aType org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding
	 * @param enclosingClassFile org.eclipse.jdt.internal.compiler.codegen.ClassFile
	 * @param creatingProblemType <CODE>boolean</CODE>
	 */
	this.referenceBinding = aType;
	initByteArrays(aType.methods().length + aType.fields().length);
	// generate the magic numbers inside the header
	this.header[this.headerOffset++] = (byte) (0xCAFEBABEL >> 24);
	this.header[this.headerOffset++] = (byte) (0xCAFEBABEL >> 16);
	this.header[this.headerOffset++] = (byte) (0xCAFEBABEL >> 8);
	this.header[this.headerOffset++] = (byte) (0xCAFEBABEL >> 0);

	long targetVersion = this.targetJDK = this.referenceBinding.scope.compilerOptions().targetJDK;
	//TODO: Might have to update even for CLDC_1_1
	this.header[this.headerOffset++] = (byte) (targetVersion >> 8); // minor high
	this.header[this.headerOffset++] = (byte) (targetVersion >> 0); // minor low
	this.header[this.headerOffset++] = (byte) (targetVersion >> 24); // major high
	this.header[this.headerOffset++] = (byte) (targetVersion >> 16); // major low

	this.constantPoolOffset = this.headerOffset;
	this.headerOffset += 2;
	this.constantPool = new ConstantPool(this);
	int accessFlags = aType.getAccessFlags();

	if (!aType.isInterface()) { // class or enum
		accessFlags |= ClassFileConstants.AccSuper;
	}
	if (aType.isNestedType()) {
		if (aType.isStatic()) {
			// clear Acc_Static
			accessFlags &= ~ClassFileConstants.AccStatic;
		}
		if (aType.isPrivate()) {
			// clear Acc_Private and Acc_Public
			accessFlags &= ~(ClassFileConstants.AccPrivate | ClassFileConstants.AccPublic);
		}
		if (aType.isProtected()) {
			// clear Acc_Protected and set Acc_Public
			accessFlags &= ~ClassFileConstants.AccProtected;
			accessFlags |= ClassFileConstants.AccPublic;
		}
	}
	// clear Acc_Strictfp
	accessFlags &= ~ClassFileConstants.AccStrictfp;

	this.enclosingClassFile = enclosingClassFile;
	// now we continue to generate the bytes inside the contents array
	this.contents[this.contentsOffset++] = (byte) (accessFlags >> 8);
	this.contents[this.contentsOffset++] = (byte) accessFlags;
	int classNameIndex = this.constantPool.literalIndexForType(aType);
	this.contents[this.contentsOffset++] = (byte) (classNameIndex >> 8);
	this.contents[this.contentsOffset++] = (byte) classNameIndex;
	int superclassNameIndex;
	if (aType.isInterface()) {
		superclassNameIndex = this.constantPool.literalIndexForType(ConstantPool.JavaLangObjectConstantPoolName);
	} else {
		superclassNameIndex =
			(aType.superclass == null ? 0 : this.constantPool.literalIndexForType(aType.superclass));
	}
	this.contents[this.contentsOffset++] = (byte) (superclassNameIndex >> 8);
	this.contents[this.contentsOffset++] = (byte) superclassNameIndex;
	ReferenceBinding[] superInterfacesBinding = aType.superInterfaces();
	int interfacesCount = superInterfacesBinding.length;
	this.contents[this.contentsOffset++] = (byte) (interfacesCount >> 8);
	this.contents[this.contentsOffset++] = (byte) interfacesCount;
	for (int i = 0; i < interfacesCount; i++) {
		int interfaceIndex = this.constantPool.literalIndexForType(superInterfacesBinding[i]);
		this.contents[this.contentsOffset++] = (byte) (interfaceIndex >> 8);
		this.contents[this.contentsOffset++] = (byte) interfaceIndex;
	}
	this.produceAttributes = this.referenceBinding.scope.compilerOptions().produceDebugAttributes;
	this.creatingProblemType = creatingProblemType;
	if (this.targetJDK >= ClassFileConstants.JDK1_6) {
		this.produceAttributes |= ClassFileConstants.ATTR_STACK_MAP_TABLE;
		if (this.targetJDK >= ClassFileConstants.JDK1_8) {
			this.produceAttributes |= ClassFileConstants.ATTR_TYPE_ANNOTATION;
			this.codeStream = new TypeAnnotationCodeStream(this);
		} else {
			this.codeStream = new StackMapFrameCodeStream(this);
		}
	} else if (this.targetJDK == ClassFileConstants.CLDC_1_1) {
		this.targetJDK = ClassFileConstants.JDK1_1; // put back 45.3
		this.produceAttributes |= ClassFileConstants.ATTR_STACK_MAP;
		this.codeStream = new StackMapFrameCodeStream(this);
	} else {
		this.codeStream = new CodeStream(this);
	}
	// retrieve the enclosing one guaranteed to be the one matching the propagated flow info
	// 1FF9ZBU: LFCOM:ALL - Local variable attributes busted (Sanity check)
	this.codeStream.maxFieldCount = aType.scope.outerMostClassScope().referenceType().maxFieldCount;
}
/**
 * INTERNAL USE-ONLY
 * Request the creation of a ClassFile compatible representation of a problematic type
 *
 * @param typeDeclaration org.eclipse.jdt.internal.compiler.ast.TypeDeclaration
 * @param unitResult org.eclipse.jdt.internal.compiler.CompilationUnitResult
 */
public static void createProblemType(TypeDeclaration typeDeclaration, CompilationResult unitResult) {
	SourceTypeBinding typeBinding = typeDeclaration.binding;
	ClassFile classFile = new CodeSnippetClassFile(typeBinding, null, true);

	// inner attributes
	if (typeBinding.hasMemberTypes()) {
		// see bug 180109
		ReferenceBinding[] members = typeBinding.memberTypes;
		for (int i = 0, l = members.length; i < l; i++)
			classFile.recordInnerClasses(members[i]);
	}
	// TODO (olivier) handle cases where a field cannot be generated (name too long)
	// TODO (olivier) handle too many methods
	// inner attributes
	if (typeBinding.isNestedType()) {
		classFile.recordInnerClasses(typeBinding);
	}
	TypeVariableBinding[] typeVariables = typeBinding.typeVariables();
	for (int i = 0, max = typeVariables.length; i < max; i++) {
		TypeVariableBinding typeVariableBinding = typeVariables[i];
		if ((typeVariableBinding.tagBits & TagBits.ContainsNestedTypeReferences) != 0) {
			Util.recordNestedType(classFile, typeVariableBinding);
		}
	}

	// add its fields
	FieldBinding[] fields = typeBinding.fields();
	if ((fields != null) && (fields != Binding.NO_FIELDS)) {
		classFile.addFieldInfos();
	} else {
		// we have to set the number of fields to be equals to 0
		classFile.contents[classFile.contentsOffset++] = 0;
		classFile.contents[classFile.contentsOffset++] = 0;
	}
	// leave some space for the methodCount
	classFile.setForMethodInfos();
	// add its user defined methods
	int problemsLength;
	CategorizedProblem[] problems = unitResult.getErrors();
	if (problems == null) {
		problems = new CategorizedProblem[0];
	}
	CategorizedProblem[] problemsCopy = new CategorizedProblem[problemsLength = problems.length];
	System.arraycopy(problems, 0, problemsCopy, 0, problemsLength);
	AbstractMethodDeclaration[] methodDecls = typeDeclaration.methods;
	boolean abstractMethodsOnly = false;
	if (methodDecls != null) {
		if (typeBinding.isInterface()) {
			if (typeBinding.scope.compilerOptions().sourceLevel < ClassFileConstants.JDK1_8)
				abstractMethodsOnly = true;
			// We generate a clinit which contains all the problems, since we may not be able to generate problem methods (< 1.8) and problem constructors (all levels).
			classFile.addProblemClinit(problemsCopy);
		}
		for (int i = 0, length = methodDecls.length; i < length; i++) {
			AbstractMethodDeclaration methodDecl = methodDecls[i];
			MethodBinding method = methodDecl.binding;
			if (method == null) continue;
			if (abstractMethodsOnly) {
				method.modifiers = ClassFileConstants.AccPublic | ClassFileConstants.AccAbstract;
			}
			if (method.isConstructor()) {
				if (typeBinding.isInterface()) continue;
				classFile.addProblemConstructor(methodDecl, method, problemsCopy);
			} else if (method.isAbstract()) {
				classFile.addAbstractMethod(methodDecl, method);
			} else {
				classFile.addProblemMethod(methodDecl, method, problemsCopy);
			}
		}
		// add abstract methods
		classFile.addDefaultAbstractMethods();
	}
	// propagate generation of (problem) member types
	if (typeDeclaration.memberTypes != null) {
		for (int i = 0, max = typeDeclaration.memberTypes.length; i < max; i++) {
			TypeDeclaration memberType = typeDeclaration.memberTypes[i];
			if (memberType.binding != null) {
				ClassFile.createProblemType(memberType, unitResult);
			}
		}
	}
	classFile.addAttributes();
	unitResult.record(typeBinding.constantPoolName(), classFile);
}
}
