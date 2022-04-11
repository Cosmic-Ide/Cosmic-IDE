/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - TypeConverters don't set enclosingType - https://bugs.eclipse.org/bugs/show_bug.cgi?id=320841
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AnnotationMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.lookup.ExtraCompilerModifiers;
import org.eclipse.jdt.internal.compiler.parser.TypeConverter;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.core.util.HashSetOfCharArrayArray;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Converter from a binary type to an AST type declaration.
 */
public class BinaryTypeConverter extends TypeConverter {

	private CompilationResult compilationResult;
	private HashSetOfCharArrayArray typeNames;

	public BinaryTypeConverter(ProblemReporter problemReporter, CompilationResult compilationResult, HashSetOfCharArrayArray typeNames) {
		super(problemReporter, Signature.C_DOLLAR);
		this.compilationResult = compilationResult;
		this.typeNames = typeNames;
	}

	public ImportReference[] buildImports(ClassFileReader reader) {
		// add remaining references to the list of type names
		// (code extracted from BinaryIndexer#extractReferenceFromConstantPool(...))
		int[] constantPoolOffsets = reader.getConstantPoolOffsets();
		int constantPoolCount = constantPoolOffsets.length;
		for (int i = 0; i < constantPoolCount; i++) {
			int tag = reader.u1At(constantPoolOffsets[i]);
			char[] name = null;
			switch (tag) {
				case ClassFileConstants.MethodRefTag :
				case ClassFileConstants.InterfaceMethodRefTag :
					int constantPoolIndex = reader.u2At(constantPoolOffsets[i] + 3);
					int utf8Offset = constantPoolOffsets[reader.u2At(constantPoolOffsets[constantPoolIndex] + 3)];
					name = reader.utf8At(utf8Offset + 3, reader.u2At(utf8Offset + 1));
					break;
				case ClassFileConstants.ClassTag :
					utf8Offset = constantPoolOffsets[reader.u2At(constantPoolOffsets[i] + 1)];
					name = reader.utf8At(utf8Offset + 3, reader.u2At(utf8Offset + 1));
					break;
			}
			if (name == null || (name.length > 0 && name[0] == '['))
				break; // skip over array references
			this.typeNames.add(CharOperation.splitOn('/', name));
		}

		// convert type names into import references
		int typeNamesLength = this.typeNames.size();
		ImportReference[] imports = new ImportReference[typeNamesLength];
		char[][][] set = this.typeNames.set;
		int index = 0;
		for (int i = 0, length = set.length; i < length; i++) {
			char[][] typeName = set[i];
			if (typeName != null) {
				imports[index++] = new ImportReference(typeName, new long[typeName.length]/*dummy positions*/, false/*not on demand*/, 0);
			}
		}
		return imports;
	}

	/**
	 * Convert a binary type into an AST type declaration and put it in the given compilation unit.
	 */
	public TypeDeclaration buildTypeDeclaration(IType type, CompilationUnitDeclaration compilationUnit)  throws JavaModelException {
		PackageFragment pkg = (PackageFragment) type.getPackageFragment();
		char[][] packageName = Util.toCharArrays(pkg.names);

		if (packageName.length > 0) {
			compilationUnit.currentPackage = new ImportReference(packageName, new long[]{0}, false, ClassFileConstants.AccDefault);
		}

		/* convert type */
		TypeDeclaration typeDeclaration = convert(type, null, null);

		IType alreadyComputedMember = type;
		IType parent = type.getDeclaringType();
		TypeDeclaration previousDeclaration = typeDeclaration;
		while(parent != null) {
			TypeDeclaration declaration = convert(parent, alreadyComputedMember, previousDeclaration);

			alreadyComputedMember = parent;
			previousDeclaration = declaration;
			parent = parent.getDeclaringType();
		}

		compilationUnit.types = new TypeDeclaration[]{previousDeclaration};

		return typeDeclaration;
	}

	private FieldDeclaration convert(IField field, IType type) throws JavaModelException {
		TypeReference typeReference = createTypeReference(field.getTypeSignature());
		if (typeReference == null) return null;
		FieldDeclaration fieldDeclaration = new FieldDeclaration();

		fieldDeclaration.name = field.getElementName().toCharArray();
		fieldDeclaration.type = typeReference;
		fieldDeclaration.modifiers = field.getFlags();

		return fieldDeclaration;
	}

	private AbstractMethodDeclaration convert(IMethod method, IType type) throws JavaModelException {

		AbstractMethodDeclaration methodDeclaration;

		org.eclipse.jdt.internal.compiler.ast.TypeParameter[] typeParams = null;

		// convert 1.5 specific constructs only if compliance is 1.5 or above
		if (this.has1_5Compliance) {
			/* convert type parameters */
			ITypeParameter[] typeParameters = method.getTypeParameters();
			if (typeParameters != null && typeParameters.length > 0) {
				int parameterCount = typeParameters.length;
				typeParams = new org.eclipse.jdt.internal.compiler.ast.TypeParameter[parameterCount];
				for (int i = 0; i < parameterCount; i++) {
					ITypeParameter typeParameter = typeParameters[i];
					typeParams[i] =
						createTypeParameter(
								typeParameter.getElementName().toCharArray(),
								stringArrayToCharArray(typeParameter.getBounds()),
								0,
								0);
				}
			}
		}

		if (method.isConstructor()) {
			ConstructorDeclaration decl = new ConstructorDeclaration(this.compilationResult);
			decl.bits &= ~ASTNode.IsDefaultConstructor;
			decl.typeParameters = typeParams;
			methodDeclaration = decl;
		} else {
			MethodDeclaration decl = type.isAnnotation() ? new AnnotationMethodDeclaration(this.compilationResult) : new MethodDeclaration(this.compilationResult);
			/* convert return type */
			TypeReference typeReference = createTypeReference(method.getReturnType());
			if (typeReference == null) return null;
			decl.returnType = typeReference;
			decl.typeParameters = typeParams;
			methodDeclaration = decl;
		}
		methodDeclaration.selector = method.getElementName().toCharArray();
		int flags = method.getFlags();
		boolean isVarargs = Flags.isVarargs(flags);
		methodDeclaration.modifiers = flags & ~Flags.AccVarargs;

		/* convert arguments */
		String[] argumentTypeNames = method.getParameterTypes();
		String[] argumentNames = method.getParameterNames();
		int argumentCount = argumentTypeNames == null ? 0 : argumentTypeNames.length;
		// Ignore synthetic arguments (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=212224)
		int startIndex = (method.isConstructor() && type.isMember() && !Flags.isStatic(type.getFlags())) ? 1 : 0;
		argumentCount -= startIndex;
		methodDeclaration.arguments = new Argument[argumentCount];
		for (int i = 0; i < argumentCount; i++) {
			String argumentTypeName = argumentTypeNames[startIndex+i];
			TypeReference typeReference = createTypeReference(argumentTypeName);
			if (typeReference == null) return null;
			if (isVarargs && i == argumentCount-1) {
				typeReference.bits |= ASTNode.IsVarArgs;
			}
			methodDeclaration.arguments[i] = new Argument(
				argumentNames[i].toCharArray(),
				0,
				typeReference,
				ClassFileConstants.AccDefault);
			// do not care whether was final or not
		}

		/* convert thrown exceptions */
		String[] exceptionTypeNames = method.getExceptionTypes();
		int exceptionCount = exceptionTypeNames == null ? 0 : exceptionTypeNames.length;
		if(exceptionCount > 0) {
			methodDeclaration.thrownExceptions = new TypeReference[exceptionCount];
			for (int i = 0; i < exceptionCount; i++) {
				TypeReference typeReference = createTypeReference(exceptionTypeNames[i]);
				if (typeReference == null) return null;
				methodDeclaration.thrownExceptions[i] = typeReference;
			}
		}
		return methodDeclaration;
	}

	private TypeDeclaration convert(IType type, IType alreadyComputedMember,TypeDeclaration alreadyComputedMemberDeclaration) throws JavaModelException {
		/* create type declaration - can be member type */
		TypeDeclaration typeDeclaration = new TypeDeclaration(this.compilationResult);

		if (type.getDeclaringType() != null) {
			typeDeclaration.bits |= ASTNode.IsMemberType;
		}
		typeDeclaration.name = type.getElementName().toCharArray();
		typeDeclaration.modifiers = type.getFlags();


		/* set superclass and superinterfaces */
		if (type.getSuperclassName() != null) {
			TypeReference typeReference = createTypeReference(type.getSuperclassTypeSignature());
			if (typeReference != null) {
				typeDeclaration.superclass = typeReference;
				typeDeclaration.superclass.bits |= ASTNode.IsSuperType;
			}
		}

		String[] interfaceTypes = type.getSuperInterfaceTypeSignatures();
		int interfaceCount = interfaceTypes == null ? 0 : interfaceTypes.length;
		typeDeclaration.superInterfaces = new TypeReference[interfaceCount];
		int count = 0;
		for (int i = 0; i < interfaceCount; i++) {
			TypeReference typeReference = createTypeReference(interfaceTypes[i]);
			if (typeReference != null) {
				typeDeclaration.superInterfaces[count] = typeReference;
				typeDeclaration.superInterfaces[count++].bits |= ASTNode.IsSuperType;
			}
		}
		if (count != interfaceCount) {
			System.arraycopy(typeDeclaration.fields, 0, typeDeclaration.superInterfaces = new TypeReference[interfaceCount], 0, interfaceCount);
		}

		// convert 1.5 specific constructs only if compliance is 1.5 or above
		if (this.has1_5Compliance) {

			/* convert type parameters */
			ITypeParameter[] typeParameters = type.getTypeParameters();
			if (typeParameters != null && typeParameters.length > 0) {
				int parameterCount = typeParameters.length;
				org.eclipse.jdt.internal.compiler.ast.TypeParameter[] typeParams = new org.eclipse.jdt.internal.compiler.ast.TypeParameter[parameterCount];
				for (int i = 0; i < parameterCount; i++) {
					ITypeParameter typeParameter = typeParameters[i];
					typeParams[i] =
						createTypeParameter(
								typeParameter.getElementName().toCharArray(),
								stringArrayToCharArray(typeParameter.getBounds()),
								0,
								0);
				}

				typeDeclaration.typeParameters = typeParams;
			}
		}

		/* convert member types */
		IType[] memberTypes = type.getTypes();
		int memberTypeCount =	memberTypes == null ? 0 : memberTypes.length;
		typeDeclaration.memberTypes = new TypeDeclaration[memberTypeCount];
		for (int i = 0; i < memberTypeCount; i++) {
			if(alreadyComputedMember != null && alreadyComputedMember.getFullyQualifiedName().equals(memberTypes[i].getFullyQualifiedName())) {
				typeDeclaration.memberTypes[i] = alreadyComputedMemberDeclaration;
			} else {
				typeDeclaration.memberTypes[i] = convert(memberTypes[i], null, null);
			}
			typeDeclaration.memberTypes[i].enclosingType = typeDeclaration;
		}

		/* convert fields */
		IField[] fields = type.getFields();
		int fieldCount = fields == null ? 0 : fields.length;
		typeDeclaration.fields = new FieldDeclaration[fieldCount];
		count = 0;
		for (int i = 0; i < fieldCount; i++) {
			FieldDeclaration fieldDeclaration = convert(fields[i], type);
			if (fieldDeclaration != null) {
				typeDeclaration.fields[count++] = fieldDeclaration;
			}
		}
		if (count != fieldCount) {
			System.arraycopy(typeDeclaration.fields, 0, typeDeclaration.fields = new FieldDeclaration[count], 0, count);
		}

		/* convert methods - need to add default constructor if necessary */
		IMethod[] methods = type.getMethods();
		int methodCount = methods == null ? 0 : methods.length;

		/* source type has a constructor ?           */
		/* by default, we assume that one is needed. */
		int neededCount = 1;
		for (int i = 0; i < methodCount; i++) {
			if (methods[i].isConstructor()) {
				neededCount = 0;
				// Does not need the extra constructor since one constructor already exists.
				break;
			}
		}
		boolean isInterface = type.isInterface();
		neededCount = isInterface ? 0 : neededCount;
		typeDeclaration.methods = new AbstractMethodDeclaration[methodCount + neededCount];
		if (neededCount != 0) { // add default constructor in first position
			typeDeclaration.methods[0] = typeDeclaration.createDefaultConstructor(false, false);
		}
		boolean hasAbstractMethods = false;
		count = 0;
		for (int i = 0; i < methodCount; i++) {
			AbstractMethodDeclaration method = convert(methods[i], type);
			if (method != null) {
				boolean isAbstract;
				if ((isAbstract = method.isAbstract()) || isInterface) { // fix-up flag
					method.modifiers |= ExtraCompilerModifiers.AccSemicolonBody;
				}
				if (isAbstract) {
					hasAbstractMethods = true;
				}
				typeDeclaration.methods[neededCount + (count++)] = method;
			}
		}
		if (count != methodCount) {
			System.arraycopy(typeDeclaration.methods, 0, typeDeclaration.methods = new AbstractMethodDeclaration[count + neededCount], 0, count + neededCount);
		}
		if (hasAbstractMethods) {
			typeDeclaration.bits |= ASTNode.HasAbstractMethods;
		}
		return typeDeclaration;
	}

	private static char[][] stringArrayToCharArray(String[] strings) {
		if (strings == null) return null;

		int length = strings.length;
		if (length == 0) return CharOperation.NO_CHAR_CHAR;

		char[][] result = new char [length][];
		for (int i = 0; i < length; i++) {
			result[i] = strings[i].toCharArray();
		}

		return result;
	}

	private TypeReference createTypeReference(String typeSignature) {
		TypeReference result = createTypeReference(typeSignature, 0, 0);
		if (this.typeNames != null && result instanceof QualifiedTypeReference) {
			this.typeNames.add(((QualifiedTypeReference)result).tokens);
		}
		return result;
	}
}
