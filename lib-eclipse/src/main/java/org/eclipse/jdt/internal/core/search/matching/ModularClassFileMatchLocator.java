/*******************************************************************************
 * Copyright (c)  2017 IBM Corporation and others.
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
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.search.ModuleReferenceMatch;
import org.eclipse.jdt.core.search.PackageReferenceMatch;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeReferenceMatch;
import org.eclipse.jdt.internal.compiler.env.IBinaryModule;
import org.eclipse.jdt.internal.compiler.env.IModule.IPackageExport;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.PackageBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.core.ModularClassFile;
import org.eclipse.jdt.internal.core.search.indexing.IIndexConstants;

public class ModularClassFileMatchLocator implements IIndexConstants {

	private IBinaryModule binaryModule;
	private ModularClassFile modularClassFile;
	private IModuleDescription moduleDesc;
	private char[] moduleName;
	private ModuleBinding module;

	public void locateMatches(MatchLocator locator, ModularClassFile mClassFile) throws CoreException {
		SearchPattern pattern = locator.pattern;
		this.modularClassFile = mClassFile;
		this.binaryModule = this.modularClassFile.getBinaryModuleInfo();
		if (this.binaryModule == null) return;

		// cache all the details
		this.moduleDesc = mClassFile.getModule();
		this.moduleName = this.binaryModule.name();
		this.module = locator.lookupEnvironment.getModule(this.moduleName);

		matchModuleDeclaration(pattern, locator);
		matchModuleReferences(pattern, locator);
		matchPackageReferences(pattern, locator);
		matchTypeReferences(pattern, locator);
	}
	private void matchModuleDeclaration(SearchPattern pattern, MatchLocator locator) throws CoreException {
		switch (pattern.kind) {
			case MODULE_PATTERN:
				break;
			case OR_PATTERN:
				SearchPattern[] patterns = ((OrPattern) pattern).patterns;
				for (int i = 0, length = patterns.length; i < length; i++) {
					SearchPattern p = patterns[i];
					if (p.kind == MODULE_PATTERN)
						matchModuleReferences(patterns[i], locator);
				}
				// $FALL-THROUGH$ - fall through default to return
			default:
				return;
		}
		ModulePattern modulePattern  = (ModulePattern) pattern;
		if (!locator.patternLocator.matchesName(modulePattern.name, this.moduleName))
			return;
		ModuleBinding moduleBinding = null;
		int level = PatternLocator.ACCURATE_MATCH;
		if (locator.patternLocator.mustResolve) {
			moduleBinding = locator.lookupEnvironment.getModule(this.moduleName);
			level = locator.patternLocator.resolveLevel(moduleBinding);
		}
		if (level == PatternLocator.IMPOSSIBLE_MATCH)
			return;
		int accuracy = level == PatternLocator.ACCURATE_MATCH ? SearchMatch.A_ACCURATE : SearchMatch.A_INACCURATE;
		SearchMatch match = locator.newDeclarationMatch(this.moduleDesc, moduleBinding, accuracy, -1, 0);
		locator.report(match);
	}
	private void matchModuleReferences(SearchPattern pattern, MatchLocator locator) throws CoreException {
		// Only process Module patterns
		switch (pattern.kind) {
			case MODULE_PATTERN:
				break;
			case OR_PATTERN:
				SearchPattern[] patterns = ((OrPattern) pattern).patterns;
				for (int i = 0, length = patterns.length; i < length; i++) {
					SearchPattern p = patterns[i];
					if (p.kind == MODULE_PATTERN)
						matchModuleReferences(patterns[i], locator);
				}
				// $FALL-THROUGH$ - fall through default to return
			default:
				return;
		}
		ModulePattern modulePattern  = (ModulePattern) pattern;
		if (!modulePattern.findReferences) {
			return;
		}
		matchModuleReferences(locator, modulePattern, this.binaryModule.exports());
		matchModuleReferences(locator, modulePattern, this.binaryModule.opens());
		if (this.module != null) {
			matchModuleReferences(locator, modulePattern, this.module.getAllRequiredModules());
		}
	}
	private void matchModuleReference(MatchLocator locator, ModulePattern modulePattern,
			char[][] modules, boolean isTarget) throws CoreException {
		if (modules == null)
			return;
		for (char[] module1 : modules) {
			if (module1 == null || module1.length == 0) continue;
			if (!locator.patternLocator.matchesName(modulePattern.name, module1)) continue;
			// no resolve for target modules - report accurate match else resolve
			ModuleReferenceMatch match = locator.newModuleReferenceMatch(this.moduleDesc, null, isTarget ? SearchMatch.A_ACCURATE : SearchMatch.A_INACCURATE, -1, 0, null);
			locator.report(match);
		}
	}
	private void matchModuleReferences(MatchLocator locator, ModulePattern modulePattern,
			IPackageExport[] pvs) throws CoreException {
		if (pvs == null) return;
		for (IPackageExport pv : pvs) {
			matchModuleReference(locator, modulePattern, pv.targets(), true /* isTarget */);
		}
	}
	private void matchModuleReferences(MatchLocator locator, ModulePattern modulePattern,
			ModuleBinding[] refs) throws CoreException {
		if (refs == null) return;
		for (ModuleBinding ref : refs) {
			char[] name = ref.name();
			if (name == null) continue;
			int level =  locator.patternLocator.resolveLevel(ref);
			if (level == PatternLocator.IMPOSSIBLE_MATCH) continue;
			int accuracy = level == PatternLocator.ACCURATE_MATCH ? SearchMatch.A_ACCURATE : SearchMatch.A_INACCURATE;
			ModuleReferenceMatch match = locator.newModuleReferenceMatch(this.moduleDesc, null, accuracy, -1, 0, null);
			locator.report(match);
		}
	}
	private void matchPackageReferences(SearchPattern pattern, MatchLocator locator) throws CoreException {
		// Only process PackageReference patterns
		switch (pattern.kind) {
			case PKG_REF_PATTERN:
				break;
			case OR_PATTERN:
				SearchPattern[] patterns = ((OrPattern) pattern).patterns;
				for (int i = 0, length = patterns.length; i < length; i++) {
					SearchPattern p = patterns[i];
					if (p.kind == PKG_REF_PATTERN)
						matchPackageReferences(patterns[i], locator);
				}
				// $FALL-THROUGH$ - fall through default to return
			default:
				return;
		}
		matchPackReferences(locator, (PackageReferencePattern) pattern, this.module.getExports());
		matchPackReferences(locator, (PackageReferencePattern) pattern, this.module.getOpens());
		matchPackReferences(locator, (PackageReferencePattern) pattern, this.module.getUses());
		TypeBinding[] services = this.module.getServices();
		if (services != null) {
			matchPackReferences(locator, (PackageReferencePattern) pattern, services);
			for (TypeBinding service : services) {
				matchPackReferences(locator, (PackageReferencePattern) pattern, this.module.getImplementations(service));
			}
		}
	}
	private void matchPackReferences(MatchLocator locator, PackageReferencePattern packReferencePattern,
			PackageBinding[] packBindings) throws CoreException {
		if (packBindings == null) return;
		for (PackageBinding pb : packBindings) {
			reportPackageMatch(locator, pb);
		}
	}
	private void reportPackageMatch(MatchLocator locator, PackageBinding packageBinding) throws CoreException{
		if (packageBinding == null) return;
		int level =  locator.patternLocator.resolveLevel(packageBinding);
		if (level == PatternLocator.IMPOSSIBLE_MATCH) return;
		int accuracy = level == PatternLocator.ACCURATE_MATCH ? SearchMatch.A_ACCURATE : SearchMatch.A_INACCURATE;
		PackageReferenceMatch match = locator.newPackageReferenceMatch(this.moduleDesc, accuracy, -1, 0, null);
		locator.report(match);
	}
	private void matchPackReferences(MatchLocator locator, PackageReferencePattern packReferencePattern,
			TypeBinding[] types) throws CoreException {
		if (types == null) return;
		for (TypeBinding type : types) {
			reportPackageMatch(locator, type.getPackage());
		}
	}
	private void matchTypeReferences(SearchPattern pattern, MatchLocator locator) throws CoreException {
		// Only process TypeReference patterns
		switch (pattern.kind) {
			case TYPE_REF_PATTERN:
				break;
			case OR_PATTERN:
				SearchPattern[] patterns = ((OrPattern) pattern).patterns;
				for (int i = 0, length = patterns.length; i < length; i++) {
					SearchPattern p = patterns[i];
					if (p.kind == TYPE_REF_PATTERN)
						matchTypeReferences(patterns[i], locator);
				}
				// $FALL-THROUGH$ - fall through default to return
			default:
				return;
		}
		matchTypeReferences(locator, (TypeReferencePattern) pattern, this.module.getUses());
		TypeBinding[] services = this.module.getServices();
		if (services != null) {
			matchTypeReferences(locator, (TypeReferencePattern) pattern, services);
			for (TypeBinding service : services) {
				matchTypeReferences(locator, (TypeReferencePattern) pattern, this.module.getImplementations(service));
			}
		}
	}
	private void matchTypeReferences(MatchLocator locator, TypeReferencePattern typeReferencePattern,
			TypeBinding[] types) throws CoreException {
		if (types == null) 	return;
		for (TypeBinding type : types) {
			int level =  locator.patternLocator.resolveLevel(type);
			if (level == PatternLocator.IMPOSSIBLE_MATCH) continue;
			int accuracy = level == PatternLocator.ACCURATE_MATCH ? SearchMatch.A_ACCURATE : SearchMatch.A_INACCURATE;
			TypeReferenceMatch match = locator.newTypeReferenceMatch(this.moduleDesc, null, accuracy, -1, 0, null);
			locator.report(match);
		}
	}
}
