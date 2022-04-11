/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.ast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReasons;
import org.eclipse.jdt.internal.compiler.lookup.ProblemReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;

public class ProvidesStatement extends ModuleStatement {

	public TypeReference serviceInterface;
	public TypeReference[] implementations;

	public boolean resolve(BlockScope scope) {
		ModuleDeclaration module = scope.referenceCompilationUnit().moduleDeclaration;
		ModuleBinding src = module.binding;
		TypeBinding infBinding = this.serviceInterface.resolveType(scope);
		boolean hasErrors = false;
		if (infBinding == null || !infBinding.isValidBinding()) {
			return false;
		}
		if (!(infBinding.isClass() || infBinding.isInterface() || infBinding.isAnnotationType())) {
			scope.problemReporter().invalidServiceRef(IProblem.InvalidServiceIntfType, this.serviceInterface);
		}
		ReferenceBinding intf = (ReferenceBinding) this.serviceInterface.resolvedType;
		Set<TypeBinding> impls = new HashSet<>();
		for (int i = 0; i < this.implementations.length; i++) {
			ReferenceBinding impl = (ReferenceBinding) this.implementations[i].resolveType(scope);
			if (impl == null || !impl.isValidBinding() || !impl.canBeSeenBy(scope)) {
				hasErrors = true;
				continue;
			}
			if (!impls.add(impl)) {
				scope.problemReporter().duplicateTypeReference(IProblem.DuplicateServices, this.implementations[i]);
				continue;
			}
			int problemId = ProblemReasons.NoError;
			ModuleBinding declaringModule = impl.module();

			if (declaringModule != src) {
				problemId = IProblem.ServiceImplNotDefinedByModule;
			} else if (!impl.isClass() && !impl.isInterface()) {
				problemId = IProblem.InvalidServiceImplType;
			} else if (impl.isNestedType() && !impl.isStatic()) {
				problemId = IProblem.NestedServiceImpl;
			} else {
				MethodBinding provider = impl.getExactMethod(TypeConstants.PROVIDER, Binding.NO_PARAMETERS, scope.compilationUnitScope());
				if (provider != null && (!provider.isValidBinding() || !(provider.isPublic() && provider.isStatic()))) {
					provider = null;
				}
				TypeBinding implType = impl;
				if (provider != null) {
					implType = provider.returnType;
					if (implType instanceof ReferenceBinding && !implType.canBeSeenBy(scope)) {
						ReferenceBinding referenceBinding = (ReferenceBinding) implType;
						scope.problemReporter().invalidType(this.implementations[i], new ProblemReferenceBinding(
								referenceBinding.compoundName, referenceBinding, ProblemReasons.NotVisible));
						hasErrors = true;
					}
				} else {
					if (impl.isAbstract()) {
						problemId = IProblem.AbstractServiceImplementation;
					} else {
						MethodBinding defaultConstructor = impl.getExactConstructor(Binding.NO_PARAMETERS);
						if (defaultConstructor == null || !defaultConstructor.isValidBinding()) {
							problemId = IProblem.ProviderMethodOrConstructorRequiredForServiceImpl;
						} else if (!defaultConstructor.isPublic()) {
							problemId = IProblem.ServiceImplDefaultConstructorNotPublic;
						}
					}
				}
				if (implType.findSuperTypeOriginatingFrom(intf) == null) {
					scope.problemReporter().typeMismatchError(implType, intf, this.implementations[i], null);
					hasErrors = true;
				}
			}
			if (problemId != ProblemReasons.NoError) {
				scope.problemReporter().invalidServiceRef(problemId, this.implementations[i]);
				hasErrors = true;
			}
		}
		return hasErrors;
	}

	public List<TypeBinding> getResolvedImplementations() {
		List<TypeBinding> resolved = new ArrayList<>();
		if (this.implementations != null) {
			for (TypeReference implRef : this.implementations) {
				TypeBinding one = implRef.resolvedType;
				if (one != null)
					resolved.add(one);
			}
		}
		return resolved;
	}

	@Override
	public StringBuffer print(int indent, StringBuffer output) {
		printIndent(indent, output);
		output.append("provides "); //$NON-NLS-1$
		this.serviceInterface.print(0, output);
		//output.append(" "); //$NON-NLS-1$
		//printIndent(indent + 1, output);
		output.append(" with "); //$NON-NLS-1$
		for (int i = 0; i < this.implementations.length; i++) {
			this.implementations[i].print(0, output);
			if (i < this.implementations.length - 1)
				output.append(", "); //$NON-NLS-1$
		}
		output.append(";"); //$NON-NLS-1$
		return output;
	}
}
