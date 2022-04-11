/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Stephan Herrmann - contributions for
 *     							bug 337868 - [compiler][model] incomplete support for package-info.java when using SearchableEnvironment
 *								bug 186342 - [compiler][null] Using annotations for null checking
 *								bug 365531 - [compiler][null] investigate alternative strategy for internally encoding nullness defaults
 *								bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								bug 392862 - [1.8][compiler][null] Evaluate null annotations on array types
 *								bug 392384 - [1.8][compiler][null] Restore nullness info from type annotations in class files
 *								Bug 392099 - [1.8][compiler][null] Apply null annotation on types for null analysis
 *								Bug 415291 - [1.8][null] differentiate type incompatibilities due to null annotations
 *								Bug 392238 - [1.8][compiler][null] Detect semantically invalid null type annotations
 *								Bug 415850 - [1.8] Ensure RunJDTCoreTests can cope with null annotations enabled
 *								Bug 415043 - [1.8][null] Follow-up re null type annotations after bug 392099
 *								Bug 416183 - [1.8][compiler][null] Overload resolution fails with null annotations
 *								Bug 416307 - [1.8][compiler][null] subclass with type parameter substitution confuses null checking
 *								Bug 417295 - [1.8[[null] Massage type annotated null analysis to gel well with deep encoded type bindings.
 *								Bug 416190 - [1.8][null] detect incompatible overrides due to null type annotations
 *								Bug 424624 - [1.8][null] if a static-object with annotation @NonNull is used, a warning is shown
 *								Bug 438458 - [1.8][null] clean up handling of null type annotations wrt type variables
 *								Bug 439516 - [1.8][null] NonNullByDefault wrongly applied to implicit type bound of binary type
 *								Bug 434602 - Possible error with inferred null annotations leading to contradictory null annotations
 *								Bug 435805 - [1.8][compiler][null] Java 8 compiler does not recognize declaration style null annotations
 *								Bug 453475 - [1.8][null] Contradictory null annotations (4.5 M3 edition)
 *								Bug 457079 - Regression: type inference
 *								Bug 440477 - [null] Infrastructure for feeding external annotations into compilation
 *								Bug 455180 - IllegalStateException in AnnotatableTypeSystem.getRawType
 *								Bug 470467 - [null] Nullness of special Enum methods not detected from .class file
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ClassFilePool;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.*;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.ITypeRequestor;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.compiler.util.HashtableOfModule;
import org.eclipse.jdt.internal.compiler.util.HashtableOfPackage;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;

@SuppressWarnings({"rawtypes", "unchecked"})
public class LookupEnvironment implements ProblemReasons, TypeConstants {

	/**
	 * Map from typeBinding -> accessRestriction rule
	 */
	private Map accessRestrictions;
	ImportBinding[] defaultImports;				// ROOT_ONLY
	/**
	 * The root environment driving the current compilation.
	 * Other mutable fields in this class marked as ROOT_ONLY must always be accessed from the root environment.
	 * It is assumed that external clients only know the root environment, whereas calls internally in the compiler
	 * have to delegate to root where necessary.
	 * Immutable fields with "global" semantics are SHARED among environments via aliasing.
	 */
	public final LookupEnvironment root;
	public ModuleBinding UnNamedModule;
	public ModuleBinding JavaBaseModule;
	public ModuleBinding module;
	public PlainPackageBinding defaultPackage;
	/** All visible toplevel packages, i.e. observable packages associated with modules read by the current module. */
	HashtableOfPackage<PackageBinding> knownPackages;
	private int lastCompletedUnitIndex = -1; 	// ROOT_ONLY
	private int lastUnitIndex = -1; 			// ROOT_ONLY

	TypeSystem typeSystem;					 	// SHARED

	public INameEnvironment nameEnvironment;	// SHARED
	public CompilerOptions globalOptions;		// SHARED

	public ProblemReporter problemReporter; 	// SHARED
	public ClassFilePool classFilePool; 		// SHARED
	// indicate in which step on the compilation we are.
	// step 1 : build the reference binding
	// step 2 : conect the hierarchy (connect bindings)
	// step 3 : build fields and method bindings.
	private int stepCompleted; 					// ROOT_ONLY
	public ITypeRequestor typeRequestor;		// SHARED

	private SimpleLookupTable uniqueParameterizedGenericMethodBindings;

	// key is a string with the method selector value is an array of method bindings
	private SimpleLookupTable uniquePolymorphicMethodBindings;
	private SimpleLookupTable uniqueGetClassMethodBinding; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=300734

	boolean useModuleSystem;					// true when compliance >= 9 and nameEnvironment is module aware
	// key is a string with the module name value is a module binding
	public HashtableOfModule knownModules;		// SHARED

	public CompilationUnitDeclaration unitBeingCompleted = null; // only set while completing units -- ROOT_ONLY
	public Object missingClassFileLocation = null; // only set when resolving certain references, to help locating problems
	private CompilationUnitDeclaration[] units = new CompilationUnitDeclaration[4]; // ROOT_ONLY
	private MethodVerifier verifier;

	private ArrayList missingTypes;
	Set<SourceTypeBinding> typesBeingConnected;	// SHARED
	public boolean isProcessingAnnotations = false; // ROOT_ONLY
	public boolean mayTolerateMissingType = false;

	PackageBinding nullableAnnotationPackage;			// the package supposed to contain the Nullable annotation type
	PackageBinding nonnullAnnotationPackage;			// the package supposed to contain the NonNull annotation type
	PackageBinding nonnullByDefaultAnnotationPackage;	// the package supposed to contain the NonNullByDefault annotation type

	AnnotationBinding nonNullAnnotation;
	AnnotationBinding nullableAnnotation;

	Map<String,Integer> allNullAnnotations = null;

	final List<MethodBinding> deferredEnumMethods; // SHARED: during early initialization we cannot mark Enum-methods as nonnull.

	/** Global access to the outermost active inference context as the universe for inference variable interning. */
	InferenceContext18 currentInferenceContext;

	/**
	 * Flag that should be set during annotation traversal or similar runs
	 * to prevent caching of failures regarding imports of yet to be generated classes.
	 */
	public boolean suppressImportErrors;			// per module

	public String moduleVersion; 	// ROOT_ONLY

	final static int BUILD_FIELDS_AND_METHODS = 4;
	final static int BUILD_TYPE_HIERARCHY = 1;
	final static int CHECK_AND_SET_IMPORTS = 2;
	final static int CONNECT_TYPE_HIERARCHY = 3;

	static final ProblemPackageBinding TheNotFoundPackage = new ProblemPackageBinding(CharOperation.NO_CHAR, NotFound, null/*not perfect*/);
	static final ProblemReferenceBinding TheNotFoundType = new ProblemReferenceBinding(CharOperation.NO_CHAR_CHAR, null, NotFound);
	static final ModuleBinding TheNotFoundModule = new ModuleBinding(CharOperation.NO_CHAR);

/** Construct the root LookupEnvironment, corresponding to the UnNamedModule. */
public LookupEnvironment(ITypeRequestor typeRequestor, CompilerOptions globalOptions, ProblemReporter problemReporter, INameEnvironment nameEnvironment) {
	this.root = this;
	this.UnNamedModule = new ModuleBinding.UnNamedModule(this);
	this.module = this.UnNamedModule;
	this.typeRequestor = typeRequestor;
	this.globalOptions = globalOptions;
	this.problemReporter = problemReporter;
	this.defaultPackage = new PlainPackageBinding(this); // assume the default package always exists
	this.defaultImports = null;
	this.nameEnvironment = nameEnvironment;
	this.knownPackages = new HashtableOfPackage();
	this.uniqueParameterizedGenericMethodBindings = new SimpleLookupTable(3);
	this.uniquePolymorphicMethodBindings = new SimpleLookupTable(3);
	this.missingTypes = null;
	this.accessRestrictions = new HashMap(3);
	this.classFilePool = ClassFilePool.newInstance();
	this.typesBeingConnected = new LinkedHashSet<>();
	this.deferredEnumMethods = new ArrayList<>();
	this.typeSystem = this.globalOptions.sourceLevel >= ClassFileConstants.JDK1_8 && this.globalOptions.storeAnnotations ? new AnnotatableTypeSystem(this) : new TypeSystem(this);
	this.knownModules = new HashtableOfModule();
	this.useModuleSystem = nameEnvironment instanceof IModuleAwareNameEnvironment && globalOptions.complianceLevel >= ClassFileConstants.JDK9;
	this.resolutionListeners = new IQualifiedTypeResolutionListener[0];
}

/** Construct a specific LookupEnvironment, corresponding to the given module. */
LookupEnvironment(LookupEnvironment rootEnv, ModuleBinding module) {
	this.root = rootEnv;
	this.UnNamedModule = rootEnv.UnNamedModule;
	this.module = module;
	this.typeRequestor = rootEnv.typeRequestor;
	this.globalOptions = rootEnv.globalOptions;
	this.problemReporter = rootEnv.problemReporter;
	this.defaultPackage = new PlainPackageBinding(this); // assume the default package always exists
	this.defaultImports = null;
	this.nameEnvironment = rootEnv.nameEnvironment;
	this.knownPackages = new HashtableOfPackage();
	this.uniqueParameterizedGenericMethodBindings = new SimpleLookupTable(3);
	this.uniquePolymorphicMethodBindings = new SimpleLookupTable(3);
	this.missingTypes = null;
	this.accessRestrictions = new HashMap(3);
	this.classFilePool = rootEnv.classFilePool;
	this.typesBeingConnected = rootEnv.typesBeingConnected;
	this.deferredEnumMethods = rootEnv.deferredEnumMethods;
	this.typeSystem = rootEnv.typeSystem;
	// knownModules is unused in specific LookupEnvironments
	this.useModuleSystem = rootEnv.useModuleSystem;
}

// NOTE: only for resolving!
public ModuleBinding getModule(char[] name) {
	if (this.root != this)
		return this.root.getModule(name);
	if (name == null || name == ModuleBinding.UNNAMED || CharOperation.equals(name, ModuleBinding.ALL_UNNAMED))
		return this.UnNamedModule;
	ModuleBinding moduleBinding = this.knownModules.get(name);
	if (moduleBinding == null) {
		if (this.useModuleSystem) {
			IModule mod = ((IModuleAwareNameEnvironment) this.nameEnvironment).getModule(name);
			if (mod != null) {
				this.typeRequestor.accept(mod, this);
				moduleBinding = this.root.knownModules.get(name);
			}
		} else
			return this.UnNamedModule;
	}
	return moduleBinding;
}

/**
 * Ask the name environment for a type which corresponds to the compoundName.
 * Answer null if the name cannot be found.
 */

public ReferenceBinding askForType(char[][] compoundName, /*@NonNull*/ModuleBinding clientModule) {
	assert clientModule != null : "lookup needs a module"; //$NON-NLS-1$
	NameEnvironmentAnswer[] answers = null;
	if (this.useModuleSystem) {
		IModuleAwareNameEnvironment moduleEnv = (IModuleAwareNameEnvironment) this.nameEnvironment;
		answers = askForTypeFromModules(clientModule, clientModule.getAllRequiredModules(),
				mod -> moduleEnv.findType(compoundName, mod.nameForLookup()));
	} else {
		NameEnvironmentAnswer answer = this.nameEnvironment.findType(compoundName);
		if (answer != null) {
			answer.moduleBinding = this.module;
			answers = new NameEnvironmentAnswer[] { answer };
		}
	}
	if (answers == null)
		return null;

	ReferenceBinding candidate = null;
	for (NameEnvironmentAnswer answer : answers) {
		if (answer == null) continue;

		ModuleBinding answerModule = answer.moduleBinding != null ? answer.moduleBinding : this.UnNamedModule;

		if (answer.isBinaryType()) {
			// the type was found as a .class file
			PackageBinding pkg = answerModule.environment.computePackageFrom(compoundName, false /* valid pkg */);
			this.typeRequestor.accept(answer.getBinaryType(), pkg, answer.getAccessRestriction());
			ReferenceBinding binding = pkg.getType0(compoundName[compoundName.length - 1]);
			if (binding instanceof BinaryTypeBinding) {
				((BinaryTypeBinding) binding).module = answerModule;
				if (pkg.enclosingModule == null)
					pkg.enclosingModule = answerModule;
			}
		} else if (answer.isCompilationUnit()) {
			// the type was found as a .java file, try to build it then search the cache
			this.typeRequestor.accept(answer.getCompilationUnit(), answer.getAccessRestriction());
		} else if (answer.isSourceType()) {
			// the type was found as a source model
			PackageBinding pkg = answerModule.environment.computePackageFrom(compoundName, false /* valid pkg */);
			this.typeRequestor.accept(answer.getSourceTypes(), pkg, answer.getAccessRestriction());
			ReferenceBinding binding = pkg.getType0(compoundName[compoundName.length - 1]);
			if (binding instanceof SourceTypeBinding) {
				((SourceTypeBinding) binding).module = answerModule;
				if (pkg.enclosingModule == null)
					pkg.enclosingModule = answerModule;
			}
		}
		candidate = combine(candidate, answerModule.environment.getCachedType(compoundName), clientModule);
	}
	return candidate;
}

/* Ask the oracle for a type named name in the packageBinding.
* Answer null if the name cannot be found.
*/
ReferenceBinding askForType(PackageBinding packageBinding, char[] name, ModuleBinding clientModule) {
	assert clientModule != null : "lookup needs a module"; //$NON-NLS-1$
	if (packageBinding == null) {
		packageBinding = this.defaultPackage;
	}
	NameEnvironmentAnswer[] answers = null;
	if (this.useModuleSystem) {
		IModuleAwareNameEnvironment moduleEnv = (IModuleAwareNameEnvironment) this.nameEnvironment;
		final PackageBinding pack = packageBinding;
		// leverage module information from the (split?) package as to prefer NotAccessible over NotFound:
		answers = askForTypeFromModules(null, packageBinding.getDeclaringModules(),
				mod -> fromSplitPackageOrOracle(moduleEnv, mod, pack, name));
	} else {
		NameEnvironmentAnswer answer = this.nameEnvironment.findType(name, packageBinding.compoundName);
		if (answer != null) {
			answer.moduleBinding = this.module;
			answers = new NameEnvironmentAnswer[] { answer };
		}
	}
	if (answers == null)
		return null;

	ReferenceBinding candidate = null;
	for (NameEnvironmentAnswer answer : answers) {
		if (answer == null) continue;
		if (candidate != null && candidate.problemId() == ProblemReasons.Ambiguous)
			return candidate; // saw enough
		ModuleBinding answerModule = answer.moduleBinding != null ? answer.moduleBinding : this.UnNamedModule;
		PackageBinding answerPackage = packageBinding;

		if (answerModule != null) {
			if (!answerPackage.isDeclaredIn(answerModule))
				continue; // this answer is not reachable via the packageBinding
			answerPackage = answerPackage.getIncarnation(answerModule);
		}
		if (answer.isResolvedBinding()) {
			candidate = combine(candidate, answer.getResolvedBinding(), clientModule);
			continue;
		} else if (answer.isBinaryType()) {
			// the type was found as a .class file
			this.typeRequestor.accept(answer.getBinaryType(), answerPackage, answer.getAccessRestriction());
			ReferenceBinding binding = answerPackage.getType0(name);
			if (binding instanceof BinaryTypeBinding) {
				((BinaryTypeBinding) binding).module = answerModule;
			}
		} else if (answer.isCompilationUnit()) {
			// the type was found as a .java file, try to build it then search the cache
			try {
				this.typeRequestor.accept(answer.getCompilationUnit(), answer.getAccessRestriction());
			} catch (AbortCompilation abort) {
				if (CharOperation.equals(name, TypeConstants.PACKAGE_INFO_NAME))
					return null; // silently, requestor may not be able to handle compilation units (HierarchyResolver)
				throw abort;
			}
		} else if (answer.isSourceType()) {
			// the type was found as a source model
			this.typeRequestor.accept(answer.getSourceTypes(), answerPackage, answer.getAccessRestriction());
			ReferenceBinding binding = answerPackage.getType0(name);
			if (binding instanceof SourceTypeBinding) {
				((SourceTypeBinding) binding).module = answerModule;
			}
			String externalAnnotationPath = answer.getExternalAnnotationPath();
			if (externalAnnotationPath != null && this.globalOptions.isAnnotationBasedNullAnalysisEnabled && binding instanceof SourceTypeBinding) {
				ExternalAnnotationSuperimposer.apply((SourceTypeBinding) binding, externalAnnotationPath);
			}
			candidate = combine(candidate, binding, clientModule);
			continue;
		}
		candidate = combine(candidate, answerPackage.getType0(name), clientModule);
	}
	return candidate;
}
/** Combine up-to two candidate types. If both types are present let accessibility from the given clientModule decide. */
private ReferenceBinding combine(ReferenceBinding one, ReferenceBinding two, ModuleBinding clientModule) {
	if (one == null) return two;
	if (two == null) return one;
	if (one.fPackage == null || !clientModule.canAccess(one.fPackage)) return two;
	if (two.fPackage == null || !clientModule.canAccess(two.fPackage)) return one;
	if (one == two) return one; //$IDENTITY-COMPARISON$
	return new ProblemReferenceBinding(one.compoundName, one, ProblemReasons.Ambiguous); // TODO(SHMOD): use a new problem ID
}
/** Collect answers from the oracle concerning the given clientModule (if present) and each of a set of other modules. */
private NameEnvironmentAnswer[] askForTypeFromModules(ModuleBinding clientModule, ModuleBinding[] otherModules,
		Function<ModuleBinding,NameEnvironmentAnswer> oracle)
{
	if (clientModule != null && clientModule.nameForLookup().length == 0) {
		NameEnvironmentAnswer answer = oracle.apply(clientModule);
		if (answer != null)
			answer.moduleBinding = this.root.getModuleFromAnswer(answer);
		return new NameEnvironmentAnswer[] { answer };
	} else {
		boolean found = false;
		NameEnvironmentAnswer[] answers = null;
		if (clientModule != null) {
			answers = new NameEnvironmentAnswer[otherModules.length+1];
			NameEnvironmentAnswer answer = oracle.apply(clientModule);
			if (answer != null) {
				answer.moduleBinding = clientModule;
				answers[answers.length-1] = answer;
				found = true;
			}
		} else {
			answers = new NameEnvironmentAnswer[otherModules.length];
		}
		for (int i = 0; i < otherModules.length; i++) {
			NameEnvironmentAnswer answer = oracle.apply(otherModules[i]);
			if (answer != null) {
				if (answer.moduleBinding == null) {
					char[] nameFromAnswer = answer.moduleName();
					if (CharOperation.equals(nameFromAnswer, otherModules[i].moduleName)) {
						answer.moduleBinding = otherModules[i];
					} else {
						answer.moduleBinding = getModule(nameFromAnswer);
					}
				}
				answers[i] = answer;
				found = true;
			}
		}
		return found ? answers : null;
	}
}
/** First check for a known type in a split package and otherwise ask the oracle. */
private static NameEnvironmentAnswer fromSplitPackageOrOracle(IModuleAwareNameEnvironment moduleEnv, ModuleBinding module,
		PackageBinding packageBinding, char[] name)
{
	if (packageBinding instanceof SplitPackageBinding) {
		// when asking a split package getType0() we may have answered null in case of ambiguity (not knowing the module context).
		// now check if the module-incarnation of the package has the type:
		// (needed because the moduleEnv will not answer initial types).
		ReferenceBinding binding = ((SplitPackageBinding) packageBinding).getType0ForModule(module, name);
		if (binding != null && binding.isValidBinding()) {
			if (binding instanceof UnresolvedReferenceBinding)
				binding = ((UnresolvedReferenceBinding) binding).resolve(module.environment, false);
			if (binding.isValidBinding())
				return new NameEnvironmentAnswer(binding, module);
		}
	}
	return moduleEnv.findType(name, packageBinding.compoundName, module.nameForLookup());
}
private ModuleBinding getModuleFromAnswer(NameEnvironmentAnswer answer) {
	char[] moduleName = answer.moduleName();
	if (moduleName != null) {
		ModuleBinding moduleBinding;
		if (!this.useModuleSystem || moduleName == ModuleBinding.UNNAMED) {
			moduleBinding = this.UnNamedModule;
		} else {
			moduleBinding = this.knownModules.get(moduleName);
			if (moduleBinding == null && this.nameEnvironment instanceof IModuleAwareNameEnvironment) {
				IModule iModule = ((IModuleAwareNameEnvironment) this.nameEnvironment).getModule(moduleName);
				try {
					this.typeRequestor.accept(iModule, this);
					moduleBinding = this.knownModules.get(moduleName);
				} catch (NullPointerException e) {
					System.err.println("Bug 529367: moduleName: " + new String(moduleName) + "iModule null" +  //$NON-NLS-1$ //$NON-NLS-2$
							(iModule == null ? "true" : "false")); //$NON-NLS-1$ //$NON-NLS-2$]
					throw e;
				}
			}
		}
		return moduleBinding;
	}
	return null;
}

public boolean canTypeBeAccessed(SourceTypeBinding binding, Scope scope) {
	ModuleBinding client = scope.module();
	return client.canAccess(binding.fPackage);
}

/* Create the initial type bindings for the compilation unit.
*
* See completeTypeBindings() for a description of the remaining steps
*
* NOTE: This method can be called multiple times as additional source files are needed
*/
public void buildTypeBindings(CompilationUnitDeclaration unit, AccessRestriction accessRestriction) {
	CompilationUnitScope scope;
	ModuleBinding unitModule = null;
	if (unit.moduleDeclaration != null) {
		char[] moduleName = unit.moduleDeclaration.moduleName;
		scope = new CompilationUnitScope(unit, this.globalOptions);
		unitModule = unit.moduleDeclaration.setBinding(new SourceModuleBinding(moduleName, scope, this.root));
	} else {
		if (this.globalOptions.sourceLevel >= ClassFileConstants.JDK9) {
			unitModule = unit.module(this);
		}
		scope = new CompilationUnitScope(unit, unitModule != null ? unitModule.environment : this);
	}
	scope.buildTypeBindings(accessRestriction);
	LookupEnvironment rootEnv = this.root;
	int unitsLength = rootEnv.units.length;
	if (++rootEnv.lastUnitIndex >= unitsLength)
		System.arraycopy(rootEnv.units, 0, rootEnv.units = new CompilationUnitDeclaration[2 * unitsLength], 0, unitsLength);
	rootEnv.units[rootEnv.lastUnitIndex] = unit;
}

/* Cache the binary type since we know it is needed during this compile.
*
* Answer the created BinaryTypeBinding or null if the type is already in the cache.
*/
public BinaryTypeBinding cacheBinaryType(IBinaryType binaryType, AccessRestriction accessRestriction) {
	return cacheBinaryType(binaryType, true, accessRestriction);
}

/* Cache the binary type since we know it is needed during this compile.
*
* Answer the created BinaryTypeBinding or null if the type is already in the cache.
*/
public BinaryTypeBinding cacheBinaryType(IBinaryType binaryType, boolean needFieldsAndMethods, AccessRestriction accessRestriction) {
	char[][] compoundName = CharOperation.splitOn('/', binaryType.getName());
	ReferenceBinding existingType = getCachedType(compoundName);

	if (existingType == null || existingType instanceof UnresolvedReferenceBinding)
		// only add the binary type if its not already in the cache
		return createBinaryTypeFrom(binaryType, computePackageFrom(compoundName, false /* valid pkg */), needFieldsAndMethods, accessRestriction);
	return null; // the type already exists & can be retrieved from the cache
}

public void completeTypeBindings() {
	if (this != this.root) {
		this.root.completeTypeBindings();
		return;
	}
	this.stepCompleted = BUILD_TYPE_HIERARCHY;

	for (int i = this.lastCompletedUnitIndex + 1; i <= this.lastUnitIndex; i++) {
	    (this.unitBeingCompleted = this.units[i]).scope.checkAndSetImports();
	}
	this.stepCompleted = CHECK_AND_SET_IMPORTS;

	for (int i = this.lastCompletedUnitIndex + 1; i <= this.lastUnitIndex; i++) {
	    (this.unitBeingCompleted = this.units[i]).scope.connectTypeHierarchy();
	}
	this.stepCompleted = CONNECT_TYPE_HIERARCHY;

	for (int i = this.lastCompletedUnitIndex + 1; i <= this.lastUnitIndex; i++) {
		CompilationUnitScope unitScope = (this.unitBeingCompleted = this.units[i]).scope;
		unitScope.checkParameterizedTypes();
		unitScope.buildFieldsAndMethods();
		this.units[i] = null; // release unnecessary reference to the parsed unit
	}
	this.stepCompleted = BUILD_FIELDS_AND_METHODS;
	this.lastCompletedUnitIndex = this.lastUnitIndex;
	this.unitBeingCompleted = null;
}

/*
* 1. Connect the type hierarchy for the type bindings created for parsedUnits.
* 2. Create the field bindings
* 3. Create the method bindings
*/

/* We know each known compilationUnit is free of errors at this point...
*
* Each step will create additional bindings unless a problem is detected, in which
* case either the faulty import/superinterface/field/method will be skipped or a
* suitable replacement will be substituted (such as Object for a missing superclass)
*/
public void completeTypeBindings(CompilationUnitDeclaration parsedUnit) {
	if (this != this.root) {
		this.root.completeTypeBindings(parsedUnit);
		return;
	}
	if (this.stepCompleted == BUILD_FIELDS_AND_METHODS) {
		// This can only happen because the original set of units are completely built and
		// are now being processed, so we want to treat all the additional units as a group
		// until they too are completely processed.
		completeTypeBindings();
	} else {
		if (parsedUnit.scope == null) return; // parsing errors were too severe

		if (this.stepCompleted >= CHECK_AND_SET_IMPORTS)
			(this.unitBeingCompleted = parsedUnit).scope.checkAndSetImports();

		if (this.stepCompleted >= CONNECT_TYPE_HIERARCHY)
			(this.unitBeingCompleted = parsedUnit).scope.connectTypeHierarchy();

		this.unitBeingCompleted = null;
	}
}

/*
* Used by other compiler tools which do not start by calling completeTypeBindings().
*
* 1. Connect the type hierarchy for the type bindings created for parsedUnits.
* 2. Create the field bindings
* 3. Create the method bindings
*/

/*
* Each step will create additional bindings unless a problem is detected, in which
* case either the faulty import/superinterface/field/method will be skipped or a
* suitable replacement will be substituted (such as Object for a missing superclass)
*/
public void completeTypeBindings(CompilationUnitDeclaration parsedUnit, boolean buildFieldsAndMethods) {
	if (parsedUnit.scope == null) return; // parsing errors were too severe
	LookupEnvironment rootEnv = this.root;
	CompilationUnitDeclaration previousUnitBeingCompleted = rootEnv.unitBeingCompleted;
	(rootEnv.unitBeingCompleted = parsedUnit).scope.checkAndSetImports();
	parsedUnit.scope.connectTypeHierarchy();
	parsedUnit.scope.checkParameterizedTypes();
	if (buildFieldsAndMethods)
		parsedUnit.scope.buildFieldsAndMethods();
	rootEnv.unitBeingCompleted = previousUnitBeingCompleted;
}

/*
* Used by other compiler tools which do not start by calling completeTypeBindings()
* and have more than 1 unit to complete.
*
* 1. Connect the type hierarchy for the type bindings created for parsedUnits.
* 2. Create the field bindings
* 3. Create the method bindings
*/
public void completeTypeBindings(CompilationUnitDeclaration[] parsedUnits, boolean[] buildFieldsAndMethods, int unitCount) {
	LookupEnvironment rootEnv = this.root;
	for (int i = 0; i < unitCount; i++) {
		CompilationUnitDeclaration parsedUnit = parsedUnits[i];
		if (parsedUnit.scope != null)
			(rootEnv.unitBeingCompleted = parsedUnit).scope.checkAndSetImports();
	}

	for (int i = 0; i < unitCount; i++) {
		CompilationUnitDeclaration parsedUnit = parsedUnits[i];
		if (parsedUnit.scope != null)
			(rootEnv.unitBeingCompleted = parsedUnit).scope.connectTypeHierarchy();
	}

	for (int i = 0; i < unitCount; i++) {
		CompilationUnitDeclaration parsedUnit = parsedUnits[i];
		if (parsedUnit.scope != null) {
			(rootEnv.unitBeingCompleted = parsedUnit).scope.checkParameterizedTypes();
			if (buildFieldsAndMethods[i])
				parsedUnit.scope.buildFieldsAndMethods();
		}
	}

	rootEnv.unitBeingCompleted = null;
}
public TypeBinding computeBoxingType(TypeBinding type) {
	TypeBinding boxedType;
	switch (type.id) {
		case TypeIds.T_JavaLangBoolean :
			return TypeBinding.BOOLEAN;
		case TypeIds.T_JavaLangByte :
			return TypeBinding.BYTE;
		case TypeIds.T_JavaLangCharacter :
			return TypeBinding.CHAR;
		case TypeIds.T_JavaLangShort :
			return TypeBinding.SHORT;
		case TypeIds.T_JavaLangDouble :
			return TypeBinding.DOUBLE;
		case TypeIds.T_JavaLangFloat :
			return TypeBinding.FLOAT;
		case TypeIds.T_JavaLangInteger :
			return TypeBinding.INT;
		case TypeIds.T_JavaLangLong :
			return TypeBinding.LONG;

		case TypeIds.T_int :
			boxedType = getType(JAVA_LANG_INTEGER, javaBaseModule());
			if (boxedType != null) return boxedType;
			return new ProblemReferenceBinding(JAVA_LANG_INTEGER, null, NotFound);
		case TypeIds.T_byte :
			boxedType = getType(JAVA_LANG_BYTE, javaBaseModule());
			if (boxedType != null) return boxedType;
			return new ProblemReferenceBinding(JAVA_LANG_BYTE, null, NotFound);
		case TypeIds.T_short :
			boxedType = getType(JAVA_LANG_SHORT, javaBaseModule());
			if (boxedType != null) return boxedType;
			return new ProblemReferenceBinding(JAVA_LANG_SHORT, null, NotFound);
		case TypeIds.T_char :
			boxedType = getType(JAVA_LANG_CHARACTER, javaBaseModule());
			if (boxedType != null) return boxedType;
			return new ProblemReferenceBinding(JAVA_LANG_CHARACTER, null, NotFound);
		case TypeIds.T_long :
			boxedType = getType(JAVA_LANG_LONG, javaBaseModule());
			if (boxedType != null) return boxedType;
			return new ProblemReferenceBinding(JAVA_LANG_LONG, null, NotFound);
		case TypeIds.T_float :
			boxedType = getType(JAVA_LANG_FLOAT, javaBaseModule());
			if (boxedType != null) return boxedType;
			return new ProblemReferenceBinding(JAVA_LANG_FLOAT, null, NotFound);
		case TypeIds.T_double :
			boxedType = getType(JAVA_LANG_DOUBLE, javaBaseModule());
			if (boxedType != null) return boxedType;
			return new ProblemReferenceBinding(JAVA_LANG_DOUBLE, null, NotFound);
		case TypeIds.T_boolean :
			boxedType = getType(JAVA_LANG_BOOLEAN, javaBaseModule());
			if (boxedType != null) return boxedType;
			return new ProblemReferenceBinding(JAVA_LANG_BOOLEAN, null, NotFound);
//		case TypeIds.T_int :
//			return getResolvedType(JAVA_LANG_INTEGER, null);
//		case TypeIds.T_byte :
//			return getResolvedType(JAVA_LANG_BYTE, null);
//		case TypeIds.T_short :
//			return getResolvedType(JAVA_LANG_SHORT, null);
//		case TypeIds.T_char :
//			return getResolvedType(JAVA_LANG_CHARACTER, null);
//		case TypeIds.T_long :
//			return getResolvedType(JAVA_LANG_LONG, null);
//		case TypeIds.T_float :
//			return getResolvedType(JAVA_LANG_FLOAT, null);
//		case TypeIds.T_double :
//			return getResolvedType(JAVA_LANG_DOUBLE, null);
//		case TypeIds.T_boolean :
//			return getResolvedType(JAVA_LANG_BOOLEAN, null);
	}
	// allow indirect unboxing conversion for wildcards and type parameters
	switch (type.kind()) {
		case Binding.WILDCARD_TYPE :
		case Binding.INTERSECTION_TYPE :
		case Binding.TYPE_PARAMETER :
		case Binding.INTERSECTION_TYPE18:
			switch (type.erasure().id) {
				case TypeIds.T_JavaLangBoolean :
					return TypeBinding.BOOLEAN;
				case TypeIds.T_JavaLangByte :
					return TypeBinding.BYTE;
				case TypeIds.T_JavaLangCharacter :
					return TypeBinding.CHAR;
				case TypeIds.T_JavaLangShort :
					return TypeBinding.SHORT;
				case TypeIds.T_JavaLangDouble :
					return TypeBinding.DOUBLE;
				case TypeIds.T_JavaLangFloat :
					return TypeBinding.FLOAT;
				case TypeIds.T_JavaLangInteger :
					return TypeBinding.INT;
				case TypeIds.T_JavaLangLong :
					return TypeBinding.LONG;
			}
			break;
		case Binding.POLY_TYPE:
			return ((PolyTypeBinding) type).computeBoxingType();
	}
	return type;
}

public ModuleBinding javaBaseModule() {
	if (this.JavaBaseModule != null)
		return this.JavaBaseModule;
	if (this.root != this)
		return this.JavaBaseModule = this.root.javaBaseModule();
	ModuleBinding resolvedModel = null;
	if (this.useModuleSystem)
		resolvedModel = getModule(TypeConstants.JAVA_BASE);
	return this.JavaBaseModule = (resolvedModel != null ? resolvedModel : this.UnNamedModule); // fall back to pre-Jigsaw view
}

private PackageBinding computePackageFrom(char[][] constantPoolName, boolean isMissing) {
	if (constantPoolName.length == 1)
		return this.defaultPackage;

	PackageBinding packageBinding = getPackage0(constantPoolName[0]);
	if (packageBinding == null || packageBinding == TheNotFoundPackage) {
		if (this.useModuleSystem) {
			if (this.module.isUnnamed()) {
				char[][] declaringModules = ((IModuleAwareNameEnvironment) this.nameEnvironment).getUniqueModulesDeclaringPackage(new char[][] {constantPoolName[0]}, ModuleBinding.ANY);
				if (declaringModules != null) {
					for (char[] mod : declaringModules) {
						ModuleBinding declaringModule = this.root.getModule(mod);
						if (declaringModule != null)
							packageBinding = SplitPackageBinding.combine(declaringModule.getTopLevelPackage(constantPoolName[0]), packageBinding, this.module);
					}
				}
			} else {
				packageBinding = this.module.getTopLevelPackage(constantPoolName[0]);
			}
		}
		if (packageBinding == null || packageBinding == TheNotFoundPackage) {
			packageBinding = this.module.createDeclaredToplevelPackage(constantPoolName[0]);
		}
		if (isMissing) packageBinding.tagBits |= TagBits.HasMissingType;
		this.knownPackages.put(constantPoolName[0], packageBinding); // TODO: split?
	}

	for (int i = 1, length = constantPoolName.length - 1; i < length; i++) {
		PackageBinding parent = packageBinding;
		if ((packageBinding = parent.getPackage0(constantPoolName[i])) == null || packageBinding == TheNotFoundPackage) {
			if (this.useModuleSystem) {
				if (this.module.isUnnamed()) {
					char[][] currentCompoundName = CharOperation.arrayConcat(parent.compoundName, constantPoolName[i]);
					char[][] declaringModules = ((IModuleAwareNameEnvironment) this.nameEnvironment).getModulesDeclaringPackage(
							currentCompoundName, ModuleBinding.ANY);
					if (declaringModules != null) {
						for (char[] mod : declaringModules) {
							ModuleBinding declaringModule = this.root.getModule(mod);
							if (declaringModule != null)
								packageBinding = SplitPackageBinding.combine(declaringModule.getVisiblePackage(currentCompoundName), packageBinding, this.module);
						}
					}
				} else {
					packageBinding = this.module.getVisiblePackage(parent, constantPoolName[i]);
				}
			}
			if (packageBinding == null || packageBinding == TheNotFoundPackage) {
				packageBinding = this.module.createDeclaredPackage(CharOperation.subarray(constantPoolName, 0, i + 1), parent);
			}
			if (isMissing) {
				packageBinding.tagBits |= TagBits.HasMissingType;
			}
			packageBinding = parent.addPackage(packageBinding, this.module);
		}
	}
	if (packageBinding instanceof SplitPackageBinding) {
		PackageBinding candidate = null;
		// select from incarnations the unique package containing CUs, if any:
		for (PackageBinding incarnation : ((SplitPackageBinding) packageBinding).incarnations) {
			if (incarnation.hasCompilationUnit(false)) {
				if (candidate != null) {
					candidate = null;
					break; // likely to report "accessible from more than one module" downstream
				}
				candidate = incarnation;
			}
		}
		if (candidate != null)
			return candidate;
	}
	return packageBinding;
}

/**
 * Convert a given source type into a parameterized form if generic.
 * generic X<E> --> param X<E>
 */
public ReferenceBinding convertToParameterizedType(ReferenceBinding originalType) {
	if (originalType != null) {
		boolean isGeneric = originalType.isGenericType();
		if (!isGeneric && !originalType.hasEnclosingInstanceContext())
			return originalType;
		ReferenceBinding originalEnclosingType = originalType.enclosingType();
		ReferenceBinding convertedEnclosingType = originalEnclosingType;
		boolean needToConvert = isGeneric;
		if (originalEnclosingType != null && originalType.hasEnclosingInstanceContext()) {
			convertedEnclosingType = convertToParameterizedType(originalEnclosingType);
			needToConvert |= TypeBinding.notEquals(originalEnclosingType, convertedEnclosingType);
		}
		if (needToConvert) {
			return createParameterizedType(originalType, isGeneric ? originalType.typeVariables() : null, convertedEnclosingType);
		}
	}
	return originalType;
}
/**
 * Returns the given binding's raw type binding.
 * @param type the TypeBinding to raw convert
 * @param forceRawEnclosingType forces recursive raw conversion of enclosing types (used in Javadoc references only)
 * @return TypeBinding the raw converted TypeBinding
 */
public TypeBinding convertToRawType(TypeBinding type, boolean forceRawEnclosingType) {
	int dimension;
	TypeBinding originalType;
	switch(type.kind()) {
		case Binding.BASE_TYPE :
		case Binding.TYPE_PARAMETER:
		case Binding.WILDCARD_TYPE:
		case Binding.INTERSECTION_TYPE:
		case Binding.RAW_TYPE:
			return type;
		case Binding.ARRAY_TYPE:
			dimension = type.dimensions();
			originalType = type.leafComponentType();
			break;
		default:
			if (type.id == TypeIds.T_JavaLangObject)
				return type; // Object is not generic
			dimension = 0;
			originalType = type;
	}
	boolean needToConvert;
	switch (originalType.kind()) {
		case Binding.BASE_TYPE :
			return type;
		case Binding.GENERIC_TYPE :
			needToConvert = true;
			break;
		case Binding.PARAMETERIZED_TYPE :
			ParameterizedTypeBinding paramType = (ParameterizedTypeBinding) originalType;
			needToConvert = paramType.genericType().isGenericType(); // only recursive call to enclosing type can find parameterizedType with arguments
			break;
		default :
			needToConvert = false;
			break;
	}
	forceRawEnclosingType &= !originalType.isStatic();
	ReferenceBinding originalEnclosing = originalType.enclosingType();
	TypeBinding convertedType;
	if (originalEnclosing == null) {
		convertedType = needToConvert ? createRawType((ReferenceBinding)originalType.erasure(), null) : originalType;
	} else {
		ReferenceBinding convertedEnclosing;
		if (!((ReferenceBinding)originalType).hasEnclosingInstanceContext()) {
			convertedEnclosing = (ReferenceBinding) originalEnclosing.original();
		} else {
			if (originalEnclosing.kind() == Binding.RAW_TYPE) {
				convertedEnclosing = originalEnclosing;
				needToConvert = true;
			} else if (forceRawEnclosingType && !needToConvert/*stop recursion when conversion occurs*/) {
				convertedEnclosing = (ReferenceBinding) convertToRawType(originalEnclosing, forceRawEnclosingType);
				needToConvert = TypeBinding.notEquals(originalEnclosing, convertedEnclosing); // only convert generic or parameterized types
			} else if (needToConvert) {
				convertedEnclosing = (ReferenceBinding) convertToRawType(originalEnclosing, false);
			} else {
				convertedEnclosing = convertToParameterizedType(originalEnclosing);
			}
		}
		if (needToConvert) {
			convertedType = createRawType((ReferenceBinding) originalType.erasure(), convertedEnclosing);
		} else if (TypeBinding.notEquals(originalEnclosing, convertedEnclosing)) {
			convertedType = createParameterizedType((ReferenceBinding) originalType.erasure(), null, convertedEnclosing);
		} else {
			convertedType = originalType;
		}
	}
	if (TypeBinding.notEquals(originalType, convertedType)) {
		return dimension > 0 ? (TypeBinding)createArrayType(convertedType, dimension) : convertedType;
	}
	return type;
}

/**
 * Convert an array of types in raw forms.
 * Only allocate an array if anything is different.
 */
public ReferenceBinding[] convertToRawTypes(ReferenceBinding[] originalTypes, boolean forceErasure, boolean forceRawEnclosingType) {
	if (originalTypes == null) return null;
    ReferenceBinding[] convertedTypes = originalTypes;
    for (int i = 0, length = originalTypes.length; i < length; i++) {
        ReferenceBinding originalType = originalTypes[i];
        ReferenceBinding convertedType = (ReferenceBinding) convertToRawType(forceErasure ? originalType.erasure() : originalType, forceRawEnclosingType);
        if (TypeBinding.notEquals(convertedType, originalType)) {
            if (convertedTypes == originalTypes) {
                System.arraycopy(originalTypes, 0, convertedTypes = new ReferenceBinding[length], 0, i);
            }
            convertedTypes[i] = convertedType;
        } else if (convertedTypes != originalTypes) {
            convertedTypes[i] = originalType;
        }
    }
    return convertedTypes;
}

// variation for unresolved types in binaries (consider generic type as raw)
public TypeBinding convertUnresolvedBinaryToRawType(TypeBinding type) {
	int dimension;
	TypeBinding originalType;
	switch(type.kind()) {
		case Binding.BASE_TYPE :
		case Binding.TYPE_PARAMETER:
		case Binding.WILDCARD_TYPE:
		case Binding.INTERSECTION_TYPE:
		case Binding.RAW_TYPE:
			return type;
		case Binding.ARRAY_TYPE:
			dimension = type.dimensions();
			originalType = type.leafComponentType();
			break;
		default:
			if (type.id == TypeIds.T_JavaLangObject)
				return type; // Object is not generic
			dimension = 0;
			originalType = type;
	}
	boolean needToConvert;
	switch (originalType.kind()) {
		case Binding.BASE_TYPE :
			return type;
		case Binding.GENERIC_TYPE :
			needToConvert = true;
			break;
		case Binding.PARAMETERIZED_TYPE :
			ParameterizedTypeBinding paramType = (ParameterizedTypeBinding) originalType;
			needToConvert = paramType.genericType().isGenericType(); // only recursive call to enclosing type can find parameterizedType with arguments
			break;
		default :
			needToConvert = false;
			break;
	}
	ReferenceBinding originalEnclosing = originalType.enclosingType();
	TypeBinding convertedType;
	if (originalEnclosing == null) {
		convertedType = needToConvert ? createRawType((ReferenceBinding)originalType.erasure(), null) : originalType;
	} else {
		if (!needToConvert && originalType.isStatic())
			return originalType;

		ReferenceBinding convertedEnclosing = (ReferenceBinding) convertUnresolvedBinaryToRawType(originalEnclosing);
		if (TypeBinding.notEquals(convertedEnclosing, originalEnclosing)) {
			needToConvert = true;
		}
		if (needToConvert) {
			convertedType = createRawType((ReferenceBinding) originalType.erasure(), convertedEnclosing);
		} else {
			convertedType = originalType;
		}
	}
	if (TypeBinding.notEquals(originalType, convertedType)) {
		return dimension > 0 ? (TypeBinding)createArrayType(convertedType, dimension) : convertedType;
	}
	return type;
}
/* Used to guarantee annotation identity: we do that only for marker annotations and others with all default values.
   We don't have the machinery for the general case as of now.
*/
public AnnotationBinding createAnnotation(ReferenceBinding annotationType, ElementValuePair[] pairs) {
	if (pairs.length != 0) {
		AnnotationBinding.setMethodBindings(annotationType, pairs);
		return new AnnotationBinding(annotationType, pairs);
	}
	return this.typeSystem.getAnnotationType(annotationType, true);
}

/* Used to guarantee annotation identity: we do that only for marker annotations and others with all default values.
   We don't have the machinery for the general case as of now.
*/
public AnnotationBinding createUnresolvedAnnotation(ReferenceBinding annotationType, ElementValuePair[] pairs) {
	if (pairs.length != 0) {
		return new UnresolvedAnnotationBinding(annotationType, pairs, this);
	}
	return this.typeSystem.getAnnotationType(annotationType, false);
}

/*
 *  Used to guarantee array type identity.
 */
public ArrayBinding createArrayType(TypeBinding leafComponentType, int dimensionCount) {
	return this.typeSystem.getArrayType(leafComponentType, dimensionCount);
}

public ArrayBinding createArrayType(TypeBinding leafComponentType, int dimensionCount, AnnotationBinding [] annotations) {
	return this.typeSystem.getArrayType(leafComponentType, dimensionCount, annotations);
}

public TypeBinding createIntersectionType18(ReferenceBinding[] intersectingTypes) {
	if (!intersectingTypes[0].isClass()) {
		Arrays.sort(intersectingTypes, new Comparator<TypeBinding>() {
			@Override
			public int compare(TypeBinding o1, TypeBinding o2) {
				//
				return o1.isClass() ? -1 : (o2.isClass() ? 1 : CharOperation.compareTo(o1.readableName(), o2.readableName()));
			}
		});
	}
	return this.typeSystem.getIntersectionType18(intersectingTypes);
}

public BinaryTypeBinding createBinaryTypeFrom(IBinaryType binaryType, PackageBinding packageBinding, AccessRestriction accessRestriction) {
	return createBinaryTypeFrom(binaryType, packageBinding, true, accessRestriction);
}

public BinaryTypeBinding createBinaryTypeFrom(IBinaryType binaryType, PackageBinding packageBinding, boolean needFieldsAndMethods, AccessRestriction accessRestriction) {
	if (this != packageBinding.environment)
		return packageBinding.environment.createBinaryTypeFrom(binaryType, packageBinding, needFieldsAndMethods, accessRestriction);
	BinaryTypeBinding binaryBinding = new BinaryTypeBinding(packageBinding, binaryType, this);

	// resolve any array bindings which reference the unresolvedType
	ReferenceBinding cachedType = packageBinding.getType0(binaryBinding.compoundName[binaryBinding.compoundName.length - 1]);
	if (cachedType != null && !cachedType.isUnresolvedType()) {
		if (cachedType.isBinaryBinding()) // sanity check... at this point the cache should ONLY contain unresolved types
			return (BinaryTypeBinding) cachedType;
		// it is possible with a large number of source files (exceeding AbstractImageBuilder.MAX_AT_ONCE) that a member type can be in the cache as an UnresolvedType,
		// but because its enclosingType is resolved while its created (call to BinaryTypeBinding constructor), its replaced with a source type
		return null;
	}
	packageBinding.addType(binaryBinding);
	setAccessRestriction(binaryBinding, accessRestriction);
	binaryBinding.cachePartsFrom(binaryType, needFieldsAndMethods);
	return binaryBinding;
}

/*
 * Used to create types denoting missing types.
 * If package is given, then reuse the package; if not then infer a package from compound name.
 * If the package is existing, then install the missing type in type cache
*/
public MissingTypeBinding createMissingType(PackageBinding packageBinding, char[][] compoundName) {
	// create a proxy for the missing BinaryType
	if (packageBinding == null) {
		packageBinding = computePackageFrom(compoundName, true /* missing */);
		if (packageBinding == TheNotFoundPackage) packageBinding = this.defaultPackage;
	}
	MissingTypeBinding missingType = new MissingTypeBinding(packageBinding, compoundName, this);
	if (missingType.id != TypeIds.T_JavaLangObject) {
		// make Object be its superclass - it could in turn be missing as well
		ReferenceBinding objectType = getType(TypeConstants.JAVA_LANG_OBJECT, javaBaseModule());
		if (objectType == null) {
			objectType = createMissingType(null, TypeConstants.JAVA_LANG_OBJECT);	// create a proxy for the missing Object type
		}
		missingType.setMissingSuperclass(objectType);
	}
	packageBinding.addType(missingType);
	if (this.missingTypes == null)
		this.missingTypes = new ArrayList(3);
	this.missingTypes.add(missingType);
	return missingType;
}

/*
 * 1. Connect the type hierarchy for the type bindings created for parsedUnits.
 * 2. Create the field bindings
 * 3. Create the method bindings
 */
public PackageBinding createPackage(char[][] compoundName) {
	return createPlainPackage(compoundName);
}
public PlainPackageBinding createPlainPackage(char[][] compoundName) {
	PackageBinding packageBinding = this.module.getDeclaredPackage(CharOperation.concatWith(compoundName, '.'));
	if (packageBinding != null && packageBinding.isValidBinding()) {
		// restart from the toplevel package to proceed with clash analysis below
		packageBinding = this.getTopLevelPackage(compoundName[0]);
	} else {
		packageBinding = getPackage0(compoundName[0]);
		if (packageBinding == null || packageBinding == TheNotFoundPackage) {
			packageBinding = this.module.getOrCreateDeclaredPackage(new char[][] {compoundName[0]});
			if (this.useModuleSystem) {
				char[][] declaringModuleNames = null;
				if (this.module.isUnnamed()) {
					IModuleAwareNameEnvironment moduleEnv = (IModuleAwareNameEnvironment) this.nameEnvironment;
					declaringModuleNames = moduleEnv.getUniqueModulesDeclaringPackage(new char[][] {packageBinding.readableName()}, ModuleBinding.ANY);
				}
				packageBinding = this.module.combineWithPackagesFromOtherRelevantModules(packageBinding, packageBinding.compoundName, declaringModuleNames);
			}
			this.knownPackages.put(compoundName[0], packageBinding); // update in case of split package
		}
	}

	for (int i = 1, length = compoundName.length; i < length; i++) {
		// check to see if it collides with a known type...
		// this case can only happen if the package does not exist as a directory in the file system
		// otherwise when the source type was defined, the correct error would have been reported
		// unless its an unresolved type which is referenced from an inconsistent class file
		// NOTE: empty packages are not packages according to changes in JLS v2, 7.4.3
		// so not all types cause collision errors when they're created even though the package did exist
		if (packageBinding.hasType0Any(compoundName[i]))
			return null;

		PackageBinding parent = packageBinding;
		if ((packageBinding = parent.getPackage0(compoundName[i])) == null || packageBinding == TheNotFoundPackage) {
			// if the package is unknown, check to see if a type exists which would collide with the new package
			// catches the case of a package statement of: package java.lang.Object;
			// since the package can be added after a set of source files have already been compiled,
			// we need to check whenever a package is created
			if(this.nameEnvironment instanceof INameEnvironmentExtension) {
				//When the nameEnvironment is an instance of INameEnvironmentWithProgress, it can get avoided to search for secondaryTypes (see flag).
				// This is a performance optimization, because it is very expensive to search for secondary types and it isn't necessary to check when creating a package,
				// because package name can not collide with a secondary type name.
				if (((INameEnvironmentExtension)this.nameEnvironment).findType(compoundName[i], parent.compoundName, false, this.module.nameForLookup()) != null) {
					return null;
				}
			} else {
				if (this.nameEnvironment.findType(compoundName[i], parent.compoundName) != null) {
					return null;
				}
			}
			PackageBinding singleParent = parent.getIncarnation(this.module);
			if (singleParent != parent && singleParent != null) {
				// parent.getPackage0() may have been too shy, so drill into the split:
				packageBinding = singleParent.getPackage0(compoundName[i]);
			}
			if (packageBinding == null) {
				packageBinding = this.module.createDeclaredPackage(CharOperation.subarray(compoundName, 0, i + 1), parent);
				packageBinding = parent.addPackage(packageBinding, this.module);
			}
		}
	}
	return packageBinding.getIncarnation(this.module);
}

public ParameterizedGenericMethodBinding createParameterizedGenericMethod(MethodBinding genericMethod, RawTypeBinding rawType) {
	// cached info is array of already created parameterized types for this type
	ParameterizedGenericMethodBinding[] cachedInfo = (ParameterizedGenericMethodBinding[])this.uniqueParameterizedGenericMethodBindings.get(genericMethod);
	boolean needToGrow = false;
	int index = 0;
	if (cachedInfo != null){
		nextCachedMethod :
			// iterate existing parameterized for reusing one with same type arguments if any
			for (int max = cachedInfo.length; index < max; index++){
				ParameterizedGenericMethodBinding cachedMethod = cachedInfo[index];
				if (cachedMethod == null) break nextCachedMethod;
				if (!cachedMethod.isRaw) continue nextCachedMethod;
				if (cachedMethod.declaringClass != (rawType == null ? genericMethod.declaringClass : rawType)) continue nextCachedMethod; //$IDENTITY-COMPARISON$
				return cachedMethod;
		}
		needToGrow = true;
	} else {
		cachedInfo = new ParameterizedGenericMethodBinding[5];
		this.uniqueParameterizedGenericMethodBindings.put(genericMethod, cachedInfo);
	}
	// grow cache ?
	int length = cachedInfo.length;
	if (needToGrow && index == length){
		System.arraycopy(cachedInfo, 0, cachedInfo = new ParameterizedGenericMethodBinding[length*2], 0, length);
		this.uniqueParameterizedGenericMethodBindings.put(genericMethod, cachedInfo);
	}
	// add new binding
	ParameterizedGenericMethodBinding parameterizedGenericMethod = new ParameterizedGenericMethodBinding(genericMethod, rawType, this);
	cachedInfo[index] = parameterizedGenericMethod;
	return parameterizedGenericMethod;
}

public ParameterizedGenericMethodBinding createParameterizedGenericMethod(MethodBinding genericMethod, TypeBinding[] typeArguments) {
	return createParameterizedGenericMethod(genericMethod, typeArguments, null);
}
public ParameterizedGenericMethodBinding createParameterizedGenericMethod(MethodBinding genericMethod, TypeBinding[] typeArguments, TypeBinding targetType) {
	return createParameterizedGenericMethod(genericMethod, typeArguments, false, false, targetType);
}
public ParameterizedGenericMethodBinding createParameterizedGenericMethod(MethodBinding genericMethod, TypeBinding[] typeArguments,
																			boolean inferredWithUncheckedConversion, boolean hasReturnProblem, TypeBinding targetType)
{
	// cached info is array of already created parameterized types for this type
	ParameterizedGenericMethodBinding[] cachedInfo = (ParameterizedGenericMethodBinding[])this.uniqueParameterizedGenericMethodBindings.get(genericMethod);
	int argLength = typeArguments == null ? 0: typeArguments.length;
	boolean needToGrow = false;
	int index = 0;
	if (cachedInfo != null){
		nextCachedMethod :
			// iterate existing parameterized for reusing one with same type arguments if any
			for (int max = cachedInfo.length; index < max; index++){
				ParameterizedGenericMethodBinding cachedMethod = cachedInfo[index];
				if (cachedMethod == null) break nextCachedMethod;
				if (cachedMethod.isRaw) continue nextCachedMethod;
				if (cachedMethod.targetType != targetType) continue nextCachedMethod; //$IDENTITY-COMPARISON$
				if (cachedMethod.inferredWithUncheckedConversion != inferredWithUncheckedConversion) continue nextCachedMethod;
				TypeBinding[] cachedArguments = cachedMethod.typeArguments;
				int cachedArgLength = cachedArguments == null ? 0 : cachedArguments.length;
				if (argLength != cachedArgLength) continue nextCachedMethod;
				for (int j = 0; j < cachedArgLength; j++){
					if (typeArguments[j] != cachedArguments[j]) continue nextCachedMethod; //$IDENTITY-COMPARISON$
				}
				if (inferredWithUncheckedConversion) { // JSL 18.5.2: "If unchecked conversion was necessary..."
					// don't tolerate remaining parameterized types / type variables, should have been eliminated by erasure:
					if (cachedMethod.returnType.isParameterizedType() || cachedMethod.returnType.isTypeVariable()) continue;
					for (TypeBinding exc : cachedMethod.thrownExceptions)
						if (exc.isParameterizedType() || exc.isTypeVariable()) continue nextCachedMethod;
				}
				// all arguments match, reuse current
				return cachedMethod;
		}
		needToGrow = true;
	} else {
		cachedInfo = new ParameterizedGenericMethodBinding[5];
		this.uniqueParameterizedGenericMethodBindings.put(genericMethod, cachedInfo);
	}
	// grow cache ?
	int length = cachedInfo.length;
	if (needToGrow && index == length){
		System.arraycopy(cachedInfo, 0, cachedInfo = new ParameterizedGenericMethodBinding[length*2], 0, length);
		this.uniqueParameterizedGenericMethodBindings.put(genericMethod, cachedInfo);
	}
	// add new binding
	ParameterizedGenericMethodBinding parameterizedGenericMethod =
			new ParameterizedGenericMethodBinding(genericMethod, typeArguments, this, inferredWithUncheckedConversion, hasReturnProblem, targetType);
	cachedInfo[index] = parameterizedGenericMethod;
	return parameterizedGenericMethod;
}
public PolymorphicMethodBinding createPolymorphicMethod(MethodBinding originalPolymorphicMethod, TypeBinding[] parameters, Scope scope) {
	// cached info is array of already created polymorphic methods for this type
	String key = new String(originalPolymorphicMethod.selector);
	PolymorphicMethodBinding[] cachedInfo = (PolymorphicMethodBinding[]) this.uniquePolymorphicMethodBindings.get(key);
	int parametersLength = parameters == null ? 0: parameters.length;
	TypeBinding[] parametersTypeBinding = new TypeBinding[parametersLength];
	for (int i = 0; i < parametersLength; i++) {
		TypeBinding parameterTypeBinding = parameters[i];
		if (parameterTypeBinding.id == TypeIds.T_null) {
			parametersTypeBinding[i] = getType(JAVA_LANG_VOID, javaBaseModule());
		} else {
			if (parameterTypeBinding.isPolyType()) {
				PolyTypeBinding ptb = (PolyTypeBinding) parameterTypeBinding;
				if (scope instanceof BlockScope && ptb.expression.resolvedType == null) {
					ptb.expression.setExpectedType(scope.getJavaLangObject());
					parametersTypeBinding[i] = ptb.expression.resolveType((BlockScope) scope);
				} else {
					parametersTypeBinding[i] = ptb.expression.resolvedType;
				}
			} else {
				parametersTypeBinding[i] = parameterTypeBinding.erasure();
			}
		}
	}
	boolean needToGrow = false;
	int index = 0;
	if (cachedInfo != null) {
		nextCachedMethod :
			// iterate existing polymorphic method for reusing one with same type arguments if any
			for (int max = cachedInfo.length; index < max; index++) {
				PolymorphicMethodBinding cachedMethod = cachedInfo[index];
				if (cachedMethod == null) {
					break nextCachedMethod;
				}
				if (cachedMethod.matches(parametersTypeBinding, originalPolymorphicMethod.returnType)) {
					return cachedMethod;
				}
		}
		needToGrow = true;
	} else {
		cachedInfo = new PolymorphicMethodBinding[5];
		this.uniquePolymorphicMethodBindings.put(key, cachedInfo);
	}
	// grow cache ?
	int length = cachedInfo.length;
	if (needToGrow && index == length) {
		System.arraycopy(cachedInfo, 0, cachedInfo = new PolymorphicMethodBinding[length*2], 0, length);
		this.uniquePolymorphicMethodBindings.put(key, cachedInfo);
	}
	// add new binding
	PolymorphicMethodBinding polymorphicMethod = new PolymorphicMethodBinding(
			originalPolymorphicMethod,
			parametersTypeBinding);
	cachedInfo[index] = polymorphicMethod;
	return polymorphicMethod;
}

public boolean usesAnnotatedTypeSystem() {
	return this.typeSystem.isAnnotatedTypeSystem();
}

public MethodBinding updatePolymorphicMethodReturnType(PolymorphicMethodBinding binding, TypeBinding typeBinding) {
	// update the return type to be the given return type, but reuse existing binding if one can match
	String key = new String(binding.selector);
	PolymorphicMethodBinding[] cachedInfo = (PolymorphicMethodBinding[]) this.uniquePolymorphicMethodBindings.get(key);
	boolean needToGrow = false;
	int index = 0;
	TypeBinding[] parameters = binding.parameters;
	if (cachedInfo != null) {
		nextCachedMethod :
			// iterate existing polymorphic method for reusing one with same type arguments if any
			for (int max = cachedInfo.length; index < max; index++) {
				PolymorphicMethodBinding cachedMethod = cachedInfo[index];
				if (cachedMethod == null) {
					break nextCachedMethod;
				}
				if (cachedMethod.matches(parameters, typeBinding)) {
					return cachedMethod;
				}
		}
		needToGrow = true;
	} else {
		cachedInfo = new PolymorphicMethodBinding[5];
		this.uniquePolymorphicMethodBindings.put(key, cachedInfo);
	}
	// grow cache ?
	int length = cachedInfo.length;
	if (needToGrow && index == length) {
		System.arraycopy(cachedInfo, 0, cachedInfo = new PolymorphicMethodBinding[length*2], 0, length);
		this.uniquePolymorphicMethodBindings.put(key, cachedInfo);
	}
	// add new binding
	PolymorphicMethodBinding polymorphicMethod = new PolymorphicMethodBinding(
			binding.original(),
			typeBinding,
			parameters);
	cachedInfo[index] = polymorphicMethod;
	return polymorphicMethod;
}
public ParameterizedMethodBinding createGetClassMethod(TypeBinding receiverType, MethodBinding originalMethod, Scope scope) {
	// see if we have already cached this method for the given receiver type.
	ParameterizedMethodBinding retVal = null;
	if (this.uniqueGetClassMethodBinding == null) {
		this.uniqueGetClassMethodBinding = new SimpleLookupTable(3);
	} else {
		retVal = (ParameterizedMethodBinding)this.uniqueGetClassMethodBinding.get(receiverType);
	}
	if (retVal == null) {
		retVal = ParameterizedMethodBinding.instantiateGetClass(receiverType, originalMethod, scope);
		this.uniqueGetClassMethodBinding.put(receiverType, retVal);
	}
	return retVal;
}
public ReferenceBinding createMemberType(ReferenceBinding memberType, ReferenceBinding enclosingType) {
	return this.typeSystem.getMemberType(memberType, enclosingType);
}
public ParameterizedTypeBinding createParameterizedType(ReferenceBinding genericType, TypeBinding[] typeArguments, ReferenceBinding enclosingType) {
	AnnotationBinding[] annotations = genericType.typeAnnotations;
	if (annotations != Binding.NO_ANNOTATIONS)
		return this.typeSystem.getParameterizedType((ReferenceBinding) genericType.unannotated(), typeArguments, enclosingType, annotations);
	return this.typeSystem.getParameterizedType(genericType, typeArguments, enclosingType);
}

public ParameterizedTypeBinding createParameterizedType(ReferenceBinding genericType, TypeBinding[] typeArguments, ReferenceBinding enclosingType, AnnotationBinding [] annotations) {
	return this.typeSystem.getParameterizedType(genericType, typeArguments, enclosingType, annotations);
}
public ReferenceBinding maybeCreateParameterizedType(ReferenceBinding nonGenericType, ReferenceBinding enclosingType) {
	boolean canSeeEnclosingTypeParameters = enclosingType != null
			&& (enclosingType.isParameterizedType() | enclosingType.isRawType())
			&& !nonGenericType.isStatic();
	if (canSeeEnclosingTypeParameters)
		return createParameterizedType(nonGenericType, null, enclosingType);
	return nonGenericType;
}

public TypeBinding createAnnotatedType(TypeBinding type, AnnotationBinding[][] annotations) {
	return this.typeSystem.getAnnotatedType(type, annotations);
}

// Variant to handle incoming type possibly carrying annotations.
public TypeBinding createAnnotatedType(TypeBinding type, AnnotationBinding[] newbies) {
	final int newLength = newbies == null ? 0 :  newbies.length;
	if (type == null || newLength == 0)
		return type;
	AnnotationBinding [] oldies = type.getTypeAnnotations();
	final int oldLength = oldies == null ? 0 : oldies.length;
	if (oldLength > 0) {
		System.arraycopy(newbies, 0, newbies = new AnnotationBinding[newLength + oldLength], 0, newLength);
		System.arraycopy(oldies, 0, newbies, newLength, oldLength);
	}
	if (this.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
		// filter duplicate null annotations
		// (do we want to filter other annotations as well? only if not repeatable?)
		long tagBitsSeen = 0;
		AnnotationBinding[] filtered = new AnnotationBinding[newbies.length];
		int count = 0;
		for (int i = 0; i < newbies.length; i++) {
			if (newbies[i] == null) {
				filtered[count++] = null;
				// reset tagBitsSeen for next array dimension
				tagBitsSeen = 0;
				continue;
			}
			long tagBits = 0;
			if (newbies[i].type.hasNullBit(TypeIds.BitNonNullAnnotation)) {
				tagBits = TagBits.AnnotationNonNull;
			} else if (newbies[i].type.hasNullBit(TypeIds.BitNullableAnnotation)) {
				tagBits = TagBits.AnnotationNullable;
			}
			if ((tagBitsSeen & tagBits) == 0) {
				tagBitsSeen |= tagBits;
				filtered[count++] = newbies[i];
			}
		}
		if (count < newbies.length)
			System.arraycopy(filtered, 0, newbies = new AnnotationBinding[count], 0, count);
	}
	return this.typeSystem.getAnnotatedType(type, new AnnotationBinding [][] { newbies });
}

public RawTypeBinding createRawType(ReferenceBinding genericType, ReferenceBinding enclosingType) {
	AnnotationBinding[] annotations = genericType.typeAnnotations;
	if (annotations != Binding.NO_ANNOTATIONS)
		return this.typeSystem.getRawType((ReferenceBinding) genericType.unannotated(), enclosingType, annotations);
	return this.typeSystem.getRawType(genericType, enclosingType);
}

public RawTypeBinding createRawType(ReferenceBinding genericType, ReferenceBinding enclosingType, AnnotationBinding [] annotations) {
	return this.typeSystem.getRawType(genericType, enclosingType, annotations);
}

public WildcardBinding createWildcard(ReferenceBinding genericType, int rank, TypeBinding bound, TypeBinding[] otherBounds, int boundKind) {
	if (genericType != null) {
		AnnotationBinding[] annotations = genericType.typeAnnotations;
		if (annotations != Binding.NO_ANNOTATIONS)
			return this.typeSystem.getWildcard((ReferenceBinding) genericType.unannotated(), rank, bound, otherBounds, boundKind, annotations);
	}
	return this.typeSystem.getWildcard(genericType, rank, bound, otherBounds, boundKind);
}

public CaptureBinding createCapturedWildcard(WildcardBinding wildcard, ReferenceBinding contextType, int start, int end, ASTNode cud, int id) {
	return this.typeSystem.getCapturedWildcard(wildcard, contextType, start, end, cud, id);
}

public WildcardBinding createWildcard(ReferenceBinding genericType, int rank, TypeBinding bound, TypeBinding[] otherBounds, int boundKind, AnnotationBinding [] annotations) {
	return this.typeSystem.getWildcard(genericType, rank, bound, otherBounds, boundKind, annotations);
}

/**
 * Returns the access restriction associated to a given type, or null if none
 */
public AccessRestriction getAccessRestriction(TypeBinding type) {
	return (AccessRestriction) this.accessRestrictions.get(type);
}

/**
 *  Answer the type for the compoundName if it exists in the cache.
 * Answer theNotFoundType if it could not be resolved the first time
 * it was looked up, otherwise answer null.
 *
 * NOTE: Do not use for nested types... the answer is NOT the same for a.b.C or a.b.C.D.E
 * assuming C is a type in both cases. In the a.b.C.D.E case, null is the answer.
 */
public ReferenceBinding getCachedType(char[][] compoundName) {
	ReferenceBinding result = getCachedType0(compoundName);
	if (result == null && this.useModuleSystem) {
		ModuleBinding[] modulesToSearch = this.module.isUnnamed() || this.module.isAuto
				? this.root.knownModules.valueTable
				: this.module.getAllRequiredModules();
		for (ModuleBinding someModule : modulesToSearch) {
			if (someModule == null) continue;
			result = someModule.environment.getCachedType0(compoundName);
			if (result != null && result.isValidBinding())
				break;
		}
	}
	return result;
}
private boolean flaggedJavaBaseTypeErrors(ReferenceBinding result, char[][] compoundName) {
	assert result != null && !result.isValidBinding();
	if (CharOperation.equals(TypeConstants.JAVA, compoundName[0])) {
		ReferenceBinding type = getType(compoundName, javaBaseModule());
		if (type != null && type.isValidBinding()) {
			PackageBinding pack = type.getPackage();
			char[] readableName = pack != null ? pack.readableName() : null;
			if (readableName != null) {
				// 7.4.3 : The packages java, java.lang, and java.io are always observable.
				if (CharOperation.equals(readableName, TypeConstants.JAVA)
						|| CharOperation.equals(readableName, CharOperation.concatWith(TypeConstants.JAVA_LANG, '.'))
						|| CharOperation.equals(readableName, CharOperation.concatWith(TypeConstants.JAVA_IO, '.'))) {
					PackageBinding currentPack = getTopLevelPackage(readableName);
					ModuleBinding visibleModule = currentPack != null ? currentPack.enclosingModule : null;
					if (visibleModule != null && visibleModule != javaBaseModule()) {
						// A type from java.base is not visible
						if (!this.globalOptions.enableJdtDebugCompileMode) {
							this.problemReporter.conflictingPackageInModules(compoundName, this.root.unitBeingCompleted, this.missingClassFileLocation,
									readableName, TypeConstants.JAVA_BASE, visibleModule.readableName());
							return true;
						}
					}
				}
			}
		}
	}
	return false;
}

public ReferenceBinding getCachedType0(char[][] compoundName) {
	if (compoundName.length == 1) {
		return this.defaultPackage.getType0(compoundName[0]);
	}
	PackageBinding packageBinding = getPackage0(compoundName[0]);
	if (packageBinding == null || packageBinding == TheNotFoundPackage)
		return null;
	// we should be asking via the correct LE, so peel any SPB at the root:
	packageBinding = packageBinding.getIncarnation(this.module);
	if (packageBinding == null || packageBinding == TheNotFoundPackage)
		return null;

	for (int i = 1, packageLength = compoundName.length - 1; i < packageLength; i++)
		if ((packageBinding = packageBinding.getPackage0Any(compoundName[i])) == null || packageBinding == TheNotFoundPackage)
			return null;
	return packageBinding.getType0(compoundName[compoundName.length - 1]);
}

public AnnotationBinding getNullableAnnotation() {
	if (this.nullableAnnotation != null)
		return this.nullableAnnotation;
	if (this.root != this) {
		return this.nullableAnnotation = this.root.getNullableAnnotation();
	}
	ReferenceBinding nullable = getResolvedType(this.globalOptions.nullableAnnotationName, null);
	return this.nullableAnnotation = this.typeSystem.getAnnotationType(nullable, true);
}

public char[][] getNullableAnnotationName() {
	return this.globalOptions.nullableAnnotationName;
}

public AnnotationBinding getNonNullAnnotation() {
	if (this.nonNullAnnotation != null)
		return this.nonNullAnnotation;
	if (this.root != this) {
		return this.nonNullAnnotation = this.root.getNonNullAnnotation();
	}
	ReferenceBinding nonNull = getResolvedType(this.globalOptions.nonNullAnnotationName, this.UnNamedModule, null, true);
	return this.nonNullAnnotation = this.typeSystem.getAnnotationType(nonNull, true);
}

public AnnotationBinding[] nullAnnotationsFromTagBits(long nullTagBits) {
	if (nullTagBits == TagBits.AnnotationNonNull)
		return new AnnotationBinding[] { getNonNullAnnotation() };
	else if (nullTagBits == TagBits.AnnotationNullable)
		return new AnnotationBinding[] { getNullableAnnotation() };
	return null;
}

public char[][] getNonNullAnnotationName() {
	return this.globalOptions.nonNullAnnotationName;
}

public char[][] getNonNullByDefaultAnnotationName() {
	return this.globalOptions.nonNullByDefaultAnnotationName;
}

int getNullAnnotationBit(char[][] qualifiedTypeName) {
	if (this.allNullAnnotations == null) {
		this.allNullAnnotations = new HashMap<>();
		this.allNullAnnotations.put(CharOperation.toString(this.globalOptions.nonNullAnnotationName), TypeIds.BitNonNullAnnotation);
		this.allNullAnnotations.put(CharOperation.toString(this.globalOptions.nullableAnnotationName), TypeIds.BitNullableAnnotation);
		this.allNullAnnotations.put(CharOperation.toString(this.globalOptions.nonNullByDefaultAnnotationName), TypeIds.BitNonNullByDefaultAnnotation);
		for (String name : this.globalOptions.nullableAnnotationSecondaryNames)
			this.allNullAnnotations.put(name, TypeIds.BitNullableAnnotation);
		for (String name : this.globalOptions.nonNullAnnotationSecondaryNames)
			this.allNullAnnotations.put(name, TypeIds.BitNonNullAnnotation);
		for (String name : this.globalOptions.nonNullByDefaultAnnotationSecondaryNames)
			this.allNullAnnotations.put(name, TypeIds.BitNonNullByDefaultAnnotation);
	}
	String qualifiedTypeString = CharOperation.toString(qualifiedTypeName);
	Integer typeBit = this.allNullAnnotations.get(qualifiedTypeString);
	return typeBit == null ? 0 : typeBit;
}
public boolean isNullnessAnnotationPackage(PackageBinding pkg) {
	return this.nonnullAnnotationPackage == pkg || this.nullableAnnotationPackage == pkg || this.nonnullByDefaultAnnotationPackage == pkg;
}

public boolean usesNullTypeAnnotations() {
	if(this.root != this) {
		return this.root.usesNullTypeAnnotations();
	}
	if (this.globalOptions.useNullTypeAnnotations != null)
		return this.globalOptions.useNullTypeAnnotations;

	initializeUsesNullTypeAnnotation();
	for (MethodBinding enumMethod : this.deferredEnumMethods) {
		int purpose = 0;
		if (CharOperation.equals(enumMethod.selector, TypeConstants.VALUEOF)) {
			purpose = SyntheticMethodBinding.EnumValueOf;
		} else if (CharOperation.equals(enumMethod.selector, TypeConstants.VALUES)) {
			purpose = SyntheticMethodBinding.EnumValues;
		}
		if (purpose != 0)
			SyntheticMethodBinding.markNonNull(enumMethod, purpose, this);
	}
	this.deferredEnumMethods.clear();
	return this.globalOptions.useNullTypeAnnotations;
}

private void initializeUsesNullTypeAnnotation() {
	this.globalOptions.useNullTypeAnnotations = Boolean.FALSE;
	if (!this.globalOptions.isAnnotationBasedNullAnalysisEnabled || this.globalOptions.originalSourceLevel < ClassFileConstants.JDK1_8)
		return;
	ReferenceBinding nullable;
	ReferenceBinding nonNull;
	boolean origMayTolerateMissingType = this.mayTolerateMissingType;
	this.mayTolerateMissingType = true;
	try {
		nullable = this.nullableAnnotation != null ? this.nullableAnnotation.getAnnotationType()
				: getType(this.getNullableAnnotationName(), this.UnNamedModule); // FIXME(SHMOD) module for null annotations??
		nonNull = this.nonNullAnnotation != null ? this.nonNullAnnotation.getAnnotationType()
				: getType(this.getNonNullAnnotationName(), this.UnNamedModule);
	} finally {
		this.mayTolerateMissingType = origMayTolerateMissingType;
	}
	if (nullable == null && nonNull == null)
		return;
	if (nullable == null || nonNull == null)
		return; // TODO should report an error about inconsistent setup
	long nullableMetaBits = nullable.getAnnotationTagBits() & TagBits.AnnotationForTypeUse;
	long nonNullMetaBits = nonNull.getAnnotationTagBits() & TagBits.AnnotationForTypeUse;
	if (nullableMetaBits != nonNullMetaBits)
		return; // TODO should report an error about inconsistent setup
	if (nullableMetaBits == 0)
		return;
	this.globalOptions.useNullTypeAnnotations = Boolean.TRUE;
}

/* Answer the top level package named name if it exists in the cache.
* Answer theNotFoundPackage if it could not be resolved the first time
* it was looked up, otherwise answer null.
*
* NOTE: Senders must convert theNotFoundPackage into a real problem
* package if its to returned.
*/
PackageBinding getPackage0(char[] name) {
	return this.knownPackages.get(name);
}

/* Answer the type corresponding to the compoundName.
* Ask the name environment for the type if its not in the cache.
* Fail with a classpath error if the type cannot be found.
*/
public ReferenceBinding getResolvedType(char[][] compoundName, Scope scope) {
	return getResolvedType(compoundName, scope == null ? this.UnNamedModule : scope.module(), scope, false);
}
public ReferenceBinding getResolvedType(char[][] compoundName, ModuleBinding moduleBinding, Scope scope, boolean implicitAnnotationUse) {
	if (this.module != moduleBinding)
		return moduleBinding.environment.getResolvedType(compoundName, moduleBinding, scope, implicitAnnotationUse);
	ReferenceBinding type = getType(compoundName, moduleBinding);
	if (type != null) return type;

	// create a proxy for the missing BinaryType
	// report the missing class file first
	this.problemReporter.isClassPathCorrect(
		compoundName,
		scope == null ? this.root.unitBeingCompleted : scope.referenceCompilationUnit(),
		this.missingClassFileLocation, implicitAnnotationUse);
	return createMissingType(null, compoundName);
}
public ReferenceBinding getResolvedJavaBaseType(char[][] compoundName, Scope scope) {
	return getResolvedType(compoundName, javaBaseModule(), scope, false);
}

/* Answer the top level package named name.
* Ask the oracle for the package if its not in the cache.
* Answer null if the package cannot be found.
*/
PackageBinding getTopLevelPackage(char[] name) {
	if (this.useModuleSystem) {
		return this.module.getTopLevelPackage(name);
	}
	PackageBinding packageBinding = getPackage0(name);
	if (packageBinding != null) {
		if (packageBinding == TheNotFoundPackage)
			return null;
		return packageBinding;
	}
	if (this.nameEnvironment.isPackage(null, name)) {
		this.knownPackages.put(name, packageBinding = this.module.createDeclaredToplevelPackage(name));
		return packageBinding;
	}

	this.knownPackages.put(name, TheNotFoundPackage); // saves asking the oracle next time
	return null;
}

public ReferenceBinding getType(char[][] compoundName) {
	return getType(compoundName, this.UnNamedModule);
}
/* Answer the type corresponding to the compoundName.
* Ask the name environment for the type if its not in the cache.
* Answer null if the type cannot be found.
*/
public ReferenceBinding getType(char[][] compoundName, ModuleBinding mod) {
	ReferenceBinding referenceBinding;

	if (compoundName.length == 1) {
		if ((referenceBinding = this.defaultPackage.getType0(compoundName[0])) == null) {
			PackageBinding packageBinding = getPackage0(compoundName[0]);
			if (packageBinding != null && packageBinding != TheNotFoundPackage)
				return null; // collides with a known package... should not call this method in such a case
			referenceBinding = askForType(this.defaultPackage, compoundName[0], mod);
		}
	} else {
		PackageBinding packageBinding = getPackage0(compoundName[0]);
		if (packageBinding == TheNotFoundPackage)
			return null;

		if (packageBinding != null) {
			for (int i = 1, packageLength = compoundName.length - 1; i < packageLength; i++) {
				if ((packageBinding = packageBinding.getPackage0(compoundName[i])) == null)
					break;
				if (packageBinding == TheNotFoundPackage)
					return null;
			}
		}

		if (packageBinding == null)
			referenceBinding = askForType(compoundName, mod);
		else if ((referenceBinding = packageBinding.getType0(compoundName[compoundName.length - 1])) == null)
			referenceBinding = askForType(packageBinding, compoundName[compoundName.length - 1], mod);
	}

	if (referenceBinding == null || referenceBinding == TheNotFoundType)
		return null;
	referenceBinding = (ReferenceBinding) BinaryTypeBinding.resolveType(referenceBinding, this, false /* no raw conversion for now */);

	// compoundName refers to a nested type incorrectly (for example, package1.A$B)
//	if (referenceBinding.isNestedType())
//		return new ProblemReferenceBinding(compoundName, referenceBinding, InternalNameProvided);
	return referenceBinding;
}

private TypeBinding[] getTypeArgumentsFromSignature(SignatureWrapper wrapper, TypeVariableBinding[] staticVariables, ReferenceBinding enclosingType, ReferenceBinding genericType,
		char[][][] missingTypeNames, ITypeAnnotationWalker walker)
{
	java.util.ArrayList args = new java.util.ArrayList(2);
	int rank = 0;
	do {
		args.add(getTypeFromVariantTypeSignature(wrapper, staticVariables, enclosingType, genericType, rank, missingTypeNames,
					walker.toTypeArgument(rank++)));
	} while (wrapper.signature[wrapper.start] != '>');
	wrapper.start++; // skip '>'
	TypeBinding[] typeArguments = new TypeBinding[args.size()];
	args.toArray(typeArguments);
	return typeArguments;
}

/* Answer the type corresponding to the compound name.
* Does not ask the oracle for the type if its not found in the cache... instead an
* unresolved type is returned which must be resolved before used.
*
* NOTE: Does NOT answer base types nor array types!
*/
private ReferenceBinding getTypeFromCompoundName(char[][] compoundName, boolean isParameterized, boolean wasMissingType) {
	ReferenceBinding binding = getCachedType(compoundName);
	if (binding == null) {
		PackageBinding packageBinding = computePackageFrom(compoundName, false /* valid pkg */);
		if(this.useModuleSystem) {
			// the package might not have been seen in getCachedType, so retry
			binding = packageBinding.getType0(compoundName[compoundName.length - 1]);
		}
		if(binding == null) {
			binding = new UnresolvedReferenceBinding(compoundName, packageBinding);
			if (wasMissingType) {
				binding.tagBits |= TagBits.HasMissingType; // record it was bound to a missing type
			}
			packageBinding.addType(binding);
		}
	}
	if (binding == TheNotFoundType) {
		// report the missing class file first
		if (!wasMissingType && !flaggedJavaBaseTypeErrors(binding, compoundName)) {
			/* Since missing types have been already been complained against while producing binaries, there is no class path
			 * misconfiguration now that did not also exist in some equivalent form while producing the class files which encode
			 * these missing types. So no need to bark again. Note that wasMissingType == true signals a type referenced in a .class
			 * file which could not be found when the binary was produced. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=364450 */
			this.problemReporter.isClassPathCorrect(compoundName, this.root.unitBeingCompleted, this.missingClassFileLocation, false);
		}
		// create a proxy for the missing BinaryType
		binding = createMissingType(null, compoundName);
	} else if (!isParameterized) {
	    // check raw type, only for resolved types
        binding = (ReferenceBinding) convertUnresolvedBinaryToRawType(binding);
	}
	return binding;
}

/* Answer the type corresponding to the name from the binary file.
* Does not ask the oracle for the type if its not found in the cache... instead an
* unresolved type is returned which must be resolved before used.
*
* NOTE: Does NOT answer base types nor array types!
*/
ReferenceBinding getTypeFromConstantPoolName(char[] signature, int start, int end, boolean isParameterized, char[][][] missingTypeNames, ITypeAnnotationWalker walker) {
	if (end == -1)
		end = signature.length;
	char[][] compoundName = CharOperation.splitOn('/', signature, start, end);
	boolean wasMissingType = false;
	if (missingTypeNames != null) {
		for (int i = 0, max = missingTypeNames.length; i < max; i++) {
			if (CharOperation.equals(compoundName, missingTypeNames[i])) {
				wasMissingType = true;
				break;
			}
		}
	}
	ReferenceBinding binding = getTypeFromCompoundName(compoundName, isParameterized, wasMissingType);
	if (walker != ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER) {
		binding = (ReferenceBinding) annotateType(binding, walker, missingTypeNames);
	}
	return binding;
}

ReferenceBinding getTypeFromConstantPoolName(char[] signature, int start, int end, boolean isParameterized, char[][][] missingTypeNames) {
	return getTypeFromConstantPoolName(signature, start, end, isParameterized, missingTypeNames, ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER);
}

/* Answer the type corresponding to the signature from the binary file.
* Does not ask the oracle for the type if its not found in the cache... instead an
* unresolved type is returned which must be resolved before used.
*
* NOTE: Does answer base types & array types.
*/
TypeBinding getTypeFromSignature(char[] signature, int start, int end, boolean isParameterized, TypeBinding enclosingType,
		char[][][] missingTypeNames, ITypeAnnotationWalker walker)
{
	int dimension = 0;
	while (signature[start] == '[') {
		start++;
		dimension++;
	}
	// annotations on dimensions?
	AnnotationBinding [][] annotationsOnDimensions = null;
	if (dimension > 0 && walker != ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER) {
		for (int i = 0; i < dimension; i++) {
			AnnotationBinding [] annotations = BinaryTypeBinding.createAnnotations(walker.getAnnotationsAtCursor(0, true), this, missingTypeNames);
			if (annotations != Binding.NO_ANNOTATIONS) {
				if (annotationsOnDimensions == null)
					annotationsOnDimensions = new AnnotationBinding[dimension][];
					annotationsOnDimensions[i] = annotations;
			}
			walker = walker.toNextArrayDimension();
		}
	}

	if (end == -1)
		end = signature.length - 1;

	// Just switch on signature[start] - the L case is the else
	TypeBinding binding = null;
	if (start == end) {
		switch (signature[start]) {
			case 'I' :
				binding = TypeBinding.INT;
				break;
			case 'Z' :
				binding = TypeBinding.BOOLEAN;
				break;
			case 'V' :
				binding = TypeBinding.VOID;
				break;
			case 'C' :
				binding = TypeBinding.CHAR;
				break;
			case 'D' :
				binding = TypeBinding.DOUBLE;
				break;
			case 'B' :
				binding = TypeBinding.BYTE;
				break;
			case 'F' :
				binding = TypeBinding.FLOAT;
				break;
			case 'J' :
				binding = TypeBinding.LONG;
				break;
			case 'S' :
				binding = TypeBinding.SHORT;
				break;
			default :
				this.problemReporter.corruptedSignature(enclosingType, signature, start);
				// will never reach here, since error will cause abort
		}
	} else {
		binding = getTypeFromConstantPoolName(signature, start + 1, end, isParameterized, missingTypeNames); // skip leading 'L' or 'T'
	}

	if (isParameterized) {
		if (dimension != 0)
			throw new IllegalStateException();
		return binding;
	}

	if (walker != ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER) {
		binding = annotateType(binding, walker, missingTypeNames);
	}

	if (dimension != 0)
		binding =  this.typeSystem.getArrayType(binding, dimension, AnnotatableTypeSystem.flattenedAnnotations(annotationsOnDimensions));

	return binding;
}

private TypeBinding annotateType(TypeBinding binding, ITypeAnnotationWalker walker, char[][][] missingTypeNames) {
	if (walker == ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER) {
		return binding;
	}
	int depth = binding.depth() + 1;
	if (depth > 1) {
		// need to count non-static nesting levels, resolved binding required for precision
		if (binding.isUnresolvedType())
			binding = ((UnresolvedReferenceBinding) binding).resolve(this, true);
		depth = countNonStaticNestingLevels(binding) + 1;
	}
	AnnotationBinding [][] annotations = null;
	for (int i = 0; i < depth; i++) {
		AnnotationBinding[] annots = BinaryTypeBinding.createAnnotations(walker.getAnnotationsAtCursor(binding.id, i == depth - 1), this, missingTypeNames);
		if (annots != null && annots.length > 0) {
			if (annotations == null)
				annotations = new AnnotationBinding[depth][];
			annotations[i] = annots;
		}
		walker = walker.toNextNestedType();
	}
	if (annotations != null)
		binding = createAnnotatedType(binding, annotations);
	return binding;
}

// compute depth below lowest static enclosingType
private int countNonStaticNestingLevels(TypeBinding binding) {
	if (binding.isUnresolvedType()) {
		throw new IllegalStateException();
	}
	int depth = -1;
	TypeBinding currentBinding = binding;
	while (currentBinding != null) {
		depth++;
		if (currentBinding.isStatic())
			break;
		currentBinding = currentBinding.enclosingType();
	}
	return depth;
}

boolean qualifiedNameMatchesSignature(char[][] name, char[] signature) {
	int s = 1; // skip 'L'
	for (int i = 0; i < name.length; i++) {
		char[] n = name[i];
		for (int j = 0; j < n.length; j++)
			if (n[j] != signature[s++])
				return false;
		if (signature[s] == ';' && i == name.length-1)
			return true;
		if (signature[s++] != '/')
			return false;
	}
	return false;
}

public TypeBinding getTypeFromTypeSignature(SignatureWrapper wrapper, TypeVariableBinding[] staticVariables, ReferenceBinding enclosingType,
		char[][][] missingTypeNames, ITypeAnnotationWalker walker)
{
	// TypeVariableSignature = 'T' Identifier ';'
	// ArrayTypeSignature = '[' TypeSignature
	// ClassTypeSignature = 'L' Identifier TypeArgs(optional) ';'
	//   or ClassTypeSignature '.' 'L' Identifier TypeArgs(optional) ';'
	// TypeArgs = '<' VariantTypeSignature VariantTypeSignatures '>'
	int dimension = 0;
	while (wrapper.signature[wrapper.start] == '[') {
		wrapper.start++;
		dimension++;
	}
	// annotations on dimensions?
	AnnotationBinding [][] annotationsOnDimensions = null;
	if (dimension > 0 && walker != ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER) {
		for (int i = 0; i < dimension; i++) {
			AnnotationBinding [] annotations = BinaryTypeBinding.createAnnotations(walker.getAnnotationsAtCursor(0, true), this, missingTypeNames);
			if (annotations != Binding.NO_ANNOTATIONS) {
				if (annotationsOnDimensions == null)
					annotationsOnDimensions = new AnnotationBinding[dimension][];
					annotationsOnDimensions[i] = annotations;
			}
			walker = walker.toNextArrayDimension();
		}
	}
	if (wrapper.signature[wrapper.start] == 'T') {
	    int varStart = wrapper.start + 1;
	    int varEnd = wrapper.computeEnd();
		for (int i = staticVariables.length; --i >= 0;)
			if (CharOperation.equals(staticVariables[i].sourceName, wrapper.signature, varStart, varEnd))
				return getTypeFromTypeVariable(staticVariables[i], dimension, annotationsOnDimensions, walker, missingTypeNames);
	    ReferenceBinding initialType = enclosingType;
		do {
			TypeVariableBinding[] enclosingTypeVariables;
			if (enclosingType instanceof BinaryTypeBinding) { // compiler normal case, no eager resolution of binary variables
				enclosingTypeVariables = ((BinaryTypeBinding)enclosingType).typeVariables; // do not trigger resolution of variables
			} else { // codepath only use by codeassist for decoding signatures
				enclosingTypeVariables = enclosingType.typeVariables();
			}
			for (int i = enclosingTypeVariables.length; --i >= 0;)
				if (CharOperation.equals(enclosingTypeVariables[i].sourceName, wrapper.signature, varStart, varEnd))
					return getTypeFromTypeVariable(enclosingTypeVariables[i], dimension, annotationsOnDimensions, walker, missingTypeNames);
		} while ((enclosingType = enclosingType.enclosingType()) != null);
		this.problemReporter.undefinedTypeVariableSignature(CharOperation.subarray(wrapper.signature, varStart, varEnd), initialType);
		return null; // cannot reach this, since previous problem will abort compilation
	}
	boolean isParameterized;
	TypeBinding type = getTypeFromSignature(wrapper.signature, wrapper.start, wrapper.computeEnd(), isParameterized = (wrapper.end == wrapper.bracket), enclosingType, missingTypeNames, walker);

	if (!isParameterized)
		return dimension == 0 ? type : createArrayType(type, dimension, AnnotatableTypeSystem.flattenedAnnotations(annotationsOnDimensions));

	// type must be a ReferenceBinding at this point, cannot be a BaseTypeBinding or ArrayTypeBinding
	ReferenceBinding actualType = (ReferenceBinding) type;
	if (walker != ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER && actualType instanceof UnresolvedReferenceBinding)
		if (actualType.depth() > 0)
			actualType = (ReferenceBinding) BinaryTypeBinding.resolveType(actualType, this, false /* no raw conversion */); // must resolve member types before asking for enclosingType
	ReferenceBinding actualEnclosing = actualType.enclosingType();

	ITypeAnnotationWalker savedWalker = walker;
	if(walker != ITypeAnnotationWalker.EMPTY_ANNOTATION_WALKER && actualType.depth() > 0) {
		int nonStaticNestingLevels = countNonStaticNestingLevels(actualType);
		for (int i = 0; i < nonStaticNestingLevels; i++) {
			walker = walker.toNextNestedType();
		}
	}

	TypeBinding[] typeArguments = getTypeArgumentsFromSignature(wrapper, staticVariables, enclosingType, actualType, missingTypeNames, walker);
	ReferenceBinding currentType = createParameterizedType(actualType, typeArguments, actualEnclosing);
	ReferenceBinding plainCurrent = actualType;

	while (wrapper.signature[wrapper.start] == '.') {
		wrapper.start++; // skip '.'
		int memberStart = wrapper.start;
		char[] memberName = wrapper.nextWord();
		plainCurrent = (ReferenceBinding) BinaryTypeBinding.resolveType(plainCurrent, this, false);
		ReferenceBinding memberType = plainCurrent.getMemberType(memberName);
		// need to protect against the member type being null when the signature is invalid
		if (memberType == null)
			this.problemReporter.corruptedSignature(currentType, wrapper.signature, memberStart); // aborts
		if(memberType.isStatic()) {
			// may happen for class files generated by eclipse before bug 460491 was fixed.
			walker = savedWalker;
		} else {
			walker = walker.toNextNestedType();
		}
		if (wrapper.signature[wrapper.start] == '<') {
			wrapper.start++; // skip '<'
			typeArguments = getTypeArgumentsFromSignature(wrapper, staticVariables, enclosingType, memberType, missingTypeNames, walker);
		} else {
			typeArguments = null;
		}
		if (typeArguments != null || 											// has type arguments, or ...
				(!memberType.isStatic() && currentType.isParameterizedType())) 	// ... can see type arguments of enclosing
		{
			if (memberType.isStatic())
				currentType = plainCurrent; // ignore bogus parameterization of enclosing
			currentType = createParameterizedType(memberType, typeArguments, currentType);
		} else {
			currentType = memberType;
		}
		plainCurrent = memberType;
	}
	wrapper.start++; // skip ';'
	TypeBinding annotatedType = annotateType(currentType, savedWalker, missingTypeNames);
	return dimension == 0 ? annotatedType : createArrayType(annotatedType, dimension, AnnotatableTypeSystem.flattenedAnnotations(annotationsOnDimensions));
}

private TypeBinding getTypeFromTypeVariable(TypeVariableBinding typeVariableBinding, int dimension, AnnotationBinding [][] annotationsOnDimensions, ITypeAnnotationWalker walker, char [][][] missingTypeNames) {
	AnnotationBinding [] annotations = BinaryTypeBinding.createAnnotations(walker.getAnnotationsAtCursor(-1, false), this, missingTypeNames);
	if (annotations != null && annotations != Binding.NO_ANNOTATIONS)
		typeVariableBinding = (TypeVariableBinding) createAnnotatedType(typeVariableBinding, new AnnotationBinding [][] { annotations });

	if (dimension == 0) {
		return typeVariableBinding;
	}
	return this.typeSystem.getArrayType(typeVariableBinding, dimension, AnnotatableTypeSystem.flattenedAnnotations(annotationsOnDimensions));
}

TypeBinding getTypeFromVariantTypeSignature(
		SignatureWrapper wrapper,
		TypeVariableBinding[] staticVariables,
		ReferenceBinding enclosingType,
		ReferenceBinding genericType,
		int rank,
		char[][][] missingTypeNames,
		ITypeAnnotationWalker walker) {
	// VariantTypeSignature = '-' TypeSignature
	//   or '+' TypeSignature
	//   or TypeSignature
	//   or '*'
	switch (wrapper.signature[wrapper.start]) {
		case '-' :
			// ? super aType
			wrapper.start++;
			TypeBinding bound = getTypeFromTypeSignature(wrapper, staticVariables, enclosingType, missingTypeNames, walker.toWildcardBound());
			AnnotationBinding [] annotations = BinaryTypeBinding.createAnnotations(walker.getAnnotationsAtCursor(-1, false), this, missingTypeNames);
			return this.typeSystem.getWildcard(genericType, rank, bound, null /*no extra bound*/, Wildcard.SUPER, annotations);
		case '+' :
			// ? extends aType
			wrapper.start++;
			bound = getTypeFromTypeSignature(wrapper, staticVariables, enclosingType, missingTypeNames, walker.toWildcardBound());
			annotations = BinaryTypeBinding.createAnnotations(walker.getAnnotationsAtCursor(-1, false), this, missingTypeNames);
			return this.typeSystem.getWildcard(genericType, rank, bound, null /*no extra bound*/, Wildcard.EXTENDS, annotations);
		case '*' :
			// ?
			wrapper.start++;
			annotations = BinaryTypeBinding.createAnnotations(walker.getAnnotationsAtCursor(-1, false), this, missingTypeNames);
			return this.typeSystem.getWildcard(genericType, rank, null, null /*no extra bound*/, Wildcard.UNBOUND, annotations);
		default :
			return getTypeFromTypeSignature(wrapper, staticVariables, enclosingType, missingTypeNames, walker);
	}
}

boolean isMissingType(char[] typeName) {
	for (int i = this.missingTypes == null ? 0 : this.missingTypes.size(); --i >= 0;) {
		MissingTypeBinding missingType = (MissingTypeBinding) this.missingTypes.get(i);
		if (CharOperation.equals(missingType.sourceName, typeName))
			return true;
	}
	return false;
}

// The method verifier is lazily initialized to guarantee the receiver, the compiler & the oracle are ready.
public MethodVerifier methodVerifier() {
	 // TODO(SHMOD): I'm not sure if the verifier would need to be created with a specific LE?
	if (this.verifier == null)
		this.verifier = newMethodVerifier();
	return this.verifier;
}

public MethodVerifier newMethodVerifier() {
	/* Always use MethodVerifier15. Even in a 1.4 project, we must internalize type variables and
	   observe any parameterization of super class and/or super interfaces in order to be able to
	   detect overriding in the presence of generics.
	   See https://bugs.eclipse.org/bugs/show_bug.cgi?id=324850
	 */
	return new MethodVerifier15(this);
}

public void releaseClassFiles(org.eclipse.jdt.internal.compiler.ClassFile[] classFiles) {
	for (int i = 0, fileCount = classFiles.length; i < fileCount; i++)
		this.classFilePool.release(classFiles[i]);
}

public void reset() {
	if (this.root != this) {
		this.root.reset();
		return;
	}
	this.stepCompleted = 0;
	this.knownModules = new HashtableOfModule();
	this.UnNamedModule = new ModuleBinding.UnNamedModule(this);
	this.module = this.UnNamedModule;
	this.JavaBaseModule = null;

	this.defaultPackage = new PlainPackageBinding(this); // assume the default package always exists
	this.defaultImports = null;
	this.knownPackages = new HashtableOfPackage();
	this.accessRestrictions = new HashMap(3);

	this.verifier = null;

	// NOTE: remember to fix #updateCaches(...) when adding unique binding caches
	this.uniqueParameterizedGenericMethodBindings = new SimpleLookupTable(3);
	this.uniquePolymorphicMethodBindings = new SimpleLookupTable(3);
	this.uniqueGetClassMethodBinding = null;
	this.missingTypes = null;
	this.typesBeingConnected = new LinkedHashSet();

	for (int i = this.units.length; --i >= 0;)
		this.units[i] = null;
	this.lastUnitIndex = -1;
	this.lastCompletedUnitIndex = -1;
	this.unitBeingCompleted = null; // in case AbortException occurred

	this.classFilePool.reset();
	this.typeSystem.reset();
	// name environment has a longer life cycle, and must be reset in
	// the code which created it.
}

/**
 * Associate a given type with some access restriction
 * (did not store the restriction directly into binding, since sparse information)
 */
public void setAccessRestriction(ReferenceBinding type, AccessRestriction accessRestriction) {
	if (accessRestriction == null) return;
	type.modifiers |= ExtraCompilerModifiers.AccRestrictedAccess;
	this.accessRestrictions.put(type, accessRestriction);
}

void updateCaches(UnresolvedReferenceBinding unresolvedType, ReferenceBinding resolvedType) {
	this.typeSystem.updateCaches(unresolvedType, resolvedType);
}

public IQualifiedTypeResolutionListener[] resolutionListeners;	// ROOT_ONLY

public void addResolutionListener(IQualifiedTypeResolutionListener resolutionListener) {
	synchronized (this.root) {
		int length = this.root.resolutionListeners.length;
		for (int i = 0; i < length; i++){
			if (this.root.resolutionListeners[i].equals(resolutionListener))
				return;
		}
		System.arraycopy(this.root.resolutionListeners, 0,
				this.root.resolutionListeners = new IQualifiedTypeResolutionListener[length + 1], 0, length);
		this.root.resolutionListeners[length] = resolutionListener;
	}
}


public TypeBinding getUnannotatedType(TypeBinding typeBinding) {
	return this.typeSystem.getUnannotatedType(typeBinding);
}

// Given a type, return all its variously annotated versions.
public TypeBinding[] getAnnotatedTypes(TypeBinding type) {
	return this.typeSystem.getAnnotatedTypes(type);
}

public AnnotationBinding[] filterNullTypeAnnotations(AnnotationBinding[] typeAnnotations) {
	if (typeAnnotations.length == 0)
		return typeAnnotations;
	AnnotationBinding[] filtered = new AnnotationBinding[typeAnnotations.length];
	int count = 0;
	for (int i = 0; i < typeAnnotations.length; i++) {
		AnnotationBinding typeAnnotation = typeAnnotations[i];
		if (typeAnnotation == null) {
			count++; // sentinel in annotation sequence for array dimensions
		} else {
			if (!typeAnnotation.type.hasNullBit(TypeIds.BitNonNullAnnotation|TypeIds.BitNullableAnnotation))
				filtered[count++] = typeAnnotation;
		}
	}
	if (count == 0)
		return Binding.NO_ANNOTATIONS;
	if (count == typeAnnotations.length)
		return typeAnnotations;
	System.arraycopy(filtered, 0, filtered = new AnnotationBinding[count], 0, count);
	return filtered;
}

public boolean containsNullTypeAnnotation(IBinaryAnnotation[] typeAnnotations) {
	if (typeAnnotations.length == 0)
		return false;
	for (int i = 0; i < typeAnnotations.length; i++) {
		IBinaryAnnotation typeAnnotation = typeAnnotations[i];
		char[] typeName = typeAnnotation.getTypeName();
		// typeName must be "Lfoo/X;"
		if (typeName == null || typeName.length < 3 || typeName[0] != 'L') continue;
		char[][] name = CharOperation.splitOn('/', typeName, 1, typeName.length-1);
		if (getNullAnnotationBit(name) != 0)
			return true;
	}
	return false;
}
public boolean containsNullTypeAnnotation(AnnotationBinding[] typeAnnotations) {
	if (typeAnnotations.length == 0)
		return false;
	for (int i = 0; i < typeAnnotations.length; i++) {
		AnnotationBinding typeAnnotation = typeAnnotations[i];
		if (typeAnnotation.type.hasNullBit(TypeIds.BitNonNullAnnotation|TypeIds.BitNullableAnnotation))
			return true;
	}
	return false;
}

public Binding getInaccessibleBinding(char[][] compoundName, ModuleBinding clientModule) {
	if (this.root != this)
		return this.root.getInaccessibleBinding(compoundName, clientModule);
	if (this.nameEnvironment instanceof IModuleAwareNameEnvironment) {
		IModuleAwareNameEnvironment moduleEnv = (IModuleAwareNameEnvironment) this.nameEnvironment;
		int length = compoundName.length;
		for (int j=length; j>0; j--) {
			char[][] candidateName = CharOperation.subarray(compoundName, 0, j);
			char[][] moduleNames = moduleEnv.getModulesDeclaringPackage(candidateName, ModuleBinding.ANY);
			if (moduleNames != null) {
				// in some module a package named candidateName exists, verify observability & inaccessibility:
				PackageBinding inaccessiblePackage = null;
				for (char[] moduleName : moduleNames) {
					if (moduleName == ModuleBinding.UNOBSERVABLE)
						continue;
					ModuleBinding mod = getModule(moduleName);
					if (mod != null) {
						PackageBinding pack = mod.getVisiblePackage(candidateName);
						if (pack != null && pack.isValidBinding()) {
							if (clientModule.canAccess(pack))
								return null;
							inaccessiblePackage = pack;
						}
					}
				}
				if (inaccessiblePackage == null)
					return null;
				if (j < length) {
					// does the package even contain a type of the next name segment?
					TypeBinding type = inaccessiblePackage.getType(compoundName[j], inaccessiblePackage.enclosingModule);
					if (type instanceof ReferenceBinding && type.isValidBinding())
						return new ProblemReferenceBinding(compoundName, (ReferenceBinding) type, ProblemReasons.NotAccessible);
				}
				return new ProblemPackageBinding(candidateName, ProblemReasons.NotAccessible, this);
			}
		}
	}
	return null;
}
}
