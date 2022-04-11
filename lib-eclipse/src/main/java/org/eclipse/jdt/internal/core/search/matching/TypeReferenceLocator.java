/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;
import org.eclipse.jdt.internal.core.JavaElement;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TypeReferenceLocator extends PatternLocator {

protected TypeReferencePattern pattern;
protected boolean isDeclarationOfReferencedTypesPattern;

private final int fineGrain;

public TypeReferenceLocator(TypeReferencePattern pattern) {

	super(pattern);

	this.pattern = pattern;
	this.fineGrain = pattern == null ? 0 : pattern.fineGrain;
	this.isDeclarationOfReferencedTypesPattern = this.pattern instanceof DeclarationOfReferencedTypesPattern;
}
protected IJavaElement findElement(IJavaElement element, int accuracy) {
	// need exact match to be able to open on type ref
	if (accuracy != SearchMatch.A_ACCURATE) return null;

	// element that references the type must be included in the enclosing element
	DeclarationOfReferencedTypesPattern declPattern = (DeclarationOfReferencedTypesPattern) this.pattern;
	while (element != null && !declPattern.enclosingElement.equals(element))
		element = element.getParent();
	return element;
}
@Override
protected int fineGrain() {
	return this.fineGrain;
}
@Override
public int match(Annotation node, MatchingNodeSet nodeSet) {
	return match(node.type, nodeSet);
}
@Override
public int match(ASTNode node, MatchingNodeSet nodeSet) { // interested in ImportReference
	if (!(node instanceof ImportReference)) return IMPOSSIBLE_MATCH;

	return nodeSet.addMatch(node, matchLevel((ImportReference) node));
}
//public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(Expression node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(FieldDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(MethodDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(MessageSend node, MatchingNodeSet nodeSet) - SKIP IT
@Override
public int match(Reference node, MatchingNodeSet nodeSet) { // interested in NameReference & its subtypes
	if (!(node instanceof NameReference)) return IMPOSSIBLE_MATCH;

	if (this.pattern.simpleName == null)
		return nodeSet.addMatch(node, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);

	if (node instanceof SingleNameReference) {
		if (matchesName(this.pattern.simpleName, ((SingleNameReference) node).token))
			return nodeSet.addMatch(node, POSSIBLE_MATCH); // resolution is needed to find out if it is a type ref
	} else {
		char[][] tokens = ((QualifiedNameReference) node).tokens;
		for (int i = 0, max = tokens.length; i < max; i++)
			if (matchesName(this.pattern.simpleName, tokens[i]))
				return nodeSet.addMatch(node, POSSIBLE_MATCH); // resolution is needed to find out if it is a type ref
	}

	return IMPOSSIBLE_MATCH;
}
//public int match(TypeDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
@Override
public int match(TypeReference node, MatchingNodeSet nodeSet) {
	if (this.pattern.simpleName == null)
		return nodeSet.addMatch(node, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);

	if (node instanceof SingleTypeReference) {
		if (matchesName(this.pattern.simpleName, ((SingleTypeReference) node).token))
			return nodeSet.addMatch(node, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
	} else {
		char[][] tokens = ((QualifiedTypeReference) node).tokens;
		for (int i = 0, max = tokens.length; i < max; i++)
			if (matchesName(this.pattern.simpleName, tokens[i]))
				return nodeSet.addMatch(node, POSSIBLE_MATCH); // resolution is needed to find out if it is a type ref
	}

	return IMPOSSIBLE_MATCH;
}

@Override
protected int matchLevel(ImportReference importRef) {
	if (this.pattern.qualification == null) {
		if (this.pattern.simpleName == null) return ACCURATE_MATCH;
		char[][] tokens = importRef.tokens;
		boolean onDemand = (importRef.bits & ASTNode.OnDemand) != 0;
		final boolean isStatic = importRef.isStatic();
		if (!isStatic && onDemand) {
			return IMPOSSIBLE_MATCH;
		}
		int length = tokens.length;
		if (matchesName(this.pattern.simpleName, tokens[length-1])) {
			return ACCURATE_MATCH;
		}
		if (isStatic && !onDemand && length > 1) {
			if (matchesName(this.pattern.simpleName, tokens[length-2])) {
				return ACCURATE_MATCH;
			}
		}
	} else {
		char[][] tokens = importRef.tokens;
		char[] qualifiedPattern = this.pattern.simpleName == null
			? this.pattern.qualification
			: CharOperation.concat(this.pattern.qualification, this.pattern.simpleName, '.');
		char[] qualifiedTypeName = CharOperation.concatWith(tokens, '.');
		if (qualifiedPattern == null) return ACCURATE_MATCH; // null is as if it was "*"
		if (qualifiedTypeName == null) return IMPOSSIBLE_MATCH; // cannot match null name
		if (qualifiedTypeName.length == 0) { // empty name
			if (qualifiedPattern.length == 0) { // can only matches empty pattern
				return ACCURATE_MATCH;
			}
			return IMPOSSIBLE_MATCH;
		}
		boolean matchFirstChar = !this.isCaseSensitive || (qualifiedPattern[0] == qualifiedTypeName[0]);
		switch (this.matchMode) {
			case SearchPattern.R_EXACT_MATCH:
			case SearchPattern.R_PREFIX_MATCH:
				if (CharOperation.prefixEquals(qualifiedPattern, qualifiedTypeName, this.isCaseSensitive)) {
					return POSSIBLE_MATCH;
				}
				break;

			case SearchPattern.R_PATTERN_MATCH:
				if (CharOperation.match(qualifiedPattern, qualifiedTypeName, this.isCaseSensitive)) {
					return POSSIBLE_MATCH;
				}
				break;

			case SearchPattern.R_REGEXP_MATCH :
				// TODO (frederic) implement regular expression match
				break;
			case SearchPattern.R_CAMELCASE_MATCH:
				if (matchFirstChar && CharOperation.camelCaseMatch(qualifiedPattern, qualifiedTypeName, false)) {
					return POSSIBLE_MATCH;
				}
				// only test case insensitive as CamelCase already verified prefix case sensitive
				if (!this.isCaseSensitive && CharOperation.prefixEquals(qualifiedPattern, qualifiedTypeName, false)) {
					return POSSIBLE_MATCH;
				}
				break;
			case SearchPattern.R_CAMELCASE_SAME_PART_COUNT_MATCH:
				if (matchFirstChar && CharOperation.camelCaseMatch(qualifiedPattern, qualifiedTypeName, true)) {
					return POSSIBLE_MATCH;
				}
				break;
		}
	}
	return IMPOSSIBLE_MATCH;
}

@Override
protected void matchLevelAndReportImportRef(ImportReference importRef, Binding binding, MatchLocator locator) throws CoreException {
	Binding refBinding = binding;
	if (importRef.isStatic()) {
		// for static import, binding can be a field binding or a member type binding
		// verify that in this case binding is static and use declaring class for fields
		if (binding instanceof FieldBinding) {
			FieldBinding fieldBinding = (FieldBinding) binding;
			if (!fieldBinding.isStatic()) return;
			refBinding = fieldBinding.declaringClass;
		} else if (binding instanceof MethodBinding) {
			MethodBinding methodBinding = (MethodBinding) binding;
			if (!methodBinding.isStatic()) return;
			refBinding = methodBinding.declaringClass;
		} else if (binding instanceof MemberTypeBinding) {
			MemberTypeBinding memberBinding = (MemberTypeBinding) binding;
			if (!memberBinding.isStatic()) return;
		}
		// resolve and report
		int level = resolveLevel(refBinding);
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
		return;
	}
	super.matchLevelAndReportImportRef(importRef, refBinding, locator);
}
@Override
protected void matchReportImportRef(ImportReference importRef, Binding binding, IJavaElement element, int accuracy, MatchLocator locator) throws CoreException {
	if (this.isDeclarationOfReferencedTypesPattern) {
		if ((element = findElement(element, accuracy)) != null) {
			SimpleSet knownTypes = ((DeclarationOfReferencedTypesPattern) this.pattern).knownTypes;
			while (binding instanceof ReferenceBinding) {
				ReferenceBinding typeBinding = (ReferenceBinding) binding;
				reportDeclaration(typeBinding, 1, locator, knownTypes);
				binding = typeBinding.enclosingType();
			}
		}
		return;
	}

	// return if this is not necessary to report
	if (this.pattern.hasTypeArguments() && !this.isEquivalentMatch &&!this.isErasureMatch) {
		return;
	}

	// Return if fine grain is on and does not concern import reference
	if ((this.pattern.fineGrain != 0 && (this.pattern.fineGrain & IJavaSearchConstants.IMPORT_DECLARATION_TYPE_REFERENCE) == 0)) {
		return;
	}

	// Create search match
	this.match = locator.newTypeReferenceMatch(element, binding, accuracy, importRef);

	// set match raw flag and rule
	this.match.setRaw(true);
	if (this.pattern.hasTypeArguments()) {
		// binding is raw => only compatible erasure if pattern has type arguments
		this.match.setRule(this.match.getRule() & (~SearchPattern.R_FULL_MATCH));
	}

	// Try to find best selection for match
	TypeBinding typeBinding = null;
	boolean lastButOne = false;
	if (binding instanceof ReferenceBinding) {
		typeBinding = (ReferenceBinding) binding;
	} else if (binding instanceof FieldBinding) { // may happen for static import
		typeBinding = ((FieldBinding)binding).declaringClass;
		lastButOne = importRef.isStatic() && ((importRef.bits & ASTNode.OnDemand) == 0);
	} else if (binding instanceof MethodBinding) { // may happen for static import
		typeBinding = ((MethodBinding)binding).declaringClass;
		lastButOne = importRef.isStatic() && ((importRef.bits & ASTNode.OnDemand) == 0);
	}
	if (typeBinding != null) {
		int lastIndex = importRef.tokens.length - 1;
		if (lastButOne) {
			// for field or method static import, use last but one token
			lastIndex--;
		}
		if (typeBinding instanceof ProblemReferenceBinding) {
			ProblemReferenceBinding pbBinding = (ProblemReferenceBinding) typeBinding;
			typeBinding = pbBinding.closestMatch();
			lastIndex = pbBinding.compoundName.length - 1;
		}
		// try to match all enclosing types for which the token matches as well.
		while (typeBinding != null && lastIndex >= 0) {
			if (resolveLevelForType(typeBinding) != IMPOSSIBLE_MATCH) {
				if (locator.encloses(element)) {
					long[] positions = importRef.sourcePositions;
					// index now depends on pattern type signature
					int index = lastIndex;
					if (this.pattern.qualification != null) {
						index = lastIndex - this.pattern.segmentsSize;
					}
					if (index < 0) index = 0;
					int start = (int) ((positions[index]) >>> 32);
					int end = (int) positions[lastIndex];
					// report match
					this.match.setOffset(start);
					this.match.setLength(end-start+1);
					locator.report(this.match);
				}
				return;
			}
			lastIndex--;
			typeBinding = typeBinding.enclosingType();
		}
	}
	locator.reportAccurateTypeReference(this.match, importRef, this.pattern.simpleName);
}
protected void matchReportReference(ArrayTypeReference arrayRef, IJavaElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	if (this.pattern.simpleName == null) {
		// TODO (frederic) need to add a test for this case while searching generic types...
		if (locator.encloses(element)) {
			int offset = arrayRef.sourceStart;
			int length = arrayRef.sourceEnd-offset+1;
			if (this.match == null) {
				this.match = locator.newTypeReferenceMatch(element, elementBinding, accuracy, offset, length, arrayRef);
			} else {
				this.match.setOffset(offset);
				this.match.setLength(length);
			}
			locator.report(this.match);
			return;
		}
	}
	this.match = locator.newTypeReferenceMatch(element, elementBinding, accuracy, arrayRef);
	if (arrayRef.resolvedType != null) {
		matchReportReference(arrayRef, -1, arrayRef.resolvedType.leafComponentType(), locator);
		return;
	}
	locator.reportAccurateTypeReference(this.match, arrayRef, this.pattern.simpleName);
}
/**
 * Reports the match of the given reference.
 */
@Override
protected void matchReportReference(ASTNode reference, IJavaElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	matchReportReference(reference, element, null, null, elementBinding, accuracy, locator);
}
/**
 * Reports the match of the given reference. Also provide a local and other elements to eventually report in match.
 */
@Override
protected void matchReportReference(ASTNode reference, IJavaElement element, IJavaElement localElement, IJavaElement[] otherElements, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	if (this.isDeclarationOfReferencedTypesPattern) {
		if ((element = findElement(element, accuracy)) != null)
			reportDeclaration(reference, element, locator, ((DeclarationOfReferencedTypesPattern) this.pattern).knownTypes);
		return;
	}

	// Create search match
	TypeReferenceMatch refMatch = locator.newTypeReferenceMatch(element, elementBinding, accuracy, reference);
	refMatch.setLocalElement(localElement);
	refMatch.setOtherElements(otherElements);
	this.match = refMatch;

	// Report match depending on reference type
	if (reference instanceof QualifiedNameReference)
		matchReportReference((QualifiedNameReference) reference, element, elementBinding, accuracy, locator);
	else if (reference instanceof QualifiedTypeReference)
		matchReportReference((QualifiedTypeReference) reference, element, elementBinding, accuracy, locator);
	else if (reference instanceof ArrayTypeReference)
		matchReportReference((ArrayTypeReference) reference, element, elementBinding, accuracy, locator);
	else {
		TypeBinding typeBinding = reference instanceof Expression  &&
				((org.eclipse.jdt.internal.compiler.ast.Expression) reference).isTrulyExpression() ?
						((Expression)reference).resolvedType : null;
		if (typeBinding != null) {
			matchReportReference((Expression)reference, -1, typeBinding, locator);
			return;
		}
		locator.report(this.match);
	}
}
protected void matchReportReference(QualifiedNameReference qNameRef, IJavaElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	Binding binding = qNameRef.binding;
	TypeBinding typeBinding = null;
	int lastIndex = qNameRef.tokens.length - 1;
	switch (qNameRef.bits & ASTNode.RestrictiveFlagMASK) {
		case Binding.FIELD : // reading a field
			typeBinding = qNameRef.actualReceiverType;
			lastIndex -= qNameRef.otherBindings == null ? 1 : qNameRef.otherBindings.length + 1;
			break;
		case Binding.TYPE : //=============only type ==============
			if (binding instanceof TypeBinding)
				typeBinding = (TypeBinding) binding;
			break;
		case Binding.VARIABLE : //============unbound cases===========
		case Binding.TYPE | Binding.VARIABLE :
			if (binding instanceof ProblemReferenceBinding) {
				typeBinding = (TypeBinding) binding;
			} else if (binding instanceof ProblemFieldBinding) {
				typeBinding = qNameRef.actualReceiverType;
				lastIndex -= qNameRef.otherBindings == null ? 1 : qNameRef.otherBindings.length + 1;
			} else if (binding instanceof ProblemBinding) {
				typeBinding = ((ProblemBinding) binding).searchType;
			}
			break;
	}
	if (typeBinding instanceof ProblemReferenceBinding) {
		ProblemReferenceBinding pbBinding = (ProblemReferenceBinding) typeBinding;
		typeBinding = pbBinding.closestMatch();
		lastIndex = pbBinding.compoundName.length - 1;
	}

	// Create search match to report
	if (this.match == null) {
		this.match = locator.newTypeReferenceMatch(element, elementBinding, accuracy, qNameRef);
	}

	// try to match all enclosing types for which the token matches as well.
	if (typeBinding instanceof ReferenceBinding) {
		ReferenceBinding refBinding = (ReferenceBinding) typeBinding;
		while (refBinding != null && lastIndex >= 0) {
			if (resolveLevelForType(refBinding) == ACCURATE_MATCH) {
				if (locator.encloses(element)) {
					long[] positions = qNameRef.sourcePositions;
					// index now depends on pattern type signature
					int index = lastIndex;
					if (this.pattern.qualification != null) {
						index = lastIndex - this.pattern.segmentsSize;
					}
					if (index < 0) index = 0;
					int start = (int) ((positions[index]) >>> 32);
					int end = (int) positions[lastIndex];
					this.match.setOffset(start);
					this.match.setLength(end-start+1);

					//  Look if there's a need to special report for parameterized type
					matchReportReference(qNameRef, lastIndex, refBinding, locator);
				}
				return;
			}
			lastIndex--;
			refBinding = refBinding.enclosingType();
		}
	}
	locator.reportAccurateTypeReference(this.match, qNameRef, this.pattern.simpleName);
}
protected void matchReportReference(QualifiedTypeReference qTypeRef, IJavaElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	TypeBinding typeBinding = qTypeRef.resolvedType;
	int lastIndex = qTypeRef.tokens.length - 1;
	if (typeBinding instanceof ArrayBinding)
		typeBinding = ((ArrayBinding) typeBinding).leafComponentType;
	if (typeBinding instanceof ProblemReferenceBinding) {
		ProblemReferenceBinding pbBinding = (ProblemReferenceBinding) typeBinding;
		typeBinding = pbBinding.closestMatch();
		lastIndex = pbBinding.compoundName.length - 1;
	}

	// Create search match to report
	if (this.match == null) {
		this.match = locator.newTypeReferenceMatch(element, elementBinding, accuracy, qTypeRef);
	}

	// try to match all enclosing types for which the token matches as well
	if (typeBinding instanceof ReferenceBinding) {
		ReferenceBinding refBinding = (ReferenceBinding) typeBinding;
		while (refBinding != null && lastIndex >= 0) {
			if (resolveLevelForType(refBinding) != IMPOSSIBLE_MATCH) {
				if (locator.encloses(element)) {
					long[] positions = qTypeRef.sourcePositions;
					// index now depends on pattern type signature
					int index = lastIndex;
					if (this.pattern.qualification != null) {
						index = lastIndex - this.pattern.segmentsSize;
					}
					if (index < 0) index = 0;
					int start = (int) ((positions[index]) >>> 32);
					int end = (int) positions[lastIndex];
					this.match.setOffset(start);
					this.match.setLength(end-start+1);

					//  Look if there's a need to special report for parameterized type
					matchReportReference(qTypeRef, lastIndex, refBinding, locator);
				}
				return;
			}
			lastIndex--;
			refBinding = refBinding.enclosingType();
		}
	}
	locator.reportAccurateTypeReference(this.match, qTypeRef, this.pattern.simpleName);
}
void matchReportReference(Expression expr, int lastIndex, TypeBinding refBinding, MatchLocator locator) throws CoreException {

	// Look if there's a need to special report for parameterized type
	if (refBinding.isParameterizedType() || refBinding.isRawType()) {

		// Try to refine accuracy
		ParameterizedTypeBinding parameterizedBinding = (ParameterizedTypeBinding)refBinding;
		updateMatch(parameterizedBinding, this.pattern.getTypeArguments(), this.pattern.hasTypeParameters(), 0, locator);

		// See whether it is necessary to report or not
		if (this.match.getRule() == 0) return; // impossible match
		boolean report = (this.isErasureMatch && this.match.isErasure()) || (this.isEquivalentMatch && this.match.isEquivalent()) || this.match.isExact();
		if (!report) return;

		// Make a special report for parameterized types if necessary
		 if (refBinding.isParameterizedType() && this.pattern.hasTypeArguments())  {
			TypeReference typeRef = null;
			TypeReference[] typeArguments = null;
			if (expr instanceof ParameterizedQualifiedTypeReference) {
				typeRef = (ParameterizedQualifiedTypeReference) expr;
				typeArguments = ((ParameterizedQualifiedTypeReference) expr).typeArguments[lastIndex];
			}
			else if (expr instanceof ParameterizedSingleTypeReference) {
				typeRef = (ParameterizedSingleTypeReference) expr;
				typeArguments = ((ParameterizedSingleTypeReference) expr).typeArguments;
			}
			if (typeRef != null) {
				locator.reportAccurateParameterizedTypeReference(this.match, typeRef, lastIndex, typeArguments);
				return;
			}
		}
	} else if (this.pattern.hasTypeArguments()) { // binding has no type params, compatible erasure if pattern does
		this.match.setRule(SearchPattern.R_ERASURE_MATCH);
	}

	// Report match
	if (expr instanceof ArrayTypeReference) {
		locator.reportAccurateTypeReference(this.match, expr, this.pattern.simpleName);
		return;
	}
	if (refBinding.isLocalType()) {
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=82673
		LocalTypeBinding local = (LocalTypeBinding) refBinding.erasure();
		IJavaElement focus = this.pattern.focus;
		if (focus != null && local.enclosingMethod != null && focus.getParent().getElementType() == IJavaElement.METHOD) {
			IMethod method = (IMethod) focus.getParent();
			if (!CharOperation.equals(local.enclosingMethod.selector, method.getElementName().toCharArray())) {
				return;
			}
		}
	}
	if (this.pattern.simpleName == null) {
		this.match.setOffset(expr.sourceStart);
		this.match.setLength(expr.sourceEnd-expr.sourceStart+1);
	}
	locator.report(this.match);
}
@Override
protected int referenceType() {
	return IJavaElement.TYPE;
}
protected void reportDeclaration(ASTNode reference, IJavaElement element, MatchLocator locator, SimpleSet knownTypes) throws CoreException {
	int maxType = -1;
	TypeBinding typeBinding = null;
	if (reference instanceof TypeReference) {
		typeBinding = ((TypeReference) reference).resolvedType;
		maxType = Integer.MAX_VALUE;
	} else if (reference instanceof QualifiedNameReference) {
		QualifiedNameReference qNameRef = (QualifiedNameReference) reference;
		Binding binding = qNameRef.binding;
		maxType = qNameRef.tokens.length - 1;
		switch (qNameRef.bits & ASTNode.RestrictiveFlagMASK) {
			case Binding.FIELD : // reading a field
				typeBinding = qNameRef.actualReceiverType;
				maxType -= qNameRef.otherBindings == null ? 1 : qNameRef.otherBindings.length + 1;
				break;
			case Binding.TYPE : //=============only type ==============
				if (binding instanceof TypeBinding)
					typeBinding = (TypeBinding) binding;
				break;
			case Binding.VARIABLE : //============unbound cases===========
			case Binding.TYPE | Binding.VARIABLE :
				if (binding instanceof ProblemFieldBinding) {
					typeBinding = qNameRef.actualReceiverType;
					maxType -= qNameRef.otherBindings == null ? 1 : qNameRef.otherBindings.length + 1;
				} else if (binding instanceof ProblemBinding) {
					ProblemBinding pbBinding = (ProblemBinding) binding;
					typeBinding = pbBinding.searchType; // second chance with recorded type so far
					char[] partialQualifiedName = pbBinding.name;
					maxType = CharOperation.occurencesOf('.', partialQualifiedName) - 1; // index of last bound token is one before the pb token
					if (typeBinding == null || maxType < 0) return;
				}
				break;
		}
	} else if (reference instanceof SingleNameReference) {
		typeBinding = (TypeBinding) ((SingleNameReference) reference).binding;
		maxType = 1;
	}

	if (typeBinding instanceof ArrayBinding)
		typeBinding = ((ArrayBinding) typeBinding).leafComponentType;
	if (typeBinding == null || typeBinding instanceof BaseTypeBinding) return;
	if (typeBinding instanceof ProblemReferenceBinding) {
		TypeBinding original = typeBinding.closestMatch();
		if (original == null) return; // original may not be set (bug 71279)
		typeBinding = original;
	}
	typeBinding = typeBinding.erasure();
	reportDeclaration((ReferenceBinding) typeBinding, maxType, locator, knownTypes);
}
protected void reportDeclaration(ReferenceBinding typeBinding, int maxType, MatchLocator locator, SimpleSet knownTypes) throws CoreException {
	IType type = locator.lookupType(typeBinding);
	if (type == null) return; // case of a secondary type

	IResource resource = type.getResource();
	boolean isBinary = type.isBinary();
	IBinaryType info = null;
	if (isBinary) {
		if (resource == null)
			resource = type.getJavaProject().getProject();
		info = locator.getBinaryInfo((org.eclipse.jdt.internal.core.ClassFile) type.getClassFile(), resource);
	}
	while (maxType >= 0 && type != null) {
		if (!knownTypes.includes(type)) {
			if (isBinary) {
				locator.reportBinaryMemberDeclaration(resource, type, typeBinding, info, SearchMatch.A_ACCURATE);
			} else {
				if (typeBinding instanceof ParameterizedTypeBinding)
					typeBinding = ((ParameterizedTypeBinding) typeBinding).genericType();
				ClassScope scope = ((SourceTypeBinding) typeBinding).scope;
				if (scope != null) {
					TypeDeclaration typeDecl = scope.referenceContext;
					int offset = typeDecl.sourceStart;
					this.match = new TypeDeclarationMatch(((JavaElement) type).resolved(typeBinding), SearchMatch.A_ACCURATE, offset, typeDecl.sourceEnd-offset+1, locator.getParticipant(), resource);
					locator.report(this.match);
				}
			}
			knownTypes.add(type);
		}
		typeBinding = typeBinding.enclosingType();
		IJavaElement parent = type.getParent();
		if (parent instanceof IType) {
			type = (IType)parent;
		} else {
			type = null;
		}
		maxType--;
	}
}
@Override
public int resolveLevel(ASTNode node) {
	if (node instanceof TypeReference)
		return resolveLevel((TypeReference) node);
	if (node instanceof NameReference)
		return resolveLevel((NameReference) node);
//	if (node instanceof ImportReference) - Not called when resolve is true, see MatchingNodeSet.reportMatching(unit)
	return IMPOSSIBLE_MATCH;
}
@Override
public int resolveLevel(Binding binding) {
	if (binding == null) return INACCURATE_MATCH;

	if(binding instanceof MethodBinding)
		return resolveLevel((MethodBinding) binding);
	if(binding instanceof VariableBinding)
		return resolveLevel((VariableBinding) binding);

	if (!(binding instanceof TypeBinding)) return IMPOSSIBLE_MATCH;

	TypeBinding typeBinding = (TypeBinding) binding;
	if (typeBinding instanceof ArrayBinding)
		typeBinding = ((ArrayBinding) typeBinding).leafComponentType;
	if (typeBinding instanceof ProblemReferenceBinding)
		typeBinding = ((ProblemReferenceBinding) typeBinding).closestMatch();

	return resolveLevelForTypeOrEnclosingTypes(this.pattern.simpleName, this.pattern.qualification, typeBinding);
}
private int resolveLevel(MethodBinding binding) {
	int level = resolveLevelForTypes(binding.parameters);
	if(level != IMPOSSIBLE_MATCH) {
		return level;
	}

	if(binding.typeVariables != null) {
		for (TypeVariableBinding tv : binding.typeVariables) {
			if(tv.superclass != null) {
				level = resolveLevelForType(tv.superclass);
				if(level != IMPOSSIBLE_MATCH) {
					return level;
				}
			}

			level = resolveLevelForTypes(tv.superInterfaces);
			if(level != IMPOSSIBLE_MATCH) {
				return level;
			}
		}
	}

	if(!binding.isVoidMethod() && binding.returnType != null) {
		return resolveLevelForType(binding.returnType);
	}

	return IMPOSSIBLE_MATCH;
}
private int resolveLevel(VariableBinding binding) {
	if(binding.type != null) {
		return resolveLevelForType(binding.type);
	}
	return IMPOSSIBLE_MATCH;
}
private int resolveLevelForTypes(TypeBinding[] types) {
	if(types != null) {
		for (TypeBinding t : types) {
			int levelForType = resolveLevelForType(t);
			if(levelForType != IMPOSSIBLE_MATCH) {
				return levelForType;
			}
		}
	}
	return IMPOSSIBLE_MATCH;
}
protected int resolveLevel(NameReference nameRef) {
	Binding binding = nameRef.binding;

	if (nameRef instanceof SingleNameReference) {
		if (binding instanceof ProblemReferenceBinding)
			binding = ((ProblemReferenceBinding) binding).closestMatch();
		if (binding instanceof ReferenceBinding)
			return resolveLevelForType((ReferenceBinding) binding);
		if (((SingleNameReference) nameRef).isLabel)
			return IMPOSSIBLE_MATCH;

		return binding == null || binding instanceof ProblemBinding ? INACCURATE_MATCH : IMPOSSIBLE_MATCH;
	}

	TypeBinding typeBinding = null;
	QualifiedNameReference qNameRef = (QualifiedNameReference) nameRef;
	switch (qNameRef.bits & ASTNode.RestrictiveFlagMASK) {
		case Binding.FIELD : // reading a field
			if (qNameRef.tokens.length < (qNameRef.otherBindings == null ? 2 : qNameRef.otherBindings.length + 2))
				return IMPOSSIBLE_MATCH; // must be at least A.x
			typeBinding = nameRef.actualReceiverType;
			break;
		case Binding.LOCAL : // reading a local variable
			return IMPOSSIBLE_MATCH; // no type match in it
		case Binding.TYPE : //=============only type ==============
			if (binding instanceof TypeBinding)
				typeBinding = (TypeBinding) binding;
			break;
		/*
		 * Handling of unbound qualified name references. The match may reside in the resolved fragment,
		 * which is recorded inside the problem binding, along with the portion of the name until it became a problem.
		 */
		case Binding.VARIABLE : //============unbound cases===========
		case Binding.TYPE | Binding.VARIABLE :
			if (binding instanceof ProblemReferenceBinding) {
				typeBinding = (TypeBinding) binding;
			} else if (binding instanceof ProblemFieldBinding) {
				if (qNameRef.tokens.length < (qNameRef.otherBindings == null ? 2 : qNameRef.otherBindings.length + 2))
					return IMPOSSIBLE_MATCH; // must be at least A.x
				typeBinding = nameRef.actualReceiverType;
			} else if (binding instanceof ProblemBinding) {
				ProblemBinding pbBinding = (ProblemBinding) binding;
				if (CharOperation.occurencesOf('.', pbBinding.name) <= 0) // index of last bound token is one before the pb token
					return INACCURATE_MATCH;
				typeBinding = pbBinding.searchType;
			}
			break;
	}
	return resolveLevel(typeBinding);
}
protected int resolveLevel(TypeReference typeRef) {
	TypeBinding typeBinding = typeRef.resolvedType;
	if (typeBinding instanceof ArrayBinding)
		typeBinding = ((ArrayBinding) typeBinding).leafComponentType;
	if (typeBinding instanceof ProblemReferenceBinding)
		typeBinding = ((ProblemReferenceBinding) typeBinding).closestMatch();

	if (typeRef instanceof SingleTypeReference) {
		return resolveLevelForType(typeBinding);
	} else
		return resolveLevelForTypeOrQualifyingTypes(typeRef, typeBinding);
}
/* (non-Javadoc)
 * Resolve level for type with a given binding.
 * This is just an helper to avoid call of method with all parameters...
 */
protected int resolveLevelForType(TypeBinding typeBinding) {
	if (typeBinding == null || !typeBinding.isValidBinding()) {
		if (this.pattern.typeSuffix != TYPE_SUFFIX) return INACCURATE_MATCH;
	} else {
		switch (this.pattern.typeSuffix) {
			case CLASS_SUFFIX:
				if (!typeBinding.isClass()) return IMPOSSIBLE_MATCH;
				break;
			case CLASS_AND_INTERFACE_SUFFIX:
				if (!(typeBinding.isClass() || (typeBinding.isInterface() && !typeBinding.isAnnotationType()))) return IMPOSSIBLE_MATCH;
				break;
			case CLASS_AND_ENUM_SUFFIX:
				if (!(typeBinding.isClass() || typeBinding.isEnum())) return IMPOSSIBLE_MATCH;
				break;
			case INTERFACE_SUFFIX:
				if (!typeBinding.isInterface() || typeBinding.isAnnotationType()) return IMPOSSIBLE_MATCH;
				break;
			case INTERFACE_AND_ANNOTATION_SUFFIX:
				if (!(typeBinding.isInterface() || typeBinding.isAnnotationType())) return IMPOSSIBLE_MATCH;
				break;
			case ENUM_SUFFIX:
				if (!typeBinding.isEnum()) return IMPOSSIBLE_MATCH;
				break;
			case ANNOTATION_TYPE_SUFFIX:
				if (!typeBinding.isAnnotationType()) return IMPOSSIBLE_MATCH;
				break;
			case TYPE_SUFFIX : // nothing
		}
	}
	return resolveLevelForType( this.pattern.simpleName,
						this.pattern.qualification,
						this.pattern.getTypeArguments(),
						0,
						typeBinding);
}
/**
 * Returns whether the given type binding or one of its enclosing types
 * matches the given simple name pattern and qualification pattern.
 * Returns ACCURATE_MATCH if it does.
 * Returns INACCURATE_MATCH if resolve failed.
 * Returns IMPOSSIBLE_MATCH if it doesn't.
 */
protected int resolveLevelForTypeOrEnclosingTypes(char[] simpleNamePattern, char[] qualificationPattern, TypeBinding binding) {
	if (binding == null) return INACCURATE_MATCH;

	if (binding instanceof ReferenceBinding) {
		ReferenceBinding type = (ReferenceBinding) binding;
		while (type != null) {
			int level = resolveLevelForType(type);
			if (level != IMPOSSIBLE_MATCH) return level;

			type = type.enclosingType();
		}
	}
	return IMPOSSIBLE_MATCH;
}
private Map/*<QualifiedTypeReference, List<TypeBinding>>*/ recordedResolutions = new HashMap();
int resolveLevelForTypeOrQualifyingTypes(TypeReference typeRef, TypeBinding typeBinding) {
	if (typeBinding == null || !typeBinding.isValidBinding()) return INACCURATE_MATCH;
	List resolutionsList = (List) this.recordedResolutions.get(typeRef);
	if (resolutionsList != null) {
		for (Iterator i = resolutionsList.iterator(); i.hasNext();) {
			TypeBinding resolution = (TypeBinding) i.next();
			int level = resolveLevelForType(resolution);
			if (level != IMPOSSIBLE_MATCH) return level;
		}
	}
	return IMPOSSIBLE_MATCH;
}
@Override
public void recordResolution(QualifiedTypeReference typeReference, TypeBinding resolution) {
	List/*<TypeBinding>*/ resolutionsForTypeReference = (List) this.recordedResolutions.get(typeReference);
	if (resolutionsForTypeReference == null) {
		resolutionsForTypeReference = new ArrayList();
	}
	resolutionsForTypeReference.add(resolution);
	this.recordedResolutions.put(typeReference, resolutionsForTypeReference);
}
@Override
public String toString() {
	return "Locator for " + this.pattern.toString(); //$NON-NLS-1$
}
}
