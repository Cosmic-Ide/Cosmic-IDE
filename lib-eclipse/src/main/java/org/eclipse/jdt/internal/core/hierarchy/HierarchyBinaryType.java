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
 *     Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *         bug 407191 - [1.8] Binary access support for type annotations
 *     Stephan Herrmann - Contribution for
 *								Bug 440474 - [null] textual encoding of external null annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.core.hierarchy;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryField;
import org.eclipse.jdt.internal.compiler.env.IBinaryMethod;
import org.eclipse.jdt.internal.compiler.env.IBinaryNestedType;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.IBinaryTypeAnnotation;
import org.eclipse.jdt.internal.compiler.env.IRecordComponent;
import org.eclipse.jdt.internal.compiler.env.ITypeAnnotationWalker;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding.ExternalAnnotationStatus;
import org.eclipse.jdt.internal.core.search.indexing.IIndexConstants;

public class HierarchyBinaryType implements IBinaryType {
	private int modifiers;
	private char[] sourceName;
	private char[] name;
	private char[] enclosingTypeName;
	private char[] superclass;
	private char[][] superInterfaces = NoInterface;
	private char[][] typeParameterSignatures;
	private char[] genericSignature;

public HierarchyBinaryType(int modifiers, char[] qualification, char[] sourceName, char[] enclosingTypeName, char[][] typeParameterSignatures, char typeSuffix){

	this.modifiers = modifiers;
	this.sourceName = sourceName;
	if (enclosingTypeName == null){
		this.name = CharOperation.concat(qualification, sourceName, '/');
	} else {
		this.name = CharOperation.concat(qualification, '/', enclosingTypeName, '$', sourceName); //rebuild A$B name
		this.enclosingTypeName = CharOperation.concat(qualification, enclosingTypeName,'/');
		CharOperation.replace(this.enclosingTypeName, '.', '/');
	}
	this.typeParameterSignatures = typeParameterSignatures;
	CharOperation.replace(this.name, '.', '/');
}

public HierarchyBinaryType(int modifiers, char[] binaryName, char[] sourceName, char[] enclosingTypeBinaryName, char[][] typeParameterSignatures) {
	this.modifiers = modifiers;
	this.sourceName = sourceName;
	this.name = binaryName;
	this.enclosingTypeName = enclosingTypeBinaryName;
	this.typeParameterSignatures = typeParameterSignatures;

	if (typeParameterSignatures != null) {
		for (char[] next : typeParameterSignatures) {
			if (next == null) {
				throw new IllegalArgumentException("Parameter's type signature must not be null"); //$NON-NLS-1$
			}
		}
	}
}

/**
 * @see org.eclipse.jdt.internal.compiler.env.IBinaryType
 */
@Override
public IBinaryAnnotation[] getAnnotations() {
	return null;
}
/**
 * @see org.eclipse.jdt.internal.compiler.env.IBinaryType
 */
@Override
public IBinaryTypeAnnotation[] getTypeAnnotations() {
	return null;
}
@Override
public char[] getEnclosingMethod() {
	return null;
}
/**
 * Answer the resolved name of the enclosing type in the
 * class file format as specified in section 4.2 of the Java 2 VM spec
 * or null if the receiver is a top level type.
 *
 * For example, java.lang.String is java/lang/String.
 */
@Override
public char[] getEnclosingTypeName() {
	return this.enclosingTypeName;
}
/**
 * Answer the receiver's fields or null if the array is empty.
 */
@Override
public IBinaryField[] getFields() {
	return null;
}
/**
 * @see org.eclipse.jdt.internal.compiler.env.IDependent#getFileName()
 */
@Override
public char[] getFileName() {
	return null;
}
@Override
public char[] getGenericSignature() {
	if (this.typeParameterSignatures != null && this.genericSignature == null) {
		StringBuilder buffer = new StringBuilder();
		buffer.append('<');
		for (int i = 0, length = this.typeParameterSignatures.length; i < length; i++) {
			buffer.append(this.typeParameterSignatures[i]);
		}
		buffer.append('>');
		if (this.superclass == null)
			buffer.append(Signature.createTypeSignature("java.lang.Object", true/*resolved*/)); //$NON-NLS-1$
		else
			buffer.append(Signature.createTypeSignature(this.superclass, true/*resolved*/));
		if (this.superInterfaces != null)
			for (int i = 0, length = this.superInterfaces.length; i < length; i++)
				buffer.append(Signature.createTypeSignature(this.superInterfaces[i], true/*resolved*/));
		this.genericSignature = buffer.toString().toCharArray();
		CharOperation.replace(this.genericSignature, '.', '/');
	}
	return this.genericSignature;
}
/**
 * Answer the resolved names of the receiver's interfaces in the
 * class file format as specified in section 4.2 of the Java 2 VM spec
 * or null if the array is empty.
 *
 * For example, java.lang.String is java/lang/String.
 */
@Override
public char[][] getInterfaceNames() {
	return this.superInterfaces;
}
/**
 * Answer the receiver's nested types or null if the array is empty.
 *
 * This nested type info is extracted from the inner class attributes.
 * Ask the name environment to find a member type using its compound name.
 */
@Override
public IBinaryNestedType[] getMemberTypes() {
	return null;
}
/**
 * Answer the receiver's methods or null if the array is empty.
 */
@Override
public IBinaryMethod[] getMethods() {
	return null;
}

/**
 * @see org.eclipse.jdt.internal.compiler.env.IBinaryType#getMissingTypeNames()
 */
@Override
public char[][][] getMissingTypeNames() {
	return null;
}

/**
 * Answer an int whose bits are set according the access constants
 * defined by the VM spec.
 */
@Override
public int getModifiers() {
	return this.modifiers;
}

/**
 * Answer the resolved name of the type in the
 * class file format as specified in section 4.2 of the Java 2 VM spec.
 *
 * For example, java.lang.String is java/lang/String.
 */
@Override
public char[] getName() {
	return this.name;
}

@Override
public char[] getSourceName() {
	return this.sourceName;
}
/**
 * Answer the resolved name of the receiver's superclass in the
 * class file format as specified in section 4.2 of the Java 2 VM spec
 * or null if it does not have one.
 *
 * For example, java.lang.String is java/lang/String.
 */
@Override
public char[] getSuperclassName() {
	return this.superclass;
}

// TODO (jerome) please verify that we don't need the tagbits for the receiver
@Override
public long getTagBits() {
	return 0;
}
@Override
public boolean isAnonymous() {
	return false; // index did not record this information (since unused for hierarchies)
}
/**
 * Answer whether the receiver contains the resolved binary form
 * or the unresolved source form of the type.
 */
@Override
public boolean isBinaryType() {
	return true;
}

@Override
public boolean isLocal() {
	return false;  // index did not record this information (since unused for hierarchies)
}
@Override
public boolean isRecord() {
	return false;  // index did not record this information (since unused for hierarchies)
}
@Override
public boolean isMember() {
	return false;  // index did not record this information (since unused for hierarchies)
}


public void recordSuperType(char[] superTypeName, char[] superQualification, char superClassOrInterface){

	// index encoding of p.A$B was B/p.A$, rebuild the proper name
	if (superQualification != null){
		int length = superQualification.length;
		if (superQualification[length-1] == '$'){
			char[] enclosingSuperName = CharOperation.lastSegment(superQualification, '.');
			superTypeName = CharOperation.concat(enclosingSuperName, superTypeName);
			superQualification = CharOperation.subarray(superQualification, 0, length - enclosingSuperName.length - 1);
		}
	}

	if (superClassOrInterface == IIndexConstants.CLASS_SUFFIX){
		// interfaces are indexed as having superclass references to Object by default,
		// this is an artifact used for being able to query them only.
		if (TypeDeclaration.kind(this.modifiers) == TypeDeclaration.INTERFACE_DECL) return;
		char[] encodedName = CharOperation.concat(superQualification, superTypeName, '/');
		CharOperation.replace(encodedName, '.', '/');
		recordSuperclass(encodedName);
	} else {
		char[] encodedName = CharOperation.concat(superQualification, superTypeName, '/');
		CharOperation.replace(encodedName, '.', '/');
		recordInterface(encodedName);
	}
}

public void recordSuperclass(char[] binaryName) {
	this.superclass = binaryName;
}

public void recordInterface(char[] binaryName) {
	if (this.superInterfaces == NoInterface){
		this.superInterfaces = new char[][] { binaryName };
	} else {
		int length = this.superInterfaces.length;
		System.arraycopy(this.superInterfaces, 0, this.superInterfaces = new char[length+1][], 0, length);
		this.superInterfaces[length] = binaryName;
	}
}

/**
 * @see org.eclipse.jdt.internal.compiler.env.IBinaryType
 */
@Override
public char[] sourceFileName() {
	return null;
}
@Override
public String toString() {
	StringBuilder buffer = new StringBuilder();
	if (this.modifiers == ClassFileConstants.AccPublic) {
		buffer.append("public "); //$NON-NLS-1$
	}
	switch (TypeDeclaration.kind(this.modifiers)) {
		case TypeDeclaration.CLASS_DECL :
			buffer.append("class "); //$NON-NLS-1$
			break;
		case TypeDeclaration.INTERFACE_DECL :
			buffer.append("interface "); //$NON-NLS-1$
			break;
		case TypeDeclaration.ENUM_DECL :
			buffer.append("enum "); //$NON-NLS-1$
			break;
		case TypeDeclaration.RECORD_DECL :
			buffer.append("record "); //$NON-NLS-1$
			break;
	}
	if (this.name != null) {
		buffer.append(this.name);
	}
	if (this.superclass != null) {
		buffer.append("\n  extends "); //$NON-NLS-1$
		buffer.append(this.superclass);
	}
	int length;
	if (this.superInterfaces != null && (length = this.superInterfaces.length) != 0) {
		buffer.append("\n implements "); //$NON-NLS-1$
		for (int i = 0; i < length; i++) {
			buffer.append(this.superInterfaces[i]);
			if (i != length - 1) {
				buffer.append(", "); //$NON-NLS-1$
			}
		}
	}
	return buffer.toString();
}
@Override
public ITypeAnnotationWalker enrichWithExternalAnnotationsFor(ITypeAnnotationWalker walker, Object member, LookupEnvironment environment) {
	return walker;
}
@Override
public char[] getModule() {
	// TODO Java 9 Auto-generated method stub
	return null;
}
@Override
public ExternalAnnotationStatus getExternalAnnotationStatus() {
	return ExternalAnnotationStatus.NOT_EEA_CONFIGURED;
}

@Override
public IRecordComponent[] getRecordComponents() {
	return null;
}
}
