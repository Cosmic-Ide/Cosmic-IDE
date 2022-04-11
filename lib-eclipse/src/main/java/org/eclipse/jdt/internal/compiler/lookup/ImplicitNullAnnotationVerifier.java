/*******************************************************************************
 * Copyright (c) 2012, 2020 GK Software SE, IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NullAnnotationMatching;
import org.eclipse.jdt.internal.compiler.ast.NullAnnotationMatching.CheckMode;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

/**
 * Extracted slice from MethodVerifier15, which is responsible only for implicit null annotations.
 * First, if enabled, it detects overridden methods from which null annotations are inherited.
 * Next, also default nullness is filled into remaining empty slots.
 * After all implicit annotations have been filled in compatibility is checked and problems are complained.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ImplicitNullAnnotationVerifier {
	public static void ensureNullnessIsKnown(MethodBinding methodBinding, Scope scope) {
		if ((methodBinding.tagBits & TagBits.IsNullnessKnown) == 0) {
			LookupEnvironment environment2 = scope.environment();
			// ensure nullness of methodBinding is known (but we are not interested in reporting problems against methodBinding)
			new ImplicitNullAnnotationVerifier(environment2, environment2.globalOptions.inheritNullAnnotations)
					.checkImplicitNullAnnotations(methodBinding, null/*srcMethod*/, false, scope);
		}
	}


	/**
	 * Simple record to store nullness info for one argument or return type
	 * while iterating over a set of overridden methods.
	 */
	static class InheritedNonNullnessInfo {
		Boolean inheritedNonNullness;
		MethodBinding annotationOrigin;
		boolean complained;
	}

	// delegate which to ask for recursive analysis of super methods
	// can be 'this', but is never a MethodVerifier (to avoid infinite recursion).
	ImplicitNullAnnotationVerifier buddyImplicitNullAnnotationsVerifier;
	private boolean inheritNullAnnotations;
	protected LookupEnvironment environment;


	public ImplicitNullAnnotationVerifier(LookupEnvironment environment, boolean inheritNullAnnotations) {
		this.buddyImplicitNullAnnotationsVerifier = this;
		this.inheritNullAnnotations = inheritNullAnnotations;
		this.environment = environment;
	}

	// for sub-classes:
	ImplicitNullAnnotationVerifier(LookupEnvironment environment) {
		CompilerOptions options = environment.globalOptions;
		this.buddyImplicitNullAnnotationsVerifier = new ImplicitNullAnnotationVerifier(environment, options.inheritNullAnnotations);
		this.inheritNullAnnotations = options.inheritNullAnnotations;
		this.environment = environment;
	}

	/**
	 * Check and fill in implicit annotations from overridden methods and from default.
	 * Precondition: caller has checked whether annotation-based null analysis is enabled.
	 */
	public void checkImplicitNullAnnotations(MethodBinding currentMethod, AbstractMethodDeclaration srcMethod, boolean complain, Scope scope) {
		// check inherited nullness from superclass and superInterfaces
		try {
			ReferenceBinding currentType = currentMethod.declaringClass;
			if (currentType.id == TypeIds.T_JavaLangObject) {
				return;
			}
			boolean usesTypeAnnotations = scope.environment().usesNullTypeAnnotations();
			boolean needToApplyReturnNonNullDefault =
					currentMethod.hasNonNullDefaultForReturnType(srcMethod);
			ParameterNonNullDefaultProvider needToApplyParameterNonNullDefault =
					currentMethod.hasNonNullDefaultForParameter(srcMethod);
			boolean needToApplyNonNullDefault = needToApplyReturnNonNullDefault | needToApplyParameterNonNullDefault.hasAnyNonNullDefault();
			// compatibility & inheritance do not consider constructors / static methods:
			boolean isInstanceMethod = !currentMethod.isConstructor() && !currentMethod.isStatic();
			complain &= isInstanceMethod;
			if (!needToApplyNonNullDefault
					&& !complain
					&& !(this.inheritNullAnnotations && isInstanceMethod)) {
				return; // short cut, no work to be done
			}

			if (isInstanceMethod) {
				List superMethodList = new ArrayList();

				// need super types connected:
				if (currentType instanceof SourceTypeBinding && !currentType.isHierarchyConnected() && !currentType.isAnonymousType()) {
					((SourceTypeBinding) currentType).scope.connectTypeHierarchy();
				}

				int paramLen = currentMethod.parameters.length;
				findAllOverriddenMethods(currentMethod.original(), currentMethod.selector, paramLen,
								currentType, new HashSet(), superMethodList);

				// prepare interim storage for nullness info so we don't pollute currentMethod before we know its conflict-free:
				InheritedNonNullnessInfo[] inheritedNonNullnessInfos = new InheritedNonNullnessInfo[paramLen+1]; // index 0 is for the return type
				for (int i=0; i<paramLen+1; i++) inheritedNonNullnessInfos[i] = new InheritedNonNullnessInfo();

				int length = superMethodList.size();
				for (int i = length; --i >= 0;) {
					MethodBinding currentSuper = (MethodBinding) superMethodList.get(i);
					if ((currentSuper.tagBits & TagBits.IsNullnessKnown) == 0) {
						// recurse to prepare currentSuper
						checkImplicitNullAnnotations(currentSuper, null, false, scope); // TODO (stephan) complain=true if currentSuper is source method??
					}
					checkNullSpecInheritance(currentMethod, srcMethod, needToApplyReturnNonNullDefault, needToApplyParameterNonNullDefault, complain, currentSuper, null, scope, inheritedNonNullnessInfos);
					needToApplyNonNullDefault = false;
				}

				// transfer collected information into currentMethod:
				InheritedNonNullnessInfo info = inheritedNonNullnessInfos[0];
				if (!info.complained) {
					long tagBits = 0;
					if (info.inheritedNonNullness == Boolean.TRUE) {
						tagBits = TagBits.AnnotationNonNull;
					} else if (info.inheritedNonNullness == Boolean.FALSE) {
						tagBits = TagBits.AnnotationNullable;
					}
					if (tagBits != 0) {
						if (!usesTypeAnnotations) {
							currentMethod.tagBits |= tagBits;
						} else {
							if (!currentMethod.returnType.isBaseType()) {
								LookupEnvironment env = scope.environment();
								currentMethod.returnType = env.createAnnotatedType(currentMethod.returnType, env.nullAnnotationsFromTagBits(tagBits));
							}
						}
					}
				}
				for (int i=0; i<paramLen; i++) {
					info = inheritedNonNullnessInfos[i+1];
					if (!info.complained && info.inheritedNonNullness != null) {
						Argument currentArg = srcMethod == null ? null : srcMethod.arguments[i];
						if (!usesTypeAnnotations)
							recordArgNonNullness(currentMethod, paramLen, i, currentArg, info.inheritedNonNullness);
						else
							recordArgNonNullness18(currentMethod, i, currentArg, info.inheritedNonNullness, scope.environment());
					}
				}

			}
			if (needToApplyNonNullDefault) {
				if (!usesTypeAnnotations)
					currentMethod.fillInDefaultNonNullness(srcMethod, needToApplyReturnNonNullDefault, needToApplyParameterNonNullDefault);
				else
					currentMethod.fillInDefaultNonNullness18(srcMethod, scope.environment());
			}
		} finally {
			currentMethod.tagBits |= TagBits.IsNullnessKnown;
		}
	}

	/*
	 * Recursively traverse the tree of ancestors but whenever we find a matching method prune the super tree.
	 * Collect all matching methods in 'result'.
	 */
	private void findAllOverriddenMethods(MethodBinding original, char[] selector, int suggestedParameterLength,
			ReferenceBinding currentType, Set ifcsSeen, List result)
	{
		if (currentType.id == TypeIds.T_JavaLangObject)
			return;

		// superclass:
		ReferenceBinding superclass = currentType.superclass();
		if (superclass == null)
			return; // pseudo root of inheritance, happens in eval contexts
		collectOverriddenMethods(original, selector, suggestedParameterLength, superclass, ifcsSeen, result);

		// superInterfaces:
		ReferenceBinding[] superInterfaces = currentType.superInterfaces();
		int ifcLen = superInterfaces.length;
		for (int i = 0; i < ifcLen; i++) {
			ReferenceBinding currentIfc = superInterfaces[i];
			if (ifcsSeen.add(currentIfc.original())) {	// process each interface at most once
				collectOverriddenMethods(original, selector, suggestedParameterLength, currentIfc, ifcsSeen, result);
			}
		}
	}

	/* collect matching methods from one supertype. */
	private void collectOverriddenMethods(MethodBinding original, char[] selector, int suggestedParameterLength,
			ReferenceBinding superType, Set ifcsSeen, List result)
	{
		MethodBinding [] ifcMethods = superType.unResolvedMethods();
		int length = ifcMethods.length;
		boolean added = false;
		for  (int i=0; i<length; i++) {
			MethodBinding currentMethod = ifcMethods[i];
			if (!CharOperation.equals(selector, currentMethod.selector))
				continue;
			if (!currentMethod.doesParameterLengthMatch(suggestedParameterLength))
				continue;
			if (currentMethod.isStatic())
				continue;
			if (MethodVerifier.doesMethodOverride(original, currentMethod, this.environment)) {
				result.add(currentMethod);
				added = true; // when overriding one or more methods from superType don't traverse to transitive superTypes
			}
		}
		if (!added)
			findAllOverriddenMethods(original, selector, suggestedParameterLength, superType, ifcsSeen, result);
	}

	/**
	 * The main algorithm in this class.
	 * @param currentMethod focus method
	 * @param srcMethod AST of 'currentMethod' if present
	 * @param hasReturnNonNullDefault is a @NonNull default applicable for the return type of currentMethod?
	 * @param hasParameterNonNullDefault is a @NonNull default applicable for parameters of currentMethod?
	 * @param shouldComplain should we report any errors found?
	 *   (see also comment about flows into this method, below).
	 * @param inheritedMethod one overridden method from a super type
	 * @param allInheritedMethods look here to see if nonnull-unannotated conflict already exists in one super type
	 * @param scope provides context for error reporting etc.
	 * @param inheritedNonNullnessInfos if non-null, this array of non-null elements is used for
	 * 	 interim recording of nullness information from inheritedMethod rather than prematurely updating currentMethod.
	 *   Index position 0 is used for the return type, positions i+1 for argument i.
	 */
	void checkNullSpecInheritance(MethodBinding currentMethod, AbstractMethodDeclaration srcMethod,
			boolean hasReturnNonNullDefault, ParameterNonNullDefaultProvider hasParameterNonNullDefault, boolean shouldComplain,
			MethodBinding inheritedMethod, MethodBinding[] allInheritedMethods, Scope scope, InheritedNonNullnessInfo[] inheritedNonNullnessInfos)
	{
		if(currentMethod.declaringClass.id == TypeIds.T_JavaLangObject) {
			// all method implementations in java.lang.Object return non-null results and accept nullable as parameter.
			return;
		}
		// Note that basically two different flows lead into this method:
		// (1) during MethodVerifyer15.checkMethods() we want to report errors (against srcMethod or against the current type)
		//     In this case this method is directly called from MethodVerifier15 (checkAgainstInheritedMethod / checkConcreteInheritedMethod)
		// (2) during on-demand invocation we are mainly interested in the side effects of copying inherited null annotations
		//     In this case this method is called via checkImplicitNullAnnotations from
		//     - MessageSend.resolveType(..)
		//     - SourceTypeBinding.createArgumentBindings(..)
		//     - recursive calls within this class
		//     Still we *might* want to complain about problems found (controlled by 'complain')

		if ((inheritedMethod.tagBits & TagBits.IsNullnessKnown) == 0) {
			// TODO (stephan): even here we may need to report problems? How to discriminate?
			this.buddyImplicitNullAnnotationsVerifier.checkImplicitNullAnnotations(inheritedMethod, null, false, scope);
		}
		boolean useTypeAnnotations = this.environment.usesNullTypeAnnotations();
		long inheritedNullnessBits = getReturnTypeNullnessTagBits(inheritedMethod, useTypeAnnotations);
		long currentNullnessBits = getReturnTypeNullnessTagBits(currentMethod, useTypeAnnotations);

		boolean shouldInherit = this.inheritNullAnnotations;

		// return type:
		returnType: {
			if (currentMethod.returnType == null || currentMethod.returnType.isBaseType())
				break returnType; // no nullness for primitive types
			if (currentNullnessBits == 0) {
				// unspecified, may fill in either from super or from default
				if (shouldInherit) {
					if (inheritedNullnessBits != 0) {
						if (hasReturnNonNullDefault) {
							// both inheritance and default: check for conflict?
							if (shouldComplain && inheritedNullnessBits == TagBits.AnnotationNullable)
								scope.problemReporter().conflictingNullAnnotations(currentMethod, ((MethodDeclaration) srcMethod).returnType, inheritedMethod);
							// 	still use the inherited bits to avoid incompatibility
						}
						if (inheritedNonNullnessInfos != null && srcMethod != null) {
							recordDeferredInheritedNullness(scope, ((MethodDeclaration) srcMethod).returnType,
									inheritedMethod, Boolean.valueOf(inheritedNullnessBits == TagBits.AnnotationNonNull), inheritedNonNullnessInfos[0]);
						} else {
							// no need to defer, record this info now:
							applyReturnNullBits(currentMethod, inheritedNullnessBits);
						}
						break returnType; // compatible by construction, skip complain phase below
					}
				}
				if (hasReturnNonNullDefault && (!useTypeAnnotations || currentMethod.returnType.acceptsNonNullDefault())) { // conflict with inheritance already checked
					currentNullnessBits = TagBits.AnnotationNonNull;
					applyReturnNullBits(currentMethod, currentNullnessBits);
				}
			}
			if (shouldComplain) {
				if ((inheritedNullnessBits & TagBits.AnnotationNonNull) != 0
						&& currentNullnessBits != TagBits.AnnotationNonNull)
				{
					if (srcMethod != null) {
						scope.problemReporter().illegalReturnRedefinition(srcMethod, inheritedMethod,
																	this.environment.getNonNullAnnotationName());
						break returnType;
					} else {
						scope.problemReporter().cannotImplementIncompatibleNullness(scope.referenceContext(), currentMethod, inheritedMethod, useTypeAnnotations);
						return;
					}
				}
				if (useTypeAnnotations) {
					TypeBinding substituteReturnType = null; // for TVB identity checks inside NullAnnotationMatching.analyze()
					TypeVariableBinding[] typeVariables = inheritedMethod.original().typeVariables;
					if (typeVariables != null && currentMethod.returnType.id != TypeIds.T_void) {
						ParameterizedGenericMethodBinding substitute = this.environment.createParameterizedGenericMethod(currentMethod, typeVariables);
						substituteReturnType = substitute.returnType;
					}
					if (NullAnnotationMatching.analyse(inheritedMethod.returnType, currentMethod.returnType, substituteReturnType, null, 0, null, CheckMode.OVERRIDE_RETURN).isAnyMismatch()) {
						if (srcMethod != null)
							scope.problemReporter().illegalReturnRedefinition(srcMethod, inheritedMethod,
																	this.environment.getNonNullAnnotationName());
						else
							scope.problemReporter().cannotImplementIncompatibleNullness(scope.referenceContext(), currentMethod, inheritedMethod, useTypeAnnotations);
						return;
					}
				}
			}
		}

		// parameters:
		TypeBinding[] substituteParameters = null; // for TVB identity checks inside NullAnnotationMatching.analyze()
		if (shouldComplain) {
			TypeVariableBinding[] typeVariables = currentMethod.original().typeVariables;
			if (typeVariables != Binding.NO_TYPE_VARIABLES) {
				ParameterizedGenericMethodBinding substitute = this.environment.createParameterizedGenericMethod(inheritedMethod, typeVariables);
				substituteParameters = substitute.parameters;
			}
		}

		Argument[] currentArguments = srcMethod == null ? null : srcMethod.arguments;

		int length = 0;
		if (currentArguments != null)
			length = currentArguments.length;
		if (useTypeAnnotations) // need to look for type annotations on all parameters:
			length = currentMethod.parameters.length;
		else if (inheritedMethod.parameterNonNullness != null)
			length = inheritedMethod.parameterNonNullness.length;
		else if (currentMethod.parameterNonNullness != null)
			length = currentMethod.parameterNonNullness.length;

		parameterLoop:
		for (int i = 0; i < length; i++) {
			if (currentMethod.parameters[i].isBaseType()) continue;

			Argument currentArgument = currentArguments == null
										? null : currentArguments[i];
			Boolean inheritedNonNullNess = getParameterNonNullness(inheritedMethod, i, useTypeAnnotations);
			Boolean currentNonNullNess = getParameterNonNullness(currentMethod, i, useTypeAnnotations);

			if (currentNonNullNess == null) {
				// unspecified, may fill in either from super or from default
				if (inheritedNonNullNess != null) {
					if (shouldInherit) {
						if (hasParameterNonNullDefault.hasNonNullDefaultForParam(i)) {
							// both inheritance and default: check for conflict?
							if (shouldComplain
									&& inheritedNonNullNess == Boolean.FALSE
									&& currentArgument != null)
							{
								scope.problemReporter().conflictingNullAnnotations(currentMethod, currentArgument, inheritedMethod);
							}
							// 	still use the inherited info to avoid incompatibility
						}
						if (inheritedNonNullnessInfos != null && srcMethod != null) {
							recordDeferredInheritedNullness(scope, srcMethod.arguments[i].type,
									inheritedMethod, inheritedNonNullNess, inheritedNonNullnessInfos[i+1]);
						} else {
							// no need to defer, record this info now:
							if (!useTypeAnnotations)
								recordArgNonNullness(currentMethod, length, i, currentArgument, inheritedNonNullNess);
							else
								recordArgNonNullness18(currentMethod, i, currentArgument, inheritedNonNullNess, this.environment);
						}
						continue; // compatible by construction, skip complain phase below
					}
				}
				if (hasParameterNonNullDefault.hasNonNullDefaultForParam(i)) { // conflict with inheritance already checked
					currentNonNullNess = Boolean.TRUE;
					if (!useTypeAnnotations)
						recordArgNonNullness(currentMethod, length, i, currentArgument, Boolean.TRUE);
					else if (currentMethod.parameters[i].acceptsNonNullDefault())
						recordArgNonNullness18(currentMethod, i, currentArgument, Boolean.TRUE, this.environment);
					else
						currentNonNullNess = null; // cancel if parameter doesn't accept the default
				}
			}
			if (shouldComplain) {
				char[][] annotationName;
				if (inheritedNonNullNess == Boolean.TRUE) {
					annotationName = this.environment.getNonNullAnnotationName();
				} else {
					annotationName = this.environment.getNullableAnnotationName();
				}
				if (inheritedNonNullNess != Boolean.TRUE		// super parameter is not restricted to @NonNull
						&& currentNonNullNess == Boolean.TRUE)	// current parameter is restricted to @NonNull
				{
					// incompatible
					if (currentArgument != null) {
						scope.problemReporter().illegalRedefinitionToNonNullParameter(
								currentArgument,
								inheritedMethod.declaringClass,
								(inheritedNonNullNess == null) ? null : this.environment.getNullableAnnotationName());
					} else {
						scope.problemReporter().cannotImplementIncompatibleNullness(scope.referenceContext(), currentMethod, inheritedMethod, false);
					}
					continue;
				} else if (currentNonNullNess == null)
				{
					// unannotated strictly conflicts only with inherited @Nullable
					if (inheritedNonNullNess == Boolean.FALSE) {
						if (currentArgument != null) {
							scope.problemReporter().parameterLackingNullableAnnotation(
									currentArgument,
									inheritedMethod.declaringClass,
									annotationName);
						} else {
							scope.problemReporter().cannotImplementIncompatibleNullness(scope.referenceContext(), currentMethod, inheritedMethod, false);
						}
						continue;
					} else if (inheritedNonNullNess == Boolean.TRUE) {
						// not strictly a conflict, but a configurable warning is given anyway:
						if (allInheritedMethods != null) {
							// avoid this optional warning if the conflict already existed in one supertype (merging of two methods into one?)
							for (MethodBinding one : allInheritedMethods)
								if (TypeBinding.equalsEquals(inheritedMethod.declaringClass, one.declaringClass) && getParameterNonNullness(one, i, useTypeAnnotations) != Boolean.TRUE)
									continue parameterLoop;
						}
						if (currentArgument != null) {
							scope.problemReporter().parameterLackingNonnullAnnotation(currentArgument, inheritedMethod.declaringClass, annotationName);
						} else {
							TypeDeclaration type = scope.classScope().referenceContext;
							ASTNode location = type.superclass != null ? type.superclass : type;
							scope.problemReporter().inheritedParameterLackingNonnullAnnotation(currentMethod, i+1, inheritedMethod.declaringClass, location, annotationName);
						}
						continue;
					}
				}
				if (useTypeAnnotations) {
					TypeBinding inheritedParameter = inheritedMethod.parameters[i];
					TypeBinding substituteParameter = substituteParameters != null ? substituteParameters[i] : null;
					if (NullAnnotationMatching.analyse(currentMethod.parameters[i], inheritedParameter, substituteParameter, null, 0, null, CheckMode.OVERRIDE).isAnyMismatch()) {
						if (currentArgument != null)
							scope.problemReporter().illegalParameterRedefinition(currentArgument, inheritedMethod.declaringClass, inheritedParameter);
						else
							scope.problemReporter().cannotImplementIncompatibleNullness(scope.referenceContext(), currentMethod, inheritedMethod, false);
					}
				}
			}
		}

		if (shouldComplain && useTypeAnnotations && srcMethod != null) {
			TypeVariableBinding[] currentTypeVariables = currentMethod.typeVariables();
			TypeVariableBinding[] inheritedTypeVariables = inheritedMethod.typeVariables();
			if (currentTypeVariables != Binding.NO_TYPE_VARIABLES && currentTypeVariables.length == inheritedTypeVariables.length) {
				for (int i = 0; i < currentTypeVariables.length; i++) {
					TypeVariableBinding inheritedVariable = inheritedTypeVariables[i];
					if (NullAnnotationMatching.analyse(inheritedVariable, currentTypeVariables[i], null, null, -1, null, CheckMode.BOUND_CHECK).isAnyMismatch())
						scope.problemReporter().cannotRedefineTypeArgumentNullity(inheritedVariable, inheritedMethod, srcMethod.typeParameters()[i]);
				}
			}
		}
	}

	void applyReturnNullBits(MethodBinding method, long nullnessBits) {
		if (this.environment.usesNullTypeAnnotations()) {
			if (!method.returnType.isBaseType()) {
				method.returnType = this.environment.createAnnotatedType(method.returnType, this.environment.nullAnnotationsFromTagBits(nullnessBits));
			}
		} else {
			method.tagBits |= nullnessBits;
		}
	}

	private Boolean getParameterNonNullness(MethodBinding method, int i, boolean useTypeAnnotations) {
		if (useTypeAnnotations) {
			TypeBinding parameter = method.parameters[i];
			if (parameter != null) {
				long nullBits = NullAnnotationMatching.validNullTagBits(parameter.tagBits);
				if (nullBits != 0L)
					return Boolean.valueOf(nullBits == TagBits.AnnotationNonNull);
			}
			return null;
		}
		return (method.parameterNonNullness == null)
						? null : method.parameterNonNullness[i];
	}

	private long getReturnTypeNullnessTagBits(MethodBinding method, boolean useTypeAnnotations) {
		if (useTypeAnnotations) {
			if (method.returnType == null)
				return 0L;
			return NullAnnotationMatching.validNullTagBits(method.returnType.tagBits);
		}
		return method.tagBits & TagBits.AnnotationNullMASK;
	}

	/* check for conflicting annotations and record here the info 'inheritedNonNullness' found in 'inheritedMethod'. */
	protected void recordDeferredInheritedNullness(Scope scope, ASTNode location,
			MethodBinding inheritedMethod, Boolean inheritedNonNullness,
			InheritedNonNullnessInfo nullnessInfo)
	{
		if (nullnessInfo.inheritedNonNullness != null && nullnessInfo.inheritedNonNullness != inheritedNonNullness) {
			scope.problemReporter().conflictingInheritedNullAnnotations(location,
					nullnessInfo.inheritedNonNullness.booleanValue(), nullnessInfo.annotationOrigin,
					inheritedNonNullness.booleanValue(), inheritedMethod);
			nullnessInfo.complained = true;
			// leave previous info intact, so subsequent errors are reported against the same first method
		} else {
			nullnessInfo.inheritedNonNullness = inheritedNonNullness;
			nullnessInfo.annotationOrigin = inheritedMethod;
		}
	}

	/* record declared nullness of a parameter into the method and into the argument (if present). */
	void recordArgNonNullness(MethodBinding method, int paramCount, int paramIdx, Argument currentArgument, Boolean nonNullNess) {
		if (method.parameterNonNullness == null)
			method.parameterNonNullness = new Boolean[paramCount];
		method.parameterNonNullness[paramIdx] = nonNullNess;
		if (currentArgument != null) {
			currentArgument.binding.tagBits |= nonNullNess.booleanValue() ?
					TagBits.AnnotationNonNull : TagBits.AnnotationNullable;
		}
	}
	void recordArgNonNullness18(MethodBinding method, int paramIdx, Argument currentArgument, Boolean nonNullNess, LookupEnvironment env) {
		AnnotationBinding annotationBinding = nonNullNess.booleanValue() ? env.getNonNullAnnotation() : env.getNullableAnnotation();
		method.parameters[paramIdx] = env.createAnnotatedType(method.parameters[paramIdx], new AnnotationBinding[]{ annotationBinding});
		if (currentArgument != null) {
			currentArgument.binding.type = method.parameters[paramIdx];
		}
	}

	// ==== minimal set of utility methods previously from MethodVerifier15: ====

	static boolean areParametersEqual(MethodBinding one, MethodBinding two) {
		TypeBinding[] oneArgs = one.parameters;
		TypeBinding[] twoArgs = two.parameters;
		if (oneArgs == twoArgs) return true;

		int length = oneArgs.length;
		if (length != twoArgs.length) return false;


		// methods with raw parameters are considered equal to inherited methods
		// with parameterized parameters for backwards compatibility, need a more complex check
		int i;
		foundRAW: for (i = 0; i < length; i++) {
			if (!areTypesEqual(oneArgs[i], twoArgs[i])) {
				if (oneArgs[i].leafComponentType().isRawType()) {
					if (oneArgs[i].dimensions() == twoArgs[i].dimensions() && oneArgs[i].leafComponentType().isEquivalentTo(twoArgs[i].leafComponentType())) {
						// raw mode does not apply if the method defines its own type variables
						if (one.typeVariables != Binding.NO_TYPE_VARIABLES)
							return false;
						// one parameter type is raw, hence all parameters types must be raw or non generic
						// otherwise we have a mismatch check backwards
						for (int j = 0; j < i; j++)
							if (oneArgs[j].leafComponentType().isParameterizedTypeWithActualArguments())
								return false;
						// switch to all raw mode
						break foundRAW;
					}
				}
				return false;
			}
		}
		// all raw mode for remaining parameters (if any)
		for (i++; i < length; i++) {
			if (!areTypesEqual(oneArgs[i], twoArgs[i])) {
				if (oneArgs[i].leafComponentType().isRawType())
					if (oneArgs[i].dimensions() == twoArgs[i].dimensions() && oneArgs[i].leafComponentType().isEquivalentTo(twoArgs[i].leafComponentType()))
						continue;
				return false;
			} else if (oneArgs[i].leafComponentType().isParameterizedTypeWithActualArguments()) {
				return false; // no remaining parameter can be a Parameterized type (if one has been converted then all RAW types must be converted)
			}
		}
		return true;
	}
	static boolean areTypesEqual(TypeBinding one, TypeBinding two) {
		if (TypeBinding.equalsEquals(one, two)) return true;
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=329584
		switch(one.kind()) {
			case Binding.TYPE:
				switch (two.kind()) {
					case Binding.PARAMETERIZED_TYPE:
					case Binding.RAW_TYPE:
						if (TypeBinding.equalsEquals(one, two.erasure()))
							return true;
				}
				break;
			case Binding.RAW_TYPE:
			case Binding.PARAMETERIZED_TYPE:
				switch(two.kind()) {
					case Binding.TYPE:
						if (TypeBinding.equalsEquals(one.erasure(), two))
							return true;
				}
		}

		// need to consider X<?> and X<? extends Object> as the same 'type'
		if (one.isParameterizedType() && two.isParameterizedType())
			return one.isEquivalentTo(two) && two.isEquivalentTo(one);

		// Can skip this since we resolved each method before comparing it, see computeSubstituteMethod()
		//	if (one instanceof UnresolvedReferenceBinding)
		//		return ((UnresolvedReferenceBinding) one).resolvedType == two;
		//	if (two instanceof UnresolvedReferenceBinding)
		//		return ((UnresolvedReferenceBinding) two).resolvedType == one;
		return false; // all other type bindings are identical
	}
}
