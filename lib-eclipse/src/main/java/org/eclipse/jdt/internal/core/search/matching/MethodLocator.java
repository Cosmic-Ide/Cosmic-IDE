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
 *     Samrat Dhillon samrat.dhillon@gmail.com - Search for method references is
 *               returning methods as overriden even if the superclass's method is
 *               only package-visible - https://bugs.eclipse.org/357547
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search.matching;

import java.util.Arrays;
import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.lookup.*;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;
import org.eclipse.jdt.internal.core.BinaryMethod;
import org.eclipse.jdt.internal.core.search.BasicSearchEngine;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MethodLocator extends PatternLocator {

protected MethodPattern pattern;
protected boolean isDeclarationOfReferencedMethodsPattern;

//extra reference info
public char[][][] allSuperDeclaringTypeNames;

// This is set only if focus is null. In these cases
// it will be hard to determine if the super class is of the same package
// at a latter point. Hence, this array is created with all the super class
// names of the same package name as of the matching class name.
// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=357547
private char[][][] samePkgSuperDeclaringTypeNames;

private MatchLocator matchLocator;
//method declarations which parameters verification fail
private HashMap methodDeclarationsWithInvalidParam = new HashMap();


public MethodLocator(MethodPattern pattern) {
	super(pattern);

	this.pattern = pattern;
	this.isDeclarationOfReferencedMethodsPattern = this.pattern instanceof DeclarationOfReferencedMethodsPattern;
}
/*
 * Clear caches
 */
@Override
protected void clear() {
	this.methodDeclarationsWithInvalidParam = new HashMap();
}
@Override
protected int fineGrain() {
	return this.pattern.fineGrain;
}

private ReferenceBinding getMatchingSuper(ReferenceBinding binding) {
	if (binding == null) return null;
	ReferenceBinding superBinding = binding.superclass();
	int level = resolveLevelForType(this.pattern.declaringSimpleName, this.pattern.declaringQualification, superBinding);
	if (level != IMPOSSIBLE_MATCH) return superBinding;
	// matches superclass
	if (!binding.isInterface() && !CharOperation.equals(binding.compoundName, TypeConstants.JAVA_LANG_OBJECT)) {
		superBinding = getMatchingSuper(superBinding);
		if (superBinding != null) return superBinding;
	}
	// matches interfaces
	ReferenceBinding[] interfaces = binding.superInterfaces();
	if (interfaces == null) return null;
	for (int i = 0; i < interfaces.length; i++) {
		level = resolveLevelForType(this.pattern.declaringSimpleName, this.pattern.declaringQualification, interfaces[i]);
		if (level != IMPOSSIBLE_MATCH) return interfaces[i];
		superBinding = getMatchingSuper(interfaces[i]);
		if (superBinding != null) return superBinding;
	}
	return null;
}

private MethodBinding getMethodBinding(ReferenceBinding type, char[] methodName, TypeBinding[] argumentTypes) {
	MethodBinding[] methods = type.getMethods(methodName);
	MethodBinding method = null;
	methodsLoop: for (int i=0, length=methods.length; i<length; i++) {
		method = methods[i];
		TypeBinding[] parameters = method.parameters;
		if (argumentTypes.length == parameters.length) {
			for (int j=0,l=parameters.length; j<l; j++) {
				if (TypeBinding.notEquals(parameters[j].erasure(), argumentTypes[j].erasure())) {
					continue methodsLoop;
				}
			}
			return method;
		}
	}
	return null;
}

@Override
public void initializePolymorphicSearch(MatchLocator locator) {
	long start = 0;
	if (BasicSearchEngine.VERBOSE) {
		start = System.currentTimeMillis();
	}
	try {
		SuperTypeNamesCollector namesCollector =
			new SuperTypeNamesCollector(
				this.pattern,
				this.pattern.declaringSimpleName,
				this.pattern.declaringQualification,
				locator,
				this.pattern.declaringType,
				locator.progressMonitor);
		this.allSuperDeclaringTypeNames = namesCollector.collect();
		this.samePkgSuperDeclaringTypeNames = namesCollector.getSamePackageSuperTypeNames();
		this.matchLocator = locator;
	} catch (JavaModelException e) {
		// inaccurate matches will be found
	}
	if (BasicSearchEngine.VERBOSE) {
		System.out.println("Time to initialize polymorphic search: "+(System.currentTimeMillis()-start)); //$NON-NLS-1$
	}
}
/*
 * Return whether a type name is in pattern all super declaring types names.
 */
private boolean isTypeInSuperDeclaringTypeNames(char[][] typeName) {
	if (this.allSuperDeclaringTypeNames == null) return false;
	int length = this.allSuperDeclaringTypeNames.length;
	for (int i= 0; i<length; i++) {
		if (CharOperation.equals(this.allSuperDeclaringTypeNames[i], typeName)) {
			return true;
		}
	}
	return false;
}
/**
 * Returns whether the code gen will use an invoke virtual for
 * this message send or not.
 */
protected boolean isVirtualInvoke(MethodBinding method, MessageSend messageSend) {
		return !method.isStatic() && !method.isPrivate() && !messageSend.isSuperAccess()
			&& !(method.isDefault() && this.pattern.focus != null
			&& !CharOperation.equals(this.pattern.declaringPackageName, method.declaringClass.qualifiedPackageName()));
}
protected ReferenceBinding checkMethodRef(MethodBinding method, ReferenceExpression referenceExpression) {
	boolean result = (!method.isStatic() && !method.isPrivate()
		&&	referenceExpression.isMethodReference()
		&& !(method.isDefault() && this.pattern.focus != null
		&& !CharOperation.equals(this.pattern.declaringPackageName, method.declaringClass.qualifiedPackageName())));
	if (result) {
		Expression lhs = referenceExpression.lhs;
		if (lhs instanceof NameReference) {
			Binding binding = ((NameReference) lhs).resolvedType;
			if (binding instanceof ReferenceBinding)
				return (ReferenceBinding) binding;
		}
	}

	return null;
}
@Override
public int match(ASTNode node, MatchingNodeSet nodeSet) {
	int declarationsLevel = IMPOSSIBLE_MATCH;
	if (this.pattern.findReferences) {
		if (node instanceof ImportReference) {
			// With static import, we can have static method reference in import reference
			ImportReference importRef = (ImportReference) node;
			int length = importRef.tokens.length-1;
			if (importRef.isStatic() && ((importRef.bits & ASTNode.OnDemand) == 0) && matchesName(this.pattern.selector, importRef.tokens[length])) {
				char[][] compoundName = new char[length][];
				System.arraycopy(importRef.tokens, 0, compoundName, 0, length);
				char[] declaringType = CharOperation.concat(this.pattern.declaringQualification, this.pattern.declaringSimpleName, '.');
				if (matchesName(declaringType, CharOperation.concatWith(compoundName, '.'))) {
					declarationsLevel = this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;
				}
			}
		}
	}
	return nodeSet.addMatch(node, declarationsLevel);
}

@Override
public int match(LambdaExpression node, MatchingNodeSet nodeSet) {
	if (!this.pattern.findDeclarations) return IMPOSSIBLE_MATCH;
	if (this.pattern.parameterSimpleNames != null && this.pattern.parameterSimpleNames.length != node.arguments().length) return IMPOSSIBLE_MATCH;

	nodeSet.mustResolve = true;
	return nodeSet.addMatch(node, POSSIBLE_MATCH);
}

//public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(Expression node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(FieldDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
@Override
public int match(MethodDeclaration node, MatchingNodeSet nodeSet) {
	if (!this.pattern.findDeclarations) return IMPOSSIBLE_MATCH;

	// Verify method name
	if (!matchesName(this.pattern.selector, node.selector)) return IMPOSSIBLE_MATCH;

	// Verify parameters types
	boolean resolve = this.pattern.mustResolve;
	if (this.pattern.parameterSimpleNames != null) {
		int length = this.pattern.parameterSimpleNames.length;
		ASTNode[] args = node.arguments;
		int argsLength = args == null ? 0 : args.length;
		if (length != argsLength) return IMPOSSIBLE_MATCH;
		for (int i = 0; i < argsLength; i++) {
			if (args != null && !matchesTypeReference(this.pattern.parameterSimpleNames[i], ((Argument) args[i]).type)) {
				// Do not return as impossible when source level is at least 1.5
				if (this.mayBeGeneric) {
					if (!this.pattern.mustResolve) {
						// Set resolution flag on node set in case of types was inferred in parameterized types from generic ones...
					 	// (see  bugs https://bugs.eclipse.org/bugs/show_bug.cgi?id=79990, 96761, 96763)
						nodeSet.mustResolve = true;
						resolve = true;
					}
					this.methodDeclarationsWithInvalidParam.put(node, null);
				} else {
					return IMPOSSIBLE_MATCH;
				}
			}
		}
	}

	// Verify type arguments (do not reject if pattern has no argument as it can be an erasure match)
	if (this.pattern.hasMethodArguments()) {
		if (node.typeParameters == null || node.typeParameters.length != this.pattern.methodArguments.length) return IMPOSSIBLE_MATCH;
	}

	// Method declaration may match pattern
	return nodeSet.addMatch(node, resolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
}
@Override
public int match(MemberValuePair node, MatchingNodeSet nodeSet) {
	if (!this.pattern.findReferences) return IMPOSSIBLE_MATCH;

	if (!matchesName(this.pattern.selector, node.name)) return IMPOSSIBLE_MATCH;

	return nodeSet.addMatch(node, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
}
@Override
public int match(MessageSend node, MatchingNodeSet nodeSet) {
	if (!this.pattern.findReferences) return IMPOSSIBLE_MATCH;

	if (!matchesName(this.pattern.selector, node.selector)) return IMPOSSIBLE_MATCH;
	if (this.pattern.parameterSimpleNames != null && (!this.pattern.varargs || ((node.bits & ASTNode.InsideJavadoc) != 0))) {
		int length = this.pattern.parameterSimpleNames.length;
		ASTNode[] args = node.arguments;
		int argsLength = args == null ? 0 : args.length;
		if (length != argsLength) return IMPOSSIBLE_MATCH;
	}

	return nodeSet.addMatch(node, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
}

@Override
public int match(ReferenceExpression node, MatchingNodeSet nodeSet) {
	if (!this.pattern.findReferences) return IMPOSSIBLE_MATCH;
	if (!matchesName(this.pattern.selector, node.selector)) return IMPOSSIBLE_MATCH;
	if (node.selector != null &&  Arrays.equals(node.selector, org.eclipse.jdt.internal.compiler.codegen.ConstantPool.Init))
		return IMPOSSIBLE_MATCH; // :: new
	nodeSet.mustResolve = true;
	return nodeSet.addMatch(node, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
}

@Override
public int match(Annotation node, MatchingNodeSet nodeSet) {
	if (!this.pattern.findReferences) return IMPOSSIBLE_MATCH;
	MemberValuePair[] pairs = node.memberValuePairs();
	if (pairs == null || pairs.length == 0) return IMPOSSIBLE_MATCH;

	int length = pairs.length;
	MemberValuePair pair = null;
	for (int i=0; i<length; i++) {
		pair = node.memberValuePairs()[i];
		if (matchesName(this.pattern.selector, pair.name)) {
			ASTNode possibleNode = (node instanceof SingleMemberAnnotation) ? (ASTNode) node : pair;
			return nodeSet.addMatch(possibleNode, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
		}
	}
	return IMPOSSIBLE_MATCH;
}
//public int match(TypeDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
//public int match(TypeReference node, MatchingNodeSet nodeSet) - SKIP IT

@Override
protected int matchContainer() {
	if (this.pattern.findReferences) {
		// need to look almost everywhere to find in javadocs and static import
		return ALL_CONTAINER;
	}
	return CLASS_CONTAINER;
}
/* (non-Javadoc)
 * @see org.eclipse.jdt.internal.core.search.matching.PatternLocator#matchLevelAndReportImportRef(org.eclipse.jdt.internal.compiler.ast.ImportReference, org.eclipse.jdt.internal.compiler.lookup.Binding, org.eclipse.jdt.internal.core.search.matching.MatchLocator)
 * Accept to report match of static field on static import
 */
@Override
protected void matchLevelAndReportImportRef(ImportReference importRef, Binding binding, MatchLocator locator) throws CoreException {
	if (importRef.isStatic() && binding instanceof MethodBinding) {
		super.matchLevelAndReportImportRef(importRef, binding, locator);
	}
}

protected int matchMethod(MethodBinding method, boolean skipImpossibleArg) {
	if (!matchesName(this.pattern.selector, method.selector)) return IMPOSSIBLE_MATCH;

	int level = ACCURATE_MATCH;
	// look at return type only if declaring type is not specified
	if (this.pattern.declaringSimpleName == null) {
		// TODO (frederic) use this call to refine accuracy on return type
		// int newLevel = resolveLevelForType(this.pattern.returnSimpleName, this.pattern.returnQualification, this.pattern.returnTypeArguments, 0, method.returnType);
		int newLevel = resolveLevelForType(this.pattern.returnSimpleName, this.pattern.returnQualification, method.returnType);
		if (level > newLevel) {
			if (newLevel == IMPOSSIBLE_MATCH) return IMPOSSIBLE_MATCH;
			level = newLevel; // can only be downgraded
		}
	}

	// parameter types
	int parameterCount = this.pattern.parameterSimpleNames == null ? -1 : this.pattern.parameterSimpleNames.length;
	if (parameterCount > -1) {
		// global verification
		if (method.parameters == null) return INACCURATE_MATCH;
		if (parameterCount != method.parameters.length) return IMPOSSIBLE_MATCH;
		if (!method.isValidBinding() && ((ProblemMethodBinding)method).problemId() == ProblemReasons.Ambiguous) {
			// return inaccurate match for ambiguous call (bug 80890)
			return INACCURATE_MATCH;
		}
		boolean foundTypeVariable = false;
		MethodBinding focusMethodBinding = null;
		boolean checkedFocus = false;
		boolean isBinary = this.pattern!= null && this.pattern.focus instanceof BinaryMethod;
		// verify each parameter
		for (int i = 0; i < parameterCount; i++) {
			TypeBinding argType = method.parameters[i];
			int newLevel = IMPOSSIBLE_MATCH;
			boolean foundLevel = false;
			if (argType.isMemberType() || this.pattern.parameterQualifications[i] != null) {
				if (!checkedFocus) {
					focusMethodBinding = this.matchLocator.getMethodBinding(this.pattern);
					checkedFocus = true;
				}
				if (focusMethodBinding != null) {// textual comparison insufficient
					TypeBinding[] parameters = focusMethodBinding.parameters;
					if (parameters.length >= parameterCount) {
						newLevel = (isBinary ? argType.erasure().isEquivalentTo((parameters[i].erasure())) :argType.isEquivalentTo((parameters[i]))) ?
								ACCURATE_MATCH : IMPOSSIBLE_MATCH;
						foundLevel = true;
					}
				}
			} else {
				// TODO (frederic) use this call to refine accuracy on parameter types
//				 newLevel = resolveLevelForType(this.pattern.parameterSimpleNames[i], this.pattern.parameterQualifications[i], this.pattern.parametersTypeArguments[i], 0, argType);
				newLevel = resolveLevelForType(this.pattern.parameterSimpleNames[i], this.pattern.parameterQualifications[i], argType);
			}
			if (level > newLevel) {
				if (newLevel == IMPOSSIBLE_MATCH) {
					if (skipImpossibleArg) {
						// Do not consider match as impossible while finding declarations and source level >= 1.5
					 	// (see  bugs https://bugs.eclipse.org/bugs/show_bug.cgi?id=79990, 96761, 96763)
						if (!foundLevel) {
							newLevel = level;
						}
					} else if (argType.isTypeVariable()) {
						newLevel = level;
						foundTypeVariable = true;
					} else {
						return IMPOSSIBLE_MATCH;
					}
				}
				level = newLevel; // can only be downgraded
			}
		}
		if (foundTypeVariable) {
			if (!method.isStatic() && !method.isPrivate()) {
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=123836, No point in textually comparing type variables, captures etc with concrete types.
				if (!checkedFocus)
					focusMethodBinding = this.matchLocator.getMethodBinding(this.pattern);
				if (focusMethodBinding != null) {
					if (matchOverriddenMethod(focusMethodBinding.declaringClass, focusMethodBinding, method)) {
						return ACCURATE_MATCH;
					}
				}
			}
			return IMPOSSIBLE_MATCH;
		}
	}

	return level;
}
// This works for only methods of parameterized types.
private boolean matchOverriddenMethod(ReferenceBinding type, MethodBinding method, MethodBinding matchMethod) {
	if (type == null || this.pattern.selector == null) return false;

	// matches superclass
	if (!type.isInterface() && !CharOperation.equals(type.compoundName, TypeConstants.JAVA_LANG_OBJECT)) {
		ReferenceBinding superClass = type.superclass();
		if (superClass.isParameterizedType()) {
			MethodBinding[] methods = superClass.getMethods(this.pattern.selector);
			int length = methods.length;
			for (int i = 0; i<length; i++) {
				if (methods[i].areParametersEqual(method)) {
					if (matchMethod == null) {
						if (methodParametersEqualsPattern(methods[i].original())) return true;
					} else {
						if (methods[i].original().areParametersEqual(matchMethod)) return true;
					}
				}
			}
		}
		if (matchOverriddenMethod(superClass, method, matchMethod)) {
			return true;
		}
	}

	// matches interfaces
	ReferenceBinding[] interfaces = type.superInterfaces();
	if (interfaces == null) return false;
	int iLength = interfaces.length;
	for (int i = 0; i<iLength; i++) {
		if (interfaces[i].isParameterizedType()) {
			MethodBinding[] methods = interfaces[i].getMethods(this.pattern.selector);
			int length = methods.length;
			for (int j = 0; j<length; j++) {
				if (methods[j].areParametersEqual(method)) {
					if (matchMethod == null) {
						if (methodParametersEqualsPattern(methods[j].original())) return true;
					} else {
						if (methods[j].original().areParametersEqual(matchMethod)) return true;
					}
				}
			}
		}
		if (matchOverriddenMethod(interfaces[i], method, matchMethod)) {
			return true;
		}
	}
	return false;
}
@Override
protected void matchReportReference(ASTNode reference, IJavaElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	matchReportReference(reference, element, null, null, elementBinding, accuracy, locator);
}
/**
 * @see org.eclipse.jdt.internal.core.search.matching.PatternLocator#matchReportReference(org.eclipse.jdt.internal.compiler.ast.ASTNode, org.eclipse.jdt.core.IJavaElement, Binding, int, org.eclipse.jdt.internal.core.search.matching.MatchLocator)
 */
@Override
protected void matchReportReference(ASTNode reference, IJavaElement element, IJavaElement localElement, IJavaElement[] otherElements, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {
	MethodBinding methodBinding = (reference instanceof MessageSend) ? ((MessageSend)reference).binding: ((elementBinding instanceof MethodBinding) ? (MethodBinding) elementBinding : null);
	if (this.isDeclarationOfReferencedMethodsPattern) {
		if (methodBinding == null) return;
		// need exact match to be able to open on type ref
		if (accuracy != SearchMatch.A_ACCURATE) return;

		// element that references the method must be included in the enclosing element
		DeclarationOfReferencedMethodsPattern declPattern = (DeclarationOfReferencedMethodsPattern) this.pattern;
		while (element != null && !declPattern.enclosingElement.equals(element))
			element = element.getParent();
		if (element != null) {
			reportDeclaration(methodBinding, locator, declPattern.knownMethods);
		}
	} else {
		MethodReferenceMatch methodReferenceMatch = locator.newMethodReferenceMatch(element, elementBinding, accuracy, -1, -1, false /*not constructor*/, false/*not synthetic*/, reference);
		methodReferenceMatch.setLocalElement(localElement);
		this.match = methodReferenceMatch;
		if (this.pattern.findReferences && reference instanceof MessageSend) {
			IJavaElement focus = this.pattern.focus;
			// verify closest match if pattern was bound
			// (see bug 70827)
			if (focus != null && focus.getElementType() == IJavaElement.METHOD) {
				if (methodBinding != null && methodBinding.declaringClass != null) {
					boolean isPrivate = Flags.isPrivate(((IMethod) focus).getFlags());
					if (isPrivate && !CharOperation.equals(methodBinding.declaringClass.sourceName, focus.getParent().getElementName().toCharArray())) {
						return; // finally the match was not possible
					}
				}
			}
			matchReportReference((MessageSend)reference, locator, accuracy, ((MessageSend)reference).binding);
		} else {
			if (reference instanceof SingleMemberAnnotation) {
				reference = ((SingleMemberAnnotation)reference).memberValuePairs()[0];
				this.match.setImplicit(true);
			}
			int offset;
			if (reference instanceof ReferenceExpression) {
				offset = ((ReferenceExpression) reference).nameSourceStart;
			} else {
				offset = reference.sourceStart;
			}
			int length =  reference.sourceEnd - offset + 1;
			this.match.setOffset(offset);
			this.match.setLength(length);
			locator.report(this.match);
		}
	}
}
void matchReportReference(MessageSend messageSend, MatchLocator locator, int accuracy, MethodBinding methodBinding) throws CoreException {

	// Look if there's a need to special report for parameterized type
	boolean isParameterized = false;
	if (methodBinding instanceof ParameterizedGenericMethodBinding) { // parameterized generic method
		isParameterized = true;

		// Update match regarding method type arguments
		ParameterizedGenericMethodBinding parameterizedMethodBinding = (ParameterizedGenericMethodBinding) methodBinding;
		this.match.setRaw(parameterizedMethodBinding.isRaw);
		TypeBinding[] typeArguments = /*parameterizedMethodBinding.isRaw ? null :*/ parameterizedMethodBinding.typeArguments;
		updateMatch(typeArguments, locator, this.pattern.methodArguments, this.pattern.hasMethodParameters());

		// Update match regarding declaring class type arguments
		if (methodBinding.declaringClass.isParameterizedType() || methodBinding.declaringClass.isRawType()) {
			ParameterizedTypeBinding parameterizedBinding = (ParameterizedTypeBinding)methodBinding.declaringClass;
			if (!this.pattern.hasTypeArguments() && this.pattern.hasMethodArguments() || parameterizedBinding.isParameterizedWithOwnVariables()) {
				// special case for pattern which defines method arguments but not its declaring type
				// in this case, we do not refine accuracy using declaring type arguments...!
			} else {
				updateMatch(parameterizedBinding, this.pattern.getTypeArguments(), this.pattern.hasTypeParameters(), 0, locator);
			}
		} else if (this.pattern.hasTypeArguments()) {
			this.match.setRule(SearchPattern.R_ERASURE_MATCH);
		}

		// Update match regarding method parameters
		// TODO ? (frederic)

		// Update match regarding method return type
		// TODO ? (frederic)

		// Special case for errors
		if (this.match.getRule() != 0 && messageSend.resolvedType == null) {
			this.match.setRule(SearchPattern.R_ERASURE_MATCH);
		}
	} else if (methodBinding instanceof ParameterizedMethodBinding) {
		isParameterized = true;
		if (methodBinding.declaringClass.isParameterizedType() || methodBinding.declaringClass.isRawType()) {
			ParameterizedTypeBinding parameterizedBinding = (ParameterizedTypeBinding)methodBinding.declaringClass;
			if (!parameterizedBinding.isParameterizedWithOwnVariables()) {
				if ((accuracy & (SUB_INVOCATION_FLAVOR | OVERRIDDEN_METHOD_FLAVOR)) != 0) {
					// type parameters need to be compared with the class that is really being searched
					// https://bugs.eclipse.org/375971
					ReferenceBinding refBinding = getMatchingSuper(((ReferenceBinding)messageSend.actualReceiverType));
					if (refBinding instanceof ParameterizedTypeBinding) {
						parameterizedBinding = ((ParameterizedTypeBinding)refBinding);
					}
				}
				if ((accuracy & SUPER_INVOCATION_FLAVOR) == 0) {
					// not able to get the type parameters if the match is super
					updateMatch(parameterizedBinding, this.pattern.getTypeArguments(), this.pattern.hasTypeParameters(), 0, locator);
				}
			}
		} else if (this.pattern.hasTypeArguments()) {
			this.match.setRule(SearchPattern.R_ERASURE_MATCH);
		}

		// Update match regarding method parameters
		// TODO ? (frederic)

		// Update match regarding method return type
		// TODO ? (frederic)

		// Special case for errors
		if (this.match.getRule() != 0 && messageSend.resolvedType == null) {
			this.match.setRule(SearchPattern.R_ERASURE_MATCH);
		}
	} else if (this.pattern.hasMethodArguments()) { // binding has no type params, compatible erasure if pattern does
		this.match.setRule(SearchPattern.R_ERASURE_MATCH);
	}

	// See whether it is necessary to report or not
	if (this.match.getRule() == 0) return; // impossible match
	boolean report = (this.isErasureMatch && this.match.isErasure()) || (this.isEquivalentMatch && this.match.isEquivalent()) || this.match.isExact();
	if (!report) return;

	// Report match
	int offset = (int) (messageSend.nameSourcePosition >>> 32);
	this.match.setOffset(offset);
	this.match.setLength(messageSend.sourceEnd - offset + 1);
	 if (isParameterized && this.pattern.hasMethodArguments())  {
		locator.reportAccurateParameterizedMethodReference(this.match, messageSend, messageSend.typeArguments);
	} else {
		locator.report(this.match);
	}
}
/*
 * Return whether method parameters are equals to pattern ones.
 */
private boolean methodParametersEqualsPattern(MethodBinding method) {
	TypeBinding[] methodParameters = method.parameters;

	int length = methodParameters.length;
	if (length != this.pattern.parameterSimpleNames.length) return false;

	for (int i = 0; i < length; i++) {
		char[] paramQualifiedName = qualifiedPattern(this.pattern.parameterSimpleNames[i], this.pattern.parameterQualifications[i]);
		if (!CharOperation.match(paramQualifiedName, methodParameters[i].readableName(), this.isCaseSensitive)) {
			return false;
		}
	}
	return true;
}
@Override
public SearchMatch newDeclarationMatch(ASTNode reference, IJavaElement element, Binding elementBinding, int accuracy, int length, MatchLocator locator) {
	if (elementBinding != null) {
		MethodBinding methodBinding = (MethodBinding) elementBinding;
		// If method parameters verification was not valid, then try to see if method arguments can match a method in hierarchy
		if (this.methodDeclarationsWithInvalidParam.containsKey(reference)) {
			// First see if this reference has already been resolved => report match if validated
			Boolean report = (Boolean) this.methodDeclarationsWithInvalidParam.get(reference);
			if (report != null) {
				if (report.booleanValue()) {
					return super.newDeclarationMatch(reference, element, elementBinding, accuracy, length, locator);
				}
				return null;
			}
			if (matchOverriddenMethod(methodBinding.declaringClass, methodBinding, null)) {
				this.methodDeclarationsWithInvalidParam.put(reference, Boolean.TRUE);
				return super.newDeclarationMatch(reference, element, elementBinding, accuracy, length, locator);
			}
			if (isTypeInSuperDeclaringTypeNames(methodBinding.declaringClass.compoundName)) {
				MethodBinding patternBinding = locator.getMethodBinding(this.pattern);
				if (patternBinding != null) {
					if (!matchOverriddenMethod(patternBinding.declaringClass, patternBinding, methodBinding)) {
						this.methodDeclarationsWithInvalidParam.put(reference, Boolean.FALSE);
						return null;
					}
				}
				this.methodDeclarationsWithInvalidParam.put(reference, Boolean.TRUE);
				return super.newDeclarationMatch(reference, element, elementBinding, accuracy, length, locator);
			}
			this.methodDeclarationsWithInvalidParam.put(reference, Boolean.FALSE);
			return null;
		}
	}
	return super.newDeclarationMatch(reference, element, elementBinding, accuracy, length, locator);
}
@Override
protected int referenceType() {
	return IJavaElement.METHOD;
}
protected void reportDeclaration(MethodBinding methodBinding, MatchLocator locator, SimpleSet knownMethods) throws CoreException {
	ReferenceBinding declaringClass = methodBinding.declaringClass;
	IType type = locator.lookupType(declaringClass);
	if (type == null) return; // case of a secondary type

	// Report match for binary
	if (type.isBinary()) {
		IMethod method = null;
		TypeBinding[] parameters = methodBinding.original().parameters;
		int parameterLength = parameters.length;
		char[][] parameterTypes = new char[parameterLength][];
		for (int i = 0; i<parameterLength; i++) {
			char[] typeName = parameters[i].qualifiedSourceName();
			for (int j=0, dim=parameters[i].dimensions(); j<dim; j++) {
				typeName = CharOperation.concat(typeName, new char[] {'[', ']'});
			}
			parameterTypes[i] = typeName;
		}
		method = locator.createBinaryMethodHandle(type, methodBinding.selector, parameterTypes);
		if (method == null || knownMethods.addIfNotIncluded(method) == null) return;

		IResource resource = type.getResource();
		if (resource == null)
			resource = type.getJavaProject().getProject();
		IBinaryType info = locator.getBinaryInfo((org.eclipse.jdt.internal.core.ClassFile)type.getClassFile(), resource);
		locator.reportBinaryMemberDeclaration(resource, method, methodBinding, info, SearchMatch.A_ACCURATE);
		return;
	}

	// When source is available, report match if method is found in the declaring type
	IResource resource = type.getResource();
	if (declaringClass instanceof ParameterizedTypeBinding)
		declaringClass = ((ParameterizedTypeBinding) declaringClass).genericType();
	ClassScope scope = ((SourceTypeBinding) declaringClass).scope;
	if (scope != null) {
		TypeDeclaration typeDecl = scope.referenceContext;
		AbstractMethodDeclaration methodDecl = typeDecl.declarationOf(methodBinding.original());
		if (methodDecl != null) {
			// Create method handle from method declaration
			String methodName = new String(methodBinding.selector);
			Argument[] arguments = methodDecl.arguments;
			int length = arguments == null ? 0 : arguments.length;
			String[] parameterTypes = new String[length];
			for (int i = 0; i  < length; i++) {
				char[][] typeName = arguments[i].type.getParameterizedTypeName();
				parameterTypes[i] = Signature.createTypeSignature(CharOperation.concatWith(typeName, '.'), false);
			}
			IMethod method = type.getMethod(methodName, parameterTypes);
			if (method == null || knownMethods.addIfNotIncluded(method) == null) return;

			// Create and report corresponding match
			int offset = methodDecl.sourceStart;
			this.match = new MethodDeclarationMatch(method, SearchMatch.A_ACCURATE, offset, methodDecl.sourceEnd-offset+1, locator.getParticipant(), resource);
			locator.report(this.match);
		}
	}
}
@Override
public int resolveLevel(ASTNode possibleMatchingNode) {
	if (this.pattern.findReferences) {
		if (possibleMatchingNode instanceof MessageSend) {
			return resolveLevel((MessageSend) possibleMatchingNode);
		}
		if (possibleMatchingNode instanceof SingleMemberAnnotation) {
			SingleMemberAnnotation annotation = (SingleMemberAnnotation) possibleMatchingNode;
			return resolveLevel(annotation.memberValuePairs()[0].binding);
		}
		if (possibleMatchingNode instanceof MemberValuePair) {
			MemberValuePair memberValuePair = (MemberValuePair) possibleMatchingNode;
			return resolveLevel(memberValuePair.binding);
		}
		if (possibleMatchingNode instanceof ReferenceExpression) {
			return resolveLevel((ReferenceExpression)possibleMatchingNode);
		}
	}
	if (this.pattern.findDeclarations) {
		if (possibleMatchingNode instanceof MethodDeclaration) {
			return resolveLevel(((MethodDeclaration) possibleMatchingNode).binding);
		}
		if (possibleMatchingNode instanceof LambdaExpression) {
			return resolveLevel(((LambdaExpression) possibleMatchingNode).descriptor);
		}
	}
	return IMPOSSIBLE_MATCH;
}
@Override
public int resolveLevel(Binding binding) {
	if (binding == null) return INACCURATE_MATCH;
	if (!(binding instanceof MethodBinding)) return IMPOSSIBLE_MATCH;

	MethodBinding method = (MethodBinding) binding;
	boolean skipVerif = this.pattern.findDeclarations && this.mayBeGeneric;
	int methodLevel = matchMethod(method, skipVerif);
	if (methodLevel == IMPOSSIBLE_MATCH) {
		if (method != method.original()) methodLevel = matchMethod(method.original(), skipVerif);
		if (methodLevel == IMPOSSIBLE_MATCH) {
			return IMPOSSIBLE_MATCH;
		} else {
			method = method.original();
		}
	}

	// declaring type
	if (this.pattern.declaringSimpleName == null && this.pattern.declaringQualification == null) return methodLevel; // since any declaring class will do

	boolean subType = !method.isStatic() && !method.isPrivate();
	if (subType && this.pattern.declaringQualification != null && method.declaringClass != null && method.declaringClass.fPackage != null) {
		subType = CharOperation.compareWith(this.pattern.declaringQualification, method.declaringClass.fPackage.shortReadableName()) == 0;
	}
	int declaringLevel = subType
		? resolveLevelAsSubtype(this.pattern.declaringSimpleName, this.pattern.declaringQualification, method.declaringClass, method.selector, null, method.declaringClass.qualifiedPackageName(), method.isDefault())
		: resolveLevelForType(this.pattern.declaringSimpleName, this.pattern.declaringQualification, method.declaringClass);
	return (methodLevel & MATCH_LEVEL_MASK) > (declaringLevel & MATCH_LEVEL_MASK) ? declaringLevel : methodLevel; // return the weaker match
}
protected int resolveLevel(MessageSend messageSend) {
	MethodBinding method = messageSend.binding;
	if (method == null) {
		return INACCURATE_MATCH;
	}
	if (messageSend.resolvedType == null) {
		// Closest match may have different argument numbers when ProblemReason is NotFound
		// see MessageSend#resolveType(BlockScope)
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=97322
		int argLength = messageSend.arguments == null ? 0 : messageSend.arguments.length;
		if (this.pattern.parameterSimpleNames == null || argLength == this.pattern.parameterSimpleNames.length) {
			return INACCURATE_MATCH;
		}
		return IMPOSSIBLE_MATCH;
	}

	int methodLevel = matchMethod(method, false);
	if (methodLevel == IMPOSSIBLE_MATCH) {
		if (method != method.original()) methodLevel = matchMethod(method.original(), false);
		if (methodLevel == IMPOSSIBLE_MATCH) return IMPOSSIBLE_MATCH;
		method = method.original();
	}

	// receiver type
	if (this.pattern.declaringSimpleName == null && this.pattern.declaringQualification == null) return methodLevel; // since any declaring class will do

	int declaringLevel;
	if (isVirtualInvoke(method, messageSend) && (messageSend.actualReceiverType instanceof ReferenceBinding)) {
		ReferenceBinding methodReceiverType = (ReferenceBinding) messageSend.actualReceiverType;
		declaringLevel = resolveLevelAsSubtype(this.pattern.declaringSimpleName, this.pattern.declaringQualification, methodReceiverType, method.selector, method.parameters, methodReceiverType.qualifiedPackageName(), method.isDefault());
		if (declaringLevel == IMPOSSIBLE_MATCH) {
			if (method.declaringClass == null || this.allSuperDeclaringTypeNames == null) {
				declaringLevel = INACCURATE_MATCH;
			} else {
				char[][][] superTypeNames = (method.isDefault() && this.pattern.focus == null) ? this.samePkgSuperDeclaringTypeNames: this.allSuperDeclaringTypeNames;
				if (superTypeNames != null && resolveLevelAsSuperInvocation(methodReceiverType, method.parameters, superTypeNames, true)) {
						declaringLevel = methodLevel // since this is an ACCURATE_MATCH so return the possibly weaker match
							| SUPER_INVOCATION_FLAVOR; // this is an overridden method => add flavor to returned level
				}
			}
		}
		if ((declaringLevel & FLAVORS_MASK) != 0) {
			// level got some flavors => return it
			return declaringLevel;
		}
	} else {
		declaringLevel = resolveLevelForType(this.pattern.declaringSimpleName, this.pattern.declaringQualification, method.declaringClass);
	}
	return (methodLevel & MATCH_LEVEL_MASK) > (declaringLevel & MATCH_LEVEL_MASK) ? declaringLevel : methodLevel; // return the weaker match
}

protected int resolveLevel(ReferenceExpression referenceExpression) {
	MethodBinding method = referenceExpression.getMethodBinding();
	if (method == null || !method.isValidBinding())
		return INACCURATE_MATCH;

	int methodLevel = matchMethod(method, false);
	if (methodLevel == IMPOSSIBLE_MATCH) {
		if (method != method.original()) methodLevel = matchMethod(method.original(), false);
		if (methodLevel == IMPOSSIBLE_MATCH) return IMPOSSIBLE_MATCH;
		method = method.original();
	}

	// receiver type
	if (this.pattern.declaringSimpleName == null && this.pattern.declaringQualification == null) return methodLevel; // since any declaring class will do
	int declaringLevel;
	ReferenceBinding ref = checkMethodRef(method, referenceExpression);
	if (ref != null) {
		declaringLevel = resolveLevelAsSubtype(this.pattern.declaringSimpleName, this.pattern.declaringQualification, ref, method.selector, method.parameters, ref.qualifiedPackageName(), method.isDefault());
		if (declaringLevel == IMPOSSIBLE_MATCH) {
			if (method.declaringClass == null || this.allSuperDeclaringTypeNames == null) {
				declaringLevel = INACCURATE_MATCH;
			} else {
				char[][][] superTypeNames = (method.isDefault() && this.pattern.focus == null) ? this.samePkgSuperDeclaringTypeNames: this.allSuperDeclaringTypeNames;
				if (superTypeNames != null && resolveLevelAsSuperInvocation(ref, method.parameters, superTypeNames, true)) {
						declaringLevel = methodLevel // since this is an ACCURATE_MATCH so return the possibly weaker match
							| SUPER_INVOCATION_FLAVOR; // TODO: not an invocation really but ref -> add flavor to returned level
				}
			}
		}
		if ((declaringLevel & FLAVORS_MASK) != 0) {
			// level got some flavors => return it
			return declaringLevel;
		}
	} else {
		declaringLevel = resolveLevelForType(this.pattern.declaringSimpleName, this.pattern.declaringQualification, method.declaringClass);
	}
	return (methodLevel & MATCH_LEVEL_MASK) > (declaringLevel & MATCH_LEVEL_MASK) ? declaringLevel : methodLevel; // return the weaker match
}

/**
 * Returns whether the given reference type binding matches or is a subtype of a type
 * that matches the given qualified pattern.
 * Returns ACCURATE_MATCH if it does.
 * Returns INACCURATE_MATCH if resolve fails
 * Returns IMPOSSIBLE_MATCH if it doesn't.
 */
protected int resolveLevelAsSubtype(char[] simplePattern, char[] qualifiedPattern, ReferenceBinding type, char[] methodName, TypeBinding[] argumentTypes, char[] packageName, boolean isDefault) {
	if (type == null) return INACCURATE_MATCH;

	int level = resolveLevelForType(simplePattern, qualifiedPattern, type);
	if (level != IMPOSSIBLE_MATCH) {
		if (isDefault && !CharOperation.equals(packageName, type.qualifiedPackageName())) {
			return IMPOSSIBLE_MATCH;
		}
		MethodBinding method = argumentTypes == null ? null : getMethodBinding(type, methodName, argumentTypes);
		if (((method != null && !method.isAbstract()) || !type.isAbstract()) && !type.isInterface()) { // if concrete, then method is overridden
			level |= OVERRIDDEN_METHOD_FLAVOR;
		}
		return level;
	}

	// matches superclass
	if (!type.isInterface() && !CharOperation.equals(type.compoundName, TypeConstants.JAVA_LANG_OBJECT)) {
		level = resolveLevelAsSubtype(simplePattern, qualifiedPattern, type.superclass(), methodName, argumentTypes, packageName, isDefault);
		if (level != IMPOSSIBLE_MATCH) {
			if (argumentTypes != null) {
				// need to verify if method may be overridden
				MethodBinding method = getMethodBinding(type, methodName, argumentTypes);
				if (method != null) { // one method match in hierarchy
					if ((level & OVERRIDDEN_METHOD_FLAVOR) != 0) {
						// this method is already overridden on a super class, current match is impossible
						return IMPOSSIBLE_MATCH;
					}
					if (!method.isAbstract() && !type.isInterface()) {
						// store the fact that the method is overridden
						level |= OVERRIDDEN_METHOD_FLAVOR;
					}
				}
			}
			return level | SUB_INVOCATION_FLAVOR; // add flavor to returned level
		}
	}

	// matches interfaces
	ReferenceBinding[] interfaces = type.superInterfaces();
	if (interfaces == null) return INACCURATE_MATCH;
	for (int i = 0; i < interfaces.length; i++) {
		level = resolveLevelAsSubtype(simplePattern, qualifiedPattern, interfaces[i], methodName, null, packageName, isDefault);
		if (level != IMPOSSIBLE_MATCH) {
			if (!type.isAbstract() && !type.isInterface()) { // if concrete class, then method is overridden
				level |= OVERRIDDEN_METHOD_FLAVOR;
			}
			return level | SUB_INVOCATION_FLAVOR; // add flavor to returned level
		}
	}
	return IMPOSSIBLE_MATCH;
}

/*
 * Return whether the given type binding or one of its possible super interfaces
 * matches a type in the declaring type names hierarchy.
 */
private boolean resolveLevelAsSuperInvocation(ReferenceBinding type, TypeBinding[] argumentTypes, char[][][] superTypeNames, boolean methodAlreadyVerified) {
	char[][] compoundName = type.compoundName;
	for (int i = 0, max = superTypeNames.length; i < max; i++) {
		if (CharOperation.equals(superTypeNames[i], compoundName)) {
			// need to verify if the type implements the pattern method
			if (methodAlreadyVerified) return true; // already verified before enter into this method (see resolveLevel(MessageSend))
			MethodBinding[] methods = type.getMethods(this.pattern.selector);
			for (int j=0, length=methods.length; j<length; j++) {
				MethodBinding method = methods[j];
				TypeBinding[] parameters = method.parameters;
				if (argumentTypes.length == parameters.length) {
					boolean found = true;
					for (int k=0,l=parameters.length; k<l; k++) {
						if (TypeBinding.notEquals(parameters[k].erasure(), argumentTypes[k].erasure())) {
							found = false;
							break;
						}
					}
					if (found) {
						return true;
					}
				}
			}
			break;
		}
	}

	// If the given type is an interface then a common super interface may be found
	// in a parallel branch of the super hierarchy, so we need to verify all super interfaces.
	// If it's a class then there's only one possible branch for the hierarchy and
	// this branch has been already verified by the test above
	if (type.isInterface()) {
		ReferenceBinding[] interfaces = type.superInterfaces();
		if (interfaces == null) return false;
		for (int i = 0; i < interfaces.length; i++) {
			if (resolveLevelAsSuperInvocation(interfaces[i], argumentTypes, superTypeNames, false)) {
				return true;
			}
		}
	}
	return false;
}
@Override
public String toString() {
	return "Locator for " + this.pattern.toString(); //$NON-NLS-1$
}
}
