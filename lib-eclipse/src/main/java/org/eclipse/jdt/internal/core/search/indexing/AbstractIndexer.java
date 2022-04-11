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
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search.indexing;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.SearchDocument;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.search.matching.ConstructorPattern;
import org.eclipse.jdt.internal.core.search.matching.FieldPattern;
import org.eclipse.jdt.internal.core.search.matching.MethodDeclarationPattern;
import org.eclipse.jdt.internal.core.search.matching.MethodPattern;
import org.eclipse.jdt.internal.core.search.matching.ModulePattern;
import org.eclipse.jdt.internal.core.search.matching.SuperTypeReferencePattern;
import org.eclipse.jdt.internal.core.search.matching.TypeDeclarationPattern;

public abstract class AbstractIndexer implements IIndexConstants {

	SearchDocument document;

	public AbstractIndexer(SearchDocument document) {
		this.document = document;
	}
	public void addAnnotationTypeDeclaration(int modifiers, char[] packageName, char[] name, char[][] enclosingTypeNames, boolean secondary) {
		addTypeDeclaration(modifiers, packageName, name, enclosingTypeNames, secondary);

		char[] annotationSuperClass = CharOperation.concatWith(TypeConstants.JAVA_LANG_ANNOTATION_ANNOTATION, '.');
		addIndexEntry(
			SUPER_REF,
			SuperTypeReferencePattern.createIndexKey(
				modifiers, packageName, name, enclosingTypeNames, null, ANNOTATION_TYPE_SUFFIX, annotationSuperClass, ANNOTATION_TYPE_SUFFIX));
		addIndexMetaQualification(annotationSuperClass, true);
	}
	public void addAnnotationTypeReference(char[] typeName) {
		addIndexEntry(ANNOTATION_REF, CharOperation.lastSegment(typeName, '.'));
		addIndexMetaQualification(typeName, false);
	}
	public void addClassDeclaration(
			int modifiers,
			char[] packageName,
			char[] name,
			char[][] enclosingTypeNames,
			char[] superclass,
			char[][] superinterfaces,
			char[][] typeParameterSignatures,
			boolean secondary) {
		addTypeDeclaration(modifiers, packageName, name, enclosingTypeNames, secondary);

		if (superclass != null) {
			superclass = erasure(superclass);
			addTypeReference(superclass, true);
		}
		addIndexEntry(
			SUPER_REF,
			SuperTypeReferencePattern.createIndexKey(
				modifiers, packageName, name, enclosingTypeNames, typeParameterSignatures, CLASS_SUFFIX, superclass, CLASS_SUFFIX));
		if (superinterfaces != null) {
			for (int i = 0, max = superinterfaces.length; i < max; i++) {
				char[] superinterface = erasure(superinterfaces[i]);
				addTypeReference(superinterface, true);
				addIndexEntry(
					SUPER_REF,
					SuperTypeReferencePattern.createIndexKey(
						modifiers, packageName, name, enclosingTypeNames, typeParameterSignatures, CLASS_SUFFIX, superinterface, INTERFACE_SUFFIX));
			}
		}
	}
	private char[] erasure(char[] typeName) {
		int genericStart = CharOperation.indexOf(Signature.C_GENERIC_START, typeName);
		if (genericStart > -1)
			typeName = CharOperation.subarray(typeName, 0, genericStart);
		return typeName;
	}
	public void addConstructorDeclaration(
			char[] typeName,
			int argCount,
			char[] signature,
			char[][] parameterTypes,
			char[][] parameterNames,
			int modifiers,
			char[] packageName,
			int typeModifiers,
			char[][] exceptionTypes,
			int extraFlags) {
		addIndexEntry(
				CONSTRUCTOR_DECL,
				ConstructorPattern.createDeclarationIndexKey(
						typeName,
						argCount,
						signature,
						parameterTypes,
						parameterNames,
						modifiers,
						packageName,
						typeModifiers,
						extraFlags));

		if (parameterTypes != null) {
			for (int i = 0; i < argCount; i++)
				addTypeReference(parameterTypes[i]);
		}
		if (exceptionTypes != null)
			for (int i = 0, max = exceptionTypes.length; i < max; i++)
				addTypeReference(exceptionTypes[i]);
	}
	public void addConstructorReference(char[] typeName, int argCount) {
		char[] simpleTypeName = CharOperation.lastSegment(typeName,'.');
		addTypeReference(typeName); // The implementation will add simple name to index.
		addIndexMetaQualification(typeName, true); // second entry for super type
		addIndexEntry(CONSTRUCTOR_REF, ConstructorPattern.createIndexKey(simpleTypeName, argCount));
		char[] innermostTypeName = CharOperation.lastSegment(simpleTypeName,'$');
		if (innermostTypeName != simpleTypeName)
			addIndexEntry(CONSTRUCTOR_REF, ConstructorPattern.createIndexKey(innermostTypeName, argCount));
	}
	public void addDefaultConstructorDeclaration(
			char[] typeName,
			char[] packageName,
			int typeModifiers,
			int extraFlags) {
		addIndexEntry(CONSTRUCTOR_DECL, ConstructorPattern.createDefaultDeclarationIndexKey(CharOperation.lastSegment(typeName,'.'), packageName, typeModifiers, extraFlags));
	}
	public void addEnumDeclaration(int modifiers, char[] packageName, char[] name, char[][] enclosingTypeNames, char[] superclass, char[][] superinterfaces, boolean secondary) {
		addTypeDeclaration(modifiers, packageName, name, enclosingTypeNames, secondary);

		addIndexEntry(
			SUPER_REF,
			SuperTypeReferencePattern.createIndexKey(
				modifiers, packageName, name, enclosingTypeNames, null, ENUM_SUFFIX, superclass, CLASS_SUFFIX));
		addIndexMetaQualification(superclass, true);

		if (superinterfaces != null) {
			for (int i = 0, max = superinterfaces.length; i < max; i++) {
				char[] superinterface = erasure(superinterfaces[i]);
				addTypeReference(superinterface, true);
				addIndexEntry(
					SUPER_REF,
					SuperTypeReferencePattern.createIndexKey(
						modifiers, packageName, name, enclosingTypeNames, null, ENUM_SUFFIX, superinterface, INTERFACE_SUFFIX));
			}
		}
	}
	public void addFieldDeclaration(char[] typeName, char[] fieldName) {
		addIndexEntry(FIELD_DECL, FieldPattern.createIndexKey(fieldName));
		addTypeReference(typeName);
	}
	public void addRecordComponentDecl(char[] typeName, char[] fieldName) {
		addFieldDeclaration(typeName, fieldName);
	}
	public void addFieldReference(char[] fieldName) {
		addNameReference(fieldName);
	}
	protected void addIndexEntry(char[] category, char[] key) {
		this.document.addIndexEntry(category, key);
	}
	public void addInterfaceDeclaration(int modifiers, char[] packageName, char[] name, char[][] enclosingTypeNames, char[][] superinterfaces, char[][] typeParameterSignatures, boolean secondary) {
		addTypeDeclaration(modifiers, packageName, name, enclosingTypeNames, secondary);

		if (superinterfaces != null) {
			for (int i = 0, max = superinterfaces.length; i < max; i++) {
				char[] superinterface = erasure(superinterfaces[i]);
				addTypeReference(superinterface, true);
				addIndexEntry(
					SUPER_REF,
					SuperTypeReferencePattern.createIndexKey(
						modifiers, packageName, name, enclosingTypeNames, typeParameterSignatures, INTERFACE_SUFFIX, superinterface, INTERFACE_SUFFIX));
			}
		}
	}
	public void addMethodDeclaration(
			char[] typeName,
			char[] declaringQualification,
			char[] methodName,
			int argCount,
			char[] signature,
			char[][] parameterTypes,
			char[][] parameterNames,
			char[] returnType,
			int modifiers,
			char[] packageName,
			int typeModifiers,
			char[][] exceptionTypes,
			int extraFlags) {
		try {
			addIndexEntry(
					METHOD_DECL_PLUS,
					MethodDeclarationPattern.createDeclarationIndexKey(
							typeName,
							declaringQualification,
							methodName,
							argCount,
							signature,
							parameterTypes,
							parameterNames,
							returnType,
							modifiers,
							packageName,
							typeModifiers,
							extraFlags));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addMethodDeclaration(char[] methodName, char[][] parameterTypes, char[] returnType, char[][] exceptionTypes) {
		int argCount = parameterTypes == null ? 0 : parameterTypes.length;
		addIndexEntry(METHOD_DECL, MethodPattern.createIndexKey(methodName, argCount));

		if (parameterTypes != null) {
			for (int i = 0; i < argCount; i++)
				addTypeReference(parameterTypes[i]);
		}
		if (exceptionTypes != null)
			for (int i = 0, max = exceptionTypes.length; i < max; i++)
				addTypeReference(exceptionTypes[i]);
		if (returnType != null)
			addTypeReference(returnType);
	}
	public void addMethodReference(char[] methodName, int argCount) {
		addIndexEntry(METHOD_REF, MethodPattern.createIndexKey(methodName, argCount));
	}
	public void addModuleDeclaration(char[] moduleName) {
		addIndexEntry(MODULE_DECL, ModulePattern.createIndexKey(moduleName));
	}
	public void addModuleExportedPackages(char[] packageName) {
		char[][] tokens = CharOperation.splitOn('.', packageName);
		for (int i = 0, l = tokens.length; i < l; ++i)
			addNameReference(tokens[i]);
	}
	public void addModuleReference(char[] moduleName) {
		addIndexEntry(MODULE_REF, ModulePattern.createIndexKey(moduleName));
	}
	public void addNameReference(char[] name) {
		addIndexEntry(REF, name);
	}
	protected void addTypeDeclaration(int modifiers, char[] packageName, char[] name, char[][] enclosingTypeNames, boolean secondary) {
		char[] indexKey = TypeDeclarationPattern.createIndexKey(modifiers, name, packageName, enclosingTypeNames, secondary);
		if (secondary)
			JavaModelManager.getJavaModelManager().secondaryTypeAdding(
				this.document.getPath(),
				name == null ? CharOperation.NO_CHAR : name,
				packageName == null ? CharOperation.NO_CHAR : packageName);

		addIndexEntry(TYPE_DECL, indexKey);
		addIndexMetaQualification(
				CharOperation.concat(packageName, '.', CharOperation.concatWith(enclosingTypeNames, '$'), '$', name),
				false);
	}
	public void addTypeReference(char[] typeName) {
		addTypeReference(typeName, false);
	}

	protected void addTypeReference(char[] typeName, boolean superType) {
		addNameReference(CharOperation.lastSegment(typeName, '.'));
		addIndexMetaQualification(typeName, superType);
	}

	protected void addIndexMetaQualification(char[] typeName, boolean superType) {
		char[] category = superType ? META_INDEX_SIMPLE_SUPER_TYPE_QUALIFIER_REF : META_INDEX_SIMPLE_TYPE_QUALIFIER_REF;
		if(CharOperation.contains('.', typeName)) {
			category = superType ? META_INDEX_QUALIFIED_SUPER_TYPE_QUALIFIER_REF : META_INDEX_QUALIFIED_TYPE_QUALIFIER_REF;
		}
		addIndexEntry(category, typeName);
	}
	public abstract void indexDocument();
	public void indexResolvedDocument() {
		// subtypes should implement where it makes sense.
	}
}
