/*******************************************************************************
 * Copyright (c) 2017, 2019 GK Software SE, and others.
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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;

public class SplitPackageBinding extends PackageBinding {
	Set<ModuleBinding> declaringModules;
	public Set<PlainPackageBinding> incarnations;

	/**
	 * Combine two potential package bindings, answering either the better of those if the other has a problem,
	 * or combine both into a split package.
	 * @param binding one candidate
	 * @param previous a previous candidate
	 * @param primaryModule when constructing a new SplitPackageBinding this primary module will define the
	 * 	focus when later an UnresolvedReferenceBinding is resolved relative to this SplitPackageBinding.
	 * @return one of: <code>null</code>, a regular PackageBinding or a SplitPackageBinding.
	 */
	public static PackageBinding combine(PackageBinding binding, PackageBinding previous, ModuleBinding primaryModule) {
		// if a candidate has problems, pick the "better" candidate:
		int prevRank = rank(previous);
		int curRank = rank(binding);
		if (prevRank < curRank)
			return binding;
		if (prevRank > curRank)
			return previous;
		if (previous == null)
			return null;
		// both are valid
		if (previous.subsumes(binding))
			return previous;
		if (binding.subsumes(previous))
			return binding;
		SplitPackageBinding split = new SplitPackageBinding(previous, primaryModule);
		split.add(binding);
		return split;
	}
	private static int rank(PackageBinding candidate) {
		if (candidate == null)
			return 0;
		if (candidate == LookupEnvironment.TheNotFoundPackage)
			return 1;
		if (!candidate.isValidBinding())
			return 2;
		return 3;
	}

	public SplitPackageBinding(PackageBinding initialBinding, ModuleBinding primaryModule) {
		super(initialBinding.compoundName, initialBinding.parent, primaryModule.environment, primaryModule);
		this.declaringModules = new LinkedHashSet<>();
		this.incarnations = new LinkedHashSet<>();
		add(initialBinding);
	}
	public void add(PackageBinding packageBinding) {
		if (packageBinding instanceof SplitPackageBinding) {
			SplitPackageBinding split = (SplitPackageBinding) packageBinding;
			this.declaringModules.addAll(split.declaringModules);
			for(PlainPackageBinding incarnation: split.incarnations) {
				if(this.incarnations.add(incarnation)) {
					incarnation.addWrappingSplitPackageBinding(this);
				}
			}
		} else if (packageBinding instanceof PlainPackageBinding) {
			this.declaringModules.add(packageBinding.enclosingModule);
			if(this.incarnations.add((PlainPackageBinding) packageBinding)) {
				packageBinding.addWrappingSplitPackageBinding(this);
			}
		}
	}
	@Override
	PackageBinding addPackage(PackageBinding element, ModuleBinding module) {
		char[] simpleName = element.compoundName[element.compoundName.length-1];
		// enrich
		element = combineWithSiblings(element, simpleName, module);

		PackageBinding visible = this.knownPackages.get(simpleName);
		visible = SplitPackageBinding.combine(element, visible, this.enclosingModule);
		this.knownPackages.put(simpleName, visible);

		// also record the PPB's as parent-child:
		PlainPackageBinding incarnation = getIncarnation(element.enclosingModule);
		if (incarnation != null) {
			// avoid adding an SPB as a child of a PPB:
			PlainPackageBinding elementIncarnation = element.getIncarnation(element.enclosingModule);
			if (elementIncarnation != null)
				incarnation.addPackage(elementIncarnation, module);
		}
		return element;
	}

	PackageBinding combineWithSiblings(PackageBinding childPackage, char[] name, ModuleBinding module) {
		ModuleBinding primaryModule = childPackage.enclosingModule;
		// see if other incarnations contribute to the child package, too:
		char[] flatName = CharOperation.concatWith(childPackage.compoundName, '.');
		for (PackageBinding incarnation :  this.incarnations) {
			ModuleBinding moduleBinding = incarnation.enclosingModule;
			if (moduleBinding == module)
				continue;
			if (childPackage.isDeclaredIn(moduleBinding))
				continue;
			PlainPackageBinding next = moduleBinding.getDeclaredPackage(flatName);
			childPackage = combine(next, childPackage, primaryModule);
		}
		return childPackage;
	}

	@Override
	ModuleBinding[] getDeclaringModules() {
		return this.declaringModules.toArray(new ModuleBinding[this.declaringModules.size()]);
	}

	@Override
	PackageBinding getPackage0(char[] name) {
		PackageBinding knownPackage = super.getPackage0(name);
		if (knownPackage != null)
			return knownPackage;

		PackageBinding candidate = null;
		for (PackageBinding incarnation : this.incarnations) {
			PackageBinding package0 = incarnation.getPackage0(name);
			if (package0 == null)
				return null; // if any incarnation lacks cached info, a full findPackage will be necessary
			candidate = combine(package0, candidate, this.enclosingModule);
		}
		if (candidate != null)
			this.knownPackages.put(name, candidate);

		return candidate;
	}

	@Override
	PackageBinding getPackage0Any(char[] name) {
		PackageBinding knownPackage = super.getPackage0(name);
		if (knownPackage != null)
			return knownPackage;

		PackageBinding candidate = null;
		for (PackageBinding incarnation : this.incarnations) {
			PackageBinding package0 = incarnation.getPackage0(name);
			if (package0 == null)
				continue;
			candidate = combine(package0, candidate, this.enclosingModule);
		}
		// don't cache the result, maybe incomplete
		return candidate;
	}

	@Override
	protected PackageBinding findPackage(char[] name, ModuleBinding module) {
		char[][] subpackageCompoundName = CharOperation.arrayConcat(this.compoundName, name);
		Set<PackageBinding> candidates = new LinkedHashSet<>();
		for (ModuleBinding candidateModule : this.declaringModules) {
			PackageBinding candidate = candidateModule.getVisiblePackage(subpackageCompoundName);
			if (candidate != null
					&& candidate != LookupEnvironment.TheNotFoundPackage
					&& ((candidate.tagBits & TagBits.HasMissingType) == 0))
			{
				candidates.add(candidate);
			}
		}
		int count = candidates.size();
		PackageBinding result = null;
		if (count == 1) {
			result = candidates.iterator().next();
		} else if (count > 1) {
			Iterator<PackageBinding> iterator = candidates.iterator();
			SplitPackageBinding split = new SplitPackageBinding(iterator.next(), this.enclosingModule);
			while (iterator.hasNext())
				split.add(iterator.next());
			result = split;
		}
		if (result == null)
			addNotFoundPackage(name);
		else
			addPackage(result, module);
		return result;
	}

	@Override
	public PlainPackageBinding getIncarnation(ModuleBinding requestedModule) {
		for (PlainPackageBinding incarnation : this.incarnations) {
			if (incarnation.enclosingModule == requestedModule)
				return incarnation;
		}
		return null;
	}

	@Override
	public boolean subsumes(PackageBinding binding) {
		if (!CharOperation.equals(this.compoundName, binding.compoundName))
			return false;
		if (binding instanceof SplitPackageBinding)
			return this.declaringModules.containsAll(((SplitPackageBinding) binding).declaringModules);
		else
			return this.declaringModules.contains(binding.enclosingModule);
	}

	@Override
	boolean hasType0Any(char[] name) {
		if (super.hasType0Any(name))
			return true;

		for (PackageBinding incarnation : this.incarnations) {
			if (incarnation.hasType0Any(name))
				return true;
		}
		return false;
	}

	/** Similar to getType0() but now we have a module and can ask the specific incarnation! */
	ReferenceBinding getType0ForModule(ModuleBinding module, char[] name) {
		if (this.declaringModules.contains(module))
			return getIncarnation(module).getType0(name);
		return null;
	}

	@Override
	ReferenceBinding getType(char[] name, ModuleBinding mod) {
		ReferenceBinding candidate = null;
		boolean accessible = false;
		for (PackageBinding incarnation : this.incarnations) {
			ReferenceBinding type = incarnation.getType(name, mod);
			if (type != null) {
				if (candidate == null || !accessible) {
					candidate = type;
					accessible = mod.canAccess(incarnation);
				} else if (mod.canAccess(incarnation)) {
					return new ProblemReferenceBinding(type.compoundName, candidate, ProblemReasons.Ambiguous); // TODO(SHMOD) add module information
				}
			}
		}
		if (candidate != null && !accessible)
			return new ProblemReferenceBinding(candidate.compoundName, candidate, ProblemReasons.NotAccessible); // TODO(SHMOD) more info
		// at this point we have only checked unique accessibility of the package, accessibility of the type will be checked by callers
		return candidate;
	}

	@Override
	public boolean isDeclaredIn(ModuleBinding moduleBinding) {
		return this.declaringModules.contains(moduleBinding);
	}

	@Override
	public PackageBinding getVisibleFor(ModuleBinding clientModule, boolean preferLocal) {
		int visibleCount = 0;
		PlainPackageBinding unique = null;
		for (PlainPackageBinding incarnation : this.incarnations) {
			if (incarnation.hasCompilationUnit(false)) {
				if (preferLocal && incarnation.enclosingModule == clientModule) {
					return incarnation;
				} else {
					if (clientModule.canAccess(incarnation)) {
						visibleCount++;
						unique = incarnation;
					}
				}
			}
		}
		if (visibleCount > 1)
			return this; // conflict, return split
		return unique;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(super.toString());
		buf.append(" (from "); //$NON-NLS-1$
		String sep = ""; //$NON-NLS-1$
		for (ModuleBinding mod : this.declaringModules) {
			buf.append(sep).append(mod.readableName());
			sep = ", "; //$NON-NLS-1$
		}
		buf.append(")"); //$NON-NLS-1$
		return buf.toString();
	}
}
