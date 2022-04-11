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
 *     Terry Parker <tparker@google.com>
 *           - Contribution for https://bugs.eclipse.org/bugs/show_bug.cgi?id=372418
 *           -  Another problem with inner classes referenced from jars or class folders: "The type ... cannot be resolved"
 *     Stephan Herrmann - Contribution for
 *								Bug 392727 - Cannot compile project when a java file contains $ in its file name
 *								Bug 440477 - [null] Infrastructure for feeding external annotations into compilation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.builder;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.*;
import org.eclipse.jdt.internal.compiler.env.IUpdatableModule.UpdateKind;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.AbortCompilation;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.eclipse.jdt.internal.core.*;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "unchecked"})
public class NameEnvironment implements IModuleAwareNameEnvironment, SuffixConstants {

boolean isIncrementalBuild;
ClasspathMultiDirectory[] sourceLocations;
ClasspathLocation[] binaryLocations;
Map<String,IModulePathEntry> modulePathEntries; // is null when performing a non-modular compilation
BuildNotifier notifier;

SimpleSet initialTypeNames; // assumed that each name is of the form "a/b/ClassName", or, if a module is given: "my.mod:a/b/ClassName"
SimpleLookupTable additionalUnits;
private CompilationGroup compilationGroup;
/** Tasks resulting from add-reads or add-exports classpath attributes. */
ModuleUpdater moduleUpdater;

NameEnvironment(IWorkspaceRoot root, JavaProject javaProject, SimpleLookupTable binaryLocationsPerProject, BuildNotifier notifier, CompilationGroup compilationGroup) throws CoreException {
	this.compilationGroup = compilationGroup;
	this.isIncrementalBuild = false;
	this.notifier = notifier;
	computeClasspathLocations(root, javaProject, binaryLocationsPerProject);
	setNames(null, null);
}

public NameEnvironment(IJavaProject javaProject, CompilationGroup compilationGroup) {
	this.isIncrementalBuild = false;
	this.compilationGroup = compilationGroup;
	try {
		computeClasspathLocations(javaProject.getProject().getWorkspace().getRoot(), (JavaProject) javaProject, null);
	} catch(CoreException e) {
		this.sourceLocations = new ClasspathMultiDirectory[0];
		this.binaryLocations = new ClasspathLocation[0];
	}
	setNames(null, null);
}

/* Some examples of resolved class path entries.
* Remember to search class path in the order that it was defined.
*
* 1a. typical project with no source folders:
*   /Test[CPE_SOURCE][K_SOURCE] -> D:/eclipse.test/Test
* 1b. project with source folders:
*   /Test/src1[CPE_SOURCE][K_SOURCE] -> D:/eclipse.test/Test/src1
*   /Test/src2[CPE_SOURCE][K_SOURCE] -> D:/eclipse.test/Test/src2
*  NOTE: These can be in any order & separated by prereq projects or libraries
* 1c. project external to workspace (only detectable using getLocation()):
*   /Test/src[CPE_SOURCE][K_SOURCE] -> d:/eclipse.zzz/src
*  Need to search source folder & output folder
*
* 2. zip files:
*   D:/j9/lib/jclMax/classes.zip[CPE_LIBRARY][K_BINARY][sourcePath:d:/j9/lib/jclMax/source/source.zip]
*      -> D:/j9/lib/jclMax/classes.zip
*  ALWAYS want to take the library path as is
*
* 3a. prereq project (regardless of whether it has a source or output folder):
*   /Test[CPE_PROJECT][K_SOURCE] -> D:/eclipse.test/Test
*  ALWAYS want to append the output folder & ONLY search for .class files
*/
private void computeClasspathLocations(
	IWorkspaceRoot root,
	JavaProject javaProject,
	SimpleLookupTable binaryLocationsPerProject) throws CoreException {

	/* Update cycle marker */
	IMarker cycleMarker = javaProject.getCycleMarker();
	if (cycleMarker != null) {
		int severity = JavaCore.ERROR.equals(javaProject.getOption(JavaCore.CORE_CIRCULAR_CLASSPATH, true))
			? IMarker.SEVERITY_ERROR
			: IMarker.SEVERITY_WARNING;
		if (severity != cycleMarker.getAttribute(IMarker.SEVERITY, severity))
			cycleMarker.setAttribute(IMarker.SEVERITY, severity);
	}

	IClasspathEntry[] classpathEntries = javaProject.getExpandedClasspath(this.compilationGroup == CompilationGroup.MAIN);
	ArrayList<ClasspathLocation> sLocations = new ArrayList<>(classpathEntries.length);
	ArrayList<ClasspathLocation> bLocations = new ArrayList<>(classpathEntries.length);
	ArrayList sLocationsForTest = new ArrayList(classpathEntries.length);
	Map<String, IModulePathEntry> moduleEntries = null;
	String compliance = javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
	if (CompilerOptions.versionToJdkLevel(compliance) >= ClassFileConstants.JDK9) {
		moduleEntries = new LinkedHashMap<>(classpathEntries.length);
		this.moduleUpdater = new ModuleUpdater(javaProject);
		if (this.compilationGroup == CompilationGroup.TEST) {
			this.moduleUpdater.addReadUnnamedForNonEmptyClasspath(javaProject, classpathEntries);
		}
	}
	IModuleDescription projectModule = javaProject.getModuleDescription();

	String patchedModuleName = ModuleEntryProcessor.pushPatchToFront(classpathEntries, javaProject);
	IModule patchedModule = null;

	nextEntry : for (int i = 0, l = classpathEntries.length; i < l; i++) {
		if (i == 1) {
			if (patchedModuleName != null) {
				// TODO(SHMOD) assert that patchModule has been assigned
				patchedModuleName = null; // expire, applies to the first entry, only
			}
		}
		ClasspathEntry entry = (ClasspathEntry) classpathEntries[i];
		IPath path = entry.getPath();
		Object target = JavaModel.getTarget(path, true);
		IPath externalAnnotationPath = ClasspathEntry.getExternalAnnotationPath(entry, javaProject.getProject(), true);
		if (target == null) continue nextEntry;
		boolean isOnModulePath = isOnModulePath(entry);

		Set<String> limitModules = ModuleEntryProcessor.computeLimitModules(entry);
		if (patchedModuleName != null &&  limitModules != null && !limitModules.contains(patchedModuleName)) {
			// TODO(SHMOD) report an error
			patchedModuleName = null;
		}

		if (this.moduleUpdater != null && (this.compilationGroup == CompilationGroup.TEST || !entry.isTest()))
			this.moduleUpdater.computeModuleUpdates(entry);

		switch(entry.getEntryKind()) {
			case IClasspathEntry.CPE_SOURCE :
				if (!(target instanceof IContainer)) continue nextEntry;
				IPath outputPath = entry.getOutputLocation() != null
					? entry.getOutputLocation()
					: javaProject.getOutputLocation();
				IContainer outputFolder;
				if (outputPath.segmentCount() == 1) {
					outputFolder = javaProject.getProject();
				} else {
					outputFolder = root.getFolder(outputPath);
					if (!outputFolder.exists())
						createOutputFolder(outputFolder);
				}
				if (this.compilationGroup == CompilationGroup.TEST && !entry.isTest()) {
					ClasspathLocation bLocation = ClasspathLocation.forBinaryFolder(outputFolder, true, entry.getAccessRuleSet(), externalAnnotationPath, isOnModulePath);
					bLocations.add(bLocation);
					sLocationsForTest.add(bLocation);
					if (patchedModule != null) {
						ModuleEntryProcessor.combinePatchIntoModuleEntry(bLocation, patchedModule, moduleEntries);
					}
					bLocation.patchModuleName = patchedModuleName;
				} else {
					ClasspathLocation sourceLocation = ClasspathLocation.forSourceFolder(
								(IContainer) target,
								outputFolder,
								entry.fullInclusionPatternChars(),
								entry.fullExclusionPatternChars(),
								entry.ignoreOptionalProblems(),
								externalAnnotationPath);
					if (patchedModule != null) {
						ModuleEntryProcessor.combinePatchIntoModuleEntry(sourceLocation, patchedModule, moduleEntries);
					}
					sLocations.add(sourceLocation);
					sourceLocation.patchModuleName = patchedModuleName;
				}
				continue nextEntry;

			case IClasspathEntry.CPE_PROJECT :
				if (!(target instanceof IProject)) continue nextEntry;
				IProject prereqProject = (IProject) target;
				if (!JavaProject.hasJavaNature(prereqProject)) continue nextEntry; // if project doesn't have java nature or is not accessible

				JavaProject prereqJavaProject = (JavaProject) JavaCore.create(prereqProject);
				IClasspathEntry[] prereqClasspathEntries = prereqJavaProject.getRawClasspath();
				ArrayList seen = new ArrayList();
				List<ClasspathLocation> projectLocations = new ArrayList<ClasspathLocation>();
				nextPrereqEntry: for (int j = 0, m = prereqClasspathEntries.length; j < m; j++) {
					IClasspathEntry prereqEntry = prereqClasspathEntries[j];
					if (prereqEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						if ((this.compilationGroup == CompilationGroup.MAIN || entry.isWithoutTestCode()) && prereqEntry.isTest())
							continue nextPrereqEntry;
						IPath srcExtAnnotPath = (externalAnnotationPath != null)
							? externalAnnotationPath
							: ClasspathEntry.getExternalAnnotationPath(prereqEntry, javaProject.getProject(), true);
						Object prereqTarget = JavaModel.getTarget(prereqEntry.getPath(), true);
						if (!(prereqTarget instanceof IContainer)) continue nextPrereqEntry;
						if (srcExtAnnotPath == null) {
							// search in other sources contributing to the same binary location (that other loc will be skipped below, due to seen.contains()):
							IPath outputLoc = prereqEntry.getOutputLocation();
							for (int k = j+1; k < m; k++) {
								IClasspathEntry other = prereqClasspathEntries[k];
								if (other.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
									IPath otherOutput = other.getOutputLocation();
									if ((outputLoc == null) ? otherOutput == null : outputLoc.equals(otherOutput)) {
										srcExtAnnotPath = ClasspathEntry.getExternalAnnotationPath(other, javaProject.getProject(),  true);
										if (srcExtAnnotPath != null)
											break; // TODO: merging of several .eea?
									}
								}
							}
						}
						IPath prereqOutputPath = prereqEntry.getOutputLocation() != null
							? prereqEntry.getOutputLocation()
							: prereqJavaProject.getOutputLocation();
						IContainer binaryFolder = prereqOutputPath.segmentCount() == 1
							? (IContainer) prereqProject
							: (IContainer) root.getFolder(prereqOutputPath);
						if (binaryFolder.exists() && !seen.contains(binaryFolder)) {
							seen.add(binaryFolder);
							ClasspathLocation bLocation = ClasspathLocation.forBinaryFolder(binaryFolder, true, entry.getAccessRuleSet(),
									srcExtAnnotPath, isOnModulePath);
							bLocations.add(bLocation);
							projectLocations.add(bLocation);
							if (binaryLocationsPerProject != null) { // normal builder mode
								ClasspathLocation[] existingLocations = (ClasspathLocation[]) binaryLocationsPerProject.get(prereqProject);
								if (existingLocations == null) {
									existingLocations = new ClasspathLocation[] {bLocation};
								} else {
									int size = existingLocations.length;
									System.arraycopy(existingLocations, 0, existingLocations = new ClasspathLocation[size + 1], 0, size);
									existingLocations[size] = bLocation;
								}
								binaryLocationsPerProject.put(prereqProject, existingLocations);
							}
						}
					}
				}
				if (moduleEntries != null && isOnModulePath && projectLocations.size() > 0) {
					IModule info = null;
					try {
						IModuleDescription mod;
						if ((mod = prereqJavaProject.getModuleDescription()) != null) {
							AbstractModule aModule = (AbstractModule) mod;
							info = aModule.getModuleInfo();
						}
					} catch (JavaModelException jme) {
						// do nothing, probably a non module project
					}
					if (info == null)
						info = IModule.createAutomatic(prereqJavaProject.getElementName(), false, prereqJavaProject.getManifest());
					ModulePathEntry projectEntry = new ModulePathEntry(prereqJavaProject.getPath(), info,
							projectLocations.toArray(new ClasspathLocation[projectLocations.size()]));
					String moduleName = String.valueOf(info.name());
					IUpdatableModule.UpdatesByKind updates = this.moduleUpdater.getUpdates(moduleName);
					for (ClasspathLocation loc : projectLocations) {
						loc.limitModuleNames = limitModules;
						loc.updates = updates;
						loc.patchModuleName = patchedModuleName;
					}
					if (limitModules == null || limitModules.contains(moduleName)) {
						moduleEntries.put(moduleName, projectEntry);
						if (moduleName.equals(patchedModuleName))
							patchedModule = info;
					}
				}
				continue nextEntry;

			case IClasspathEntry.CPE_LIBRARY :
				if (target instanceof IResource) {
					IResource resource = (IResource) target;
					ClasspathLocation bLocation = null;
					if (resource instanceof IFile) {
						AccessRuleSet accessRuleSet =
							(JavaCore.IGNORE.equals(javaProject.getOption(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, true))
							&& JavaCore.IGNORE.equals(javaProject.getOption(JavaCore.COMPILER_PB_DISCOURAGED_REFERENCE, true)))
								? null
								: entry.getAccessRuleSet();
						bLocation = ClasspathLocation.forLibrary((IFile) resource, accessRuleSet, externalAnnotationPath, isOnModulePath, compliance);
					} else if (resource instanceof IContainer) {
						AccessRuleSet accessRuleSet =
							(JavaCore.IGNORE.equals(javaProject.getOption(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, true))
							&& JavaCore.IGNORE.equals(javaProject.getOption(JavaCore.COMPILER_PB_DISCOURAGED_REFERENCE, true)))
								? null
								: entry.getAccessRuleSet();
						bLocation = ClasspathLocation.forBinaryFolder((IContainer) target, false, accessRuleSet, externalAnnotationPath, isOnModulePath);	 // is library folder not output folder
					}
					bLocations.add(bLocation);
					// TODO: Ideally we need to do something like mapToModulePathEntry using the path and if it is indeed
					// a module path entry, then add the corresponding entry here, but that would need the target platform
					if (moduleEntries != null) {
						patchedModule = collectModuleEntries(bLocation, path, isOnModulePath,
											limitModules, patchedModuleName, patchedModule, moduleEntries);
					}
					if (binaryLocationsPerProject != null) { // normal builder mode
						IProject p = resource.getProject(); // can be the project being built
						ClasspathLocation[] existingLocations = (ClasspathLocation[]) binaryLocationsPerProject.get(p);
						if (existingLocations == null) {
							existingLocations = new ClasspathLocation[] {bLocation};
						} else {
							int size = existingLocations.length;
							System.arraycopy(existingLocations, 0, existingLocations = new ClasspathLocation[size + 1], 0, size);
							existingLocations[size] = bLocation;
						}
						binaryLocationsPerProject.put(p, existingLocations);
					}
				} else if (target instanceof File) {
					AccessRuleSet accessRuleSet =
						(JavaCore.IGNORE.equals(javaProject.getOption(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, true))
							&& JavaCore.IGNORE.equals(javaProject.getOption(JavaCore.COMPILER_PB_DISCOURAGED_REFERENCE, true)))
								? null
								: entry.getAccessRuleSet();
					String release = JavaCore.ENABLED.equals(javaProject.getOption(JavaCore.COMPILER_RELEASE, false)) ? compliance : null;
					ClasspathLocation bLocation = null;
					String libPath = path.toOSString();
					if (Util.isJrt(libPath)) {
						bLocation = ClasspathLocation.forJrtSystem(path.toOSString(), accessRuleSet, externalAnnotationPath, release);
					} else {
						bLocation = ClasspathLocation.forLibrary(path.toOSString(), accessRuleSet, externalAnnotationPath, isOnModulePath, compliance);
					}
					bLocations.add(bLocation);
					if (moduleEntries != null) {
						Set<String> libraryLimitModules = (limitModules == null && projectModule != null) ? ClasspathJrt.NO_LIMIT_MODULES : limitModules;
						patchedModule = collectModuleEntries(bLocation, path, isOnModulePath,
											libraryLimitModules, patchedModuleName, patchedModule, moduleEntries);
					}
				}
				continue nextEntry;
		}
	}

	if (JavaCore.ENABLED.equals(javaProject.getOption(JavaCore.CORE_JAVA_BUILD_EXTERNAL_ANNOTATIONS_FROM_ALL_LOCATIONS, true))) {
		// make all locations known to each other:
		List<ClasspathLocation> allLocationsForEEA = new ArrayList<>();
		for (ClasspathLocation loc : bLocations)
			loc.connectAllLocationsForEEA(allLocationsForEEA, true);
		for (ClasspathLocation loc : sLocations)
			loc.connectAllLocationsForEEA(allLocationsForEEA, true);
	}

	// now split the classpath locations... place the output folders ahead of the other .class file folders & jars
	ArrayList outputFolders = new ArrayList(1);
	this.sourceLocations = new ClasspathMultiDirectory[sLocations.size()];
	if (!sLocations.isEmpty()) {
		sLocations.toArray(this.sourceLocations);
		if (moduleEntries != null && projectModule != null) {
			try {
				AbstractModule sourceModule = (AbstractModule)projectModule;
				IModule info = (IModule) sourceModule.getElementInfo();
				final ClasspathLocation[] sourceLocations2;
				if(sLocationsForTest.size() == 0) {
					sourceLocations2 = this.sourceLocations;
				} else {
					ArrayList<ClasspathLocation> sourceLocationsForModulePathEntry=new ArrayList<>(sLocations.size()+sLocationsForTest.size());
					sourceLocationsForModulePathEntry.addAll(sLocations);
					sourceLocationsForModulePathEntry.addAll(sLocationsForTest);
					sourceLocations2= sourceLocationsForModulePathEntry
							.toArray(new ClasspathLocation[sourceLocationsForModulePathEntry.size()]);
				}
				ModulePathEntry projectEntry = new ModulePathEntry(javaProject.getPath(), info, sourceLocations2);
				if (!moduleEntries.containsKey(sourceModule.getElementName())) { // can be registered already, if patching
					moduleEntries.put(sourceModule.getElementName(), projectEntry);
				}
			} catch (JavaModelException jme) {
				// do nothing, probably a non module project
			}
		}
		// collect the output folders, skipping duplicates
		next : for (int i = 0, l = this.sourceLocations.length; i < l; i++) {
			ClasspathMultiDirectory md = this.sourceLocations[i];
			IPath outputPath = md.binaryFolder.getFullPath();
			for (int j = 0; j < i; j++) { // compare against previously walked source folders
				if (outputPath.equals(this.sourceLocations[j].binaryFolder.getFullPath())) {
					md.hasIndependentOutputFolder = this.sourceLocations[j].hasIndependentOutputFolder;
					continue next;
				}
			}
			outputFolders.add(md);

			// also tag each source folder whose output folder is an independent folder & is not also a source folder
			for (int j = 0, m = this.sourceLocations.length; j < m; j++)
				if (outputPath.equals(this.sourceLocations[j].sourceFolder.getFullPath()))
					continue next;
			md.hasIndependentOutputFolder = true;
		}
	}

	// combine the output folders with the binary folders & jars... place the output folders before other .class file folders & jars
	this.binaryLocations = new ClasspathLocation[outputFolders.size() + bLocations.size()];
	int index = 0;
	for (int i = 0, l = outputFolders.size(); i < l; i++)
		this.binaryLocations[index++] = (ClasspathLocation) outputFolders.get(i);
	for (int i = 0, l = bLocations.size(); i < l; i++)
		this.binaryLocations[index++] = bLocations.get(i);

	if (moduleEntries != null && !moduleEntries.isEmpty())
		this.modulePathEntries = moduleEntries;
}

/** Returns the patched module if that is served by the current (binary) location. */
IModule collectModuleEntries(ClasspathLocation bLocation, IPath path, boolean isOnModulePath, Set<String> limitModules,
								String patchedModuleName, IModule patchedModule, Map<String, IModulePathEntry> moduleEntries) {
	if (bLocation instanceof IMultiModuleEntry) {
		IMultiModuleEntry binaryModulePathEntry = (IMultiModuleEntry) bLocation;
		bLocation.limitModuleNames = limitModules;
		bLocation.patchModuleName = patchedModuleName;
		IUpdatableModule.UpdatesByKind updates = null;//new IUpdatableModule.UpdatesByKind();
		IUpdatableModule.UpdatesByKind finalUpdates = new IUpdatableModule.UpdatesByKind();
		List<Consumer<IUpdatableModule>> packageUpdates = null;
		List<Consumer<IUpdatableModule>> moduleUpdates = null;
		for (String moduleName : binaryModulePathEntry.getModuleNames(limitModules)) {
			moduleEntries.put(moduleName, binaryModulePathEntry);
			updates = this.moduleUpdater.getUpdates(moduleName);
			if (updates != null) {
				List<Consumer<IUpdatableModule>> pu = updates.getList(UpdateKind.PACKAGE, false);
				if (pu != null) {
					(packageUpdates = finalUpdates.getList(UpdateKind.PACKAGE, true)).addAll(pu);
				}
				List<Consumer<IUpdatableModule>> mu = updates.getList(UpdateKind.MODULE, false);
				if (mu != null) {
					(moduleUpdates = finalUpdates.getList(UpdateKind.MODULE, true)).addAll(mu);
				}
			}
		}
		if (packageUpdates != null || moduleUpdates != null) {
			bLocation.updates = finalUpdates;
		}
		if (patchedModuleName != null) {
			IModule module = binaryModulePathEntry.getModule(patchedModuleName.toCharArray());
			if (module != null)
				return module;
			// TODO(SHMOD): report problem: patchedModuleName didn't match a module from this location
		}
	} else if (isOnModulePath) {
		IModulePathEntry binaryModulePathEntry = new ModulePathEntry(path, bLocation);
		IModule module = binaryModulePathEntry.getModule();
		if (module != null) {
			String moduleName = String.valueOf(module.name());
			bLocation.updates = this.moduleUpdater.getUpdates(moduleName);
			bLocation.limitModuleNames = limitModules;
			bLocation.patchModuleName = patchedModuleName;
			if (limitModules == null || limitModules == ClasspathJrt.NO_LIMIT_MODULES || limitModules.contains(moduleName)) {
				moduleEntries.put(moduleName, binaryModulePathEntry);
				if (patchedModuleName != null) {
					if (moduleName.equals(patchedModuleName))
						return module;
					// TODO(SHMOD): report problem: patchedModuleName didn't match a module from this location
				}
			}
		}
	}
	return patchedModule;
}

protected boolean isOnModulePath(ClasspathEntry entry) {
	return entry.isModular();
}

@Override
public void cleanup() {
	this.initialTypeNames = null;
	this.additionalUnits = null;
	for (int i = 0, l = this.sourceLocations.length; i < l; i++)
		this.sourceLocations[i].cleanup();
	for (int i = 0, l = this.binaryLocations.length; i < l; i++)
		this.binaryLocations[i].cleanup();
	// assume modulePathEntries are cleaned-up via the corresponding source/binaryLocations
}

private void createOutputFolder(IContainer outputFolder) throws CoreException {
	createParentFolder(outputFolder.getParent());
	((IFolder) outputFolder).create(IResource.FORCE | IResource.DERIVED, true, null);
}

private void createParentFolder(IContainer parent) throws CoreException {
	if (!parent.exists()) {
		createParentFolder(parent.getParent());
		((IFolder) parent).create(true, true, null);
	}
}

private NameEnvironmentAnswer findClass(String qualifiedTypeName, char[] typeName, LookupStrategy strategy, String moduleName) {
	if (this.notifier != null)
		this.notifier.checkCancelWithinCompiler();

	String moduleQualifiedName = moduleName != null ? moduleName+':'+qualifiedTypeName : qualifiedTypeName;
	if (this.initialTypeNames != null && this.initialTypeNames.includes(moduleQualifiedName)) {
		if (this.isIncrementalBuild)
			// catch the case that a type inside a source file has been renamed but other class files are looking for it
			throw new AbortCompilation(true, new AbortIncrementalBuildException(qualifiedTypeName));
		return null; // looking for a file which we know was provided at the beginning of the compilation
	}

	if (this.additionalUnits != null && this.sourceLocations.length > 0) {
		// if an additional source file is waiting to be compiled, answer it BUT not if this is a secondary type search
		// if we answer X.java & it no longer defines Y then the binary type looking for Y will think the class path is wrong
		// let the recompile loop fix up dependents when the secondary type Y has been deleted from X.java
		// Only enclosing type names are present in the additional units table, so strip off inner class specifications
		// when doing the lookup (https://bugs.eclipse.org/372418).
		// Also take care of $ in the name of the class (https://bugs.eclipse.org/377401)
		// and prefer name with '$' if unit exists rather than failing to search for nested class (https://bugs.eclipse.org/392727)
		SourceFile unit = (SourceFile) this.additionalUnits.get(qualifiedTypeName); // doesn't have file extension
		if (unit != null)
			return new NameEnvironmentAnswer(unit, null /*no access restriction*/);
		int index = qualifiedTypeName.indexOf('$');
		if (index > 0) {
			String enclosingTypeName = qualifiedTypeName.substring(0, index);
			unit = (SourceFile) this.additionalUnits.get(enclosingTypeName); // doesn't have file extension
			if (unit != null)
				return new NameEnvironmentAnswer(unit, null /*no access restriction*/);
		}
	}

	String qBinaryFileName = qualifiedTypeName + SUFFIX_STRING_class;
	String qPackageName =  (qualifiedTypeName.length() == typeName.length) ? Util.EMPTY_STRING :
		qBinaryFileName.substring(0, qBinaryFileName.length() - typeName.length - 7);
	char[] binaryFileName = CharOperation.concat(typeName, SUFFIX_class);

	ClasspathLocation[] relevantLocations;
	if (moduleName != null && this.modulePathEntries != null) {
		IModulePathEntry modulePathEntry = this.modulePathEntries.get(moduleName);
		if (modulePathEntry instanceof ModulePathEntry) {
			relevantLocations = ((ModulePathEntry) modulePathEntry).getClasspathLocations();
		} else if (modulePathEntry instanceof ClasspathLocation) {
			return ((ClasspathLocation) modulePathEntry).findClass(typeName, qPackageName, moduleName, qBinaryFileName, false,
																	null/*module already checked*/);
		} else {
			return null;
		}
	} else {
		relevantLocations = this.binaryLocations;
	}
	NameEnvironmentAnswer suggestedAnswer = null;
	for (ClasspathLocation classpathLocation : relevantLocations) {
		if (!strategy.matches(classpathLocation, ClasspathLocation::hasModule)) {
			continue;
		}
		NameEnvironmentAnswer answer = classpathLocation.findClass(binaryFileName, qPackageName, moduleName, qBinaryFileName, false,
																	this.modulePathEntries != null ? this.modulePathEntries::containsKey : null);
		if (answer != null) {
			char[] answerMod = answer.moduleName();
			if (answerMod != null && this.modulePathEntries != null) {
				if (!this.modulePathEntries.containsKey(String.valueOf(answerMod)))
					continue; // assumed to be filtered out by --limit-modules
			}
			if (!answer.ignoreIfBetter()) {
				if (answer.isBetter(suggestedAnswer))
					return answer;
			} else if (answer.isBetter(suggestedAnswer))
				// remember suggestion and keep looking
				suggestedAnswer = answer;
		}
	}
	return suggestedAnswer;
}

@Override
public NameEnvironmentAnswer findType(char[][] compoundName, char[] moduleName) {
	if (compoundName != null)
		return findClass(
			String.valueOf(CharOperation.concatWith(compoundName, '/')),
			compoundName[compoundName.length - 1],
			LookupStrategy.get(moduleName),
			LookupStrategy.getStringName(moduleName));
	return null;
}

@Override
public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName, char[] moduleName) {
	return findClass(
			String.valueOf(CharOperation.concatWith(packageName, typeName, '/')),
			typeName,
			LookupStrategy.get(moduleName),
			LookupStrategy.getStringName(moduleName));
}

@Override
public char[][] getModulesDeclaringPackage(char[][] packageName, char[] moduleName) {
	String pkgName = new String(CharOperation.concatWith(packageName, '/'));
	String modName = new String(moduleName);
	LookupStrategy strategy = LookupStrategy.get(moduleName);
	switch (strategy) {
		// include unnamed (search all locations):
		case Any:
		case Unnamed:
			char[][] names = CharOperation.NO_CHAR_CHAR;
			for (ClasspathLocation location : this.binaryLocations) {
				if (strategy.matches(location, ClasspathLocation::hasModule)) {
					char[][] declaringModules = location.getModulesDeclaringPackage(pkgName, null);
					if (declaringModules != null)
						names = CharOperation.arrayConcat(names, declaringModules);
				}
			}
			for (ClasspathLocation location : this.sourceLocations) {
				if (strategy.matches(location, ClasspathLocation::hasModule)) {
					char[][] declaringModules = location.getModulesDeclaringPackage(pkgName, null);
					if (declaringModules != null)
						names = CharOperation.arrayConcat(names, declaringModules);
				}
			}
			return names == CharOperation.NO_CHAR_CHAR ? null : names;

		// only named (rely on modulePathEntries):
		case AnyNamed:
			modName = null;
			//$FALL-THROUGH$
		default:
			if (this.modulePathEntries != null) {
				names = CharOperation.NO_CHAR_CHAR;
				Collection<IModulePathEntry> entries = new HashSet<>(this.modulePathEntries.values());
				for (IModulePathEntry modulePathEntry : entries) {
					char[][] declaringModules = modulePathEntry.getModulesDeclaringPackage(pkgName, modName);
					if (declaringModules != null)
						names = CharOperation.arrayConcat(names, declaringModules);
				}
				return names == CharOperation.NO_CHAR_CHAR ? null : names;
			}
	}
	return null;
}

@Override
public boolean hasCompilationUnit(char[][] qualifiedPackageName, char[] moduleName, boolean checkCUs) {
	String pkgName = String.valueOf(CharOperation.concatWith(qualifiedPackageName, '/'));
	LookupStrategy strategy = LookupStrategy.get(moduleName);
	String modName = LookupStrategy.getStringName(moduleName);
	switch (strategy) {
		// include unnamed (search all locations):
		case Any:
		case Unnamed:
			for (ClasspathLocation location : this.binaryLocations) {
				if (strategy.matches(location, ClasspathLocation::hasModule))
					if (location.hasCompilationUnit(pkgName, null))
						return true;
			}
			for (ClasspathLocation location : this.sourceLocations) {
				if (strategy.matches(location, ClasspathLocation::hasModule))
					if (location.hasCompilationUnit(pkgName, null))
						return true;
			}
			return false;
		// only named (rely on modulePathEntries):
		case Named:
			if (this.modulePathEntries != null) {
				IModulePathEntry modulePathEntry = this.modulePathEntries.get(modName);
				return modulePathEntry != null && modulePathEntry.hasCompilationUnit(pkgName, modName);
			}
			return false;
		case AnyNamed:
			if (this.modulePathEntries != null) {
				for (IModulePathEntry modulePathEntry : this.modulePathEntries.values())
					if (modulePathEntry.hasCompilationUnit(pkgName, modName))
						return true;
			}
			return false;
		default:
			throw new IllegalArgumentException("Unexpected LookupStrategy "+strategy); //$NON-NLS-1$
	}
}
public boolean isPackage(String qualifiedPackageName, char[] moduleName) {
	String stringModuleName = null;

	LookupStrategy strategy = LookupStrategy.get(moduleName);
	Collection<IModulePathEntry> entries = null;
	switch (strategy) {
		case Any:
		case Unnamed:
			// NOTE: the output folders are added at the beginning of the binaryLocations
			for (int i = 0, l = this.binaryLocations.length; i < l; i++) {
				if (strategy.matches(this.binaryLocations[i], ClasspathLocation::hasModule))
					if (this.binaryLocations[i].isPackage(qualifiedPackageName, null))
						return true;
			}
			for (int i = 0, l = this.sourceLocations.length; i < l; i++) {
				if (strategy.matches(this.sourceLocations[i], ClasspathLocation::hasModule))
					if (this.sourceLocations[i].isPackage(qualifiedPackageName, null))
						return true;
			}
			return false;
		case AnyNamed:
			entries = this.modulePathEntries.values();
			break;
		default:
			stringModuleName = String.valueOf(moduleName);
			IModulePathEntry entry = this.modulePathEntries.get(stringModuleName);
			if (entry == null)
				return false;
			entries = Collections.singletonList(entry);
	}
	for (IModulePathEntry modulePathEntry : entries) {
		if (modulePathEntry instanceof ModulePathEntry) {
			for (ClasspathLocation classpathLocation : ((ModulePathEntry) modulePathEntry).getClasspathLocations()) {
				if (classpathLocation.isPackage(qualifiedPackageName, stringModuleName))
					return true;
			}
		} else if (modulePathEntry instanceof ClasspathLocation) {
			return ((ClasspathLocation) modulePathEntry).isPackage(qualifiedPackageName, stringModuleName);
		}
	}
	return false;
}
@Override
public char[][] listPackages(char[] moduleName) {
	LookupStrategy strategy = LookupStrategy.get(moduleName);
	switch (strategy) {
		case Named:
			IModulePathEntry entry = this.modulePathEntries.get(String.valueOf(moduleName));
			if (entry == null)
				return CharOperation.NO_CHAR_CHAR;
			return entry.listPackages();
		default:
			throw new UnsupportedOperationException("can list packages only of a named module"); //$NON-NLS-1$
	}
}
void setNames(String[] typeNames, SourceFile[] additionalFiles) {
	// convert the initial typeNames to a set
	if (typeNames == null) {
		this.initialTypeNames = null;
	} else {
		this.initialTypeNames = new SimpleSet(typeNames.length);
		for (int i = 0, l = typeNames.length; i < l; i++)
			this.initialTypeNames.add(typeNames[i]);
	}
	// map the additional source files by qualified type name
	if (additionalFiles == null) {
		this.additionalUnits = null;
	} else {
		this.additionalUnits = new SimpleLookupTable(additionalFiles.length);
		for (int i = 0, l = additionalFiles.length; i < l; i++) {
			SourceFile additionalUnit = additionalFiles[i];
			if (additionalUnit != null)
				this.additionalUnits.put(additionalUnit.initialTypeName, additionalFiles[i]);
		}
	}

	for (int i = 0, l = this.sourceLocations.length; i < l; i++)
		this.sourceLocations[i].reset();
	for (int i = 0, l = this.binaryLocations.length; i < l; i++)
		this.binaryLocations[i].reset();
}

@Override
public IModule getModule(char[] name) {
	if (this.modulePathEntries != null) {
		IModulePathEntry modulePathEntry = this.modulePathEntries.get(String.valueOf(name));
		if (modulePathEntry instanceof IMultiModuleEntry)
			return modulePathEntry.getModule(name);
		else if (modulePathEntry != null)
			return modulePathEntry.getModule();
	}
	return null;
}

@Override
public char[][] getAllAutomaticModules() {
	if (this.modulePathEntries == null)
		return CharOperation.NO_CHAR_CHAR;
	Set<char[]> set = this.modulePathEntries.values().stream().filter(m -> m.isAutomaticModule()).map(e -> e.getModule().name())
			.collect(Collectors.toSet());
	return set.toArray(new char[set.size()][]);
}
@Override
public void applyModuleUpdates(IUpdatableModule compilerModule, IUpdatableModule.UpdateKind kind) {
	if (this.moduleUpdater != null)
		this.moduleUpdater.applyModuleUpdates(compilerModule, kind);
}
}
