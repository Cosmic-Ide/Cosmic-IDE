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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.SearchDocument;
import org.eclipse.jdt.internal.compiler.ExtraFlags;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.classfmt.FieldInfo;
import org.eclipse.jdt.internal.compiler.classfmt.MethodInfo;
import org.eclipse.jdt.internal.compiler.env.ClassSignature;
import org.eclipse.jdt.internal.compiler.env.EnumConstantSignature;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryElementValuePair;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.IModule.IModuleReference;
import org.eclipse.jdt.internal.compiler.env.IModule.IPackageExport;
import org.eclipse.jdt.internal.compiler.env.IModule.IService;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.util.Util;

public class BinaryIndexer extends AbstractIndexer implements SuffixConstants {
	private static final char[] BYTE = "byte".toCharArray(); //$NON-NLS-1$
	private static final char[] CHAR = "char".toCharArray(); //$NON-NLS-1$
	private static final char[] DOUBLE = "double".toCharArray(); //$NON-NLS-1$
	private static final char[] FLOAT = "float".toCharArray(); //$NON-NLS-1$
	private static final char[] INT = "int".toCharArray(); //$NON-NLS-1$
	private static final char[] LONG = "long".toCharArray(); //$NON-NLS-1$
	private static final char[] SHORT = "short".toCharArray(); //$NON-NLS-1$
	private static final char[] BOOLEAN = "boolean".toCharArray(); //$NON-NLS-1$
	private static final char[] VOID = "void".toCharArray(); //$NON-NLS-1$
	private static final char[] INIT = "<init>".toCharArray(); //$NON-NLS-1$

	public BinaryIndexer(SearchDocument document) {
		super(document);
	}
	private void addBinaryStandardAnnotations(long annotationTagBits) {
		if ((annotationTagBits & TagBits.AllStandardAnnotationsMask) == 0) {
			return;
		}
		if ((annotationTagBits & TagBits.AnnotationTargetMASK) != 0) {
			char[][] compoundName = TypeConstants.JAVA_LANG_ANNOTATION_TARGET;
			addAnnotationTypeReference(compoundName[compoundName.length-1]);
			addBinaryTargetAnnotation(annotationTagBits);
		}
		if ((annotationTagBits & TagBits.AnnotationRetentionMASK) != 0) {
			char[][] compoundName = TypeConstants.JAVA_LANG_ANNOTATION_RETENTION;
			addAnnotationTypeReference(compoundName[compoundName.length-1]);
			addBinaryRetentionAnnotation(annotationTagBits);
		}
		if ((annotationTagBits & TagBits.AnnotationDeprecated) != 0) {
			char[][] compoundName = TypeConstants.JAVA_LANG_DEPRECATED;
			addAnnotationTypeReference(compoundName[compoundName.length-1]);
		}
		if ((annotationTagBits & TagBits.AnnotationDocumented) != 0) {
			char[][] compoundName = TypeConstants.JAVA_LANG_ANNOTATION_DOCUMENTED;
			addAnnotationTypeReference(compoundName[compoundName.length-1]);
		}
		if ((annotationTagBits & TagBits.AnnotationInherited) != 0) {
			char[][] compoundName = TypeConstants.JAVA_LANG_ANNOTATION_INHERITED;
			addAnnotationTypeReference(compoundName[compoundName.length-1]);
		}
		if ((annotationTagBits & TagBits.AnnotationOverride) != 0) {
			char[][] compoundName = TypeConstants.JAVA_LANG_OVERRIDE;
			addAnnotationTypeReference(compoundName[compoundName.length-1]);
		}
		if ((annotationTagBits & TagBits.AnnotationSuppressWarnings) != 0) {
			char[][] compoundName = TypeConstants.JAVA_LANG_SUPPRESSWARNINGS;
			addAnnotationTypeReference(compoundName[compoundName.length-1]);
		}
		if ((annotationTagBits & TagBits.AnnotationSafeVarargs) != 0) {
			char[][] compoundName = TypeConstants.JAVA_LANG_SAFEVARARGS;
			addAnnotationTypeReference(compoundName[compoundName.length-1]);
		}
		if ((annotationTagBits & TagBits.AnnotationPolymorphicSignature) != 0) {
			char[][] compoundName =
					TypeConstants.JAVA_LANG_INVOKE_METHODHANDLE_$_POLYMORPHICSIGNATURE;
			addAnnotationTypeReference(compoundName[compoundName.length-1]);
		}
	}
	private void addBinaryTargetAnnotation(long bits) {
		char[][] compoundName = null;
		if ((bits & TagBits.AnnotationForAnnotationType) != 0) {
			compoundName = TypeConstants.JAVA_LANG_ANNOTATION_ELEMENTTYPE;
			addTypeReference(compoundName[compoundName.length-1]);
			addFieldReference(TypeConstants.UPPER_ANNOTATION_TYPE);
		}
		if ((bits & TagBits.AnnotationForConstructor) != 0) {
			if (compoundName == null) {
				compoundName = TypeConstants.JAVA_LANG_ANNOTATION_ELEMENTTYPE;
				addTypeReference(compoundName[compoundName.length-1]);
			}
			addFieldReference(TypeConstants.UPPER_CONSTRUCTOR);
		}
		if ((bits & TagBits.AnnotationForField) != 0) {
			if (compoundName == null) {
				compoundName = TypeConstants.JAVA_LANG_ANNOTATION_ELEMENTTYPE;
				addTypeReference(compoundName[compoundName.length-1]);
			}
			addFieldReference(TypeConstants.UPPER_FIELD);
		}
		if ((bits & TagBits.AnnotationForLocalVariable) != 0) {
			if (compoundName == null) {
				compoundName = TypeConstants.JAVA_LANG_ANNOTATION_ELEMENTTYPE;
				addTypeReference(compoundName[compoundName.length-1]);
			}
			addFieldReference(TypeConstants.UPPER_LOCAL_VARIABLE);
		}
		if ((bits & TagBits.AnnotationForMethod) != 0) {
			if (compoundName == null) {
				compoundName = TypeConstants.JAVA_LANG_ANNOTATION_ELEMENTTYPE;
				addTypeReference(compoundName[compoundName.length-1]);
			}
			addFieldReference(TypeConstants.UPPER_METHOD);
		}
		if ((bits & TagBits.AnnotationForPackage) != 0) {
			if (compoundName == null) {
				compoundName = TypeConstants.JAVA_LANG_ANNOTATION_ELEMENTTYPE;
				addTypeReference(compoundName[compoundName.length-1]);
			}
			addFieldReference(TypeConstants.UPPER_PACKAGE);
		}
		if ((bits & TagBits.AnnotationForParameter) != 0) {
			if (compoundName == null) {
				compoundName = TypeConstants.JAVA_LANG_ANNOTATION_ELEMENTTYPE;
				addTypeReference(compoundName[compoundName.length-1]);
			}
			addFieldReference(TypeConstants.UPPER_PARAMETER);
		}
		if ((bits & TagBits.AnnotationForType) != 0) {
			if (compoundName == null) {
				compoundName = TypeConstants.JAVA_LANG_ANNOTATION_ELEMENTTYPE;
				addTypeReference(compoundName[compoundName.length-1]);
			}
			addFieldReference(TypeConstants.TYPE);
		}
		if ((bits & TagBits.AnnotationForModule) != 0) {
			if (compoundName == null) {
				compoundName = TypeConstants.JAVA_LANG_ANNOTATION_ELEMENTTYPE;
				addTypeReference(compoundName[compoundName.length-1]);
			}
			addFieldReference(TypeConstants.UPPER_MODULE);
		}
		if ((bits & TagBits.AnnotationForRecordComponent) != 0) {
			if (compoundName == null) {
				compoundName = TypeConstants.JAVA_LANG_ANNOTATION_ELEMENTTYPE;
				addTypeReference(compoundName[compoundName.length-1]);
			}
			addFieldReference(TypeConstants.UPPER_RECORD_COMPONENT);
		}
	}
	private void addBinaryRetentionAnnotation(long bits) {
		char[][] compoundName = TypeConstants.JAVA_LANG_ANNOTATION_RETENTIONPOLICY;
		addTypeReference(compoundName[compoundName.length-1]);
		if ((bits & TagBits.AnnotationRuntimeRetention) == TagBits.AnnotationRuntimeRetention) {
			addFieldReference(TypeConstants.UPPER_RUNTIME);
		} else if ((bits & TagBits.AnnotationClassRetention) != 0) {
			addFieldReference(TypeConstants.UPPER_CLASS);
		} else if ((bits & TagBits.AnnotationSourceRetention) != 0) {
			addFieldReference(TypeConstants.UPPER_SOURCE);
		}
	}
	private void addBinaryAnnotation(IBinaryAnnotation annotation) {
		addAnnotationTypeReference(replace('/', '.', Signature.toCharArray(annotation.getTypeName())));
		IBinaryElementValuePair[] valuePairs = annotation.getElementValuePairs();
		if (valuePairs != null) {
			for (int j=0, vpLength=valuePairs.length; j<vpLength; j++) {
				IBinaryElementValuePair valuePair = valuePairs[j];
				addMethodReference(valuePair.getName(), 0);
				Object pairValue = valuePair.getValue();
				addPairValue(pairValue);
			}
		}
	}
	private void addPairValue(Object pairValue) {
		if (pairValue instanceof EnumConstantSignature) {
			EnumConstantSignature enumConstant = (EnumConstantSignature) pairValue;
			addTypeReference(replace('/', '.', Signature.toCharArray(enumConstant.getTypeName())));
			addNameReference(enumConstant.getEnumConstantName());
		} else if (pairValue instanceof ClassSignature) {
			ClassSignature classConstant = (ClassSignature) pairValue;
			addTypeReference(replace('/', '.', Signature.toCharArray(classConstant.getTypeName())));
		} else if (pairValue instanceof IBinaryAnnotation) {
			addBinaryAnnotation((IBinaryAnnotation) pairValue);
		} else if (pairValue instanceof Object[]) {
			Object[] objects = (Object[]) pairValue;
			for (int i=0,l=objects.length; i<l; i++) {
				addPairValue(objects[i]);
			}
		}
	}
	@Override
	public void addTypeReference(char[] typeName) {
		int length = typeName.length;
		if (length > 2 && typeName[length - 2] == '$') {
			switch (typeName[length - 1]) {
				case '0' :
				case '1' :
				case '2' :
				case '3' :
				case '4' :
				case '5' :
				case '6' :
				case '7' :
				case '8' :
				case '9' :
					return; // skip local type names
			}
		}

	 	// consider that A$B is a member type: so replace '$' with '.'
	 	// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=40116)
		typeName = CharOperation.replaceOnCopy(typeName, '$', '.'); // copy it so the original is not modified

		super.addTypeReference(typeName);
	}
	/**
	 * For example:
	 *   - int foo(String[]) is ([Ljava/lang/String;)I => java.lang.String[] in a char[][]
	 *   - void foo(int) is (I)V ==> int
	 */
	private void convertToArrayType(char[][] parameterTypes, int counter, int arrayDim) {
		int length = parameterTypes[counter].length;
		char[] arrayType = new char[length + arrayDim*2];
		System.arraycopy(parameterTypes[counter], 0, arrayType, 0, length);
		for (int i = 0; i < arrayDim; i++) {
			arrayType[length + (i * 2)] = '[';
			arrayType[length + (i * 2) + 1] = ']';
		}
		parameterTypes[counter] = arrayType;
	}
	/**
	 * For example:
	 *   - int foo(String[]) is ([Ljava/lang/String;)I => java.lang.String[] in a char[][]
	 *   - void foo(int) is (I)V ==> int
	 */
	private char[] convertToArrayType(char[] typeName, int arrayDim) {
		int length = typeName.length;
		char[] arrayType = new char[length + arrayDim*2];
		System.arraycopy(typeName, 0, arrayType, 0, length);
		for (int i = 0; i < arrayDim; i++) {
			arrayType[length + (i * 2)] = '[';
			arrayType[length + (i * 2) + 1] = ']';
		}
		return arrayType;
	}
	private char[] decodeFieldType(char[] signature) throws ClassFormatException {
		if (signature == null) return null;
		int arrayDim = 0;
		for (int i = 0, max = signature.length; i < max; i++) {
			switch(signature[i]) {
				case 'B':
					if (arrayDim > 0)
						return convertToArrayType(BYTE, arrayDim);
					return BYTE;

				case 'C':
					if (arrayDim > 0)
						return convertToArrayType(CHAR, arrayDim);
					return CHAR;

				case 'D':
					if (arrayDim > 0)
						return convertToArrayType(DOUBLE, arrayDim);
					return DOUBLE;

				case 'F':
					if (arrayDim > 0)
						return convertToArrayType(FLOAT, arrayDim);
					return FLOAT;

				case 'I':
					if (arrayDim > 0)
					return convertToArrayType(INT, arrayDim);
					return INT;

				case 'J':
					if (arrayDim > 0)
						return convertToArrayType(LONG, arrayDim);
					return LONG;

				case 'L':
					int indexOfSemiColon = CharOperation.indexOf(';', signature, i+1);
					if (indexOfSemiColon == -1) throw new ClassFormatException(ClassFormatException.ErrInvalidMethodSignature);
					if (arrayDim > 0) {
						return convertToArrayType(replace('/','.',CharOperation.subarray(signature, i + 1, indexOfSemiColon)), arrayDim);
					}
					return replace('/','.',CharOperation.subarray(signature, i + 1, indexOfSemiColon));

				case 'S':
					if (arrayDim > 0)
						return convertToArrayType(SHORT, arrayDim);
					return SHORT;

				case 'Z':
					if (arrayDim > 0)
						return convertToArrayType(BOOLEAN, arrayDim);
					return BOOLEAN;

				case 'V':
					return VOID;

				case '[':
					arrayDim++;
					break;

				default:
					throw new ClassFormatException(ClassFormatException.ErrInvalidMethodSignature);
			}
		}
		return null;
	}
	/**
	 * For example:
	 *   - int foo(String[]) is ([Ljava/lang/String;)I => java.lang.String[] in a char[][]
	 *   - void foo(int) is (I)V ==> int
	 */
	private char[][] decodeParameterTypes(char[] signature, boolean firstIsSynthetic) throws ClassFormatException {
		if (signature == null) return null;
		int indexOfClosingParen = CharOperation.lastIndexOf(')', signature);
		if (indexOfClosingParen == 1) {
			// there is no parameter
			return null;
		}
		if (indexOfClosingParen == -1) {
			throw new ClassFormatException(ClassFormatException.ErrInvalidMethodSignature);
		}
		char[][] parameterTypes = new char[3][];
		int parameterTypesCounter = 0;
		int arrayDim = 0;
		for (int i = 1; i < indexOfClosingParen; i++) {
			if (parameterTypesCounter == parameterTypes.length) {
				// resize
				System.arraycopy(parameterTypes, 0, (parameterTypes = new char[parameterTypesCounter * 2][]), 0, parameterTypesCounter);
			}
			switch(signature[i]) {
				case 'B':
					parameterTypes[parameterTypesCounter++] = BYTE;
					if (arrayDim > 0)
						convertToArrayType(parameterTypes, parameterTypesCounter-1, arrayDim);
					arrayDim = 0;
					break;

				case 'C':
					parameterTypes[parameterTypesCounter++] = CHAR;
					if (arrayDim > 0)
						convertToArrayType(parameterTypes, parameterTypesCounter-1, arrayDim);
					arrayDim = 0;
					break;

				case 'D':
					parameterTypes[parameterTypesCounter++] = DOUBLE;
					if (arrayDim > 0)
						convertToArrayType(parameterTypes, parameterTypesCounter-1, arrayDim);
					arrayDim = 0;
					break;

				case 'F':
					parameterTypes[parameterTypesCounter++] = FLOAT;
					if (arrayDim > 0)
						convertToArrayType(parameterTypes, parameterTypesCounter-1, arrayDim);
					arrayDim = 0;
					break;

				case 'I':
					parameterTypes[parameterTypesCounter++] = INT;
					if (arrayDim > 0)
						convertToArrayType(parameterTypes, parameterTypesCounter-1, arrayDim);
					arrayDim = 0;
					break;

				case 'J':
					parameterTypes[parameterTypesCounter++] = LONG;
					if (arrayDim > 0)
						convertToArrayType(parameterTypes, parameterTypesCounter-1, arrayDim);
					arrayDim = 0;
					break;

				case 'L':
					int indexOfSemiColon = CharOperation.indexOf(';', signature, i+1);
					if (indexOfSemiColon == -1) throw new ClassFormatException(ClassFormatException.ErrInvalidMethodSignature);
					if (firstIsSynthetic && parameterTypesCounter == 0) {
						// skip first synthetic parameter
						firstIsSynthetic = false;
					} else {
						parameterTypes[parameterTypesCounter++] = replace('/','.',CharOperation.subarray(signature, i + 1, indexOfSemiColon));
						if (arrayDim > 0)
							convertToArrayType(parameterTypes, parameterTypesCounter-1, arrayDim);
					}
					i = indexOfSemiColon;
					arrayDim = 0;
					break;

				case 'S':
					parameterTypes[parameterTypesCounter++] = SHORT;
					if (arrayDim > 0)
						convertToArrayType(parameterTypes, parameterTypesCounter-1, arrayDim);
					arrayDim = 0;
					break;

				case 'Z':
					parameterTypes[parameterTypesCounter++] = BOOLEAN;
					if (arrayDim > 0)
						convertToArrayType(parameterTypes, parameterTypesCounter-1, arrayDim);
					arrayDim = 0;
					break;

				case '[':
					arrayDim++;
					break;

				default:
					throw new ClassFormatException(ClassFormatException.ErrInvalidMethodSignature);
			}
		}
		if (parameterTypes.length != parameterTypesCounter) {
			System.arraycopy(parameterTypes, 0, parameterTypes = new char[parameterTypesCounter][], 0, parameterTypesCounter);
		}
		return parameterTypes;
	}
	private char[] decodeReturnType(char[] signature) throws ClassFormatException {
		if (signature == null) return null;
		int indexOfClosingParen = CharOperation.lastIndexOf(')', signature);
		if (indexOfClosingParen == -1) throw new ClassFormatException(ClassFormatException.ErrInvalidMethodSignature);
		int arrayDim = 0;
		for (int i = indexOfClosingParen + 1, max = signature.length; i < max; i++) {
			switch(signature[i]) {
				case 'B':
					if (arrayDim > 0)
						return convertToArrayType(BYTE, arrayDim);
					return BYTE;

				case 'C':
					if (arrayDim > 0)
						return convertToArrayType(CHAR, arrayDim);
					return CHAR;

				case 'D':
					if (arrayDim > 0)
						return convertToArrayType(DOUBLE, arrayDim);
					return DOUBLE;

				case 'F':
					if (arrayDim > 0)
						return convertToArrayType(FLOAT, arrayDim);
					return FLOAT;

				case 'I':
					if (arrayDim > 0)
						return convertToArrayType(INT, arrayDim);
					return INT;

				case 'J':
					if (arrayDim > 0)
						return convertToArrayType(LONG, arrayDim);
					return LONG;

				case 'L':
					int indexOfSemiColon = CharOperation.indexOf(';', signature, i+1);
					if (indexOfSemiColon == -1) throw new ClassFormatException(ClassFormatException.ErrInvalidMethodSignature);
					if (arrayDim > 0) {
						return convertToArrayType(replace('/','.',CharOperation.subarray(signature, i + 1, indexOfSemiColon)), arrayDim);
					}
					return replace('/','.',CharOperation.subarray(signature, i + 1, indexOfSemiColon));

				case 'S':
					if (arrayDim > 0)
						return convertToArrayType(SHORT, arrayDim);
					return SHORT;

				case 'Z':
					if (arrayDim > 0)
						return convertToArrayType(BOOLEAN, arrayDim);
					return BOOLEAN;

				case 'V':
					return VOID;

				case '[':
					arrayDim++;
					break;

				default:
					throw new ClassFormatException(ClassFormatException.ErrInvalidMethodSignature);
			}
		}
		return null;
	}
	private int extractArgCount(char[] signature, char[] className) throws ClassFormatException {
		int indexOfClosingParen = CharOperation.lastIndexOf(')', signature);
		if (indexOfClosingParen == 1) {
			// there is no parameter
			return 0;
		}
		if (indexOfClosingParen == -1) {
			throw new ClassFormatException(ClassFormatException.ErrInvalidMethodSignature);
		}
		int parameterTypesCounter = 0;
		for (int i = 1; i < indexOfClosingParen; i++) {
			switch(signature[i]) {
				case 'B':
				case 'C':
				case 'D':
				case 'F':
				case 'I':
				case 'J':
				case 'S':
				case 'Z':
					parameterTypesCounter++;
					break;
				case 'L':
					int indexOfSemiColon = CharOperation.indexOf(';', signature, i+1);
					if (indexOfSemiColon == -1) throw new ClassFormatException(ClassFormatException.ErrInvalidMethodSignature);
					// verify if first parameter is synthetic
					if (className != null && parameterTypesCounter == 0) {
						char[] classSignature = Signature.createCharArrayTypeSignature(className, true);
						int length = indexOfSemiColon-i+1;
						if (classSignature.length > (length+1)) {
							// synthetic means that parameter type has same signature than given class
							for (int j=i, k=0; j<indexOfSemiColon; j++, k++) {
								if (!(signature[j] == classSignature[k] || (signature[j] == '/' && classSignature[k] == '.' ))) {
									parameterTypesCounter++;
									break;
								}
							}
						} else {
							parameterTypesCounter++;
						}
						className = null; // do not verify following parameters
					} else {
						parameterTypesCounter++;
					}
					i = indexOfSemiColon;
					break;
				case '[':
					break;
				default:
					throw new ClassFormatException(ClassFormatException.ErrInvalidMethodSignature);
			}
		}
		return parameterTypesCounter;
	}
	private char[] extractClassName(int[] constantPoolOffsets, ClassFileReader reader, int index) {
		// the entry at i has to be a field ref or a method/interface method ref.
		int class_index = reader.u2At(constantPoolOffsets[index] + 1);
		int utf8Offset = constantPoolOffsets[reader.u2At(constantPoolOffsets[class_index] + 1)];
		return reader.utf8At(utf8Offset + 3, reader.u2At(utf8Offset + 1));
	}
	private char[] extractName(int[] constantPoolOffsets, ClassFileReader reader, int index) {
		int nameAndTypeIndex = reader.u2At(constantPoolOffsets[index] + 3);
		int utf8Offset = constantPoolOffsets[reader.u2At(constantPoolOffsets[nameAndTypeIndex] + 1)];
		return reader.utf8At(utf8Offset + 3, reader.u2At(utf8Offset + 1));
	}
	private char[] extractClassReference(int[] constantPoolOffsets, ClassFileReader reader, int index) {
		// the entry at i has to be a class ref.
		int utf8Offset = constantPoolOffsets[reader.u2At(constantPoolOffsets[index] + 1)];
		return reader.utf8At(utf8Offset + 3, reader.u2At(utf8Offset + 1));
	}
	/**
	 * Extract all type, method, field and interface method references from the constant pool
	 */
	private void extractReferenceFromConstantPool(byte[] contents, ClassFileReader reader) throws ClassFormatException {
		int[] constantPoolOffsets = reader.getConstantPoolOffsets();
		int constantPoolCount = constantPoolOffsets.length;
		for (int i = 1; i < constantPoolCount; i++) {
			int tag = reader.u1At(constantPoolOffsets[i]);
			/**
			 * u1 tag
			 * u2 class_index
			 * u2 name_and_type_index
			 */
			char[] name = null;
			char[] type = null;
			switch (tag) {
				case ClassFileConstants.FieldRefTag :
					// add reference to the class/interface and field name and type
					name = extractName(constantPoolOffsets, reader, i);
					addFieldReference(name);
					break;
				case ClassFileConstants.MethodRefTag :
					// add reference to the class and method name and type
				case ClassFileConstants.InterfaceMethodRefTag :
					// add reference to the interface and method name and type
					name = extractName(constantPoolOffsets, reader, i);
					type = extractType(constantPoolOffsets, reader, i);
					if (CharOperation.equals(INIT, name)) {
						// get class name and see if it's a local type or not
						char[] className = extractClassName(constantPoolOffsets, reader, i);
						boolean localType = false;
						if (className !=  null) {
							for (int c = 0, max = className.length; c < max; c++) {
								switch (className[c]) {
									case '/':
										className[c] = '.';
										break;
									case '$':
										localType = true;
										break;
								}
							}
						}
						// add a constructor reference, use class name to extract arg count if it's a local type to remove synthetic parameter
						addConstructorReference(className, extractArgCount(type, localType?className:null));
					} else {
						// add a method reference
						addMethodReference(name, extractArgCount(type, null));
					}
					break;
				case ClassFileConstants.ClassTag :
					// add a type reference
					name = extractClassReference(constantPoolOffsets, reader, i);
					if (name.length > 0 && name[0] == '[')
						break; // skip over array references
					name = replace('/', '.', name); // so that it looks like java.lang.String
					addTypeReference(name);

					// also add a simple reference on each segment of the qualification (see http://bugs.eclipse.org/bugs/show_bug.cgi?id=24741)
					char[][] qualification = CharOperation.splitOn('.', name);
					for (int j = 0, length = qualification.length; j < length; j++) {
						addNameReference(qualification[j]);
					}
					break;
			}
		}
	}
	private char[] extractType(int[] constantPoolOffsets, ClassFileReader reader, int index) {
		int constantPoolIndex = reader.u2At(constantPoolOffsets[index] + 3);
		int utf8Offset = constantPoolOffsets[reader.u2At(constantPoolOffsets[constantPoolIndex] + 3)];
		return reader.utf8At(utf8Offset + 3, reader.u2At(utf8Offset + 1));
	}
	@Override
	public void indexDocument() {
		try {
			final byte[] contents = this.document.getByteContents();
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=107124
			// contents can potentially be null if a IOException occurs while retrieving the contents
			if (contents == null) return;
			final String path = this.document.getPath();
			ClassFileReader reader = new ClassFileReader(contents, path == null ? null : path.toCharArray());

			IModule module = reader.getModuleDeclaration();
			if (module != null) {
				indexModule(module);
				return;
			}

			// first add type references
			char[] className = replace('/', '.', reader.getName()); // looks like java/lang/String
			// need to extract the package name and the simple name
			int packageNameIndex = CharOperation.lastIndexOf('.', className);
			char[] packageName = null;
			char[] name = null;
			if (packageNameIndex >= 0) {
				packageName = CharOperation.subarray(className, 0, packageNameIndex);
				name = CharOperation.subarray(className, packageNameIndex + 1, className.length);
			} else {
				packageName = CharOperation.NO_CHAR;
				name = className;
			}
			char[] enclosingTypeName = null;
			boolean isNestedType = reader.isNestedType();
			if (isNestedType) {
				if (reader.isAnonymous()) {
					name = CharOperation.NO_CHAR;
				} else {
					name = reader.getInnerSourceName();
				}
				if (reader.isLocal() || reader.isAnonymous()) {
					// set specific ['0'] value for local and anonymous to be able to filter them
					enclosingTypeName = ONE_ZERO;
				} else {
					char[] fullEnclosingName = reader.getEnclosingTypeName();
					int nameLength = fullEnclosingName.length - packageNameIndex - 1;
					if (nameLength <= 0) {
						// See PR 1GIR345: ITPJCORE:ALL - Indexer: NegativeArraySizeException
						return;
					}
					enclosingTypeName = new char[nameLength];
					System.arraycopy(fullEnclosingName, packageNameIndex + 1, enclosingTypeName, 0, nameLength);
				}
			}
			// type parameters
			char[][] typeParameterSignatures = null;
			char[] genericSignature = reader.getGenericSignature();
			if (genericSignature != null) {
				CharOperation.replace(genericSignature, '/', '.');
				typeParameterSignatures = Signature.getTypeParameters(genericSignature);
			}

			// eliminate invalid innerclasses (1G4KCF7)
			if (name == null) return;

			char[][] superinterfaces = replace('/', '.', reader.getInterfaceNames());
			char[][] enclosingTypeNames = enclosingTypeName == null ? null : new char[][] {enclosingTypeName};
			int modifiers = reader.getModifiers();
			switch (TypeDeclaration.kind(modifiers)) {
				case TypeDeclaration.CLASS_DECL :
					char[] superclass = replace('/', '.', reader.getSuperclassName());
					addClassDeclaration(modifiers, packageName, name, enclosingTypeNames, superclass, superinterfaces, typeParameterSignatures, false);
					break;
				case TypeDeclaration.INTERFACE_DECL :
					addInterfaceDeclaration(modifiers, packageName, name, enclosingTypeNames, superinterfaces, typeParameterSignatures, false);
					break;
				case TypeDeclaration.ENUM_DECL :
					superclass = replace('/', '.', reader.getSuperclassName());
					addEnumDeclaration(modifiers, packageName, name, enclosingTypeNames, superclass, superinterfaces, false);
					break;
				case TypeDeclaration.ANNOTATION_TYPE_DECL :
					addAnnotationTypeDeclaration(modifiers, packageName, name, enclosingTypeNames, false);
					break;
				case TypeDeclaration.RECORD_DECL :
					superclass = replace('/', '.', reader.getSuperclassName());
					addClassDeclaration(modifiers, packageName, name, enclosingTypeNames, superclass, superinterfaces, typeParameterSignatures, false);
					break;
			}

			// Look for references in class annotations
			IBinaryAnnotation[] annotations = reader.getAnnotations();
			if (annotations != null) {
				for (int a=0, length=annotations.length; a<length; a++) {
					IBinaryAnnotation annotation = annotations[a];
					addBinaryAnnotation(annotation);
				}
			}
			long tagBits = reader.getTagBits() & TagBits.AllStandardAnnotationsMask;
			if (tagBits != 0) {
				addBinaryStandardAnnotations(tagBits);
			}

			int extraFlags = ExtraFlags.getExtraFlags(reader);

			// first reference all methods declarations and field declarations
			MethodInfo[] methods = (MethodInfo[]) reader.getMethods();
			boolean noConstructor = true;
			if (methods != null) {
				for (int i = 0, max = methods.length; i < max; i++) {
					MethodInfo method = methods[i];
					boolean isConstructor = method.isConstructor();
					char[] descriptor = method.getMethodDescriptor();
					char[][] parameterTypes = decodeParameterTypes(descriptor, isConstructor && isNestedType);
					char[] returnType = decodeReturnType(descriptor);
					char[][] exceptionTypes = replace('/', '.', method.getExceptionTypeNames());
					if (isConstructor) {
						noConstructor = false;
						char[] signature = method.getGenericSignature();
						if (signature == null) {
							if (reader.isNestedType() && ((modifiers & ClassFileConstants.AccStatic) == 0)) {
								signature = removeFirstSyntheticParameter(descriptor);
							} else {
								signature = descriptor;
							}
						}
						addConstructorDeclaration(
								name,
								parameterTypes == null ? 0 : parameterTypes.length,
								signature,
								parameterTypes,
								method.getArgumentNames(),
								method.getModifiers(),
								packageName,
								modifiers,
								exceptionTypes,
								extraFlags);
					} else {
						if (!method.isClinit()) {
							char[] selector = method.getSelector();
							addMethodDeclaration(selector, parameterTypes, returnType, exceptionTypes);
							char[] signature = method.getGenericSignature();
							if (signature == null) {
								signature = descriptor;
							}
							if (name.length > 0)  {
								addMethodDeclaration(
										name,
										null,
										selector,
										parameterTypes == null ? 0 : parameterTypes.length,
												signature,
												parameterTypes,
												method.getArgumentNames(),
												returnType,
												method.getModifiers(),
												packageName,
												modifiers,
												exceptionTypes,
												extraFlags);
							}
						}
					}
					// look for references in method annotations
					annotations = method.getAnnotations();
					if (annotations != null) {
						for (int a=0, length=annotations.length; a<length; a++) {
							IBinaryAnnotation annotation = annotations[a];
							addBinaryAnnotation(annotation);
						}
					}
					tagBits = method.getTagBits() & TagBits.AllStandardAnnotationsMask;
					if (tagBits != 0) {
						addBinaryStandardAnnotations(tagBits);
					}
				}
			}
			if (noConstructor) {
				addDefaultConstructorDeclaration(className, packageName, modifiers, extraFlags);
			}
			FieldInfo[] fields = (FieldInfo[]) reader.getFields();
			if (fields != null) {
				for (int i = 0, max = fields.length; i < max; i++) {
					FieldInfo field = fields[i];
					char[] fieldName = field.getName();
					char[] fieldType = decodeFieldType(replace('/', '.', field.getTypeName()));
					addFieldDeclaration(fieldType, fieldName);
					// look for references in field annotations
					annotations = field.getAnnotations();
					if (annotations != null) {
						for (int a=0, length=annotations.length; a<length; a++) {
							IBinaryAnnotation annotation = annotations[a];
							addBinaryAnnotation(annotation);
						}
					}
					tagBits = field.getTagBits() & TagBits.AllStandardAnnotationsMask;
					if (tagBits != 0) {
						addBinaryStandardAnnotations(tagBits);
					}
				}
			}
			// record all references found inside the .class file
			extractReferenceFromConstantPool(contents, reader);
		} catch (ClassFormatException | RuntimeException e) {
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=182154
			// logging the entry that could not be indexed and continue with the next one
			// we remove all entries relative to the boggus document
			this.document.removeAllIndexEntries();
			Util.log(new Status(IStatus.WARNING,
					JavaCore.PLUGIN_ID,
					"The Java indexing could not index " + this.document.getPath() + ". This .class file doesn't follow the class file format specification. Please report this issue against the .class file vendor", //$NON-NLS-1$ //$NON-NLS-2$
					e));
		}
	}

	private void indexModule(IModule module) {
		addModuleDeclaration(module.name());
		IModuleReference[] requiredModules = module.requires();
		if (requiredModules != null) {
			for (IModuleReference req : requiredModules) {
				addModuleReference(req.name());
			}
		}
		indexPackageVisibilityDirective(module.exports());
		indexPackageVisibilityDirective(module.opens());
		char[][] refUsed = module.uses();
		if (refUsed != null) {
			for (char[] ref : refUsed) {
				indexTypeReference(ref);
			}
		}
		IService[] services = module.provides();
		if (services != null) {
			for (IService service : services) {
				indexTypeReference(service.name());
				indexTypeReferences(service.with());
			}
		}
	}
	private void indexPackageVisibilityDirective(IPackageExport[] exportedPackages) {
		if (exportedPackages != null) {
			for (IPackageExport pack : exportedPackages) {
				addModuleExportedPackages(pack.name());
				char[][] tgts = pack.targets();
				if (tgts == null || tgts == CharOperation.NO_CHAR_CHAR) continue;
				for (char[] tgt : tgts) {
					if (tgt != null && tgt != CharOperation.NO_CHAR)
						addModuleReference(tgt);
				}
			}
		}
	}
	private void indexTypeReferences(char[][] ref) {
		if (ref == null || ref == CharOperation.NO_CHAR_CHAR)
			return;
		for (int i = 0; i < ref.length; i++) {
			addTypeReference(ref[i]);
		}
	}
	private void indexTypeReference(char[] ref) {
		if (ref == null || ref == CharOperation.NO_CHAR)
			return;
		addTypeReference(ref);
	}

	private char[] removeFirstSyntheticParameter(char[] descriptor) {
		if (descriptor == null) return null;
		if (descriptor.length < 3) return descriptor;
		if (descriptor[0] != '(') return descriptor;
		if (descriptor[1] != ')') {
			// remove the first synthetic parameter
			int start = org.eclipse.jdt.internal.compiler.util.Util.scanTypeSignature(descriptor, 1) + 1;
			int length = descriptor.length - start;
			char[] signature = new char[length + 1];
			signature[0] = descriptor[0];
			System.arraycopy(descriptor, start, signature, 1, length);
			return signature;
		} else {
			return descriptor;
		}
	}
	/*
	 * Modify the array by replacing all occurences of toBeReplaced with newChar
	 */
	private char[][] replace(char toBeReplaced, char newChar, char[][] array) {
		if (array == null) return null;
		for (int i = 0, max = array.length; i < max; i++) {
			replace(toBeReplaced, newChar, array[i]);
		}
		return array;
	}
	/*
	 * Modify the array by replacing all occurences of toBeReplaced with newChar
	 */
	private char[] replace(char toBeReplaced, char newChar, char[] array) {
		if (array == null) return null;
		for (int i = 0, max = array.length; i < max; i++) {
			if (array[i] == toBeReplaced) {
				array[i] = newChar;
			}
		}
		return array;
	}
}
