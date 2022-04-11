/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
 *
 *******************************************************************************/
package org.eclipse.jdt.core.dom;

import java.util.Arrays;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.core.NameLookup;
import org.eclipse.jdt.internal.core.NameLookup.Answer;
import org.eclipse.jdt.internal.core.SearchableEnvironment;

/**
 * Internal implementation of module bindings.
 * @since 3.14
 */
class ModuleBinding implements IModuleBinding {

	protected static final ITypeBinding[] NO_TYPE_BINDINGS = new ITypeBinding[0];
	private String name = null;
	private volatile String key;
	private boolean isOpen = false;

	private org.eclipse.jdt.internal.compiler.lookup.ModuleBinding binding;
	protected BindingResolver resolver;

	private IAnnotationBinding[] annotations;
	private IModuleBinding[] requiredModules;
	private IPackageBinding[] exports; // cached
	private IPackageBinding[] opens; // cached
	private ITypeBinding[] uses; // cached
	private ITypeBinding[] services; // cached

	ModuleBinding(BindingResolver resolver, org.eclipse.jdt.internal.compiler.lookup.ModuleBinding binding) {
		this.resolver = resolver;
		this.binding = binding;
		this.isOpen = binding.isOpen();
	}

	@Override
	public IAnnotationBinding[] getAnnotations() {
		if (this.annotations == null) {
			this.annotations = resolveAnnotationBindings(this.binding.getAnnotations());
		}
		return this.annotations;
	}

	private IAnnotationBinding[] resolveAnnotationBindings(org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding[] internalAnnotations) {
		int length = internalAnnotations == null ? 0 : internalAnnotations.length;
		if (length != 0) {
			IAnnotationBinding[] tempAnnotations = new IAnnotationBinding[length];
			int convertedAnnotationCount = 0;
			for (int i = 0; i < length; i++) {
				org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding internalAnnotation = internalAnnotations[i];
				if (internalAnnotation == null)
					break;
				IAnnotationBinding annotationInstance = this.resolver.getAnnotationInstance(internalAnnotation);
				if (annotationInstance == null)
					continue;
				tempAnnotations[convertedAnnotationCount++] = annotationInstance;
			}
			if (convertedAnnotationCount != length) {
				if (convertedAnnotationCount == 0) {
					return this.annotations = AnnotationBinding.NoAnnotations;
				}
				System.arraycopy(tempAnnotations, 0, (tempAnnotations = new IAnnotationBinding[convertedAnnotationCount]), 0, convertedAnnotationCount);
			}
			return tempAnnotations;
		}
		return AnnotationBinding.NoAnnotations;
	}

	@Override
	public String getName() {
		if (this.name == null) {
			char[] tmp = this.binding.moduleName;
			return tmp != null && tmp.length != 0 ? new String(tmp) : Util.EMPTY_STRING;
		}
		return this.name;
	}

	@Override
	public int getModifiers() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isDeprecated() {
		return false;
	}

	@Override
	public boolean isRecovered() {
		return false;
	}

	@Override
	public boolean isSynthetic() {
		// TODO Auto-generated method stub
		// TODO Java 9 no reference seen in jvms draft - only in sotm
		// check on version change and after compiler ast implements isSynthetic return this.binding.isSynthetic();

		return false;
	}

	@Override
	public IJavaElement getJavaElement() {
		INameEnvironment nameEnvironment = this.binding.environment.nameEnvironment;
		if (!(nameEnvironment instanceof SearchableEnvironment)) return null;
		NameLookup nameLookup = ((SearchableEnvironment) nameEnvironment).nameLookup;
		if (nameLookup == null) return null;
		Answer answer = nameLookup.findModule(this.getName().toCharArray());
		if (answer == null) return null;
		return answer.module;
	}

	@Override
	public String getKey() {
		if (this.key == null) {
			char[] k = this.binding.computeUniqueKey();
			this.key = k == null || k == CharOperation.NO_CHAR ? Util.EMPTY_STRING : new String(k);
		}
		return this.key;
	}

	@Override
	public boolean isEqualTo(IBinding other) {
		if (other == this) // identical binding - equal (key or no key)
			return true;
		if (other == null) // other binding missing
			return false;

		if (!(other instanceof ModuleBinding))
			return false;

		org.eclipse.jdt.internal.compiler.lookup.ModuleBinding otherBinding = ((ModuleBinding) other).binding;
		return BindingComparator.isEqual(this.binding, otherBinding);
	}

	@Override
	public boolean isOpen() {
		return this.isOpen;
	}
	@Override
	public IModuleBinding[] getRequiredModules() {
		if (this.requiredModules != null)
			return this.requiredModules;

		org.eclipse.jdt.internal.compiler.lookup.ModuleBinding[] reqs = this.binding.getRequires();
		IModuleBinding[] result = new IModuleBinding[reqs != null ? reqs.length : 0];
		for (int i = 0, l = result.length; i < l; ++i) {
			org.eclipse.jdt.internal.compiler.lookup.ModuleBinding req = reqs[i];
			result[i] = req != null ? this.resolver.getModuleBinding(req) : null;
		}
		return this.requiredModules = result;
	}

	@Override
	public IPackageBinding[] getExportedPackages() {
		if (this.exports == null) {
			org.eclipse.jdt.internal.compiler.lookup.PackageBinding[] compilerExports = this.binding.getExports();
			this.exports = Arrays.stream(compilerExports)
					.map(e -> this.resolver.getPackageBinding(e))
					.toArray(IPackageBinding[]::new);
		}
		return this.exports;
	}

	@Override
	public String[] getExportedTo(IPackageBinding packageBinding) {
		return this.binding.getExportRestrictions(((PackageBinding) packageBinding).getCompilerBinding());
	}

	@Override
	public IPackageBinding[] getOpenedPackages() {
		if (this.opens == null) {
			org.eclipse.jdt.internal.compiler.lookup.PackageBinding[] compilerOpens = this.binding.getOpens();
			this.opens = Arrays.stream(compilerOpens)
					.map(o -> this.resolver.getPackageBinding(o))
					.toArray(IPackageBinding[]::new);
		}
		return this.opens;
	}

	@Override
	public String[] getOpenedTo(IPackageBinding packageBinding) {
		return this.binding.getOpenRestrictions(((PackageBinding) packageBinding).getCompilerBinding());
	}

	/*
	 * helper method
	 */
	private ITypeBinding[] getTypes(org.eclipse.jdt.internal.compiler.lookup.TypeBinding[] types) {
		int length = types == null ? 0 : types.length;
		TypeBinding[] result = new TypeBinding[length];
		for (int i = 0; i < length; ++i) {
			result[i] = (TypeBinding) this.resolver.getTypeBinding(types[i]);
		}
		return result;
	}

	@Override
	public ITypeBinding[] getUses() {
		if (this.uses == null)
			this.uses = getTypes(this.binding.getUses());
		return this.uses;
	}

	@Override
	public ITypeBinding[] getServices() {
		if (this.services == null)
			this.services = getTypes(this.binding.getServices());
		return this.services;
	}
	@Override
	public ITypeBinding[] getImplementations(ITypeBinding service) {
		return getTypes(this.binding.getImplementations(((TypeBinding) service).binding));
	}
	/**
	 * For debugging purpose only.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.binding.toString();
	}
}