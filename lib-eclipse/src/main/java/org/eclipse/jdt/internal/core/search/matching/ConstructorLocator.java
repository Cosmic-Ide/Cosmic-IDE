/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedGenericMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class ConstructorLocator extends PatternLocator {

protected ConstructorPattern pattern;

public ConstructorLocator(ConstructorPattern pattern) {
	super(pattern);

	this.pattern = pattern;
}
@Override
protected int fineGrain() {
	return this.pattern.fineGrain;
}
@Override
public int match(ASTNode node, MatchingNodeSet nodeSet) { // interested in ExplicitConstructorCall
	if (!this.pattern.findReferences) return IMPOSSIBLE_MATCH;
	if (!(node instanceof ExplicitConstructorCall)) return IMPOSSIBLE_MATCH;

	if (!matchParametersCount(node, ((ExplicitConstructorCall) node).arguments)) return IMPOSSIBLE_MATCH;

	return nodeSet.addMatch(node, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
}
@Override
public int match(ConstructorDeclaration node, MatchingNodeSet nodeSet) {
	if (this.pattern.fineGrain != 0 && !this.pattern.findDeclarations) return IMPOSSIBLE_MATCH;
	int referencesLevel = this.pattern.findReferences ? matchLevelForReferences(node) : IMPOSSIBLE_MATCH;
	int declarationsLevel = this.pattern.findDeclarations ? matchLevelForDeclarations(node) : IMPOSSIBLE_MATCH;

	return nodeSet.addMatch(node, referencesLevel >= declarationsLevel ? referencesLevel : declarationsLevel); // use the stronger match
}
@Override
public int match(Expression node, MatchingNodeSet nodeSet) { // interested in AllocationExpression
	if (!this.pattern.findReferences) return IMPOSSIBLE_MATCH;
	if (!(node instanceof AllocationExpression)) return IMPOSSIBLE_MATCH;

	// constructor name is simple type name
	AllocationExpression allocation = (AllocationExpression) node;
	char[][] typeName = allocation.type.getTypeName();
	if (this.pattern.declaringSimpleName != null && !matchesName(this.pattern.declaringSimpleName, typeName[typeName.length-1]))
		return IMPOSSIBLE_MATCH;

	if (!matchParametersCount(node, allocation.arguments)) return IMPOSSIBLE_MATCH;

	return nodeSet.addMatch(node, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
}
@Override
public int match(FieldDeclaration field, MatchingNodeSet nodeSet) {
	if (!this.pattern.findReferences) return IMPOSSIBLE_MATCH;
	// look only for enum constant
	if (field.type != null || !(field.initialization instanceof AllocationExpression)) return IMPOSSIBLE_MATCH;

	AllocationExpression allocation = (AllocationExpression) field.initialization;
	if (field.binding != null && field.binding.declaringClass != null) {
		if (this.pattern.declaringSimpleName != null && !matchesName(this.pattern.declaringSimpleName, field.binding.declaringClass.sourceName()))
			return IMPOSSIBLE_MATCH;
	}

	if (!matchParametersCount(field, allocation.arguments)) return IMPOSSIBLE_MATCH;

	return nodeSet.addMatch(field, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
}
//public int match(MethodDeclaration node, MatchingNodeSet nodeSet) - SKIP IT
/**
 * Special case for message send in javadoc comment. They can be in fact bound to a constructor.
 * @see "http://bugs.eclipse.org/bugs/show_bug.cgi?id=83285"
 */
@Override
public int match(MessageSend msgSend, MatchingNodeSet nodeSet)  {
	if ((msgSend.bits & ASTNode.InsideJavadoc) == 0) return IMPOSSIBLE_MATCH;
	if (!this.pattern.findReferences) return IMPOSSIBLE_MATCH;
	if (this.pattern.declaringSimpleName == null || CharOperation.equals(msgSend.selector, this.pattern.declaringSimpleName)) {
		return nodeSet.addMatch(msgSend, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
	}
	return IMPOSSIBLE_MATCH;
}
@Override
public int match(ReferenceExpression node, MatchingNodeSet nodeSet) {
	if (!this.pattern.findReferences || node.isMethodReference()) return IMPOSSIBLE_MATCH;
	return nodeSet.addMatch(node, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
}

//public int match(Reference node, MatchingNodeSet nodeSet) - SKIP IT
@Override
public int match(TypeDeclaration node, MatchingNodeSet nodeSet) {
	if (!this.pattern.findReferences) return IMPOSSIBLE_MATCH;

	if (this.pattern.fineGrain != 0 &&
			(this.pattern.fineGrain & ~IJavaSearchConstants.METHOD_REFERENCE_EXPRESSION) == 0 )
		return IMPOSSIBLE_MATCH;

	// need to look for a generated default constructor
	return nodeSet.addMatch(node, this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH);
}
//public int match(TypeReference node, MatchingNodeSet nodeSet) - SKIP IT

protected int matchConstructor(MethodBinding constructor) {
	if (!constructor.isConstructor()) return IMPOSSIBLE_MATCH;

	// declaring type, simple name has already been matched by matchIndexEntry()
	int level = resolveLevelForType(this.pattern.declaringSimpleName, this.pattern.declaringQualification, constructor.declaringClass);
	if (level == IMPOSSIBLE_MATCH) return IMPOSSIBLE_MATCH;

	// parameter types
	int parameterCount = this.pattern.parameterCount;
	if (parameterCount > -1) {
		if (constructor.parameters == null) return INACCURATE_MATCH;
		if (parameterCount != constructor.parameters.length) return IMPOSSIBLE_MATCH;
		for (int i = 0; i < parameterCount; i++) {
			// TODO (frederic) use this call to refine accuracy on parameter types
//			int newLevel = resolveLevelForType(this.pattern.parameterSimpleNames[i], this.pattern.parameterQualifications[i], this.pattern.parametersTypeArguments[i], 0, constructor.parameters[i]);
			int newLevel = resolveLevelForType(this.pattern.parameterSimpleNames[i], this.pattern.parameterQualifications[i], constructor.parameters[i]);
			if (level > newLevel) {
				if (newLevel == IMPOSSIBLE_MATCH) {
//					if (isErasureMatch) {
//						return ERASURE_MATCH;
//					}
					return IMPOSSIBLE_MATCH;
				}
				level = newLevel; // can only be downgraded
			}
		}
	}
	return level;
}
@Override
protected int matchContainer() {
	if (this.pattern.findReferences) return ALL_CONTAINER; // handles both declarations + references & just references
	// COMPILATION_UNIT_CONTAINER - implicit constructor call: case of Y extends X and Y doesn't define any constructor
	// CLASS_CONTAINER - implicit constructor call: case of constructor declaration with no explicit super call
	// METHOD_CONTAINER - reference in another constructor
	// FIELD_CONTAINER - anonymous in a field initializer

	// declarations are only found in Class
	return CLASS_CONTAINER;
}
protected int matchLevelForReferences(ConstructorDeclaration constructor) {
	ExplicitConstructorCall constructorCall = constructor.constructorCall;
	if (constructorCall == null || constructorCall.accessMode != ExplicitConstructorCall.ImplicitSuper)
		return IMPOSSIBLE_MATCH;

	if (this.pattern.parameterSimpleNames != null) {
		int length = this.pattern.parameterSimpleNames.length;
		Expression[] args = constructorCall.arguments;
		int argsLength = args == null ? 0 : args.length;
		if (length != argsLength) return IMPOSSIBLE_MATCH;
	}
	return this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;
}
protected int matchLevelForDeclarations(ConstructorDeclaration constructor) {
	// constructor name is stored in selector field
	if (this.pattern.declaringSimpleName != null && !matchesName(this.pattern.declaringSimpleName, constructor.selector))
		return IMPOSSIBLE_MATCH;

	if (this.pattern.parameterSimpleNames != null) {
		int length = this.pattern.parameterSimpleNames.length;
		Argument[] args = constructor.arguments;
		int argsLength = args == null ? 0 : args.length;
		if (length != argsLength) return IMPOSSIBLE_MATCH;
	}

	// Verify type arguments (do not reject if pattern has no argument as it can be an erasure match)
	if (this.pattern.hasConstructorArguments()) {
		if (constructor.typeParameters == null || constructor.typeParameters.length != this.pattern.constructorArguments.length) return IMPOSSIBLE_MATCH;
	}

	return this.pattern.mustResolve ? POSSIBLE_MATCH : ACCURATE_MATCH;
}
boolean matchParametersCount(ASTNode node, Expression[] args) {
	if (this.pattern.parameterSimpleNames != null && (!this.pattern.varargs || ((node.bits & ASTNode.InsideJavadoc) != 0))) {
		int length = this.pattern.parameterCount;
		if (length < 0) length = this.pattern.parameterSimpleNames.length;
		int argsLength = args == null ? 0 : args.length;
		if (length != argsLength) {
			return false;
		}
	}
	return true;
}
@Override
protected void matchReportReference(ASTNode reference, IJavaElement element, Binding elementBinding, int accuracy, MatchLocator locator) throws CoreException {

	MethodBinding constructorBinding = null;
	boolean isSynthetic = false;
	if (reference instanceof ExplicitConstructorCall) {
		ExplicitConstructorCall call = (ExplicitConstructorCall) reference;
		isSynthetic = call.isImplicitSuper();
		constructorBinding = call.binding;
	} else if (reference instanceof AllocationExpression) {
		AllocationExpression alloc = (AllocationExpression) reference;
		constructorBinding = alloc.binding;
	} else if (reference instanceof TypeDeclaration || reference instanceof FieldDeclaration) {
		super.matchReportReference(reference, element, elementBinding, accuracy, locator);
		if (this.match != null) return;
	}

	// Create search match
	this.match = locator.newMethodReferenceMatch(element, elementBinding, accuracy, -1, -1, true, isSynthetic, reference);

	// Look to refine accuracy
	if (constructorBinding instanceof ParameterizedGenericMethodBinding) { // parameterized generic method
		// Update match regarding constructor type arguments
		ParameterizedGenericMethodBinding parameterizedMethodBinding = (ParameterizedGenericMethodBinding) constructorBinding;
		this.match.setRaw(parameterizedMethodBinding.isRaw);
		TypeBinding[] typeBindings = parameterizedMethodBinding.isRaw ? null : parameterizedMethodBinding.typeArguments;
		updateMatch(typeBindings, locator, this.pattern.constructorArguments, this.pattern.hasConstructorParameters());

		// Update match regarding declaring class type arguments
		if (constructorBinding.declaringClass.isParameterizedType() || constructorBinding.declaringClass.isRawType()) {
			ParameterizedTypeBinding parameterizedBinding = (ParameterizedTypeBinding)constructorBinding.declaringClass;
			if (!this.pattern.hasTypeArguments() && this.pattern.hasConstructorArguments() || parameterizedBinding.isParameterizedWithOwnVariables()) {
				// special case for constructor pattern which defines arguments but no type
				// in this case, we only use refined accuracy for constructor
			} else if (this.pattern.hasTypeArguments() && !this.pattern.hasConstructorArguments()) {
				// special case for constructor pattern which defines no constructor arguments but has type ones
				// in this case, we do not use refined accuracy
				updateMatch(parameterizedBinding, this.pattern.getTypeArguments(), this.pattern.hasTypeParameters(), 0, locator);
			} else {
				updateMatch(parameterizedBinding, this.pattern.getTypeArguments(), this.pattern.hasTypeParameters(), 0, locator);
			}
		} else if (this.pattern.hasTypeArguments()) {
			this.match.setRule(SearchPattern.R_ERASURE_MATCH);
		}

		// Update match regarding constructor parameters
		// TODO ? (frederic)
	} else if (constructorBinding instanceof ParameterizedMethodBinding) {
		// Update match regarding declaring class type arguments
		if (constructorBinding.declaringClass.isParameterizedType() || constructorBinding.declaringClass.isRawType()) {
			ParameterizedTypeBinding parameterizedBinding = (ParameterizedTypeBinding)constructorBinding.declaringClass;
			if (!this.pattern.hasTypeArguments() && this.pattern.hasConstructorArguments()) {
				// special case for constructor pattern which defines arguments but no type
				updateMatch(parameterizedBinding, new char[][][] {this.pattern.constructorArguments}, this.pattern.hasTypeParameters(), 0, locator);
			} else if (!parameterizedBinding.isParameterizedWithOwnVariables()) {
				updateMatch(parameterizedBinding, this.pattern.getTypeArguments(), this.pattern.hasTypeParameters(), 0, locator);
			}
		} else if (this.pattern.hasTypeArguments()) {
			this.match.setRule(SearchPattern.R_ERASURE_MATCH);
		}

		// Update match regarding constructor parameters
		// TODO ? (frederic)
	} else if (this.pattern.hasConstructorArguments()) { // binding has no type params, compatible erasure if pattern does
		this.match.setRule(SearchPattern.R_ERASURE_MATCH);
	}

	// See whether it is necessary to report or not
	if (this.match.getRule() == 0) return; // impossible match
	boolean report = (this.isErasureMatch && this.match.isErasure()) || (this.isEquivalentMatch && this.match.isEquivalent()) || this.match.isExact();
	if (!report) return;

	// Report match
	int offset = reference.sourceStart;
	this.match.setOffset(offset);
	this.match.setLength(reference.sourceEnd - offset + 1);
	if (reference instanceof FieldDeclaration) { // enum declaration
		FieldDeclaration enumConstant  = (FieldDeclaration) reference;
		if (enumConstant.initialization instanceof QualifiedAllocationExpression) {
			locator.reportAccurateEnumConstructorReference(this.match, enumConstant, (QualifiedAllocationExpression) enumConstant.initialization);
			return;
		}
	}
	locator.report(this.match);
}
@Override
public SearchMatch newDeclarationMatch(ASTNode reference, IJavaElement element, Binding binding, int accuracy, int length, MatchLocator locator) {
	this.match = null;
	int offset = reference.sourceStart;
	if (this.pattern.findReferences) {
		if (reference instanceof TypeDeclaration) {
			TypeDeclaration type = (TypeDeclaration) reference;
			AbstractMethodDeclaration[] methods = type.methods;
			if (methods != null) {
				for (int i = 0, max = methods.length; i < max; i++) {
					AbstractMethodDeclaration method = methods[i];
					boolean synthetic = method.isDefaultConstructor() && method.sourceStart < type.bodyStart;
					this.match = locator.newMethodReferenceMatch(element, binding, accuracy, offset, length, method.isConstructor(), synthetic, method);
				}
			}
		} else if (reference instanceof ConstructorDeclaration) {
			ConstructorDeclaration constructor = (ConstructorDeclaration) reference;
			ExplicitConstructorCall call = constructor.constructorCall;
			boolean synthetic = call != null && call.isImplicitSuper();
			this.match = locator.newMethodReferenceMatch(element, binding, accuracy, offset, length, constructor.isConstructor(), synthetic, constructor);
		}
	}
	if (this.match != null) {
		return this.match;
	}
	// super implementation...
    return locator.newDeclarationMatch(element, binding, accuracy, reference.sourceStart, length);
}
@Override
public int resolveLevel(ASTNode node) {
	if (this.pattern.findReferences) {
		if (node instanceof AllocationExpression)
			return resolveLevel((AllocationExpression) node);
		if (node instanceof ExplicitConstructorCall)
			return resolveLevel(((ExplicitConstructorCall) node).binding);
		if (node instanceof TypeDeclaration)
			return resolveLevel((TypeDeclaration) node);
		if (node instanceof FieldDeclaration)
			return resolveLevel((FieldDeclaration) node);
		if (node instanceof JavadocMessageSend) {
			return resolveLevel(((JavadocMessageSend)node).binding);
		}
		if (node instanceof ReferenceExpression) {
			return resolveLevel(((ReferenceExpression)node).getMethodBinding());
		}
	}
	if (node instanceof ConstructorDeclaration)
		return resolveLevel((ConstructorDeclaration) node, true);
	return IMPOSSIBLE_MATCH;
}
@Override
protected int referenceType() {
	return IJavaElement.METHOD;
}
protected int resolveLevel(AllocationExpression allocation) {
	// constructor name is simple type name
	char[][] typeName = allocation.type.getTypeName();
	if (this.pattern.declaringSimpleName != null && !matchesName(this.pattern.declaringSimpleName, typeName[typeName.length-1]))
		return IMPOSSIBLE_MATCH;

	return resolveLevel(allocation.binding);
}
protected int resolveLevel(FieldDeclaration field) {
	// only accept enum constants
	if (field.type != null || field.binding == null) return IMPOSSIBLE_MATCH;
	if (this.pattern.declaringSimpleName != null && !matchesName(this.pattern.declaringSimpleName, field.binding.type.sourceName()))
		return IMPOSSIBLE_MATCH;
	if (!(field.initialization instanceof AllocationExpression) || field.initialization.resolvedType.isLocalType()) return IMPOSSIBLE_MATCH;

	return resolveLevel(((AllocationExpression)field.initialization).binding);
}
@Override
public int resolveLevel(Binding binding) {
	if (binding == null) return INACCURATE_MATCH;
	if (!(binding instanceof MethodBinding)) return IMPOSSIBLE_MATCH;

	MethodBinding constructor = (MethodBinding) binding;
	int level= matchConstructor(constructor);
	if (level== IMPOSSIBLE_MATCH) {
		if (constructor != constructor.original()) {
			level= matchConstructor(constructor.original());
		}
	}
	return level;
}
protected int resolveLevel(ConstructorDeclaration constructor, boolean checkDeclarations) {
	int referencesLevel = IMPOSSIBLE_MATCH;
	if (this.pattern.findReferences) {
		ExplicitConstructorCall constructorCall = constructor.constructorCall;
		if (constructorCall != null && constructorCall.accessMode == ExplicitConstructorCall.ImplicitSuper) {
			// eliminate explicit super call as it will be treated with matchLevel(ExplicitConstructorCall, boolean)
			int callCount = (constructorCall.arguments == null) ? 0 : constructorCall.arguments.length;
			int patternCount = (this.pattern.parameterSimpleNames == null) ? 0 : this.pattern.parameterSimpleNames.length;
			if (patternCount != callCount) {
				referencesLevel = IMPOSSIBLE_MATCH;
			} else {
				referencesLevel = resolveLevel(constructorCall.binding);
				if (referencesLevel == ACCURATE_MATCH) return ACCURATE_MATCH; // cannot get better
			}
		}
	}
	if (!checkDeclarations) return referencesLevel;

	int declarationsLevel = this.pattern.findDeclarations ? resolveLevel(constructor.binding) : IMPOSSIBLE_MATCH;
	return referencesLevel >= declarationsLevel ? referencesLevel : declarationsLevel; // answer the stronger match
}
protected int resolveLevel(TypeDeclaration type) {
	// find default constructor
	AbstractMethodDeclaration[] methods = type.methods;
	if (methods != null) {
		for (int i = 0, length = methods.length; i < length; i++) {
			AbstractMethodDeclaration method = methods[i];
			if (method.isDefaultConstructor() && method.sourceStart < type.bodyStart) // if synthetic
				return resolveLevel((ConstructorDeclaration) method, false);
		}
	}
	return IMPOSSIBLE_MATCH;
}
@Override
public String toString() {
	return "Locator for " + this.pattern.toString(); //$NON-NLS-1$
}
}
