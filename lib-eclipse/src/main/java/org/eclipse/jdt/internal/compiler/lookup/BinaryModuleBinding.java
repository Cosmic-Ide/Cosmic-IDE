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

import java.util.LinkedHashMap;
import java.util.stream.Stream;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryModule;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.IModule.IModuleReference;
import org.eclipse.jdt.internal.compiler.env.IModule.IPackageExport;
import org.eclipse.jdt.internal.compiler.env.IModule.IService;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.compiler.env.IModuleAwareNameEnvironment;

public class BinaryModuleBinding extends ModuleBinding {

	private static class AutomaticModuleBinding extends ModuleBinding {

		boolean autoNameFromManifest;
		boolean hasScannedPackages;

		public AutomaticModuleBinding(IModule module, LookupEnvironment existingEnvironment) {
			super(module.name(), existingEnvironment);
			existingEnvironment.root.knownModules.put(this.moduleName, this);
			this.isAuto = true;
			this.autoNameFromManifest = module.isAutoNameFromManifest();
			this.requires = Binding.NO_MODULES;
			this.requiresTransitive = Binding.NO_MODULES;
			this.exportedPackages = Binding.NO_PLAIN_PACKAGES;
			this.hasScannedPackages = false;
		}
		@Override
		public boolean hasUnstableAutoName() {
			return !this.autoNameFromManifest;
		}
		@Override
		public ModuleBinding[] getRequiresTransitive() {
			if (this.requiresTransitive == NO_MODULES) {
				char[][] autoModules = ((IModuleAwareNameEnvironment)this.environment.nameEnvironment).getAllAutomaticModules();
				this.requiresTransitive = Stream.of(autoModules)
					.filter(name -> !CharOperation.equals(name, this.moduleName))
					.map(name -> this.environment.getModule(name)).filter(m -> m != null)
					.toArray(ModuleBinding[]::new);
			}
			return this.requiresTransitive;
		}
		@Override
		PlainPackageBinding getDeclaredPackage(char[] flatName) {
			if (!this.hasScannedPackages) {
				for (char[] packageName : (((IModuleAwareNameEnvironment)this.environment.nameEnvironment).listPackages(nameForCUCheck()))) {
					getOrCreateDeclaredPackage(CharOperation.splitOn('.', packageName));
				}
				this.hasScannedPackages = true;
			}
			return this.declaredPackages.get(flatName);
		}

		@Override
		public char[] nameForLookup() {
			return ANY_NAMED;
		}

		@Override
		public char[] nameForCUCheck() {
			return this.moduleName;
		}
	}

	private IPackageExport[] unresolvedExports;
	private IPackageExport[] unresolvedOpens;
	private char[][] unresolvedUses;
	private IService[] unresolvedProvides;

	/**
	 * Construct a named module from binary, could be an auto module - or from an info from Java Model.
	 * <p>
	 * <strong>precondition:</strong> module must be either IBinaryModule or IModule.AutoModule
	 * </p>
	 * <p>
	 * <strong>Side effects:</strong> adds the new module to root.knownModules and resolves its directives.
	 * </p>
	 */
	public static ModuleBinding create(IModule module, LookupEnvironment existingEnvironment) {
		if (module.isAutomatic())
			return new AutomaticModuleBinding(module, existingEnvironment);
		return new BinaryModuleBinding((IBinaryModule) module, existingEnvironment);
	}

	private BinaryModuleBinding(IBinaryModule module, LookupEnvironment existingEnvironment) {
		super(module.name(), existingEnvironment);
		existingEnvironment.root.knownModules.put(this.moduleName, this);
		cachePartsFrom(module);
	}

	void cachePartsFrom(IBinaryModule module) {
		if (module.isOpen())
			this.modifiers |= ClassFileConstants.ACC_OPEN;
		this.tagBits |= module.getTagBits();

		IModuleReference[] requiresReferences = module.requires();
		this.requires = new ModuleBinding[requiresReferences.length];
		this.requiresTransitive = new ModuleBinding[requiresReferences.length];
		int count = 0;
		int transitiveCount = 0;
		for (int i = 0; i < requiresReferences.length; i++) {
			ModuleBinding requiredModule = this.environment.getModule(requiresReferences[i].name());
			if (requiredModule != null) {
				this.requires[count++] = requiredModule;
				if (requiresReferences[i].isTransitive())
					this.requiresTransitive[transitiveCount++] = requiredModule;
			}
			// TODO(SHMOD): handle null case
		}
		if (count < this.requires.length)
			System.arraycopy(this.requires, 0, this.requires = new ModuleBinding[count], 0, count);
		if (transitiveCount < this.requiresTransitive.length)
			System.arraycopy(this.requiresTransitive, 0, this.requiresTransitive = new ModuleBinding[transitiveCount], 0, transitiveCount);

		this.unresolvedExports = module.exports();
		this.unresolvedOpens = module.opens();
		this.unresolvedUses = module.uses();
		this.unresolvedProvides = module.provides();
		if (this.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
			scanForNullDefaultAnnotation(module);
		}
		if ((this.tagBits & TagBits.AnnotationDeprecated) != 0 || this.environment.globalOptions.storeAnnotations) {
			this.setAnnotations(BinaryTypeBinding.createAnnotations(module.getAnnotations(), this.environment, null), true);
		}
	}

	private void scanForNullDefaultAnnotation(IBinaryModule binaryModule) {
		// trimmed-down version of BinaryTypeBinding.scanForNullDefaultAnnotation()
		char[][] nonNullByDefaultAnnotationName = this.environment.getNonNullByDefaultAnnotationName();
		if (nonNullByDefaultAnnotationName == null)
			return; // not well-configured to use null annotations

		IBinaryAnnotation[] annotations = binaryModule.getAnnotations();
		if (annotations != null) {
			int nullness = NO_NULL_DEFAULT;
			int length = annotations.length;
			for (int i = 0; i < length; i++) {
				char[] annotationTypeName = annotations[i].getTypeName();
				if (annotationTypeName[0] != Util.C_RESOLVED)
					continue;
				int typeBit = this.environment.getNullAnnotationBit(BinaryTypeBinding.signature2qualifiedTypeName(annotationTypeName));
				if (typeBit == TypeIds.BitNonNullByDefaultAnnotation) {
					// using NonNullByDefault we need to inspect the details of the value() attribute:
					nullness |= BinaryTypeBinding.getNonNullByDefaultValue(annotations[i], this.environment);
				}
			}
			this.defaultNullness = nullness;
		}
	}

	@Override
	public PlainPackageBinding[] getExports() {
		if (this.exportedPackages == null && this.unresolvedExports != null)
			resolvePackages();
		return super.getExports();
	}

	@Override
	public PlainPackageBinding[] getOpens() {
		if (this.openedPackages == null && this.unresolvedOpens != null)
			resolvePackages();
		return super.getOpens();
	}

	private void resolvePackages() {
		this.exportedPackages = new PlainPackageBinding[this.unresolvedExports.length];
		int count = 0;
		for (int i = 0; i < this.unresolvedExports.length; i++) {
			IPackageExport export = this.unresolvedExports[i];
			// when resolving "exports" in a binary module we simply assume the package must exist,
			// since this has been checked already when compiling that module.
			PlainPackageBinding declaredPackage = getOrCreateDeclaredPackage(CharOperation.splitOn('.', export.name()));
			this.exportedPackages[count++] = declaredPackage;
			declaredPackage.isExported = Boolean.TRUE;
			recordExportRestrictions(declaredPackage, export.targets());
		}
		if (count < this.exportedPackages.length)
			System.arraycopy(this.exportedPackages, 0, this.exportedPackages = new PlainPackageBinding[count], 0, count);

		this.openedPackages = new PlainPackageBinding[this.unresolvedOpens.length];
		count = 0;
		for (int i = 0; i < this.unresolvedOpens.length; i++) {
			IPackageExport opens = this.unresolvedOpens[i];
			PlainPackageBinding declaredPackage = getOrCreateDeclaredPackage(CharOperation.splitOn('.', opens.name()));
			this.openedPackages[count++] = declaredPackage;
			recordOpensRestrictions(declaredPackage, opens.targets());
		}
		if (count < this.openedPackages.length)
			System.arraycopy(this.openedPackages, 0, this.openedPackages = new PlainPackageBinding[count], 0, count);
	}

	@Override
	PlainPackageBinding getDeclaredPackage(char[] flatName) {
		getExports(); // triggers initialization of exported packages into declaredPackages
		completeIfNeeded(UpdateKind.PACKAGE);
		return super.getDeclaredPackage(flatName);
	}

	@Override
	public TypeBinding[] getUses() {
		if (this.uses == null) {
			this.uses = new TypeBinding[this.unresolvedUses.length];
			for (int i = 0; i < this.unresolvedUses.length; i++)
				this.uses[i] = this.environment.getType(CharOperation.splitOn('.', this.unresolvedUses[i]), this);
		}
		return super.getUses();
	}

	@Override
	public TypeBinding[] getServices() {
		if (this.services == null)
			resolveServices();
		return super.getServices();
	}

	@Override
	public TypeBinding[] getImplementations(TypeBinding binding) {
		if (this.implementations == null)
			resolveServices();
		return super.getImplementations(binding);
	}
	private void resolveServices() {
		this.services = new TypeBinding[this.unresolvedProvides.length];
		this.implementations = new LinkedHashMap<>();
		for (int i = 0; i < this.unresolvedProvides.length; i++) {
			this.services[i] = this.environment.getType(CharOperation.splitOn('.', this.unresolvedProvides[i].name()), this);
			char[][] implNames = this.unresolvedProvides[i].with();
			TypeBinding[] impls = new TypeBinding[implNames.length];
			for (int j = 0; j < implNames.length; j++)
				impls[j] = this.environment.getType(CharOperation.splitOn('.', implNames[j]), this);
			this.implementations.put(this.services[i], impls);
		}
	}
	@Override
	public AnnotationBinding[] getAnnotations() {
		return retrieveAnnotations(this);
	}
}
