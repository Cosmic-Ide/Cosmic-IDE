/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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


import java.util.regex.Pattern;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.core.search.indexing.IIndexConstants;

public abstract class PatternLocator implements IIndexConstants, IQualifiedTypeResolutionListener {

// store pattern info
protected int matchMode;
protected boolean isCaseSensitive;
protected boolean isEquivalentMatch;
protected boolean isErasureMatch;
protected boolean mustResolve;
protected boolean mayBeGeneric;

// match to report
SearchMatch match = null;

/* match levels */
public static final int IMPOSSIBLE_MATCH = 0;
public static final int INACCURATE_MATCH = 1;
public static final int POSSIBLE_MATCH = 2;
public static final int ACCURATE_MATCH = 3;
public static final int ERASURE_MATCH = 4;

// Possible rule match flavors
int flavors = 0;
// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=79866
public static final int NO_FLAVOR = 0x0000;
public static final int EXACT_FLAVOR = 0x0010;
public static final int PREFIX_FLAVOR = 0x0020;
public static final int PATTERN_FLAVOR = 0x0040;
public static final int REGEXP_FLAVOR = 0x0080;
public static final int CAMELCASE_FLAVOR = 0x0100;
public static final int SUPER_INVOCATION_FLAVOR = 0x0200;
public static final int SUB_INVOCATION_FLAVOR = 0x0400;
public static final int OVERRIDDEN_METHOD_FLAVOR = 0x0800;
public static final int SUPERTYPE_REF_FLAVOR = 0x1000;
public static final int MATCH_LEVEL_MASK = 0x0F;
public static final int FLAVORS_MASK = ~MATCH_LEVEL_MASK;

/* match container */
public static final int COMPILATION_UNIT_CONTAINER = 1;
public static final int CLASS_CONTAINER = 2;
public static final int METHOD_CONTAINER = 4;
public static final int FIELD_CONTAINER = 8;
public static final int ALL_CONTAINER =
	COMPILATION_UNIT_CONTAINER | CLASS_CONTAINER | METHOD_CONTAINER | FIELD_CONTAINER;

/* match rule */
public static final int RAW_MASK = SearchPattern.R_EQUIVALENT_MATCH | SearchPattern.R_ERASURE_MATCH;
public static final int RULE_MASK = RAW_MASK; // no other values for the while...

public static PatternLocator patternLocator(SearchPattern pattern) {
	switch (pattern.kind) {
		case IIndexConstants.PKG_REF_PATTERN :
			return new PackageReferenceLocator((PackageReferencePattern) pattern);
		case IIndexConstants.PKG_DECL_PATTERN :
			return new PackageDeclarationLocator((PackageDeclarationPattern) pattern);
		case IIndexConstants.TYPE_REF_PATTERN :
			return new TypeReferenceLocator((TypeReferencePattern) pattern);
		case IIndexConstants.TYPE_DECL_PATTERN :
			return new TypeDeclarationLocator((TypeDeclarationPattern) pattern);
		case IIndexConstants.SUPER_REF_PATTERN :
			return new SuperTypeReferenceLocator((SuperTypeReferencePattern) pattern);
		case IIndexConstants.CONSTRUCTOR_PATTERN :
			return new ConstructorLocator((ConstructorPattern) pattern);
		case IIndexConstants.FIELD_PATTERN :
			return new FieldLocator((FieldPattern) pattern);
		case IIndexConstants.METHOD_PATTERN :
			return new MethodLocator((MethodPattern) pattern);
		case IIndexConstants.OR_PATTERN :
			return new OrLocator((OrPattern) pattern);
		case IIndexConstants.AND_PATTERN :
			return new AndLocator((AndPattern) pattern);
		case IIndexConstants.LOCAL_VAR_PATTERN :
			return new LocalVariableLocator((LocalVariablePattern) pattern);
		case IIndexConstants.TYPE_PARAM_PATTERN:
			return new TypeParameterLocator((TypeParameterPattern) pattern);
		case IIndexConstants.MODULE_PATTERN:
			return new ModuleLocator((ModulePattern) pattern);
	}
	return null;
}
public static char[] qualifiedPattern(char[] simpleNamePattern, char[] qualificationPattern) {
	// NOTE: if case insensitive search then simpleNamePattern & qualificationPattern are assumed to be lowercase
	if (simpleNamePattern == null) {
		if (qualificationPattern == null) return null;
		return CharOperation.concat(qualificationPattern, ONE_STAR, '.');
	} else {
		return qualificationPattern == null
			? CharOperation.concat(ONE_STAR, simpleNamePattern)
			: CharOperation.concat(qualificationPattern, simpleNamePattern, '.');
	}
}
public static char[] qualifiedSourceName(TypeBinding binding) {
	if (binding instanceof ReferenceBinding) {
		ReferenceBinding type = (ReferenceBinding) binding;
		if (type.isLocalType())
			return type.isMemberType()
				? CharOperation.concat(qualifiedSourceName(type.enclosingType()), type.sourceName(), '.')
				: CharOperation.concat(qualifiedSourceName(type.enclosingType()), new char[] {'.', '1', '.'}, type.sourceName());
	}
	return binding != null ? binding.qualifiedSourceName() : null;
}

public PatternLocator(SearchPattern pattern) {
	int matchRule = pattern.getMatchRule();
	this.isCaseSensitive = (matchRule & SearchPattern.R_CASE_SENSITIVE) != 0;
	this.isErasureMatch = (matchRule & SearchPattern.R_ERASURE_MATCH) != 0;
	this.isEquivalentMatch = (matchRule & SearchPattern.R_EQUIVALENT_MATCH) != 0;
	this.matchMode = matchRule & JavaSearchPattern.MATCH_MODE_MASK;
	this.mustResolve = pattern.mustResolve;
}
/*
 * Clear caches
 */
protected void clear() {
	// nothing to clear by default
}
/* (non-Javadoc)
 * Modify PatternLocator.qualifiedPattern behavior:
 * do not add star before simple name pattern when qualification pattern is null.
 * This avoid to match p.X when pattern is only X...
 */
protected char[] getQualifiedPattern(char[] simpleNamePattern, char[] qualificationPattern) {
	// NOTE: if case insensitive search then simpleNamePattern & qualificationPattern are assumed to be lowercase
	if (simpleNamePattern == null) {
		if (qualificationPattern == null) return null;
		return CharOperation.concat(qualificationPattern, ONE_STAR, '.');
	} else if (qualificationPattern == null) {
		return simpleNamePattern;
	} else {
		return CharOperation.concat(qualificationPattern, simpleNamePattern, '.');
	}
}
/* (non-Javadoc)
 * Modify PatternLocator.qualifiedSourceName behavior:
 * also concatenate enclosing type name when type is a only a member type.
 */
protected char[] getQualifiedSourceName(TypeBinding binding) {
	TypeBinding type = binding instanceof ArrayBinding ? ((ArrayBinding)binding).leafComponentType : binding;
	if (type instanceof ReferenceBinding) {
		if (type.isLocalType()) {
			return CharOperation.concat(qualifiedSourceName(type.enclosingType()), new char[] {'.', '1', '.'}, binding.sourceName());
		} else if (type.isMemberType()) {
			return CharOperation.concat(qualifiedSourceName(type.enclosingType()), binding.sourceName(), '.');
		}
	}
	return binding != null ? binding.qualifiedSourceName() : null;
}
/*
 * Get binding of type argument from a class unit scope and its index position.
 * Cache is lazy initialized and if no binding is found, then store a problem binding
 * to avoid making research twice...
 */
protected TypeBinding getTypeNameBinding(int index) {
	return null;
}
/**
 * Initializes this search pattern so that polymorphic search can be performed.
 */
public void initializePolymorphicSearch(MatchLocator locator) {
	// default is to do nothing
}
public int match(Annotation node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
/**
 * Check if the given ast node syntactically matches this pattern.
 * If it does, add it to the match set.
 * Returns the match level.
 */
public int match(ASTNode node, MatchingNodeSet nodeSet) { // needed for some generic nodes
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(Expression node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(FieldDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(LambdaExpression node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(LocalDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(MethodDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(MemberValuePair node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(MessageSend node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
protected int match(ModuleDeclaration node, MatchingNodeSet nodeSet) {
	return IMPOSSIBLE_MATCH;
}
protected int match(ModuleReference node, MatchingNodeSet nodeSet) {
	return IMPOSSIBLE_MATCH;
}
public int match(Reference node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(ReferenceExpression node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(TypeDeclaration node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(TypeParameter node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
public int match(TypeReference node, MatchingNodeSet nodeSet) {
	// each subtype should override if needed
	return IMPOSSIBLE_MATCH;
}
/**
 * Returns the type(s) of container for this pattern.
 * It is a bit combination of types, denoting compilation unit, class declarations, field declarations or method declarations.
 */
protected int matchContainer() {
	// override if the pattern can be more specific
	return ALL_CONTAINER;
}
protected int fineGrain() {
	return 0;
}
/**
 * Returns whether the given name matches the given pattern.
 */
protected boolean matchesName(char[] pattern, char[] name) {
	if (pattern == null) return true; // null is as if it was "*"
	if (name == null) return false; // cannot match null name
	return matchNameValue(pattern, name) != IMPOSSIBLE_MATCH;
}
/**
 * Return how the given name matches the given pattern.
 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=79866"
 *
 * @param pattern
 * @param name
 * @return Possible values are:
 * <ul>
 * 	<li> {@link #ACCURATE_MATCH}</li>
 * 	<li> {@link #IMPOSSIBLE_MATCH}</li>
 * 	<li> {@link #POSSIBLE_MATCH} which may be flavored with following values:
 * 		<ul>
 * 		<li>{@link #EXACT_FLAVOR}: Given name is equals to pattern</li>
 * 		<li>{@link #PREFIX_FLAVOR}: Given name prefix equals to pattern</li>
 * 		<li>{@link #CAMELCASE_FLAVOR}: Given name matches pattern as Camel Case</li>
 * 		<li>{@link #PATTERN_FLAVOR}: Given name matches pattern as Pattern (i.e. using '*' and '?' characters)</li>
 * 		</ul>
 * 	</li>
 * </ul>
 */
protected int matchNameValue(char[] pattern, char[] name) {
	if (pattern == null) return ACCURATE_MATCH; // null is as if it was "*"
	if (name == null) return IMPOSSIBLE_MATCH; // cannot match null name
	if (name.length == 0) { // empty name
		if (pattern.length == 0) { // can only matches empty pattern
			return ACCURATE_MATCH;
		}
		return IMPOSSIBLE_MATCH;
	} else if (pattern.length == 0) {
		return IMPOSSIBLE_MATCH; // need to have both name and pattern length==0 to be accurate
	}
	boolean matchFirstChar = !this.isCaseSensitive || pattern[0] == name[0];
	boolean sameLength = pattern.length == name.length;
	boolean canBePrefix = name.length >= pattern.length;
	switch (this.matchMode) {
		case SearchPattern.R_EXACT_MATCH:
			if (sameLength && matchFirstChar && CharOperation.equals(pattern, name, this.isCaseSensitive)) {
				return POSSIBLE_MATCH | EXACT_FLAVOR;
			}
			break;

		case SearchPattern.R_PREFIX_MATCH:
			if (canBePrefix && matchFirstChar && CharOperation.prefixEquals(pattern, name, this.isCaseSensitive)) {
				return POSSIBLE_MATCH;
			}
			break;

		case SearchPattern.R_PATTERN_MATCH:
			// TODO_PERFS (frederic) Not sure this lowercase is necessary
			if (!this.isCaseSensitive) {
				pattern = CharOperation.toLowerCase(pattern);
			}
			if (CharOperation.match(pattern, name, this.isCaseSensitive)) {
				return POSSIBLE_MATCH;
			}
			break;

		case SearchPattern.R_REGEXP_MATCH :
			if (Pattern.matches(new String(pattern), new String(name))) {
				return POSSIBLE_MATCH;
			}
			break;

		case SearchPattern.R_CAMELCASE_MATCH:
			if (CharOperation.camelCaseMatch(pattern, name, false)) {
				return POSSIBLE_MATCH;
			}
			// only test case insensitive as CamelCase same part count already verified prefix case sensitive
			if (!this.isCaseSensitive && CharOperation.prefixEquals(pattern, name, false)) {
				return POSSIBLE_MATCH;
			}
			break;

		case SearchPattern.R_CAMELCASE_SAME_PART_COUNT_MATCH:
			if (CharOperation.camelCaseMatch(pattern, name, true)) {
				return POSSIBLE_MATCH;
			}
			break;
	}
	return IMPOSSIBLE_MATCH;
}
/**
 * Returns whether the given type reference matches the given pattern.
 */
protected boolean matchesTypeReference(char[] pattern, TypeReference type) {
	if (pattern == null) return true; // null is as if it was "*"
	if (type == null) return true; // treat as an inexact match

	char[][] compoundName = type.getTypeName();
	char[] simpleName = compoundName[compoundName.length - 1];
	int dimensions = type.dimensions() * 2;
	if (dimensions > 0) {
		int length = simpleName.length;
		char[] result = new char[length + dimensions];
		System.arraycopy(simpleName, 0, result, 0, length);
		for (int i = length, l = result.length; i < l;) {
			result[i++] = '[';
			result[i++] = ']';
		}
		simpleName = result;
	}

	return matchesName(pattern, simpleName);
}
/**
 * Returns the match level for the given importRef.
 */
protected int matchLevel(ImportReference importRef) {
	// override if interested in import references which are caught by the generic version of match(ASTNode, MatchingNodeSet)
	return IMPOSSIBLE_MATCH;
}
/**
 * Reports the match of the given import reference if the resolveLevel is high enough.
 */
protected void matchLevelAndReportImportRef(ImportReference importRef, Binding binding, MatchLocator locator) throws CoreException {
	int level = resolveLevel(binding);
	if (level >= INACCURATE_MATCH) {
		matchReportImportRef(
			importRef,
			binding,
			locator.createImportHandle(importRef),
			level == ACCURATE_MATCH
				? SearchMatch.A_ACCURATE
				: SearchMatch.A_INACCURATE,
			locator);
	}
}
/**
 * Reports the match of the given import reference.
 */
protected void matchReportImportRef(ImportReference importRef, Binding binding, IJavaElement element, int accuracy, MatchLocator locator) throws CoreException {
	if (locator.encloses(element)) {
		// default is to report a match as a regular ref.
		this.matchReportReference(importRef, element, null/*no binding*/, accuracy, locator);
	}
}
/**
 * Reports the match of the given reference.
 */
protected void matchReportReference(ASTNode reference, IJavaElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	this.match = null;
	int referenceType = referenceType();
	int offset = reference.sourceStart;
	switch (referenceType) {
		case IJavaElement.PACKAGE_FRAGMENT:
			this.match = locator.newPackageReferenceMatch(element, accuracy, offset, reference.sourceEnd-offset+1, reference);
			break;
		case IJavaElement.TYPE:
			this.match = locator.newTypeReferenceMatch(element, elementBinding, accuracy, offset, reference.sourceEnd-offset+1, reference);
			break;
		case IJavaElement.FIELD:
			this.match = locator.newFieldReferenceMatch(element, null, elementBinding, accuracy, offset, reference.sourceEnd-offset+1, reference);
			break;
		case IJavaElement.LOCAL_VARIABLE:
			this.match = locator.newLocalVariableReferenceMatch(element, accuracy, offset, reference.sourceEnd-offset+1, reference);
			break;
		case IJavaElement.TYPE_PARAMETER:
			this.match = locator.newTypeParameterReferenceMatch(element, accuracy, offset, reference.sourceEnd-offset+1, reference);
			break;
		case IJavaElement.JAVA_MODULE:
			this.match = locator.newModuleReferenceMatch(element, elementBinding, accuracy, offset, reference.sourceEnd-offset+1, reference);
			break;
	}
	if (this.match != null) {
		locator.report(this.match);
	}
}
/**
 * Reports the match of the given reference. Also provide a local element to eventually report in match.
 */
protected void matchReportReference(ASTNode reference, IJavaElement element, IJavaElement localElement, IJavaElement[] otherElements, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	matchReportReference(reference, element, elementBinding, accuracy, locator);
}
public SearchMatch newDeclarationMatch(ASTNode reference, IJavaElement element, Binding elementBinding, int accuracy, int length, MatchLocator locator) {
    return locator.newDeclarationMatch(element, elementBinding, accuracy, reference.sourceStart, length);
}
protected int referenceType() {
	return 0; // defaults to unknown (a generic JavaSearchMatch will be created)
}
/**
 * Finds out whether the given ast node matches this search pattern.
 * Returns IMPOSSIBLE_MATCH if it doesn't.
 * Returns INACCURATE_MATCH if it potentially matches this search pattern (i.e.
 * it has already been resolved but resolving failed.)
 * Returns ACCURATE_MATCH if it matches exactly this search pattern (i.e.
 * it doesn't need to be resolved or it has already been resolved.)
 */
public int resolveLevel(ASTNode possibleMatchingNode) {
	// only called with nodes which were possible matches to the call to matchLevel
	// need to do instance of checks to find out exact type of ASTNode
	return IMPOSSIBLE_MATCH;
}
/**
 * Set the flavors for which the locator has to be focused on.
 * If not set, the locator will accept all matches with or without flavors.
 * When set, the locator will only accept match having the corresponding flavors.
 *
 * @param flavors Bits mask specifying the flavors to be accepted or
 * 	<code>0</code> to ignore the flavors while accepting matches.
 */
void setFlavors(int flavors) {
	this.flavors = flavors;
}
/*
 * Update pattern locator match for parameterized top level types.
 * Set match raw flag and recurse to enclosing types if any...
 */
protected void updateMatch(ParameterizedTypeBinding parameterizedBinding, char[][][] patternTypeArguments, MatchLocator locator) {
	// Only possible if locator has an unit scope.
	if (locator.unitScope != null) {
		updateMatch(parameterizedBinding, patternTypeArguments, false, 0, locator);
	}
}
protected void updateMatch(ParameterizedTypeBinding parameterizedBinding, char[][][] patternTypeArguments, boolean patternHasTypeParameters, int depth, MatchLocator locator) {
	// Only possible if locator has an unit scope.
	if (locator.unitScope == null) return;

	// Set match raw flag
	boolean endPattern = patternTypeArguments==null  ? true  : depth>=patternTypeArguments.length;
	TypeBinding[] argumentsBindings = parameterizedBinding.arguments;
	boolean isRaw = parameterizedBinding.isRawType()|| (argumentsBindings==null && parameterizedBinding.genericType().isGenericType());
	if (isRaw && !this.match.isRaw()) {
		this.match.setRaw(isRaw);
	}

	// Update match
	if (!endPattern && patternTypeArguments != null) {
		// verify if this is a reference to the generic type itself
		if (!isRaw && patternHasTypeParameters && argumentsBindings != null) {
			boolean needUpdate = false;
			TypeVariableBinding[] typeVariables = parameterizedBinding.genericType().typeVariables();
			int length = argumentsBindings.length;
			if (length == typeVariables.length) {
				for (int i=0; i<length; i++) {
					if (TypeBinding.notEquals(argumentsBindings[i], typeVariables[i])) {
						needUpdate = true;
						break;
					}
				}
			}
			if (needUpdate) {
				char[][] patternArguments =  patternTypeArguments[depth];
				updateMatch(argumentsBindings, locator, patternArguments, patternHasTypeParameters);
			}
		} else {
			char[][] patternArguments =  patternTypeArguments[depth];
			updateMatch(argumentsBindings, locator, patternArguments, patternHasTypeParameters);
		}
	}

	// Recurse
	TypeBinding enclosingType = parameterizedBinding.enclosingType();
	if (enclosingType != null && (enclosingType.isParameterizedType() || enclosingType.isRawType())) {
		updateMatch((ParameterizedTypeBinding)enclosingType, patternTypeArguments, patternHasTypeParameters, depth+1, locator);
	}
}
/*
 * Update pattern locator match comparing type arguments with pattern ones.
 * Try to resolve pattern and look for compatibility with type arguments
 * to set match rule.
 */
protected void updateMatch(TypeBinding[] argumentsBinding, MatchLocator locator, char[][] patternArguments, boolean hasTypeParameters) {
	// Only possible if locator has an unit scope.
	if (locator.unitScope == null) return;

	// First compare lengthes
	int patternTypeArgsLength = patternArguments==null ? 0 : patternArguments.length;
	int typeArgumentsLength = argumentsBinding == null ? 0 : argumentsBinding.length;

	// Initialize match rule
	int matchRule = this.match.getRule();
	if (this.match.isRaw()) {
		if (patternTypeArgsLength != 0) {
			matchRule &= ~SearchPattern.R_FULL_MATCH;
		}
	}
	if (hasTypeParameters) {
		matchRule = SearchPattern.R_ERASURE_MATCH;
	}

	// Compare arguments lengthes
	if (patternTypeArgsLength == typeArgumentsLength) {
		if (!this.match.isRaw() && hasTypeParameters) {
			// generic patterns are always not compatible match
			this.match.setRule(SearchPattern.R_ERASURE_MATCH);
			return;
		}
	} else {
		if (patternTypeArgsLength==0) {
			if (!this.match.isRaw() || hasTypeParameters) {
				this.match.setRule(matchRule & ~SearchPattern.R_FULL_MATCH);
			}
		} else  if (typeArgumentsLength==0) {
			// raw binding is always compatible
			this.match.setRule(matchRule & ~SearchPattern.R_FULL_MATCH);
		} else {
			this.match.setRule(0); // impossible match
		}
		return;
	}
	if (argumentsBinding == null || patternArguments == null) {
		this.match.setRule(matchRule);
		return;
	}

	// Compare binding for each type argument only if pattern is not erasure only and at first level
	if (!hasTypeParameters && !this.match.isRaw() && (this.match.isEquivalent() || this.match.isExact())) {
		for (int i=0; i<typeArgumentsLength; i++) {
			// Get parameterized type argument binding
			TypeBinding argumentBinding = argumentsBinding[i];
			if (argumentBinding instanceof CaptureBinding) {
				WildcardBinding capturedWildcard = ((CaptureBinding)argumentBinding).wildcard;
				if (capturedWildcard != null) argumentBinding = capturedWildcard;
			}
			// Get binding for pattern argument
			char[] patternTypeArgument = patternArguments[i];
			char patternWildcard = patternTypeArgument[0];
			char[] patternTypeName = patternTypeArgument;
			int patternWildcardKind = -1;
			switch (patternWildcard) {
				case Signature.C_STAR:
					if (argumentBinding.isWildcard()) {
						WildcardBinding wildcardBinding = (WildcardBinding) argumentBinding;
						if (wildcardBinding.boundKind == Wildcard.UNBOUND) continue;
					}
					matchRule &= ~SearchPattern.R_FULL_MATCH;
					continue; // unbound parameter always match
				case Signature.C_EXTENDS :
					patternWildcardKind = Wildcard.EXTENDS;
					patternTypeName = CharOperation.subarray(patternTypeArgument, 1, patternTypeArgument.length);
					break;
				case Signature.C_SUPER :
					patternWildcardKind = Wildcard.SUPER;
					patternTypeName = CharOperation.subarray(patternTypeArgument, 1, patternTypeArgument.length);
					break;
				default :
					break;
			}
			patternTypeName = Signature.toCharArray(patternTypeName);
			TypeBinding patternBinding = locator.getType(patternTypeArgument, patternTypeName);

			// If have no binding for pattern arg, then we won't be able to refine accuracy
			if (patternBinding == null) {
				if (argumentBinding.isWildcard()) {
					WildcardBinding wildcardBinding = (WildcardBinding) argumentBinding;
					if (wildcardBinding.boundKind == Wildcard.UNBOUND) {
						matchRule &= ~SearchPattern.R_FULL_MATCH;
					} else {
						this.match.setRule(SearchPattern.R_ERASURE_MATCH);
						return;
					}
				}
				continue;
			}

			// Verify the pattern binding is compatible with match type argument binding
			switch (patternWildcard) {
				case Signature.C_STAR : // UNBOUND pattern
					// unbound always match => skip to next argument
					matchRule &= ~SearchPattern.R_FULL_MATCH;
					continue;
				case Signature.C_EXTENDS : // EXTENDS pattern
					if (argumentBinding.isWildcard()) { // argument is a wildcard
						WildcardBinding wildcardBinding = (WildcardBinding) argumentBinding;
						// It's ok if wildcards are identical
						if (wildcardBinding.boundKind == patternWildcardKind && TypeBinding.equalsEquals(wildcardBinding.bound, patternBinding)) {
							continue;
						}
						// Look for wildcard compatibility
						switch (wildcardBinding.boundKind) {
							case Wildcard.EXTENDS:
								if (wildcardBinding.bound== null || wildcardBinding.bound.isCompatibleWith(patternBinding)) {
									// valid when arg extends a subclass of pattern
									matchRule &= ~SearchPattern.R_FULL_MATCH;
									continue;
								}
								break;
							case Wildcard.SUPER:
								break;
							case Wildcard.UNBOUND:
								matchRule &= ~SearchPattern.R_FULL_MATCH;
								continue;
						}
					} else if (argumentBinding.isCompatibleWith(patternBinding)) {
						// valid when arg is a subclass of pattern
						matchRule &= ~SearchPattern.R_FULL_MATCH;
						continue;
					}
					break;
				case Signature.C_SUPER : // SUPER pattern
					if (argumentBinding.isWildcard()) { // argument is a wildcard
						WildcardBinding wildcardBinding = (WildcardBinding) argumentBinding;
						// It's ok if wildcards are identical
						if (wildcardBinding.boundKind == patternWildcardKind && TypeBinding.equalsEquals(wildcardBinding.bound, patternBinding)) {
							continue;
						}
						// Look for wildcard compatibility
						switch (wildcardBinding.boundKind) {
							case Wildcard.EXTENDS:
								break;
							case Wildcard.SUPER:
								if (wildcardBinding.bound== null || patternBinding.isCompatibleWith(wildcardBinding.bound)) {
									// valid only when arg super a superclass of pattern
									matchRule &= ~SearchPattern.R_FULL_MATCH;
									continue;
								}
								break;
							case Wildcard.UNBOUND:
								matchRule &= ~SearchPattern.R_FULL_MATCH;
								continue;
						}
					} else if (patternBinding.isCompatibleWith(argumentBinding)) {
						// valid only when arg is a superclass of pattern
						matchRule &= ~SearchPattern.R_FULL_MATCH;
						continue;
					}
					break;
				default:
					if (argumentBinding.isWildcard()) {
						WildcardBinding wildcardBinding = (WildcardBinding) argumentBinding;
						switch (wildcardBinding.boundKind) {
							case Wildcard.EXTENDS:
								if (wildcardBinding.bound== null || patternBinding.isCompatibleWith(wildcardBinding.bound)) {
									// valid only when arg extends a superclass of pattern
									matchRule &= ~SearchPattern.R_FULL_MATCH;
									continue;
								}
								break;
							case Wildcard.SUPER:
								if (wildcardBinding.bound== null || wildcardBinding.bound.isCompatibleWith(patternBinding)) {
									// valid only when arg super a subclass of pattern
									matchRule &= ~SearchPattern.R_FULL_MATCH;
									continue;
								}
								break;
							case Wildcard.UNBOUND:
								matchRule &= ~SearchPattern.R_FULL_MATCH;
								continue;
						}
					} else if (TypeBinding.equalsEquals(argumentBinding, patternBinding))
						// valid only when arg is equals to pattern
						continue;
					break;
			}

			// Argument does not match => erasure match will be the only possible one
			this.match.setRule(SearchPattern.R_ERASURE_MATCH);
			return;
		}
	}

	// Set match rule
	this.match.setRule(matchRule);
}
/**
 * Finds out whether the given binding matches this search pattern.
 * Returns ACCURATE_MATCH if it does.
 * Returns INACCURATE_MATCH if resolve failed but match is still possible.
 * Returns IMPOSSIBLE_MATCH otherwise.
 * Default is to return INACCURATE_MATCH.
 */
public int resolveLevel(Binding binding) {
	// override if the pattern can match the binding
	return INACCURATE_MATCH;
}
/**
 * Returns whether the given type binding matches the given simple name pattern
 * and qualification pattern.
 * Note that from since 3.1, this method resolve to accurate member or local types
 * even if they are not fully qualified (i.e. X.Member instead of p.X.Member).
 * Returns ACCURATE_MATCH if it does.
 * Returns INACCURATE_MATCH if resolve failed.
 * Returns IMPOSSIBLE_MATCH if it doesn't.
 */
protected int resolveLevelForType(char[] simpleNamePattern, char[] qualificationPattern, TypeBinding binding) {
//	return resolveLevelForType(qualifiedPattern(simpleNamePattern, qualificationPattern), type);
	char[] qualifiedPattern = getQualifiedPattern(simpleNamePattern, qualificationPattern);
	int level = resolveLevelForType(qualifiedPattern, binding);
	if (level == ACCURATE_MATCH || binding == null  || !binding.isValidBinding()) return level;
	TypeBinding type = binding instanceof ArrayBinding ? ((ArrayBinding)binding).leafComponentType : binding;
	char[] sourceName = null;
	if (type.isMemberType() || type.isLocalType()) {
		if (qualificationPattern != null) {
			sourceName =  getQualifiedSourceName(binding);
		} else {
			sourceName =  binding.sourceName();
		}
	} else if (qualificationPattern == null) {
		sourceName =  getQualifiedSourceName(binding);
	}
	if (sourceName == null) return IMPOSSIBLE_MATCH;
	switch (this.matchMode) {
		case SearchPattern.R_PREFIX_MATCH:
			if (CharOperation.prefixEquals(qualifiedPattern, sourceName, this.isCaseSensitive)) {
				return ACCURATE_MATCH;
			}
			break;
		case SearchPattern.R_CAMELCASE_MATCH:
			if ((qualifiedPattern.length>0 && sourceName.length>0 && qualifiedPattern[0] == sourceName[0])) {
				if (CharOperation.camelCaseMatch(qualifiedPattern, sourceName, false)) {
					return ACCURATE_MATCH;
				}
				if (!this.isCaseSensitive && CharOperation.prefixEquals(qualifiedPattern, sourceName, false)) {
					return ACCURATE_MATCH;
				}
			}
			break;
		case SearchPattern.R_CAMELCASE_SAME_PART_COUNT_MATCH:
			if ((qualifiedPattern.length>0 && sourceName.length>0 && qualifiedPattern[0] == sourceName[0])) {
				if (CharOperation.camelCaseMatch(qualifiedPattern, sourceName, true)) {
					return ACCURATE_MATCH;
				}
			}
			break;
		default:
			if (CharOperation.match(qualifiedPattern, sourceName, this.isCaseSensitive)) {
				return ACCURATE_MATCH;
			}
	}
	return IMPOSSIBLE_MATCH;
}

/**
 * Returns whether the given type binding matches the given qualified pattern.
 * Returns ACCURATE_MATCH if it does.
 * Returns INACCURATE_MATCH if resolve failed.
 * Returns IMPOSSIBLE_MATCH if it doesn't.
 */
protected int resolveLevelForType(char[] qualifiedPattern, TypeBinding type) {
	if (qualifiedPattern == null) return ACCURATE_MATCH;
	if (type == null || !type.isValidBinding()) return INACCURATE_MATCH;

	// Type variable cannot be specified through pattern => this kind of binding cannot match it (see bug 79803)
	if (type.isTypeVariable()) return IMPOSSIBLE_MATCH;

	if (type instanceof IntersectionTypeBinding18) {
		int result = IMPOSSIBLE_MATCH, prev = IMPOSSIBLE_MATCH;
		IntersectionTypeBinding18 i18 = (IntersectionTypeBinding18) type;
		for (ReferenceBinding ref : i18.intersectingTypes) {
			result = resolveLevelForType(qualifiedPattern, ref);
			if (result == ACCURATE_MATCH) return result;
			if (result == IMPOSSIBLE_MATCH) continue;
			if (prev == IMPOSSIBLE_MATCH) prev = result;
		}
		return prev;
	}
	// NOTE: if case insensitive search then qualifiedPattern is assumed to be lowercase

	char[] qualifiedPackageName = type.qualifiedPackageName();
	char[] qualifiedSourceName = qualifiedSourceName(type);
	char[] fullyQualifiedTypeName = qualifiedPackageName.length == 0
		? qualifiedSourceName
		: CharOperation.concat(qualifiedPackageName, qualifiedSourceName, '.');
	return CharOperation.match(qualifiedPattern, fullyQualifiedTypeName, this.isCaseSensitive)
		? ACCURATE_MATCH
		: IMPOSSIBLE_MATCH;
}
/* (non-Javadoc)
 * Resolve level for type with a given binding with all pattern information.
 */
protected int resolveLevelForType (char[] simpleNamePattern,
									char[] qualificationPattern,
									char[][][] patternTypeArguments,
									int depth,
									TypeBinding type) {
	// standard search with no generic additional information must succeed
	int level = resolveLevelForType(simpleNamePattern, qualificationPattern, type);
	if (level == IMPOSSIBLE_MATCH) return IMPOSSIBLE_MATCH;
	if (type == null || patternTypeArguments == null || patternTypeArguments.length == 0 || depth >= patternTypeArguments.length) {
		return level;
	}

	// if pattern is erasure match (see bug 79790), commute impossible to erasure
	int impossible = this.isErasureMatch ? ERASURE_MATCH : IMPOSSIBLE_MATCH;

	// pattern has type parameter(s) or type argument(s)
	if (type.isGenericType()) {
		// Binding is generic, get its type variable(s)
		TypeVariableBinding[] typeVariables = null;
		if (type instanceof SourceTypeBinding) {
			SourceTypeBinding sourceTypeBinding = (SourceTypeBinding) type;
			typeVariables = sourceTypeBinding.typeVariables;
		} else if (type instanceof BinaryTypeBinding) {
			BinaryTypeBinding binaryTypeBinding = (BinaryTypeBinding) type;
			if (this.mustResolve)
				typeVariables = binaryTypeBinding.typeVariables(); // TODO (frederic) verify performance
		}
		if (patternTypeArguments[depth] != null && patternTypeArguments[depth].length > 0 &&
			typeVariables != null && typeVariables.length > 0) {
			if (typeVariables.length != patternTypeArguments[depth].length) return IMPOSSIBLE_MATCH;
		}
		// TODO (frederic) do we need to verify each parameter?
		return level; // we can't do better
	}

	// raw type always match
	if (type.isRawType()) {
		return level;
	}

	// Standard types (i.e. neither generic nor parameterized nor raw types)
	// cannot match pattern with type parameters or arguments
	TypeBinding leafType = type.leafComponentType();
	if (!leafType.isParameterizedType()) {
		return (patternTypeArguments[depth]==null || patternTypeArguments[depth].length==0) ? level : IMPOSSIBLE_MATCH;
	}

	// Parameterized type
	ParameterizedTypeBinding paramTypeBinding = (ParameterizedTypeBinding) leafType;

	// Compare arguments only if there ones on both sides
	if (patternTypeArguments[depth] != null && patternTypeArguments[depth].length > 0 &&
		paramTypeBinding.arguments != null && paramTypeBinding.arguments.length > 0) {

		// type parameters length must match at least specified type names length
		int length = patternTypeArguments[depth].length;
		if (paramTypeBinding.arguments.length != length) return IMPOSSIBLE_MATCH;

		// verify each pattern type parameter
		nextTypeArgument: for (int i= 0; i<length; i++) {
			char[] patternTypeArgument = patternTypeArguments[depth][i];
			TypeBinding argTypeBinding = paramTypeBinding.arguments[i];
			// get corresponding pattern wildcard
			switch (patternTypeArgument[0]) {
				case Signature.C_STAR : // unbound parameter always match
				case Signature.C_SUPER : // needs pattern type parameter binding
					// skip to next type argument as it will be resolved later
					continue nextTypeArgument;
				case Signature.C_EXTENDS :
					// remove wildcard from patter type argument
					patternTypeArgument = CharOperation.subarray(patternTypeArgument, 1, patternTypeArgument.length);
					break;
				default :
					// no wildcard
					break;
			}
			// get pattern type argument from its signature
			patternTypeArgument = Signature.toCharArray(patternTypeArgument);
			if (!this.isCaseSensitive) patternTypeArgument = CharOperation.toLowerCase(patternTypeArgument);
			boolean patternTypeArgHasAnyChars = CharOperation.contains(new char[] {'*', '?'}, patternTypeArgument);

			// Verify that names match...
			// ...special case for wildcard
			if (argTypeBinding instanceof CaptureBinding) {
				WildcardBinding capturedWildcard = ((CaptureBinding)argTypeBinding).wildcard;
				if (capturedWildcard != null) argTypeBinding = capturedWildcard;
			}
			if (argTypeBinding.isWildcard()) {
				WildcardBinding wildcardBinding = (WildcardBinding) argTypeBinding;
				switch (wildcardBinding.boundKind) {
					case Wildcard.EXTENDS:
						// Invalid if type argument is not exact
						if (patternTypeArgHasAnyChars) return impossible;
						continue nextTypeArgument;
					case Wildcard.UNBOUND:
						// there's no bound name to match => valid
						continue nextTypeArgument;
				}
				// Look if bound name match pattern type argument
				ReferenceBinding boundBinding = (ReferenceBinding) wildcardBinding.bound;
				if (CharOperation.match(patternTypeArgument, boundBinding.shortReadableName(), this.isCaseSensitive) ||
					CharOperation.match(patternTypeArgument, boundBinding.readableName(), this.isCaseSensitive)) {
					// found name in hierarchy => match
					continue nextTypeArgument;
				}

				// If pattern is not exact then match fails
				if (patternTypeArgHasAnyChars) return impossible;

				// Look for bound name in type argument superclasses
				boundBinding = boundBinding.superclass();
				while (boundBinding != null) {
					if (CharOperation.equals(patternTypeArgument, boundBinding.shortReadableName(), this.isCaseSensitive) ||
						CharOperation.equals(patternTypeArgument, boundBinding.readableName(), this.isCaseSensitive)) {
						// found name in hierarchy => match
						continue nextTypeArgument;
					} else if (boundBinding.isLocalType() || boundBinding.isMemberType()) {
						// for local or member type, verify also source name (bug 81084)
						if (CharOperation.match(patternTypeArgument, boundBinding.sourceName(), this.isCaseSensitive))
							continue nextTypeArgument;
					}
					boundBinding = boundBinding.superclass();
				}
				return impossible;
			}

			// See if names match
			if (CharOperation.match(patternTypeArgument, argTypeBinding.shortReadableName(), this.isCaseSensitive) ||
				CharOperation.match(patternTypeArgument, argTypeBinding.readableName(), this.isCaseSensitive)) {
				continue nextTypeArgument;
			} else if (argTypeBinding.isLocalType() || argTypeBinding.isMemberType()) {
				// for local or member type, verify also source name (bug 81084)
				if (CharOperation.match(patternTypeArgument, argTypeBinding.sourceName(), this.isCaseSensitive))
					continue nextTypeArgument;
			}

			// If pattern is not exact then match fails
			if (patternTypeArgHasAnyChars) return impossible;

			// Scan hierarchy
			TypeBinding leafTypeBinding = argTypeBinding.leafComponentType();
			if (leafTypeBinding.isBaseType()) return impossible;
			ReferenceBinding refBinding = ((ReferenceBinding) leafTypeBinding).superclass();
			while (refBinding != null) {
				if (CharOperation.equals(patternTypeArgument, refBinding.shortReadableName(), this.isCaseSensitive) ||
					CharOperation.equals(patternTypeArgument, refBinding.readableName(), this.isCaseSensitive)) {
					// found name in hierarchy => match
					continue nextTypeArgument;
				} else if (refBinding.isLocalType() || refBinding.isMemberType()) {
					// for local or member type, verify also source name (bug 81084)
					if (CharOperation.match(patternTypeArgument, refBinding.sourceName(), this.isCaseSensitive))
						continue nextTypeArgument;
				}
				refBinding = refBinding.superclass();
			}
			return impossible;
		}
	}

	// Recurse on enclosing type
	TypeBinding enclosingType = paramTypeBinding.enclosingType();
	if (enclosingType != null && enclosingType.isParameterizedType() && depth < patternTypeArguments.length && qualificationPattern != null) {
		int lastDot = CharOperation.lastIndexOf('.', qualificationPattern);
		char[] enclosingQualificationPattern = lastDot==-1 ? null : CharOperation.subarray(qualificationPattern, 0, lastDot);
		char[] enclosingSimpleNamePattern = lastDot==-1 ? qualificationPattern : CharOperation.subarray(qualificationPattern, lastDot+1, qualificationPattern.length);
		int enclosingLevel = resolveLevelForType(enclosingSimpleNamePattern, enclosingQualificationPattern, patternTypeArguments, depth+1, enclosingType);
		if (enclosingLevel == impossible) return impossible;
		if (enclosingLevel == IMPOSSIBLE_MATCH) return IMPOSSIBLE_MATCH;
	}
	return level;
}
@Override
public String toString(){
	return "SearchPattern"; //$NON-NLS-1$
}
@Override
public void recordResolution(QualifiedTypeReference typeReference, TypeBinding resolution) {
	// noop by default
}
}
