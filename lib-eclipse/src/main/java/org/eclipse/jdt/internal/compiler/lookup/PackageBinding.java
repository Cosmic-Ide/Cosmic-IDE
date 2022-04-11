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
 *     Stephan Herrmann - Contributions for
 *								bug 186342 - [compiler][null] Using annotations for null checking
 *								bug 365519 - editorial cleanup after bug 186342 and bug 365387
 *								bug 365531 - [compiler][null] investigate alternative strategy for internally encoding nullness defaults
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.ArrayList;
import java.util.function.Predicate;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.IModuleAwareNameEnvironment;
import org.eclipse.jdt.internal.compiler.util.HashtableOfPackage;
import org.eclipse.jdt.internal.compiler.util.HashtableOfType;

public abstract class PackageBinding extends Binding implements TypeConstants {
	public long tagBits = 0; // See values in the interface TagBits below

	public char[][] compoundName;
	PackageBinding parent;
	ArrayList<SplitPackageBinding> wrappingSplitPackageBindings;
	public LookupEnvironment environment;
	/** Types in this map are either uniquely visible in the current module or ProblemReferenceBindings. */
	public HashtableOfType knownTypes;
	/** All visible member packages, i.e. observable packages associated with modules read by the current module. */
	HashtableOfPackage<PackageBinding> knownPackages;

	// code representing the default that has been defined for this package (using @NonNullByDefault)
	// once initialized it will be one of Binding.{NO_NULL_DEFAULT,NULL_UNSPECIFIED_BY_DEFAULT,NONNULL_BY_DEFAULT}
	private int defaultNullness = -1;

	public ModuleBinding enclosingModule;

	/** Is this package exported from its module? NB: to query this property use {@link #isExported()} to ensure initialization. */
	Boolean isExported;

protected PackageBinding(char[][] compoundName, LookupEnvironment environment) {
	// for creating problem package
	this.compoundName = compoundName;
	this.environment = environment;
}

/* Create a normal package.
*/
public PackageBinding(char[][] compoundName, PackageBinding parent, LookupEnvironment environment, ModuleBinding enclosingModule) {
	this.compoundName = compoundName;
	this.parent = parent;
	this.environment = environment;
	this.knownTypes = null; // initialized if used... class counts can be very large 300-600
	this.knownPackages = new HashtableOfPackage<PackageBinding>(3); // sub-package counts are typically 0-3

	if (compoundName != CharOperation.NO_CHAR_CHAR)
		checkIfNullAnnotationPackage();

	if (enclosingModule != null)
		this.enclosingModule = enclosingModule;
	else if (parent != null)
		this.enclosingModule = parent.enclosingModule; // stop-gap for any remaining calls that don't provide an enclosingModule (they should)

	if (this.enclosingModule == null)
		throw new IllegalStateException("Package should have an enclosing module"); //$NON-NLS-1$
}

protected void addNotFoundPackage(char[] simpleName) {
	if (!this.environment.suppressImportErrors)
		this.knownPackages.put(simpleName, LookupEnvironment.TheNotFoundPackage);
}
private void addNotFoundType(char[] simpleName) {
	if (this.environment.suppressImportErrors)
		return;
	if (this.knownTypes == null)
		this.knownTypes = new HashtableOfType(25);
	this.knownTypes.put(simpleName, LookupEnvironment.TheNotFoundType);
}
/**
 * Remembers a sub-package.
 * For a split parent package this will include potentially enriching with siblings,
 * in which case the enriched (split) binding will be returned.
 */
PackageBinding addPackage(PackageBinding element, ModuleBinding module) {
	if ((element.tagBits & TagBits.HasMissingType) == 0) clearMissingTagBit();
	this.knownPackages.put(element.compoundName[element.compoundName.length - 1], element);
	return element;
}
void addType(ReferenceBinding element) {
	if ((element.tagBits & TagBits.HasMissingType) == 0) clearMissingTagBit();
	if (this.knownTypes == null)
		this.knownTypes = new HashtableOfType(25);
	char [] name = element.compoundName[element.compoundName.length - 1];
	ReferenceBinding priorType = this.knownTypes.getput(name, element);
	if (priorType != null && priorType.isUnresolvedType() && !element.isUnresolvedType()) {
		((UnresolvedReferenceBinding) priorType).setResolvedType(element, this.environment);
	}
	if (this.environment.globalOptions.isAnnotationBasedNullAnalysisEnabled)
		if (element.isAnnotationType() || element instanceof UnresolvedReferenceBinding) // unresolved types don't yet have the modifiers set
			checkIfNullAnnotationType(element);

	if (!element.isUnresolvedType() && this.wrappingSplitPackageBindings != null) {
		for (SplitPackageBinding splitPackageBinding : this.wrappingSplitPackageBindings) {
			if (splitPackageBinding.knownTypes != null) {
				ReferenceBinding prior = splitPackageBinding.knownTypes.get(name);
				if (prior != null && prior.isUnresolvedType() && !element.isUnresolvedType()) {
					((UnresolvedReferenceBinding) prior).setResolvedType(element, this.environment);
					splitPackageBinding.knownTypes.put(name, null); // forces re-checking for conflicts
				}
			}
		}
	}
}

ModuleBinding[] getDeclaringModules() {
	return new ModuleBinding[] { this.enclosingModule };
}

void clearMissingTagBit() {
	PackageBinding current = this;
	do {
		current.tagBits &= ~TagBits.HasMissingType;
	} while ((current = current.parent) != null);
}
/*
 * slash separated name
 * org.eclipse.jdt.core --> org/eclipse/jdt/core
 */
@Override
public char[] computeUniqueKey(boolean isLeaf) {
	return CharOperation.concatWith(this.compoundName, '/');
}
protected PackageBinding findPackage(char[] name, ModuleBinding module) {
	// delegate to the module to consider the module graph:
	return module.getVisiblePackage(CharOperation.arrayConcat(this.compoundName, name));
}
/* Answer the subpackage named name; ask the oracle for the package if its not in the cache.
* Answer null if it could not be resolved.
*
* NOTE: This should only be used when we know there is NOT a type with the same name.
*/
PackageBinding getPackage(char[] name, ModuleBinding mod) {
	PackageBinding binding = getPackage0(name);
	if (binding != null) {
		if (binding == LookupEnvironment.TheNotFoundPackage)
			return null;
		else
			return binding;
	}
	if ((binding = findPackage(name, mod)) != null)
		return binding;

	// not found so remember a problem package binding in the cache for future lookups
	addNotFoundPackage(name);
	return null;
}
/** Answer the subpackage named name if it exists in the cache.
* Answer theNotFoundPackage if it could not be resolved the first time
* it was looked up, otherwise answer null.
* <p>
* NOTE: The returned package binding is guaranteed to be complete wrt. SplitPackageBinding,
* or, if no complete binding is yet available, we shyly answer null.
* </p><p>
* NOTE: Senders must convert theNotFoundPackage into a real problem
* package if its to returned.</p>
*/
PackageBinding getPackage0(char[] name) {
	return this.knownPackages.get(name);
}
/** Variant (see {@link #getPackage0(char[])}), that may even answer an incompletely
 *  combined package (in the case of SplitPackageBinding).
 */
PackageBinding getPackage0Any(char[] name) {
	return this.knownPackages.get(name);
}
/* Answer the type named name; ask the oracle for the type if its not in the cache.
* Answer a NotVisible problem type if the type is not visible from the invocationPackage.
* Answer null if it could not be resolved.
*
* NOTE: This should only be used by source types/scopes which know there is NOT a
* package with the same name.
*/

ReferenceBinding getType(char[] name, ModuleBinding mod) {
	ReferenceBinding referenceBinding = getType0(name);
	if (referenceBinding == null) {
		if ((referenceBinding = this.environment.askForType(this, name, mod)) == null) {
			// not found so remember a problem type binding in the cache for future lookups
			addNotFoundType(name);
			return null;
		}
	}

	if (referenceBinding == LookupEnvironment.TheNotFoundType)
		return null;

	referenceBinding = (ReferenceBinding) BinaryTypeBinding.resolveType(referenceBinding, this.environment, false /* no raw conversion for now */);
	if (referenceBinding.isNestedType())
		return new ProblemReferenceBinding(new char[][]{ name }, referenceBinding, ProblemReasons.InternalNameProvided);
	if (!mod.canAccess(this))
		return new ProblemReferenceBinding(referenceBinding.compoundName, referenceBinding, ProblemReasons.NotAccessible);
	// at this point we have only checked accessibility of the package, accessibility of the type will be checked by callers
	return referenceBinding;
}
/* Answer the type named name if it exists in the cache.
* Answer theNotFoundType if it could not be resolved the first time
* it was looked up, otherwise answer null.
*
* NOTE: Senders must convert theNotFoundType into a real problem
* reference type if its to returned.
*/

ReferenceBinding getType0(char[] name) {
	if (this.knownTypes == null)
		return null;
	return this.knownTypes.get(name);
}

/**
 * Test if this package (or any of its incarnations in case of a SplitPackageBinding) has recorded
 * an actual, resolved type of the given name (based on answers from getType0()).
 * Useful for clash detection.
 */
boolean hasType0Any(char[] name) {
	ReferenceBinding type0 = getType0(name);
	return type0 != null && type0.isValidBinding() && !(type0 instanceof UnresolvedReferenceBinding);
}

/* Answer the package or type named name; ask the oracle if it is not in the cache.
* Answer null if it could not be resolved.
*
* When collisions exist between a type name & a package name, answer the type.
* Treat the package as if it does not exist... a problem was already reported when the type was defined.
*
* NOTE: no visibility checks are performed.
* THIS SHOULD ONLY BE USED BY SOURCE TYPES/SCOPES.
*/

public Binding getTypeOrPackage(char[] name, ModuleBinding mod, boolean splitPackageAllowed) {
	ReferenceBinding problemBinding = null;
	ReferenceBinding referenceBinding = getType0(name);
	lookForType0:
	if (referenceBinding != null && referenceBinding != LookupEnvironment.TheNotFoundType) {
		referenceBinding = (ReferenceBinding) BinaryTypeBinding.resolveType(referenceBinding, this.environment, false /* no raw conversion for now */);
		if (referenceBinding.isNestedType()) {
			return new ProblemReferenceBinding(new char[][]{name}, referenceBinding, ProblemReasons.InternalNameProvided);
		}
		boolean isSameModule = (this instanceof SplitPackageBinding) ? referenceBinding.module() == mod : this.enclosingModule == mod;
		if (!isSameModule && referenceBinding.isValidBinding() && !mod.canAccess(referenceBinding.fPackage)) {
			problemBinding = new ProblemReferenceBinding(referenceBinding.compoundName, referenceBinding, ProblemReasons.NotAccessible);
			break lookForType0;
		}
		if ((referenceBinding.tagBits & TagBits.HasMissingType) == 0) {
			return referenceBinding;
		}
		// referenceBinding is a MissingType, will return it if no package is found
	}

	PackageBinding packageBinding = getPackage0(name);
	if (packageBinding != null && packageBinding != LookupEnvironment.TheNotFoundPackage) {
		if (!splitPackageAllowed) {
			return packageBinding.getVisibleFor(mod, false);
		}
		return packageBinding;
	}
	lookForType:
	if (referenceBinding == null && problemBinding == null) { // have not looked for it before
		if ((referenceBinding = this.environment.askForType(this, name, mod)) != null) {
			if (referenceBinding.isNestedType()) {
				return new ProblemReferenceBinding(new char[][]{name}, referenceBinding, ProblemReasons.InternalNameProvided);
			}
			if (referenceBinding.isValidBinding() && !mod.canAccess(referenceBinding.fPackage)) {
				problemBinding = new ProblemReferenceBinding(referenceBinding.compoundName, referenceBinding, ProblemReasons.NotAccessible);
				break lookForType;
			} else {
				return referenceBinding;
			}
		}

		// Since name could not be found, add a problem binding
		// to the collections so it will be reported as an error next time.
		addNotFoundType(name);
	}

	if (packageBinding == null) { // have not looked for it before
		if ((packageBinding = findPackage(name, mod)) != null) {
			if (!splitPackageAllowed) {
				return packageBinding.getVisibleFor(mod, false);
			}
			return packageBinding;
		}
		if (referenceBinding != null && referenceBinding != LookupEnvironment.TheNotFoundType) {
			if (problemBinding != null)
				return problemBinding;
			return referenceBinding; // found cached missing type - check if package conflict
		}
		addNotFoundPackage(name);
	}

	return problemBinding;
}
public final boolean isViewedAsDeprecated() {
	if ((this.tagBits & TagBits.DeprecatedAnnotationResolved) == 0) {
		this.tagBits |= TagBits.DeprecatedAnnotationResolved;
		if (this.compoundName != CharOperation.NO_CHAR_CHAR) {
			ReferenceBinding packageInfo = this.getType(TypeConstants.PACKAGE_INFO_NAME, this.enclosingModule);
			if (packageInfo != null) {
				packageInfo.initializeDeprecatedAnnotationTagBits();
				this.tagBits |= packageInfo.tagBits & TagBits.AllStandardAnnotationsMask;
			}
		}
	}
	return (this.tagBits & TagBits.AnnotationDeprecated) != 0;
}
private void initDefaultNullness() {
	if (this.defaultNullness == -1) {
		ReferenceBinding packageInfo = getType(TypeConstants.PACKAGE_INFO_NAME, this.enclosingModule);
		if (packageInfo != null) {
			packageInfo.getAnnotationTagBits();
			if (packageInfo instanceof SourceTypeBinding) {
				this.defaultNullness = ((SourceTypeBinding) packageInfo).defaultNullness;
			} else {
				this.defaultNullness = ((BinaryTypeBinding) packageInfo).defaultNullness;
			}
		} else {
			this.defaultNullness = NO_NULL_DEFAULT;
		}
	}
}
public int getDefaultNullness() {
	initDefaultNullness();
	if (this.defaultNullness == NO_NULL_DEFAULT)
		return this.enclosingModule.getDefaultNullness();
	return this.defaultNullness;
}
public void setDefaultNullness(int nullness) {
	this.defaultNullness = nullness;
}
/**
 * Find a binding (either this package or its enclosing ModuleBinding)
 * where 'defaultNullness' matches the given predicate.
 */
public Binding findDefaultNullnessTarget(Predicate<Integer> predicate) {
	initDefaultNullness();
	if (predicate.test(this.defaultNullness))
		return this;
	if (this.defaultNullness == NO_NULL_DEFAULT)
		if (predicate.test(this.enclosingModule.getDefaultNullness()))
			return this.enclosingModule;
	return null;
}
/* API
* Answer the receiver's binding type from Binding.BindingID.
*/
@Override
public final int kind() {
	return Binding.PACKAGE;
}

@Override
public int problemId() {
	if ((this.tagBits & TagBits.HasMissingType) != 0)
		return ProblemReasons.NotFound;
	return ProblemReasons.NoError;
}


void checkIfNullAnnotationPackage() {
	LookupEnvironment env = this.environment;
	if (env.globalOptions.isAnnotationBasedNullAnalysisEnabled) {
		if (isPackageOfQualifiedTypeName(this.compoundName, env.getNullableAnnotationName()))
			env.nullableAnnotationPackage = this;
		if (isPackageOfQualifiedTypeName(this.compoundName, env.getNonNullAnnotationName()))
			env.nonnullAnnotationPackage = this;
		if (isPackageOfQualifiedTypeName(this.compoundName, env.getNonNullByDefaultAnnotationName()))
			env.nonnullByDefaultAnnotationPackage = this;
	}
}
private boolean isPackageOfQualifiedTypeName(char[][] packageName, char[][] typeName) {
	int length;
	if (typeName == null || (length = packageName.length) != typeName.length -1)
		return false;
	for (int i=0; i<length; i++)
		if (!CharOperation.equals(packageName[i], typeName[i]))
			return false;
	return true;
}

void checkIfNullAnnotationType(ReferenceBinding type) {
	// check if type is one of the configured null annotation types
	// if so mark as a well known type using the corresponding typeBit:
	if (this.environment.nullableAnnotationPackage == this
			&& CharOperation.equals(type.compoundName, this.environment.getNullableAnnotationName())) {
		type.typeBits |= TypeIds.BitNullableAnnotation;
		if (!(type instanceof UnresolvedReferenceBinding)) // unresolved will need to check back for the resolved type
			this.environment.nullableAnnotationPackage = null; // don't check again
	} else if (this.environment.nonnullAnnotationPackage == this
			&& CharOperation.equals(type.compoundName, this.environment.getNonNullAnnotationName())) {
		type.typeBits |= TypeIds.BitNonNullAnnotation;
		if (!(type instanceof UnresolvedReferenceBinding)) // unresolved will need to check back for the resolved type
			this.environment.nonnullAnnotationPackage = null; // don't check again
	} else if (this.environment.nonnullByDefaultAnnotationPackage == this
			&& CharOperation.equals(type.compoundName, this.environment.getNonNullByDefaultAnnotationName())) {
		type.typeBits |= TypeIds.BitNonNullByDefaultAnnotation;
		if (!(type instanceof UnresolvedReferenceBinding)) // unresolved will need to check back for the resolved type
			this.environment.nonnullByDefaultAnnotationPackage = null; // don't check again
	} else {
		type.typeBits |= this.environment.getNullAnnotationBit(type.compoundName);
	}
}

@Override
public char[] readableName() /*java.lang*/ {
	return CharOperation.concatWith(this.compoundName, '.');
}
@Override
public String toString() {
	String str;
	if (this.compoundName == CharOperation.NO_CHAR_CHAR) {
		str = "The Default Package"; //$NON-NLS-1$
	} else {
		str = "package " + ((this.compoundName != null) ? CharOperation.toString(this.compoundName) : "UNNAMED"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	if ((this.tagBits & TagBits.HasMissingType) != 0) {
		str += "[MISSING]"; //$NON-NLS-1$
	}
	return str;
}
public boolean isDeclaredIn(ModuleBinding moduleBinding) {
	return this.enclosingModule == moduleBinding;
}
public boolean subsumes(PackageBinding binding) {
	return binding == this;
}
/**
 * Is this package exported from its module?
 * Does not consider export restrictions.
 */
public boolean isExported() {
	if (this.isExported == null) {
		if (this.enclosingModule.isAuto) {
			this.isExported = Boolean.TRUE;
		} else {
			this.enclosingModule.getExports(); // ensure resolved and completed
			if (this.isExported == null)
				this.isExported = Boolean.FALSE;
		}
	}
	return this.isExported == Boolean.TRUE;
}
/**
 * If this package is uniquely visible to 'module' return a plain PackageBinding.
 * In case of a conflict between a local package and foreign package flag <b>preferLocal</b>
 * will select the behavior:
 * <ul>
 * <li>if {@code true} the plain local package is returned, because this conflict will more
 * appropriately be reported against the package declaration, not its references.</li>
 * <li>if {@code false} a conflict local vs. foreign will be treated just like any other conflict,
 * see next.</li>
 * </ul>
 * In case of multiple accessible foreign packages a SplitPackageBinding is returned
 * to indicate a conflict.
 */
public PackageBinding getVisibleFor(ModuleBinding module, boolean preferLocal) {
	return this;
}

public abstract PlainPackageBinding getIncarnation(ModuleBinding moduleBinding);

public boolean hasCompilationUnit(boolean checkCUs) {
	if (this.knownTypes != null) {
		for (ReferenceBinding knownType : this.knownTypes.valueTable) {
			if (knownType != null && knownType != LookupEnvironment.TheNotFoundType && !knownType.isUnresolvedType())
				return true;
		}
	}
	if (this.environment.useModuleSystem) {
		IModuleAwareNameEnvironment moduleEnv = (IModuleAwareNameEnvironment) this.environment.nameEnvironment;
		return moduleEnv.hasCompilationUnit(this.compoundName, this.enclosingModule.nameForCUCheck(), checkCUs);
	}
	return false;
}

public void addWrappingSplitPackageBinding(SplitPackageBinding splitPackageBinding) {
	if (this.wrappingSplitPackageBindings == null) {
		this.wrappingSplitPackageBindings = new ArrayList<>();
	}
	this.wrappingSplitPackageBindings.add(splitPackageBinding);
}
}
