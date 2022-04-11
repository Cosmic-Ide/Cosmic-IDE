/*******************************************************************************
 * Copyright (c) 2013, 2017 GK Software AG.
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

/**
 * A type variable substitution strategy based on inference variables (JLS8 18.1.1)
 */
public class InferenceSubstitution extends Scope.Substitutor implements Substitution {

	private LookupEnvironment environment;
	private InferenceVariable[] variables;
	private InvocationSite[] sites;

	public InferenceSubstitution(LookupEnvironment environment, InferenceVariable[] variables, InvocationSite site) {
		this.environment = environment;
		this.variables = variables;
		this.sites = new InvocationSite[] {site};
	}

	public InferenceSubstitution(InferenceContext18 context) {
		this(context.environment, context.inferenceVariables, context.currentInvocation);
	}

	/** Answer a substitution that is able to substitute into inference variables of several inference contexts (outer & inner) */
	public InferenceSubstitution addContext(InferenceContext18 otherContext) {
		InferenceSubstitution subst = new InferenceSubstitution(this.environment, null, null) {

			@Override
			protected boolean isSameParameter(TypeBinding p1, TypeBinding originalType) {
				if (TypeBinding.equalsEquals(p1, originalType))
					return true;
				if (p1 instanceof TypeVariableBinding && originalType instanceof TypeVariableBinding) {
					// may need to 'normalize' if inner & outer have different degree of parameterization / original:
					TypeVariableBinding var1= (TypeVariableBinding) p1, var2 = (TypeVariableBinding) originalType;
					Binding declaring1 = var1.declaringElement;
					Binding declaring2 = var2.declaringElement;
					if (declaring1 instanceof MethodBinding && declaring2 instanceof MethodBinding) {
						declaring1 = ((MethodBinding) declaring1).original();
						declaring2 = ((MethodBinding) declaring2).original();
					}
					// TODO: handle TypeBinding if needed
					return declaring1 == declaring2 && var1.rank == var2.rank;
				}
				return false;
			}
		};

		int l1 = this.sites.length;
		subst.sites = new InvocationSite[l1+1];
		System.arraycopy(this.sites, 0, subst.sites, 0, l1);
		subst.sites[l1] = otherContext.currentInvocation;

		subst.variables = this.variables;

// TODO: switch to also combining variables, if needed (filter duplicates?):
//		l1 = this.variables.length;
//		int l2 = otherContext.inferenceVariables.length;
//		subst.variables = new InferenceVariable[l1+l2];
//		System.arraycopy(this.variables, 0, subst.variables, 0, l1);
//		System.arraycopy(otherContext.inferenceVariables, 0, subst.variables, l1, l2);

		return subst;
	}

	/**
	 * Override method {@link Scope.Substitutor#substitute(Substitution, TypeBinding)},
	 * to add substitution of types other than type variables.
	 */
	@Override
	public TypeBinding substitute(Substitution substitution, TypeBinding originalType) {
		for (int i = 0; i < this.variables.length; i++) {
			InferenceVariable variable = this.variables[i];
			if (variable.isFromInitialSubstitution && isInSites(variable.site) && isSameParameter(getP(i), originalType)) {
				if (this.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled && originalType.hasNullTypeAnnotations())
					return this.environment.createAnnotatedType(variable.withoutToplevelNullAnnotation(), originalType.getTypeAnnotations());
				return variable;
			}
		}

		return super.substitute(substitution, originalType);
	}

	private boolean isInSites(InvocationSite otherSite) {
		for (int i = 0; i < this.sites.length; i++)
			if (InferenceContext18.isSameSite(this.sites[i], otherSite))
				return true;
		return false;
	}

	protected boolean isSameParameter(TypeBinding p1, TypeBinding originalType) {
		return TypeBinding.equalsEquals(p1, originalType);
	}

	/**
	 * Get the type corresponding to the ith inference variable.
	 * Default behavior is to answer the inference variable's type parameter.
	 * Sub-class may override to substitute other types.
	 */
	protected TypeBinding getP(int i) {
		return this.variables[i].typeParameter;
	}

	@Override
	public TypeBinding substitute(TypeVariableBinding typeVariable) {
		ReferenceBinding superclass = typeVariable.superclass;
		ReferenceBinding[] superInterfaces = typeVariable.superInterfaces;
		boolean hasSubstituted = false;
		variableLoop: for (int i = 0; i < this.variables.length; i++) {
			InferenceVariable variable = this.variables[i];
			TypeBinding pi = getP(i);
			if (TypeBinding.equalsEquals(pi, typeVariable))
				return variable;
			if (TypeBinding.equalsEquals(pi, superclass)) {
				superclass = variable;
				hasSubstituted = true;
				continue;
			}
			if (superInterfaces != null) {
				int ifcLen = superInterfaces.length;
				for (int j = 0; j < ifcLen; j++) {
					if (TypeBinding.equalsEquals(pi, superInterfaces[j])) {
						if (superInterfaces == typeVariable.superInterfaces)
							System.arraycopy(superInterfaces, 0, superInterfaces = new ReferenceBinding[ifcLen], 0, ifcLen);
						superInterfaces[j] = variable;
						hasSubstituted = true;
						continue variableLoop;
					}
				}
			}
		}
		if (hasSubstituted) {
			typeVariable = new TypeVariableBinding(typeVariable.sourceName, typeVariable.declaringElement, typeVariable.rank, this.environment);
			typeVariable.superclass = superclass;
			typeVariable.superInterfaces = superInterfaces;
			typeVariable.firstBound = superclass != null ? superclass : superInterfaces[0];
			if (typeVariable.firstBound.hasNullTypeAnnotations())
				typeVariable.tagBits |= TagBits.HasNullTypeAnnotation;
		}
		return typeVariable;
	}

	@Override
	public LookupEnvironment environment() {
		return this.environment;
	}

	@Override
	public boolean isRawSubstitution() {
		// FIXME Auto-generated method stub
		return false;
	}
}
