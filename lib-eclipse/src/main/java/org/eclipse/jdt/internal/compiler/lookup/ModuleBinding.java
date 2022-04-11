/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.IModuleAwareNameEnvironment;
import org.eclipse.jdt.internal.compiler.env.IUpdatableModule;
import org.eclipse.jdt.internal.compiler.util.HashtableOfPackage;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.compiler.util.SimpleSetOfCharArray;

/**
 * This class serves a dual purpose
 * <p>
 * First, it canonically represents modules in the world of bindings.
 * </p>
 * <p>
 * Secondly, it adds a graph layer on top of {@link LookupEnvironment}:
 * ModuleBindins are linked through "read" edges as per JPMS (see java.lang.module.Configuration).
 * Additionally, each ModuleBinding holds its own instance of LookupEnviroment,
 * capturing all packages and types that are visible to the current module.
 * As a subset of all visible types, the ModuleBinding knows the set of
 * packages locally declared in this module.
 * </p>
 */
public class ModuleBinding extends Binding implements IUpdatableModule {

	/** Name of the unnamed module. */
	public static final char[] UNNAMED = "".toCharArray(); //$NON-NLS-1$
	/** Name to represent unnamed modules in --add-exports & --add-reads options. */
	public static final char[] ALL_UNNAMED = "ALL-UNNAMED".toCharArray(); //$NON-NLS-1$
	/** Module name for package/type lookup that doesn't care about modules. */
	public static final char[] ANY = "".toCharArray(); //$NON-NLS-1$
	/** Module name for package/type lookup that should look into all named modules. */
	public static final char[] ANY_NAMED = "".toCharArray(); //$NON-NLS-1$
	/** Module name for an unobservable module */
	public static final char[] UNOBSERVABLE = "".toCharArray();  //$NON-NLS-1$

	public static class UnNamedModule extends ModuleBinding {

		private static final char[] UNNAMED_READABLE_NAME = "<unnamed>".toCharArray(); //$NON-NLS-1$

		UnNamedModule(LookupEnvironment env) {
			super(env);
		}
		@Override
		public ModuleBinding[] getAllRequiredModules() {
			return Binding.NO_MODULES;
		}
		@Override
		public boolean canAccess(PackageBinding pkg) {
			if (pkg instanceof SplitPackageBinding) {
				for (PackageBinding p : ((SplitPackageBinding) pkg).incarnations) {
					if (canAccess(p)) {
						return true;
					}
				}
				return false;
			} else {
				ModuleBinding mod = pkg.enclosingModule;
				if (mod != null && mod != this)
					return mod.isPackageExportedTo(pkg, this);
			}
			return true;
		}
		@Override
		public boolean isPackageExportedTo(PackageBinding pkg, ModuleBinding client) {
			// per JLS 7.7.5 an unnamed module exports all its packages
			return pkg.isDeclaredIn(this) && pkg.hasCompilationUnit(false);
		}
		@Override
		PlainPackageBinding getDeclaredPackage(char[] flatName) {
			PlainPackageBinding declaredPackage = super.getDeclaredPackage(flatName);
			if (declaredPackage == null && this.environment.useModuleSystem) {
				IModuleAwareNameEnvironment moduleEnv = (IModuleAwareNameEnvironment) this.environment.nameEnvironment;
				char[][] compoundName = CharOperation.splitOn('.', flatName);
				char[][] declaringModuleNames = moduleEnv.getUniqueModulesDeclaringPackage(compoundName, nameForLookup());
				if (declaringModuleNames != null && CharOperation.containsEqual(declaringModuleNames, this.moduleName)) {
					declaredPackage = getOrCreateDeclaredPackage(compoundName);
				}
			}
			return declaredPackage;
		}
		@Override
		public boolean isUnnamed() {
			return true;
		}
		@Override
		public char[] nameForLookup() {
			return ANY;
		}
		@Override
		public char[] nameForCUCheck() {
			return UNNAMED;
		}
		@Override
		public char[] readableName() {
			return UNNAMED_READABLE_NAME;
		}
		@Override
		public String toString() {
			return "The Unnamed Module"; //$NON-NLS-1$
		}
	}
	public char[] moduleName;
	protected ModuleBinding[] requires;
	protected ModuleBinding[] requiresTransitive;
	protected PlainPackageBinding[] exportedPackages;
	private Map<PlainPackageBinding,SimpleSetOfCharArray> exportRestrictions; // RHS is unresolved names, because unresolvable names are legal in this position
	protected PlainPackageBinding[] openedPackages;
	private Map<PlainPackageBinding,SimpleSetOfCharArray> openRestrictions; // RHS is unresolved names, because unresolvable names are legal in this position
	protected TypeBinding[] uses;
	protected TypeBinding[] services;
	public Map<TypeBinding,TypeBinding[]> implementations;
	public char[] mainClassName;
	private SimpleSetOfCharArray packageNames;
	public int modifiers;
	public LookupEnvironment environment;
	public long tagBits;
	public int defaultNullness = NO_NULL_DEFAULT;
	ModuleBinding[] requiredModules = null;
	boolean isAuto = false;
	private boolean[] isComplete = new boolean[UpdateKind.values().length];
	private Set<ModuleBinding> transitiveRequires;
	SimpleLookupTable storedAnnotations = null;

	/**
	 * Packages declared in this module (indexed by qualified name).
	 * We consider a package as declared in a module,
	 * if a compilation unit associated with the module
	 * declares the package or a subpackage thereof.
	 * <p>
	 * A package in this structures is always represented by a {@link PlainPackageBinding},
	 * as opposed to {@link SplitPackageBinding}, which are only maintained in the trees
	 * below {@link LookupEnvironment#knownPackages}.
	 * <p>
	 * This structure is populated early during compilation with all packages that
	 * are referenced in exports and opens directives, plus their parent packages.
	 * </p>
	 */
	public HashtableOfPackage<PlainPackageBinding> declaredPackages;

	/** Constructor for the unnamed module. */
	ModuleBinding(LookupEnvironment env) {
		this.moduleName = ModuleBinding.UNNAMED;
		this.environment = env;
		this.requires = Binding.NO_MODULES;
		this.requiresTransitive = Binding.NO_MODULES;
		this.exportedPackages = Binding.NO_PLAIN_PACKAGES;
		this.openedPackages = Binding.NO_PLAIN_PACKAGES;
		this.declaredPackages = new HashtableOfPackage<PlainPackageBinding>();
		Arrays.fill(this.isComplete, true);
	}
	/* For error binding and sub class SourceModuleBinding. */
	ModuleBinding(char[] moduleName) {
		this.moduleName = moduleName;
		this.requires = Binding.NO_MODULES;
		this.requiresTransitive = Binding.NO_MODULES;
		this.exportedPackages = Binding.NO_PLAIN_PACKAGES;
		this.openedPackages = Binding.NO_PLAIN_PACKAGES;
		this.uses = Binding.NO_TYPES;
		this.services = Binding.NO_TYPES;
		this.declaredPackages = new HashtableOfPackage<PlainPackageBinding>(5);
	}

	/* For sub class BinaryModuleBinding */
	protected ModuleBinding(char[] moduleName, LookupEnvironment existingEnvironment) {
		this.moduleName = moduleName;
		this.requires = Binding.NO_MODULES;
		this.requiresTransitive = Binding.NO_MODULES;
		this.environment = new LookupEnvironment(existingEnvironment.root, this);
		this.declaredPackages = new HashtableOfPackage<PlainPackageBinding>(5);
	}

	public PlainPackageBinding[] getExports() {
		completeIfNeeded(UpdateKind.PACKAGE);
		return this.exportedPackages;
	}
	public String[] getExportRestrictions(PackageBinding pack) {
		completeIfNeeded(UpdateKind.PACKAGE);
		if (this.exportRestrictions != null) {
			SimpleSetOfCharArray set = this.exportRestrictions.get(pack);
			if (set != null) {
				char[][] names = new char[set.elementSize][];
				set.asArray(names);
				return CharOperation.charArrayToStringArray(names);
			}
		}
		return CharOperation.NO_STRINGS;
	}
	public PlainPackageBinding[] getOpens() {
		completeIfNeeded(UpdateKind.PACKAGE);
		return this.openedPackages;
	}
	public String[] getOpenRestrictions(PackageBinding pack) {
		completeIfNeeded(UpdateKind.PACKAGE);
		if (this.openRestrictions != null) {
			SimpleSetOfCharArray set = this.openRestrictions.get(pack);
			if (set != null) {
				char[][] names = new char[set.elementSize][];
				set.asArray(names);
				return CharOperation.charArrayToStringArray(names);
			}
		}
		return CharOperation.NO_STRINGS;
	}
	public TypeBinding[] getImplementations(TypeBinding binding) {
		if (this.implementations != null) {
			return this.implementations.get(binding);
		}
		return null;
	}
	public ModuleBinding[] getRequires() {
		completeIfNeeded(UpdateKind.MODULE);
		return this.requires;
	}
	public ModuleBinding[] getRequiresTransitive() {
		completeIfNeeded(UpdateKind.MODULE);
		return this.requiresTransitive;
	}

	public TypeBinding[] getUses() {
		return this.uses;
	}

	public TypeBinding[] getServices() {
		return this.services;
	}

	void completeIfNeeded(IUpdatableModule.UpdateKind kind) {
		if (!this.isComplete[kind.ordinal()]) {
			this.isComplete[kind.ordinal()] = true;
			if (this.environment.nameEnvironment instanceof IModuleAwareNameEnvironment) {
				((IModuleAwareNameEnvironment) this.environment.nameEnvironment).applyModuleUpdates(this, kind);
			}
		}
	}

	// ---  Implement IUpdatableModule: ---

	@Override
	public void addReads(char[] requiredModuleName) {
		ModuleBinding requiredModule = this.environment.getModule(requiredModuleName);
		if (requiredModule != null) {
			int len = this.requires.length;
			if (len == 0) {
				this.requires = new ModuleBinding[] { requiredModule };
			} else {
				System.arraycopy(this.requires, 0, this.requires = new ModuleBinding[len+1], 0, len);
				this.requires[len] = requiredModule;
			}
		} else {
			this.environment.problemReporter.missingModuleAddReads(requiredModuleName);
			return;
		}
	}
	@Override
	public void addExports(char[] packageName, char[][] targetModules) {
		PlainPackageBinding declaredPackage = getOrCreateDeclaredPackage(CharOperation.splitOn('.', packageName));
		if (declaredPackage != null && declaredPackage.isValidBinding())
			addResolvedExport(declaredPackage, targetModules);
	}

	@Override
	public void setMainClassName(char[] mainClassName) {
		this.mainClassName = mainClassName;
	}

	@Override
	public void setPackageNames(SimpleSetOfCharArray packageNames) {
		this.packageNames = packageNames;
	}

	// for code gen:
	/** @return array of names, which may contain nulls. */
	public char[][] getPackageNamesForClassFile() {
		if (this.packageNames == null)
			return null;
		for (PlainPackageBinding packageBinding : this.exportedPackages)
			this.packageNames.add(packageBinding.readableName());
		for (PlainPackageBinding packageBinding : this.openedPackages)
			this.packageNames.add(packageBinding.readableName());
		if (this.implementations != null)
			for (TypeBinding[] types : this.implementations.values())
				for (TypeBinding typeBinding : types)
					this.packageNames.add(((ReferenceBinding)typeBinding).fPackage.readableName());
		return this.packageNames.values;
	}

	// ---
	PlainPackageBinding createDeclaredToplevelPackage(char[] name) {
		PlainPackageBinding packageBinding = new PlainPackageBinding(name, this.environment, this);
		this.declaredPackages.put(name, packageBinding);
		return packageBinding;
	}

	PlainPackageBinding createDeclaredPackage(char[][] compoundName, PackageBinding parent) {
		PlainPackageBinding packageBinding = new PlainPackageBinding(compoundName, parent, this.environment, this);
		this.declaredPackages.put(CharOperation.concatWith(compoundName, '.'), packageBinding);
		return packageBinding;
	}

	public PlainPackageBinding getOrCreateDeclaredPackage(char[][] compoundName) {
		char[] flatName = CharOperation.concatWith(compoundName, '.');
		PlainPackageBinding pkgBinding = this.declaredPackages.get(flatName);
		if (pkgBinding != null)
			return pkgBinding;
		if (compoundName.length > 1) {
			PlainPackageBinding parent = getOrCreateDeclaredPackage(CharOperation.subarray(compoundName, 0, compoundName.length-1));
			pkgBinding = new PlainPackageBinding(compoundName, parent, this.environment, this);
			parent.addPackage(pkgBinding, this);
		} else {
			pkgBinding = new PlainPackageBinding(compoundName[0], this.environment, this);
			PackageBinding problemPackage = this.environment.getPackage0(compoundName[0]);
			if (problemPackage == LookupEnvironment.TheNotFoundPackage)
				this.environment.knownPackages.put(compoundName[0], null); // forget TheNotFoundPackage if package was detected late (e.g. with APT in the loop)
		}
		this.declaredPackages.put(flatName, pkgBinding);
		return pkgBinding;
	}

	public void addResolvedExport(PlainPackageBinding declaredPackage, char[][] targetModules) {
		if (declaredPackage == null || !declaredPackage.isValidBinding()) {
			// TODO(SHMOD) use a problem binding (if needed by DOM clients)? See https://bugs.eclipse.org/518794#c13
			return;
		}
		if (this.exportedPackages == null || this.exportedPackages.length == 0) {
			this.exportedPackages = new PlainPackageBinding[] { declaredPackage };
		} else {
			int len = this.exportedPackages.length;
			System.arraycopy(this.exportedPackages, 0, this.exportedPackages = new PlainPackageBinding[len+1], 0, len);
			this.exportedPackages[len] = declaredPackage;
		}
		declaredPackage.isExported = Boolean.TRUE;
		recordExportRestrictions(declaredPackage, targetModules);
	}

	public void addResolvedOpens(PlainPackageBinding declaredPackage, char[][] targetModules) {
		int len = this.openedPackages.length;
		if (declaredPackage == null || !declaredPackage.isValidBinding()) {
			// TODO(SHMOD) use a problem binding (if needed by DOM clients)? See https://bugs.eclipse.org/518794#c13
			return;
		}
		if (len == 0) {
			this.openedPackages = new PlainPackageBinding[] { declaredPackage };
		} else {
			System.arraycopy(this.openedPackages, 0, this.openedPackages = new PlainPackageBinding[len+1], 0, len);
			this.openedPackages[len] = declaredPackage;
		}
		recordOpensRestrictions(declaredPackage, targetModules);
	}

	protected void recordExportRestrictions(PlainPackageBinding exportedPackage, char[][] targetModules) {
		if (targetModules != null && targetModules.length > 0) {
			SimpleSetOfCharArray targetModuleSet = null;
			if (this.exportRestrictions != null) {
				targetModuleSet = this.exportRestrictions.get(exportedPackage);
			} else {
				this.exportRestrictions = new HashMap<>();
			}
			if (targetModuleSet == null) {
				targetModuleSet = new SimpleSetOfCharArray(targetModules.length);
				this.exportRestrictions.put(exportedPackage, targetModuleSet);
			}
			for (int i = 0; i < targetModules.length; i++) {
				targetModuleSet.add(targetModules[i]);
			}
		}
	}

	protected void recordOpensRestrictions(PlainPackageBinding openedPackage, char[][] targetModules) {
		if (targetModules != null && targetModules.length > 0) {
			SimpleSetOfCharArray targetModuleSet = null;
			if (this.openRestrictions != null) {
				targetModuleSet = this.openRestrictions.get(openedPackage);
			} else {
				this.openRestrictions = new HashMap<>();
			}
			if (targetModuleSet == null) {
				targetModuleSet = new SimpleSetOfCharArray(targetModules.length);
				this.openRestrictions.put(openedPackage, targetModuleSet);
			}
			for (int i = 0; i < targetModules.length; i++) {
				targetModuleSet.add(targetModules[i]);
			}
		}
	}

	Stream<ModuleBinding> getRequiredModules(boolean transitiveOnly) {
		return Stream.of(transitiveOnly ? this.getRequiresTransitive() : this.getRequires());
	}
	private void collectAllDependencies(Set<ModuleBinding> deps) {
		getRequiredModules(false).forEach(m -> {
			if (deps.add(m)) {
				m.collectAllDependencies(deps);
			}
		});
	}
	private void collectTransitiveDependencies(Set<ModuleBinding> deps) {
		getRequiredModules(true).forEach(m -> {
			if (deps.add(m)) {
				m.collectTransitiveDependencies(deps);
			}
		});
	}

	// All modules required by this module, either directly or indirectly
	public Supplier<Collection<ModuleBinding>> dependencyGraphCollector() {
		return () -> getRequiredModules(false)
			.collect(HashSet::new,
				(set, mod) -> {
					set.add(mod);
					mod.collectAllDependencies(set);
				},
				HashSet::addAll);
	}
	// All direct and transitive dependencies of this module
	public Supplier<Collection<ModuleBinding>> dependencyCollector() {
		return () -> getRequiredModules(false)
			.collect(HashSet::new,
				(set, mod) -> {
					set.add(mod);
					mod.collectTransitiveDependencies(set);
				},
				HashSet::addAll);
	}

	/**
	 * Get all the modules required by this module
	 * All required modules include modules explicitly specified as required in the module declaration
	 * as well as implicit dependencies - those specified as ' requires transitive ' by one of the
	 * dependencies
	 *
	 * @return
	 *   An array of all required modules
	 */
	public ModuleBinding[] getAllRequiredModules() {
		if (this.requiredModules != null)
			return this.requiredModules;

		Collection<ModuleBinding> allRequires = dependencyCollector().get();
		if (allRequires.contains(this)) {
			return NO_MODULES; // avoid entering unbounded recursion due to cyclic requires
		}
		ModuleBinding javaBase = this.environment.javaBaseModule();
																			// add java.base?
		if (!CharOperation.equals(this.moduleName, TypeConstants.JAVA_BASE)	// ... not if this *is* java.base
				&& javaBase != null 										// ... nor when java.base is absent
				&& javaBase != this.environment.UnNamedModule)				// ..... or faked by the unnamed module
		{
			allRequires.add(javaBase);
		}
		return this.requiredModules = allRequires.size() > 0 ? allRequires.toArray(new ModuleBinding[allRequires.size()]) : Binding.NO_MODULES;
	}

	/** Answer the name of this module. The unnamed module is identified by {@link #UNNAMED}. */
	@Override
	public char[] name() {
		return this.moduleName;
	}

	/**
	 * Answer the name of this module as it should be used for package or type lookup.
	 * Unnamed and automatic modules answer {@link #ANY} or {@link #ANY_NAMED} resp.,
	 * to signal that lookup should search in all accessible (named) modules.
	 */
	public char[] nameForLookup() {
		return this.moduleName;
	}

	/**
	 * Answer the name of this module as it should be used for hasCompilationUnit() checks.
	 */
	public char[] nameForCUCheck() {
		return nameForLookup();
	}

	/**
	 * Check if the specified package is owned by the current module and exported to the client module.
	 * True if the package appears in the list of exported packages and when the export is targeted,
	 * the module appears in the targets of the exports statement.
	 * @param pkg - the package whose visibility is to be checked
	 * @param client - the module that wishes to use the package
	 * @return true if the package is visible to the client module, false otherwise
	 */
	public boolean isPackageExportedTo(PackageBinding pkg, ModuleBinding client) {
		// TODO(SHMOD): cache the result?
		PackageBinding resolved = pkg.getIncarnation(this);
		if (resolved != null) {
			if (this.isAuto) { // all packages are exported by an automatic module
				return pkg.enclosingModule == this; // no transitive export
			}
			PackageBinding[] initializedExports = getExports();
			for (int i = 0; i < initializedExports.length; i++) {
				PackageBinding export = initializedExports[i];
				if (export.subsumes(resolved)) {
					if (this.exportRestrictions != null) {
						SimpleSetOfCharArray restrictions = this.exportRestrictions.get(export);
						if (restrictions != null) {
							if (client.isUnnamed())
								return restrictions.includes(ALL_UNNAMED);
							else
								return restrictions.includes(client.name());
						}
					}
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return a package binding if there exists a package named name in this module's context and it can be seen by this module.
	 * A package can be seen by this module if it is declared in this module or any other module read by this module
	 * (JLS 7.4.3 for packages based on JLS 7.3 for compilation units).
	 * Package exports are not considered for visibility check (only when checking "uniquely visible" (JLS 7.4.3)).
	 * <p>
	 * The returned package may be a {@link SplitPackageBinding}, if more than one package of the given name is visible.
	 * </p>
	 * <p>
	 * When asked via the unnamed module or an automatic module all other named modules are considered visible.
	 * </p>
	 */
	public PackageBinding getTopLevelPackage(char[] name) {
		return getVisiblePackage(null, name);
	}

	PlainPackageBinding getDeclaredPackage(char[] flatName) {
		return this.declaredPackages.get(flatName);
	}

	// Given parent is visible in this module, see if there is sub package named name visible in this module
	PackageBinding getVisiblePackage(PackageBinding parent, char[] name) {
		// check caches in PackageBinding/LookupEnvironment, which contain the full SplitPackageBinding if appropriate:
		PackageBinding pkg;
		if (parent != null)
			pkg = parent.getPackage0(name);
		else
			pkg = this.environment.getPackage0(name);
		if (pkg != null) {
			if (pkg == LookupEnvironment.TheNotFoundPackage)
				return null;
			else
				return pkg;
		}

		// check cached plain PackageBinding in declaredPackages (which may need combining with siblings):
		char[][] parentName = parent == null ? CharOperation.NO_CHAR_CHAR : parent.compoundName;
		char[][] subPkgCompoundName = CharOperation.arrayConcat(parentName, name);
		char[] fullFlatName = CharOperation.concatWith(subPkgCompoundName, '.');
		PackageBinding binding = this.declaredPackages.get(fullFlatName);

		char[][] declaringModuleNames = null;
		if (this.environment.useModuleSystem) {
			IModuleAwareNameEnvironment moduleEnv = (IModuleAwareNameEnvironment) this.environment.nameEnvironment;
			declaringModuleNames = moduleEnv.getUniqueModulesDeclaringPackage(subPkgCompoundName, nameForLookup());
			if (binding == null) {
				if (declaringModuleNames != null) {
					if (CharOperation.containsEqual(declaringModuleNames, this.moduleName)) {
						if (parent != null) {
							PackageBinding singleParent = parent.getIncarnation(this);
							if (singleParent != null && singleParent != parent) {
								// parent.getPackage0() may have been too shy, so drill into the split:
								binding = singleParent.getPackage0(name);
							}
						}
						if (binding == null) {
							// declared here, not yet known, so create it now:
							binding = this.createDeclaredPackage(subPkgCompoundName, parent);
						}
					} else {
						// visible but foreign (when current is unnamed or auto):
						for (char[] declaringModuleName : declaringModuleNames) {
							ModuleBinding declaringModule = this.environment.root.getModule(declaringModuleName);
							if (declaringModule != null) {
								PlainPackageBinding declaredPackage = declaringModule.getDeclaredPackage(fullFlatName);
								binding = SplitPackageBinding.combine(declaredPackage, binding, this);
							}
						}
					}
				}
			}
		} else {
			if (this.environment.nameEnvironment.isPackage(parentName, name))
				binding = this.createDeclaredPackage(subPkgCompoundName, parent);
		}

		binding = combineWithPackagesFromOtherRelevantModules(binding, subPkgCompoundName, declaringModuleNames);

		assert binding == null || binding instanceof PlainPackageBinding || binding.enclosingModule == this;

		if (binding == null || !binding.isValidBinding()) {
			if (parent != null) {
				if (binding == null) {
					parent.addNotFoundPackage(name);
				} else {
					parent.knownPackages.put(name, binding);
				}
			} else {
				this.environment.knownPackages.put(name, LookupEnvironment.TheNotFoundPackage);
			}
			return null;
		}
		// remember
		if (parentName.length == 0) {
			this.environment.knownPackages.put(name, binding);
		} else if (parent != null) {
			binding = parent.addPackage(binding, this);
		}
		return binding;
	}

	/**
	 * Answer the package of the given qualified name and visible in this module,
	 * or {@code null} if no such package exists.
	 * Accessibility (based on package exports) is <strong>not</strong> checked.
	 * <p>
	 * May answer a {@link SplitPackageBinding}.
	 * </p>
	 */
	public PackageBinding getVisiblePackage(char[][] qualifiedPackageName) {
		if (qualifiedPackageName == null || qualifiedPackageName.length == 0) {
			return this.environment.defaultPackage;
		}

		PackageBinding parent = getTopLevelPackage(qualifiedPackageName[0]);
		if (parent == null)
			return null;

		// check each sub package
		for (int i = 1; i < qualifiedPackageName.length; i++) {
			PackageBinding binding = getVisiblePackage(parent, qualifiedPackageName[i]);
			if (binding == null) {
				return null;
			}
			parent = binding;
		}
		return parent;
	}

	PackageBinding combineWithPackagesFromOtherRelevantModules(PackageBinding currentBinding, char[][] compoundName, char[][] declaringModuleNames) {
		for (ModuleBinding moduleBinding : otherRelevantModules(declaringModuleNames)) {
			PlainPackageBinding nextBinding = moduleBinding.getDeclaredPackage(CharOperation.concatWith(compoundName, '.'));
			currentBinding = SplitPackageBinding.combine(nextBinding, currentBinding, this);
		}
		return currentBinding;
	}

	List<ModuleBinding> otherRelevantModules(char[][] declaringModuleNames) {
		if (isUnnamed() && declaringModuleNames != null) {
			// unnamed module reads all named modules,
			// so all modules declaring the given package are relevant:
			return Arrays.stream(declaringModuleNames)
				.filter(modName -> modName != UNNAMED)
				.map(modName -> this.environment.getModule(modName))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		} else {
			return Arrays.asList(getAllRequiredModules());
		}
	}

	/**
	 * Check if the given package is accessible by this module. True when the package is declared in
	 * this module or exported by some required module to this module.
	 * See {@link #isPackageExportedTo(PackageBinding, ModuleBinding)}
	 *
	 * @param pkg
	 *
	 * @return True, if the package is accessible by this module, false otherwise
	 */
	public boolean canAccess(PackageBinding pkg) {
		if (pkg.isDeclaredIn(this))
			return true;
		for (ModuleBinding requiredModule : getAllRequiredModules()) {
			// If pkg is a SplitPackageBinding, we actually ask the intersection of all required modules
			// and modules declaring the package, if any of them exports the package to this module.
			// The intersection is computed when inside isPackageExportedTo we ask for pkg's incarnation in requiredModule.
			if (requiredModule.isPackageExportedTo(pkg, ModuleBinding.this))
				return true;
			// TODO(SHMOD): store export status in the PackageBinding?
		}
		return false;
	}
	@Override
	public char[] computeUniqueKey(boolean isLeaf) {
		return CharOperation.prepend('"', this.moduleName);
	}

	@Override
	public int kind() {
		//
		return Binding.MODULE;
	}

	@Override
	public char[] readableName() {
		return this.moduleName;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder(30);
		if (isOpen())
			buffer.append("open "); //$NON-NLS-1$
		buffer.append("module " + new String(readableName())); //$NON-NLS-1$
		if (this.requires.length > 0) {
			buffer.append("\n/*    requires    */\n"); //$NON-NLS-1$
			for (int i = 0; i < this.requires.length; i++) {
				buffer.append("\n\t"); //$NON-NLS-1$
				if (this.requiresTransitive != null) {
					for (ModuleBinding reqTrans : this.requiresTransitive) {
						if (reqTrans == this.requires[i]) {
							buffer.append("transitive "); //$NON-NLS-1$
							break;
						}
					}
				}
				buffer.append(this.requires[i].moduleName);
			}
		} else {
			buffer.append("\nNo Requires"); //$NON-NLS-1$
		}
		if (this.exportedPackages != null && this.exportedPackages.length > 0) {
			buffer.append("\n/*    exports    */\n"); //$NON-NLS-1$
			for (int i = 0; i < this.exportedPackages.length; i++) {
				PackageBinding export = this.exportedPackages[i];
				buffer.append("\n\t"); //$NON-NLS-1$
				if (export == null) {
					buffer.append("<unresolved>"); //$NON-NLS-1$
					continue;
				}
				buffer.append(export.readableName());
				SimpleSetOfCharArray restrictions = this.exportRestrictions != null ? this.exportRestrictions.get(export) : null;
				if (restrictions != null) {
					buffer.append(" to "); //$NON-NLS-1$
					String sep = ""; //$NON-NLS-1$
					char[][] allNames = new char[restrictions.elementSize][];
					restrictions.asArray(allNames);
					for (char[] targetModule : allNames) {
						buffer.append(sep);
						buffer.append(targetModule);
						sep = ", "; //$NON-NLS-1$
					}
				}
			}
		} else {
			buffer.append("\nNo Exports"); //$NON-NLS-1$
		}
		if (this.openedPackages != null && this.openedPackages.length > 0) {
			buffer.append("\n/*    exports    */\n"); //$NON-NLS-1$
			for (int i = 0; i < this.openedPackages.length; i++) {
				PackageBinding opens = this.openedPackages[i];
				buffer.append("\n\t"); //$NON-NLS-1$
				if (opens == null) {
					buffer.append("<unresolved>"); //$NON-NLS-1$
					continue;
				}
				buffer.append(opens.readableName());
				SimpleSetOfCharArray restrictions = this.openRestrictions != null ? this.openRestrictions.get(opens) : null;
				if (restrictions != null) {
					buffer.append(" to "); //$NON-NLS-1$
					String sep = ""; //$NON-NLS-1$
					char[][] allNames = new char[restrictions.elementSize][];
					restrictions.asArray(allNames);
					for (char[] targetModule : allNames) {
						buffer.append(sep);
						buffer.append(targetModule);
						sep = ", "; //$NON-NLS-1$
					}
				}
			}
		} else {
			buffer.append("\nNo Opens"); //$NON-NLS-1$
		}
		if (this.uses != null && this.uses.length > 0) {
			buffer.append("\n/*    uses    /*\n"); //$NON-NLS-1$
			for (int i = 0; i < this.uses.length; i++) {
				buffer.append("\n\t"); //$NON-NLS-1$
				buffer.append(this.uses[i].debugName());
			}
		} else {
			buffer.append("\nNo Uses"); //$NON-NLS-1$
		}
		if (this.services != null && this.services.length > 0) {
			buffer.append("\n/*    Services    */\n"); //$NON-NLS-1$
			for (int i = 0; i < this.services.length; i++) {
				buffer.append("\n\t"); //$NON-NLS-1$
				buffer.append("provides "); //$NON-NLS-1$
				buffer.append(this.services[i].debugName());
				buffer.append(" with "); //$NON-NLS-1$
				if (this.implementations != null && this.implementations.containsKey(this.services[i])) {
					String sep = ""; //$NON-NLS-1$
					for (TypeBinding impl : this.implementations.get(this.services[i])) {
						buffer.append(sep).append(impl.debugName());
						sep = ", "; //$NON-NLS-1$
					}
				} else {
					buffer.append("<missing implementations>"); //$NON-NLS-1$
				}
			}
		} else {
			buffer.append("\nNo Services"); //$NON-NLS-1$
		}
		return buffer.toString();
	}
	public boolean isUnnamed() {
		return false;
	}
	public boolean isAutomatic() {
		return this.isAuto;
	}
	public boolean isOpen() {
		return (this.modifiers & ClassFileConstants.ACC_OPEN) != 0;
	}
	public boolean isDeprecated() {
		return (this.tagBits & TagBits.AnnotationDeprecated) != 0;
	}
	public boolean hasUnstableAutoName() {
		return false;
	}
	public boolean isTransitivelyRequired(ModuleBinding otherModule) {
		if (this.transitiveRequires == null) {
			Set<ModuleBinding> transitiveDeps = new HashSet<>();
			collectTransitiveDependencies(transitiveDeps);
			this.transitiveRequires = transitiveDeps;
		}
		return this.transitiveRequires.contains(otherModule);
	}

	public int getDefaultNullness() {
		getAnnotationTagBits(); // ensure annotations are initialized
		return this.defaultNullness;
	}
	SimpleLookupTable storedAnnotations(boolean forceInitialize, boolean forceStore) {

		if (forceInitialize && this.storedAnnotations == null) {
			if (!this.environment.globalOptions.storeAnnotations && !forceStore)
				return null; // not supported during this compile
			this.storedAnnotations = new SimpleLookupTable(3);
		}
		return this.storedAnnotations;
	}
	public AnnotationHolder retrieveAnnotationHolder(Binding binding, boolean forceInitialization) {
		SimpleLookupTable store = storedAnnotations(forceInitialization, false);
		return store == null ? null : (AnnotationHolder) store.get(binding);
	}

	AnnotationBinding[] retrieveAnnotations(Binding binding) {
		AnnotationHolder holder = retrieveAnnotationHolder(binding, true);
		return holder == null ? Binding.NO_ANNOTATIONS : holder.getAnnotations();
	}

	@Override
	public void setAnnotations(AnnotationBinding[] annotations, boolean forceStore) {
		storeAnnotations(this, annotations, forceStore);
	}
	void storeAnnotationHolder(Binding binding, AnnotationHolder holder) {
		if (holder == null) {
			SimpleLookupTable store = storedAnnotations(false, false);
			if (store != null)
				store.removeKey(binding);
		} else {
			SimpleLookupTable store = storedAnnotations(true, false);
			if (store != null)
				store.put(binding, holder);
		}
	}

	void storeAnnotations(Binding binding, AnnotationBinding[] annotations, boolean forceStore) {
		AnnotationHolder holder = null;
		if (annotations == null || annotations.length == 0) {
			SimpleLookupTable store = storedAnnotations(false, forceStore);
			if (store != null)
				holder = (AnnotationHolder) store.get(binding);
			if (holder == null) return; // nothing to delete
		} else {
			SimpleLookupTable store = storedAnnotations(true, forceStore);
			if (store == null) return; // not supported
			holder = (AnnotationHolder) store.get(binding);
			if (holder == null)
				holder = new AnnotationHolder();
		}
		storeAnnotationHolder(binding, holder.setAnnotations(annotations));
	}
}
