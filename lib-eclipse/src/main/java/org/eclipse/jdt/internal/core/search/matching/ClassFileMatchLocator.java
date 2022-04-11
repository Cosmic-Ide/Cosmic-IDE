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
package org.eclipse.jdt.internal.core.search.matching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.env.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.core.BinaryType;
import org.eclipse.jdt.internal.core.*;
import org.eclipse.jdt.internal.core.search.indexing.IIndexConstants;

public class ClassFileMatchLocator implements IIndexConstants {

private static final long TARGET_ANNOTATION_BITS =
	TagBits.AnnotationForType |
	TagBits.AnnotationForParameter |
	TagBits.AnnotationForPackage |
	TagBits.AnnotationForMethod |
	TagBits.AnnotationForLocalVariable |
	TagBits.AnnotationForField |
	TagBits.AnnotationForConstructor |
	TagBits.AnnotationForAnnotationType |
	TagBits.AnnotationForModule |
	TagBits.AnnotationForRecordComponent;
private static final char[] JAVA_LANG_ANNOTATION_ELEMENTTYPE = CharOperation.concatWith(TypeConstants.JAVA_LANG_ANNOTATION_ELEMENTTYPE, '.');

/**
 * Convert binary name internally used in class files to binary name as specified by jls-13.1.
 * <p/>
 * Examples:
 * <ul>
 * <li>"java/lang/String" -> "java.lang.String",</li>
 * <li>"java/util/Map$Entry" -> "java.util.Map$Entry"</li>
 * </ul>
 *
 * @param name
 *            class files internal form of binary name
 * @return binary name as specified by
 *         <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-6.html#jls-6.7">jls-13.1</a>
 * @see Class#getName()
 * @see IBinaryType#getName()
 */
public static char[] convertClassFileFormat(char[] name) {
	return CharOperation.replaceOnCopy(name, '/', '.');
}

/**
 * Returns null or a qualified name suitable for search. Unlike "qualified name" as specified by
 * <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-6.html#jls-6.7">jls-6.7</a> (which defines that a
 * local class or anonymous class does not have a full qualified name) this will return a name for local classes.
 *
 * <p>
 * Note that the returned name could contain a "$" for a class or package that has a "$" in its simple name.
 * </p>
 *
 * @param type
 *            the IBinaryType to get the name from.
 * @return full qualified name suitable for search or null for anonymous classes.<br>
 *         Examples:
 *         <ul>
 *         <li>returns null for anonymous class like <code>new Object(){}</code>,</li>
 *         <li>returns "java.lang.String" for String,</li>
 *         <li>returns "java.util.Map.Entry" for the inner type java.util.Map$Entry,</li>
 *         <li>returns "mypackage.MyLocal" for local class inside a block statement like <code>{class MyLocal{}}</code>,</li>
 *         <li>should return "$.$.$1" (XXX currently does return ".....1") for $1 from <br>
 *         <code>package $;<br> class $ {class $1{}}</code></li>
 *         </ul>
 * @see Class#getPackageName()
 * @see Class#getSimpleName()
 * @see Class#getCanonicalName()
 **/
static char[] getSearchFullQualifiedSearchName(IBinaryType type) {
	if (type.isAnonymous()) {
		return null;
	}
	char[] binaryName = convertClassFileFormat(type.getName());
	// XXX textual replacement of "$" is not sufficient for classes/packages which contain "$" in their simple name:
	// nevertheless this at least solves finding normal named inner Types in .class files
	// "$" is rarely used since jls-3.8 states:
	// "the $ sign should be used only in mechanically generated source code or, rarely, to access pre-existing names on legacy systems"
	char[] qualifiedName = CharOperation.replaceOnCopy(binaryName, '$', '.');
	// Note on how a type name is calculated for the UI:
	// @see org.eclipse.jdt.internal.core.manipulation.BindingLabelProviderCore#getTypeLabel(ITypeBinding, long, StringBuffer)
	return qualifiedName;
}

private  boolean checkAnnotation(IBinaryAnnotation annotation, TypeReferencePattern pattern) {
	if (checkTypeName(pattern.simpleName, pattern.qualification, convertClassFileFormat(Signature.toCharArray(annotation.getTypeName())), pattern.isCaseSensitive, pattern.isCamelCase)) {
		return true;
	}
	IBinaryElementValuePair[] valuePairs = annotation.getElementValuePairs();
	if (valuePairs != null) {
		for (int j=0, vpLength=valuePairs.length; j<vpLength; j++) {
			IBinaryElementValuePair valuePair = valuePairs[j];
			Object pairValue = valuePair.getValue();
			if (pairValue instanceof IBinaryAnnotation) {
				if (checkAnnotation((IBinaryAnnotation) pairValue, pattern)) {
					return true;
				}
			}
		}
	}
	return false;
}
private boolean checkAnnotations(TypeReferencePattern pattern, IBinaryAnnotation[] annotations, long tagBits) {
	if (annotations != null) {
		for (int a=0, length=annotations.length; a<length; a++) {
			IBinaryAnnotation annotation = annotations[a];
			if (checkAnnotation(annotation, pattern)) {
				return true;
			}
		}
	}
	if ((tagBits & TagBits.AllStandardAnnotationsMask) != 0 && checkStandardAnnotations(tagBits, pattern)) {
		return true;
	}
	return false;
}
private boolean checkAnnotationTypeReference(char[] fullyQualifiedName, TypeReferencePattern pattern) {
	return checkTypeName(pattern.simpleName, pattern.qualification, fullyQualifiedName, pattern.isCaseSensitive, pattern.isCamelCase);
}
private boolean checkDeclaringType(IBinaryType enclosingBinaryType, char[] simpleName, char[] qualification, boolean isCaseSensitive, boolean isCamelCase) {
	if (simpleName == null && qualification == null) return true;
	if (enclosingBinaryType == null) return true;

	char[] declaringTypeName = convertClassFileFormat(enclosingBinaryType.getName());
	return checkTypeName(simpleName, qualification, declaringTypeName, isCaseSensitive, isCamelCase);
}
private boolean checkParameters(char[] methodDescriptor, char[][] parameterSimpleNames, char[][] parameterQualifications, boolean isCaseSensitive, boolean isCamelCase) {
	char[][] arguments = Signature.getParameterTypes(methodDescriptor);
	int parameterCount = parameterSimpleNames.length;
	if (parameterCount != arguments.length) return false;
	for (int i = 0; i < parameterCount; i++)
		if (!checkTypeName(parameterSimpleNames[i], parameterQualifications[i], Signature.toCharArray(arguments[i]), isCaseSensitive, isCamelCase))
			return false;
	return true;
}
private boolean checkStandardAnnotations(long annotationTagBits, TypeReferencePattern pattern) {
	if ((annotationTagBits & TagBits.AllStandardAnnotationsMask) == 0) {
		return false;
	}
	if ((annotationTagBits & TagBits.AnnotationTargetMASK) != 0) {
		char[][] compoundName = TypeConstants.JAVA_LANG_ANNOTATION_TARGET;
		if (checkAnnotationTypeReference(CharOperation.concatWith(compoundName, '.'), pattern) ||
			((annotationTagBits & TARGET_ANNOTATION_BITS) != 0 && checkAnnotationTypeReference(JAVA_LANG_ANNOTATION_ELEMENTTYPE, pattern))) {
			return true;
		}
	}
	if ((annotationTagBits & TagBits.AnnotationRetentionMASK) != 0) {
		char[][] compoundName = TypeConstants.JAVA_LANG_ANNOTATION_RETENTION;
		if (checkAnnotationTypeReference(CharOperation.concatWith(compoundName, '.'), pattern) ||
			checkAnnotationTypeReference(CharOperation.concatWith(TypeConstants.JAVA_LANG_ANNOTATION_RETENTIONPOLICY, '.'), pattern)) {
			return true;
		}
	}
	if ((annotationTagBits & TagBits.AnnotationDeprecated) != 0) {
		char[][] compoundName = TypeConstants.JAVA_LANG_DEPRECATED;
		if (checkAnnotationTypeReference(CharOperation.concatWith(compoundName, '.'), pattern)) {
			return true;
		}
	}
	if ((annotationTagBits & TagBits.AnnotationDocumented) != 0) {
		char[][] compoundName = TypeConstants.JAVA_LANG_ANNOTATION_DOCUMENTED;
		if (checkAnnotationTypeReference(CharOperation.concatWith(compoundName, '.'), pattern)) {
			return true;
		}
	}
	if ((annotationTagBits & TagBits.AnnotationInherited) != 0) {
		char[][] compoundName = TypeConstants.JAVA_LANG_ANNOTATION_INHERITED;
		if (checkAnnotationTypeReference(CharOperation.concatWith(compoundName, '.'), pattern)) {
			return true;
		}
	}
	if ((annotationTagBits & TagBits.AnnotationOverride) != 0) {
		char[][] compoundName = TypeConstants.JAVA_LANG_OVERRIDE;
		if (checkAnnotationTypeReference(CharOperation.concatWith(compoundName, '.'), pattern)) {
			return true;
		}
	}
	if ((annotationTagBits & TagBits.AnnotationSuppressWarnings) != 0) {
		char[][] compoundName = TypeConstants.JAVA_LANG_SUPPRESSWARNINGS;
		if (checkAnnotationTypeReference(CharOperation.concatWith(compoundName, '.'), pattern)) {
			return true;
		}
	}
	if ((annotationTagBits & TagBits.AnnotationSafeVarargs) != 0) {
		char[][] compoundName = TypeConstants.JAVA_LANG_SAFEVARARGS;
		if (checkAnnotationTypeReference(CharOperation.concatWith(compoundName, '.'), pattern)) {
			return true;
		}
	}
	if ((annotationTagBits & TagBits.AnnotationPolymorphicSignature) != 0) {
		char[][] compoundName = TypeConstants.JAVA_LANG_INVOKE_METHODHANDLE_$_POLYMORPHICSIGNATURE;
		if (checkAnnotationTypeReference(CharOperation.concatWith(compoundName, '.'), pattern)) {
			return true;
		}
	}
	return false;
}
private boolean checkTypeName(char[] simpleName, char[] qualification, char[] fullyQualifiedTypeName, boolean isCaseSensitive, boolean isCamelCase) {
	// NOTE: if case insensitive then simpleName & qualification are assumed to be lowercase
	char[] wildcardPattern = PatternLocator.qualifiedPattern(simpleName, qualification);
	if (wildcardPattern == null) return true;
	return CharOperation.match(wildcardPattern, fullyQualifiedTypeName, isCaseSensitive);
}
/**
 * Locate declaration in the current class file. This class file is always in a jar.
 */
public void locateMatches(MatchLocator locator, ClassFile classFile, IBinaryType info) throws CoreException {
	SearchPattern pattern = locator.pattern;

	// check annotations references
	matchAnnotations(pattern, locator, classFile, info);

	// check class definition
	BinaryType binaryType = (BinaryType) classFile.getType();
	if (matchBinary(pattern, info, null)) {
		binaryType = new ResolvedBinaryType(binaryType.getParent(), binaryType.getElementName(), binaryType.getKey());
		locator.reportBinaryMemberDeclaration(null, binaryType, null, info, SearchMatch.A_ACCURATE);
		return;
	}

	// Define arrays to store methods/fields from binary type if necessary
	IBinaryMethod[] binaryMethods = info.getMethods();
	int bMethodsLength = binaryMethods == null ? 0 : binaryMethods.length;
	IBinaryMethod[] unresolvedMethods = null;
	char[][] binaryMethodSignatures = null;
	boolean hasUnresolvedMethods = false;

	// Get fields from binary type info
	IBinaryField[] binaryFields = info.getFields();
	int bFieldsLength = binaryFields == null ? 0 : binaryFields.length;
	IBinaryField[] unresolvedFields = null;
	boolean hasUnresolvedFields = false;

	// Report as many accurate matches as possible
	int accuracy = SearchMatch.A_ACCURATE;
	boolean mustResolve = pattern.mustResolve;
	if (mustResolve) {
		BinaryTypeBinding binding = locator.cacheBinaryType(binaryType, info);
		if (binding != null) {
			// filter out element not in hierarchy scope
			if (!locator.typeInHierarchy(binding)) return;

			// Search matches on resolved methods
			MethodBinding[] availableMethods = binding.availableMethods();
			int aMethodsLength = availableMethods == null ? 0 : availableMethods.length;
			hasUnresolvedMethods = bMethodsLength != aMethodsLength;
			for (int i = 0; i < aMethodsLength; i++) {
				MethodBinding method = availableMethods[i];
				char[] methodSignature = method.genericSignature();
				if (methodSignature == null) methodSignature = method.signature();

				// Report the match if possible
				int level = locator.patternLocator.resolveLevel(method);
				if (level != PatternLocator.IMPOSSIBLE_MATCH) {
					IMethod methodHandle = binaryType.getMethod(
						new String(method.isConstructor() ? binding.compoundName[binding.compoundName.length-1] : method.selector),
						CharOperation.toStrings(Signature.getParameterTypes(convertClassFileFormat(methodSignature))));
					accuracy = level == PatternLocator.ACCURATE_MATCH ? SearchMatch.A_ACCURATE : SearchMatch.A_INACCURATE;
					locator.reportBinaryMemberDeclaration(null, methodHandle, method, info, accuracy);
				}

				// Remove method from unresolved list
				if (hasUnresolvedMethods) {
					if (binaryMethodSignatures == null) { // Store binary method signatures to avoid multiple computation
						binaryMethodSignatures = new char[bMethodsLength][];
						for (int j=0; j<bMethodsLength; j++) {
							IBinaryMethod binaryMethod = binaryMethods[j];
							char[] signature = binaryMethod.getGenericSignature();
							if (signature == null) signature = binaryMethod.getMethodDescriptor();
							binaryMethodSignatures[j] = signature;
						}
					}
					for (int j=0; j<bMethodsLength; j++) {
						if (CharOperation.equals(binaryMethods[j].getSelector(), method.selector) && CharOperation.equals(binaryMethodSignatures[j], methodSignature)) {
							if (unresolvedMethods == null) {
								System.arraycopy(binaryMethods, 0, unresolvedMethods = new IBinaryMethod[bMethodsLength], 0, bMethodsLength);
							}
							unresolvedMethods[j] = null;
							break;
						}
					}
				}
			}

			// Search matches on resolved fields
			FieldBinding[] availableFields = binding.availableFields();
			int aFieldsLength = availableFields == null ? 0 : availableFields.length;
			hasUnresolvedFields = bFieldsLength != aFieldsLength;
			for (int i = 0; i < aFieldsLength; i++) {
				FieldBinding field = availableFields[i];

				// Report the match if possible
				int level = locator.patternLocator.resolveLevel(field);
				if (level != PatternLocator.IMPOSSIBLE_MATCH) {
					IField fieldHandle = binaryType.getField(new String(field.name));
					accuracy = level == PatternLocator.ACCURATE_MATCH ? SearchMatch.A_ACCURATE : SearchMatch.A_INACCURATE;
					locator.reportBinaryMemberDeclaration(null, fieldHandle, field, info, accuracy);
				}

				// Remove the field from unresolved list
				if (hasUnresolvedFields) {
					for (int j=0; j<bFieldsLength; j++) {
						if ( CharOperation.equals(binaryFields[j].getName(), field.name)) {
							if (unresolvedFields == null) {
								System.arraycopy(binaryFields, 0, unresolvedFields = new IBinaryField[bFieldsLength], 0, bFieldsLength);
							}
							unresolvedFields[j] = null;
							break;
						}
					}
				}
			}

			// If all methods/fields were accurate then returns now
			if (!hasUnresolvedMethods && !hasUnresolvedFields) {
				return;
			}
		}
		accuracy = SearchMatch.A_INACCURATE;
	}

	// Report inaccurate methods
	if (mustResolve) binaryMethods = unresolvedMethods;
	bMethodsLength = binaryMethods == null ? 0 : binaryMethods.length;
	for (int i=0; i < bMethodsLength; i++) {
		IBinaryMethod method = binaryMethods[i];
		if (method == null) continue; // impossible match or already reported as accurate
		if (matchBinary(pattern, method, info)) {
			char[] name;
			if (method.isConstructor()) {
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=329727
				// We don't need the enclosing type name for the constructor name
				name = info.getSourceName();
			} else {
				name = method.getSelector();
			}
			String selector = new String(name);
			char[] methodSignature = binaryMethodSignatures == null ? null : binaryMethodSignatures[i];
			if (methodSignature == null) {
				methodSignature = method.getGenericSignature();
				if (methodSignature == null) methodSignature = method.getMethodDescriptor();
			}
			String[] parameterTypes = CharOperation.toStrings(Signature.getParameterTypes(convertClassFileFormat(methodSignature)));
			IMethod methodHandle = binaryType.getMethod(selector, parameterTypes);
			methodHandle = new ResolvedBinaryMethod(binaryType, selector, parameterTypes, methodHandle.getKey());
			locator.reportBinaryMemberDeclaration(null, methodHandle, null, info, accuracy);
		}
	}

	// Report inaccurate fields
	if (mustResolve) binaryFields =  unresolvedFields;
	bFieldsLength = binaryFields == null ? 0 : binaryFields.length;
	for (int i=0; i<bFieldsLength; i++) {
		IBinaryField field = binaryFields[i];
		if (field == null) continue; // impossible match or already reported as accurate
		if (matchBinary(pattern, field, info)) {
			String fieldName = new String(field.getName());
			IField fieldHandle = binaryType.getField(fieldName);
			fieldHandle = new ResolvedBinaryField(binaryType, fieldName, fieldHandle.getKey());
			locator.reportBinaryMemberDeclaration(null, fieldHandle, null, info, accuracy);
		}
	}
}
/*
 * Look for annotations references
 */
private void matchAnnotations(SearchPattern pattern, MatchLocator locator, ClassFile classFile, IBinaryType binaryType) throws CoreException {
	// Only process TypeReference patterns
	switch (pattern.kind) {
		case TYPE_REF_PATTERN:
			break;
		case OR_PATTERN:
			SearchPattern[] patterns = ((OrPattern) pattern).patterns;
			for (int i = 0, length = patterns.length; i < length; i++) {
				matchAnnotations(patterns[i], locator, classFile, binaryType);
			}
			// $FALL-THROUGH$ - fall through default to return
		default:
			return;
	}
	TypeReferencePattern typeReferencePattern  = (TypeReferencePattern) pattern;

	// Look for references in class annotations
	IBinaryAnnotation[] annotations = binaryType.getAnnotations();
	BinaryType classFileBinaryType = (BinaryType) classFile.getType();
	BinaryTypeBinding binaryTypeBinding = null;
	if (checkAnnotations(typeReferencePattern, annotations, binaryType.getTagBits())) {
		classFileBinaryType = new ResolvedBinaryType(classFileBinaryType.getParent(), classFileBinaryType.getElementName(), classFileBinaryType.getKey());
		TypeReferenceMatch match = new TypeReferenceMatch(classFileBinaryType, SearchMatch.A_ACCURATE, -1, 0, false, locator.getParticipant(), locator.currentPossibleMatch.resource);
		// TODO 3.4 M7 (frederic) - bug 209996: see how create the annotation handle from the binary and put it in the local element
		match.setLocalElement(null);
		locator.report(match);
	}

	// Look for references in methods annotations
	IBinaryMethod[] methods = binaryType.getMethods();
	if (methods != null) {
		for (int i = 0, max = methods.length; i < max; i++) {
			IBinaryMethod method = methods[i];
			if (checkAnnotations(typeReferencePattern, method.getAnnotations(), method.getTagBits())) {
					binaryTypeBinding = locator.cacheBinaryType(classFileBinaryType, binaryType);
					IMethod methodHandle = classFileBinaryType.getMethod(
						new String(method.isConstructor() ? binaryTypeBinding.compoundName[binaryTypeBinding.compoundName.length-1] : method.getSelector()),
						CharOperation.toStrings(Signature.getParameterTypes(convertClassFileFormat(method.getMethodDescriptor()))));
					TypeReferenceMatch match = new TypeReferenceMatch(methodHandle, SearchMatch.A_ACCURATE, -1, 0, false, locator.getParticipant(), locator.currentPossibleMatch.resource);
					// TODO 3.4 M7 (frederic) - bug 209996: see how create the annotation handle from the binary and put it in the local element
					match.setLocalElement(null);
					locator.report(match);
			}
		}
	}

	// Look for references in fields annotations
	IBinaryField[] fields = binaryType.getFields();
	if (fields != null) {
		for (int i = 0, max = fields.length; i < max; i++) {
			IBinaryField field = fields[i];
			if (checkAnnotations(typeReferencePattern, field.getAnnotations(), field.getTagBits())) {
					IField fieldHandle = classFileBinaryType.getField(new String(field.getName()));
					TypeReferenceMatch match = new TypeReferenceMatch(fieldHandle, SearchMatch.A_ACCURATE, -1, 0, false, locator.getParticipant(), locator.currentPossibleMatch.resource);
					// TODO 3.4 M7 (frederic) - bug 209996: see how create the annotation handle from the binary and put it in the local element
					match.setLocalElement(null);
					locator.report(match);
			}
		}
	}
}
/**
 * Finds out whether the given binary info matches the search pattern.
 * Default is to return false.
 */
boolean matchBinary(SearchPattern pattern, Object binaryInfo, IBinaryType enclosingBinaryType) {
	switch (pattern.kind) {
		case CONSTRUCTOR_PATTERN :
			return matchConstructor((ConstructorPattern) pattern, binaryInfo, enclosingBinaryType);
		case FIELD_PATTERN :
			return matchField((FieldPattern) pattern, binaryInfo, enclosingBinaryType);
		case METHOD_PATTERN :
			return matchMethod((MethodPattern) pattern, binaryInfo, enclosingBinaryType);
		case SUPER_REF_PATTERN :
			return matchSuperTypeReference((SuperTypeReferencePattern) pattern, binaryInfo, enclosingBinaryType);
		case TYPE_DECL_PATTERN :
			return matchTypeDeclaration((TypeDeclarationPattern) pattern, binaryInfo, enclosingBinaryType);
		case OR_PATTERN :
			SearchPattern[] patterns = ((OrPattern) pattern).patterns;
			for (int i = 0, length = patterns.length; i < length; i++)
				if (matchBinary(patterns[i], binaryInfo, enclosingBinaryType)) return true;
	}
	return false;
}
boolean matchConstructor(ConstructorPattern pattern, Object binaryInfo, IBinaryType enclosingBinaryType) {
	if (!pattern.findDeclarations) return false; // only relevant when finding declarations
	if (!(binaryInfo instanceof IBinaryMethod)) return false;

	IBinaryMethod method = (IBinaryMethod) binaryInfo;
	if (!method.isConstructor()) return false;
	if (!checkDeclaringType(enclosingBinaryType, pattern.declaringSimpleName, pattern.declaringQualification, pattern.isCaseSensitive(), pattern.isCamelCase()))
		return false;
	if (pattern.parameterSimpleNames != null) {
		char[] methodDescriptor = convertClassFileFormat(method.getMethodDescriptor());
		if (!checkParameters(methodDescriptor, pattern.parameterSimpleNames, pattern.parameterQualifications, pattern.isCaseSensitive(), pattern.isCamelCase()))
			return false;
	}
	return true;
}
boolean matchField(FieldPattern pattern, Object binaryInfo, IBinaryType enclosingBinaryType) {
	if (!pattern.findDeclarations) return false; // only relevant when finding declarations
	if (!(binaryInfo instanceof IBinaryField)) return false;

	IBinaryField field = (IBinaryField) binaryInfo;
	if (!pattern.matchesName(pattern.name, field.getName())) return false;
	if (!checkDeclaringType(enclosingBinaryType, pattern.declaringSimpleName, pattern.declaringQualification, pattern.isCaseSensitive(), pattern.isCamelCase()))
		return false;

	char[] fieldTypeSignature = Signature.toCharArray(convertClassFileFormat(field.getTypeName()));
	return checkTypeName(pattern.typeSimpleName, pattern.typeQualification, fieldTypeSignature, pattern.isCaseSensitive(), pattern.isCamelCase());
}
boolean matchMethod(MethodPattern pattern, Object binaryInfo, IBinaryType enclosingBinaryType) {
	if (!pattern.findDeclarations) return false; // only relevant when finding declarations
	if (!(binaryInfo instanceof IBinaryMethod)) return false;

	IBinaryMethod method = (IBinaryMethod) binaryInfo;
	if (!pattern.matchesName(pattern.selector, method.getSelector())) return false;
	if (!checkDeclaringType(enclosingBinaryType, pattern.declaringSimpleName, pattern.declaringQualification, pattern.isCaseSensitive(), pattern.isCamelCase()))
		return false;

	// look at return type only if declaring type is not specified
	boolean checkReturnType = pattern.declaringSimpleName == null && (pattern.returnSimpleName != null || pattern.returnQualification != null);
	boolean checkParameters = pattern.parameterSimpleNames != null;
	if (checkReturnType || checkParameters) {
		char[] methodDescriptor = convertClassFileFormat(method.getMethodDescriptor());
		if (checkReturnType) {
			char[] returnTypeSignature = Signature.toCharArray(Signature.getReturnType(methodDescriptor));
			if (!checkTypeName(pattern.returnSimpleName, pattern.returnQualification, returnTypeSignature, pattern.isCaseSensitive(), pattern.isCamelCase()))
				return false;
		}
		if (checkParameters &&  !checkParameters(methodDescriptor, pattern.parameterSimpleNames, pattern.parameterQualifications, pattern.isCaseSensitive(), pattern.isCamelCase()))
			return false;
	}
	return true;
}
boolean matchSuperTypeReference(SuperTypeReferencePattern pattern, Object binaryInfo, IBinaryType enclosingBinaryType) {
	if (!(binaryInfo instanceof IBinaryType)) return false;

	IBinaryType type = (IBinaryType) binaryInfo;

	// regarding SuperTypeReferencePattern.ONLY_SUPER_INTERFACES: superClass is also a SuperInterface:
	char[] vmName = type.getSuperclassName();
	if (vmName != null) {
		char[] superclassName = convertClassFileFormat(vmName);
		if (checkTypeName(pattern.superSimpleName, pattern.superQualification, superclassName, pattern.isCaseSensitive(), pattern.isCamelCase()))
			return true;
	}

	if (pattern.superRefKind != SuperTypeReferencePattern.ONLY_SUPER_CLASSES) {
		char[][] superInterfaces = type.getInterfaceNames();
		if (superInterfaces != null) {
			for (int i = 0, max = superInterfaces.length; i < max; i++) {
				char[] superInterfaceName = convertClassFileFormat(superInterfaces[i]);
				if (checkTypeName(pattern.superSimpleName, pattern.superQualification, superInterfaceName, pattern.isCaseSensitive(), pattern.isCamelCase()))
					return true;
			}
		}
	}
	return false;
}
boolean matchTypeDeclaration(TypeDeclarationPattern pattern, Object binaryInfo, IBinaryType enclosingBinaryType) {
	if (!(binaryInfo instanceof IBinaryType)) return false;

	IBinaryType type = (IBinaryType) binaryInfo;
	char[] fullyQualifiedTypeName = getSearchFullQualifiedSearchName(type);
	boolean qualifiedPattern = pattern instanceof QualifiedTypeDeclarationPattern;
	if (pattern.enclosingTypeNames == null || qualifiedPattern) {
		char[] simpleName = (pattern.getMatchMode() == SearchPattern.R_PREFIX_MATCH)
			? CharOperation.concat(pattern.simpleName, IIndexConstants.ONE_STAR)
			: pattern.simpleName;
		char[] pkg = qualifiedPattern ? ((QualifiedTypeDeclarationPattern)pattern).qualification : pattern.pkg;
		if (!checkTypeName(simpleName, pkg, fullyQualifiedTypeName, pattern.isCaseSensitive(), pattern.isCamelCase())) return false;
	} else {
		char[] enclosingTypeName = CharOperation.concatWith(pattern.enclosingTypeNames, '.');
		char[] patternString = pattern.pkg == null
			? enclosingTypeName
			: CharOperation.concat(pattern.pkg, enclosingTypeName, '.');
		if (!checkTypeName(pattern.simpleName, patternString, fullyQualifiedTypeName, pattern.isCaseSensitive(), pattern.isCamelCase())) return false;
	}

	int kind  = TypeDeclaration.kind(type.getModifiers());
	switch (pattern.typeSuffix) {
		case CLASS_SUFFIX:
			return kind == TypeDeclaration.CLASS_DECL || kind == TypeDeclaration.RECORD_DECL;
		case INTERFACE_SUFFIX:
			return kind == TypeDeclaration.INTERFACE_DECL;
		case ENUM_SUFFIX:
			return kind == TypeDeclaration.ENUM_DECL;
		case ANNOTATION_TYPE_SUFFIX:
			return kind == TypeDeclaration.ANNOTATION_TYPE_DECL;
		case CLASS_AND_INTERFACE_SUFFIX:
			return kind == TypeDeclaration.CLASS_DECL || kind == TypeDeclaration.INTERFACE_DECL;
		case CLASS_AND_ENUM_SUFFIX:
			return kind == TypeDeclaration.CLASS_DECL || kind == TypeDeclaration.ENUM_DECL;
		case INTERFACE_AND_ANNOTATION_SUFFIX:
			return kind == TypeDeclaration.INTERFACE_DECL || kind == TypeDeclaration.ANNOTATION_TYPE_DECL;
		case TYPE_SUFFIX: // nothing
	}
	return true;
}
}
