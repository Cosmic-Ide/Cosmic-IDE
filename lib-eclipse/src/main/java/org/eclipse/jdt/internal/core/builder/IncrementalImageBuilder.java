/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.builder;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.internal.compiler.*;
import org.eclipse.jdt.internal.compiler.classfmt.*;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.problem.*;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.CompilationGroup;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

/**
 * The incremental image builder
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class IncrementalImageBuilder extends AbstractImageBuilder {

protected LinkedHashSet<SourceFile> sourceFiles;
protected LinkedHashSet<SourceFile> previousSourceFiles;
protected Set<String> qualifiedStrings;
protected Set<String> simpleStrings;
protected Set<String> rootStrings;
protected SimpleLookupTable secondaryTypesToRemove;
protected boolean hasStructuralChanges;
protected boolean makeOutputFolderConsistent;

private IncrementalImageBuilder testImageBuilder;

public static int MaxCompileLoop = 5; // perform a full build if it takes more than ? incremental compile loops

protected IncrementalImageBuilder(JavaBuilder javaBuilder, State buildState, CompilationGroup compilationGroup) {
	super(javaBuilder, true, buildState, compilationGroup);
	this.nameEnvironment.isIncrementalBuild = true;
	this.makeOutputFolderConsistent = JavaCore.ENABLED.equals(
		javaBuilder.javaProject.getOption(JavaCore.CORE_JAVA_BUILD_RECREATE_MODIFIED_CLASS_FILES_IN_OUTPUT_FOLDER, true));
	if (compilationGroup == CompilationGroup.MAIN) {
		final IncrementalImageBuilder builder = new IncrementalImageBuilder(javaBuilder, this.newState,
				CompilationGroup.TEST);
		if (builder.sourceLocations.length > 0) {
			this.testImageBuilder = builder;
			this.testImageBuilder.resetCollections();
		}
	}
}

protected IncrementalImageBuilder(JavaBuilder javaBuilder) {
	this(javaBuilder, null, CompilationGroup.MAIN);
	this.newState.copyFrom(javaBuilder.lastState);
}

protected IncrementalImageBuilder(BatchImageBuilder batchBuilder, CompilationGroup compilationGroup) {
	this(batchBuilder.javaBuilder, batchBuilder.newState, compilationGroup);
	resetCollections();
}

public boolean build(SimpleLookupTable deltas) {
	if(this.sourceLocations.length == 0) {
		if (this.testImageBuilder != null) {
			return this.testImageBuilder.build(deltas);
		} else {
			return true;
		}
	}
	// initialize builder
	// walk this project's deltas, find changed source files
	// walk prereq projects' deltas, find changed class files & add affected source files
	//   use the build state # to skip the deltas for certain prereq projects
	//   ignore changed zip/jar files since they caused a full build
	// compile the source files & acceptResult()
	// compare the produced class files against the existing ones on disk
	// recompile all dependent source files of any type with structural changes or new/removed secondary type
	// keep a loop counter to abort & perform a full build

	if (JavaBuilder.DEBUG)
		System.out.println("INCREMENTAL build"); //$NON-NLS-1$

	try {
		resetCollections();

		this.notifier.subTask(Messages.build_analyzingDeltas);
		if (this.javaBuilder.hasBuildpathErrors()) {
			// if a missing class file was detected in the last build, a build state was saved since its no longer fatal
			// but we need to rebuild every source file since problems were not recorded
			// AND to avoid the infinite build scenario if this project is involved in a cycle, see bug 160550
			// we need to avoid unnecessary deltas caused by doing a full build in this case
			if (JavaBuilder.DEBUG)
				System.out.println("COMPILING all source files since the buildpath has errors "); //$NON-NLS-1$
			this.javaBuilder.currentProject.deleteMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
			addAllSourceFiles(this.sourceFiles);
			this.notifier.updateProgressDelta(0.25f);
		} else {
			IResourceDelta sourceDelta = (IResourceDelta) deltas.get(this.javaBuilder.currentProject);
			if (sourceDelta != null) {
				if (!findSourceFiles(sourceDelta)) return this.testImageBuilder != null ? this.testImageBuilder.build(deltas) : false;
				if(this.testImageBuilder != null) {
					this.testImageBuilder.findSourceFiles(sourceDelta);
				}
			}
			this.notifier.updateProgressDelta(0.10f);

			Object[] keyTable = deltas.keyTable;
			Object[] valueTable = deltas.valueTable;
			for (int i = 0, l = valueTable.length; i < l; i++) {
				IResourceDelta delta = (IResourceDelta) valueTable[i];
				if (delta != null) {
					IProject p = (IProject) keyTable[i];
					ClasspathLocation[] classFoldersAndJars = (ClasspathLocation[]) this.javaBuilder.binaryLocationsPerProject.get(p);
					if (classFoldersAndJars != null)
						if (!findAffectedSourceFiles(delta, classFoldersAndJars, p)) return false;
				}
			}
			this.notifier.updateProgressDelta(0.10f);

			this.notifier.subTask(Messages.build_analyzingSources);
			addAffectedSourceFiles();
			if (this.testImageBuilder != null) {
				this.testImageBuilder.addAffectedSourceFiles();
			}
			this.notifier.updateProgressDelta(0.05f);
		}

		if (incrementalBuildLoop() == false) {
			return false;
		}
		if (this.testImageBuilder != null && this.testImageBuilder.incrementalBuildLoop() == false) {
			return false;
		}
		if (this.hasStructuralChanges && this.javaBuilder.javaProject.hasCycleMarker())
			this.javaBuilder.mustPropagateStructuralChanges();
	} catch (AbortIncrementalBuildException e) {
		// abort the incremental build and let the batch builder handle the problem
		if (JavaBuilder.DEBUG)
			System.out.println("ABORTING incremental build... problem with " + e.qualifiedTypeName + //$NON-NLS-1$
				". Likely renamed inside its existing source file."); //$NON-NLS-1$
		return false;
	} catch (CoreException e) {
		throw internalException(e);
	} finally {
		cleanUp();
		if (this.testImageBuilder != null) {
			this.testImageBuilder.cleanUp();
		}
	}
	return true;
}

private boolean incrementalBuildLoop() throws CoreException {
	int compileLoop = 0;
	float increment = 0.40f;
	while (this.sourceFiles.size() > 0) { // added to in acceptResult
		if (++compileLoop > MaxCompileLoop) {
			if (JavaBuilder.DEBUG)
				System.out.println("ABORTING incremental build... exceeded loop count"); //$NON-NLS-1$
			return false;
		}
		this.notifier.checkCancel();

		SourceFile[] allSourceFiles = new SourceFile[this.sourceFiles.size()];
		this.sourceFiles.toArray(allSourceFiles);
		resetCollections();

		this.workQueue.addAll(allSourceFiles);
		this.notifier.setProgressPerCompilationUnit(increment / allSourceFiles.length);
		increment = increment / 2;
		compile(allSourceFiles);
		removeSecondaryTypes();
		addAffectedSourceFiles();
	}
	return true;
}

protected void buildAfterBatchBuild() {
	// called from a batch builder once all source files have been compiled AND some changes
	// need to be propagated incrementally (annotations, missing secondary types)

	if (JavaBuilder.DEBUG)
		System.out.println("INCREMENTAL build after batch build @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$

	// this is a copy of the incremental build loop
	try {
		addAffectedSourceFiles();
		while (this.sourceFiles.size() > 0) {
			this.notifier.checkCancel();
			SourceFile[] allSourceFiles = new SourceFile[this.sourceFiles.size()];
			this.sourceFiles.toArray(allSourceFiles);
			resetCollections();
			this.notifier.setProgressPerCompilationUnit(0.08f / allSourceFiles.length);
			this.workQueue.addAll(allSourceFiles);
			compile(allSourceFiles);
			removeSecondaryTypes();
			addAffectedSourceFiles();
		}
	} catch (CoreException e) {
		throw internalException(e);
	} finally {
		cleanUp();
	}
}

protected void addAffectedSourceFiles() {
	if (this.qualifiedStrings.size() == 0 && this.simpleStrings.size() == 0) return;
	if(this.testImageBuilder != null) {
		this.testImageBuilder.addAffectedSourceFiles(this.qualifiedStrings, this.simpleStrings, this.rootStrings, null);
	}
	addAffectedSourceFiles(this.qualifiedStrings, this.simpleStrings, this.rootStrings, null);
}

protected void addAffectedSourceFiles(Set<String> qualifiedSet, Set<String> simpleSet, Set<String> rootSet, Set<String> affectedTypes) {
	// the qualifiedStrings are of the form 'p1/p2' & the simpleStrings are just 'X'
	char[][][] internedQualifiedNames = ReferenceCollection.internQualifiedNames(qualifiedSet);
	// if a well known qualified name was found then we can skip over these
	if (internedQualifiedNames.length < qualifiedSet.size())
		internedQualifiedNames = null;
	char[][] internedSimpleNames = ReferenceCollection.internSimpleNames(simpleSet, true);
	// if a well known name was found then we can skip over these
	if (internedSimpleNames.length < simpleSet.size())
		internedSimpleNames = null;
	char[][] internedRootNames = ReferenceCollection.internSimpleNames(rootSet, false);

	next: for (Entry<String, ReferenceCollection> entry : this.newState.references.entrySet()) {
		String typeLocator = entry.getKey();
		if (affectedTypes != null && !affectedTypes.contains(typeLocator)) continue next;
		ReferenceCollection refs = entry.getValue();
		if (refs.includes(internedQualifiedNames, internedSimpleNames, internedRootNames)) {
			IFile file = this.javaBuilder.currentProject.getFile(typeLocator);
			SourceFile sourceFile = findSourceFile(file, true);
			if (sourceFile == null) continue next;
			if (this.sourceFiles.contains(sourceFile)) continue next;
			if (this.compiledAllAtOnce && this.previousSourceFiles != null && this.previousSourceFiles.contains(sourceFile))
				continue next; // can skip previously compiled files since already saw hierarchy related problems

			if (JavaBuilder.DEBUG)
				System.out.println("  adding affected source file " + typeLocator); //$NON-NLS-1$
			this.sourceFiles.add(sourceFile);
		}
	}
}

protected void addDependentsOf(IPath path, boolean isStructuralChange) {
	addDependentsOf(path, isStructuralChange, this.qualifiedStrings, this.simpleStrings, this.rootStrings);
}

protected void addDependentsOf(IPath path, boolean isStructuralChange, Set<String> qualifiedNames, Set<String> simpleNames, Set<String> rootNames) {
	path = path.setDevice(null);
	if (isStructuralChange) {
		String last = path.lastSegment();
		if (last.length() == TypeConstants.PACKAGE_INFO_NAME.length)
			if (CharOperation.equals(last.toCharArray(), TypeConstants.PACKAGE_INFO_NAME)) {
				path = path.removeLastSegments(1); // the package-info file has changed so blame the package itself
				/* https://bugs.eclipse.org/bugs/show_bug.cgi?id=323785, in the case of default package,
				   there is no need to blame the package itself as there can be no annotations or documentation
				   comment tags in the package-info file that can influence the rest of the package. Just bail out
				   so we don't touch null objects below.
				 */
				if (path.isEmpty())
					return;
			}
	}

	if (isStructuralChange && !this.hasStructuralChanges) {
		this.newState.tagAsStructurallyChanged();
		this.hasStructuralChanges = true;
	}
	// the qualifiedStrings are of the form 'p1/p2' & the simpleStrings are just 'X'
	rootNames.add(path.segment(0));
	String packageName = path.removeLastSegments(1).toString();
	boolean wasNew = qualifiedNames.add(packageName);
	String typeName = path.lastSegment();
	int memberIndex = typeName.indexOf('$');
	if (memberIndex > 0)
		typeName = typeName.substring(0, memberIndex);
	wasNew = simpleNames.add(typeName) | wasNew;
	if (wasNew && JavaBuilder.DEBUG)
		System.out.println("  will look for dependents of " //$NON-NLS-1$
			+ typeName + " in " + packageName); //$NON-NLS-1$
}

protected boolean checkForClassFileChanges(IResourceDelta binaryDelta, ClasspathMultiDirectory md, int segmentCount) throws CoreException {
	IResource resource = binaryDelta.getResource();
	// remember that if inclusion & exclusion patterns change then a full build is done
	boolean isExcluded = (md.exclusionPatterns != null || md.inclusionPatterns != null)
		&& Util.isExcluded(resource, md.inclusionPatterns, md.exclusionPatterns);
	switch(resource.getType()) {
		case IResource.FOLDER :
			if (isExcluded && md.inclusionPatterns == null)
		        return true; // no need to go further with this delta since its children cannot be included

			IResourceDelta[] children = binaryDelta.getAffectedChildren();
			for (int i = 0, l = children.length; i < l; i++)
				if (!checkForClassFileChanges(children[i], md, segmentCount))
					return false;
			return true;
		case IResource.FILE :
			if (!isExcluded && org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(resource.getName())) {
				// perform full build if a managed class file has been changed
				IPath typePath = resource.getFullPath().removeFirstSegments(segmentCount).removeFileExtension();
				if (this.newState.isKnownType(typePath.toString())) {
					if (JavaBuilder.DEBUG)
						System.out.println("MUST DO FULL BUILD. Found change to class file " + typePath); //$NON-NLS-1$
					return false;
				}
				return true;
			}
	}
	return true;
}

@Override
protected void cleanUp() {
	super.cleanUp();

	this.sourceFiles = null;
	this.previousSourceFiles = null;
	this.qualifiedStrings = null;
	this.simpleStrings = null;
	this.rootStrings = null;
	this.secondaryTypesToRemove = null;
	this.hasStructuralChanges = false;
}

@Override
protected void compile(SourceFile[] units, SourceFile[] additionalUnits, boolean compilingFirstGroup) {
	if (compilingFirstGroup && additionalUnits != null) {
		// add any source file from additionalUnits to units if it defines secondary types
		// otherwise its possible during testing with MAX_AT_ONCE == 1 that a secondary type
		// can cause an infinite loop as it alternates between not found and defined, see bug 146324
		ArrayList extras = null;
		for (int i = 0, l = additionalUnits.length; i < l; i++) {
			SourceFile unit = additionalUnits[i];
			if (unit != null && this.newState.getDefinedTypeNamesFor(unit.typeLocator()) != null) {
				if (JavaBuilder.DEBUG)
					System.out.println("About to compile file with secondary types "+ unit.typeLocator()); //$NON-NLS-1$
				if (extras == null)
					extras = new ArrayList(3);
				extras.add(unit);
			}
		}
		if (extras != null) {
			int oldLength = units.length;
			int toAdd = extras.size();
			System.arraycopy(units, 0, units = new SourceFile[oldLength + toAdd], 0, oldLength);
			for (int i = 0; i < toAdd; i++)
				units[oldLength++] = (SourceFile) extras.get(i);
		}
	}
	super.compile(units, additionalUnits, compilingFirstGroup);
}

@Override
protected void deleteGeneratedFiles(IFile[] deletedGeneratedFiles) {
	// delete generated files and recompile any affected source files
	try {
		for (int j = deletedGeneratedFiles.length; --j >= 0;) {
			IFile deletedFile = deletedGeneratedFiles[j];
			if (deletedFile.exists()) continue; // only delete .class files for source files that were actually deleted

			SourceFile sourceFile = findSourceFile(deletedFile, false);
			String typeLocator = sourceFile.typeLocator();
			int mdSegmentCount = sourceFile.sourceLocation.sourceFolder.getFullPath().segmentCount();
			IPath typePath = sourceFile.resource.getFullPath().removeFirstSegments(mdSegmentCount).removeFileExtension();
			addDependentsOf(typePath, true); // add dependents of the source file since its now deleted
			this.previousSourceFiles = null; // existing source files did not see it as deleted since they were compiled before it was
			char[][] definedTypeNames = this.newState.getDefinedTypeNamesFor(typeLocator);
			if (definedTypeNames == null) { // defined a single type matching typePath
				removeClassFile(typePath, sourceFile.sourceLocation.binaryFolder);
			} else {
				if (definedTypeNames.length > 0) { // skip it if it failed to successfully define a type
					IPath packagePath = typePath.removeLastSegments(1);
					for (int d = 0, l = definedTypeNames.length; d < l; d++)
						removeClassFile(packagePath.append(new String(definedTypeNames[d])), sourceFile.sourceLocation.binaryFolder);
				}
			}
			this.newState.removeLocator(typeLocator);
		}
	} catch (CoreException e) {
		// must continue with compile loop so just log the CoreException
		Util.log(e, "JavaBuilder logging CompilationParticipant's CoreException to help debugging"); //$NON-NLS-1$
	}
}

protected boolean findAffectedSourceFiles(IResourceDelta delta, ClasspathLocation[] classFoldersAndJars, IProject prereqProject) {
	for (int i = 0, l = classFoldersAndJars.length; i < l; i++) {
		ClasspathLocation bLocation = classFoldersAndJars[i];
		// either a .class file folder or a zip/jar file
		if (bLocation != null) { // skip unchanged output folder
			IPath p = bLocation.getProjectRelativePath();
			if (p != null) {
				IResourceDelta binaryDelta = delta.findMember(p);
				if (binaryDelta != null) {
					if (bLocation instanceof ClasspathJar) {
						if (JavaBuilder.DEBUG)
							System.out.println("ABORTING incremental build... found delta to jar/zip file"); //$NON-NLS-1$
						return false; // do full build since jar file was changed (added/removed were caught as classpath change)
					}
					if (binaryDelta.getKind() == IResourceDelta.ADDED || binaryDelta.getKind() == IResourceDelta.REMOVED) {
						if (JavaBuilder.DEBUG)
							System.out.println("ABORTING incremental build... found added/removed binary folder"); //$NON-NLS-1$
						return false; // added/removed binary folder should not make it here (classpath change), but handle anyways
					}
					int segmentCount = binaryDelta.getFullPath().segmentCount();
					IResourceDelta[] children = binaryDelta.getAffectedChildren(); // .class files from class folder
					StringSet structurallyChangedTypes = null;
					if (bLocation.isOutputFolder())
						structurallyChangedTypes = this.newState.getStructurallyChangedTypes(this.javaBuilder.getLastState(prereqProject));
					for (int j = 0, m = children.length; j < m; j++)
						findAffectedSourceFiles(children[j], segmentCount, structurallyChangedTypes);
					this.notifier.checkCancel();
				}
			}
		}
	}
	return true;
}

protected void findAffectedSourceFiles(IResourceDelta binaryDelta, int segmentCount, StringSet structurallyChangedTypes) {
	// When a package becomes a type or vice versa, expect 2 deltas,
	// one on the folder & one on the class file
	IResource resource = binaryDelta.getResource();
	switch(resource.getType()) {
		case IResource.FOLDER :
			switch (binaryDelta.getKind()) {
				case IResourceDelta.ADDED :
				case IResourceDelta.REMOVED :
					IPath packagePath = resource.getFullPath().removeFirstSegments(segmentCount);
					String packageName = packagePath.toString();
					if (binaryDelta.getKind() == IResourceDelta.ADDED) {
						// see if any known source file is from the same package... classpath already includes new package
						if (!this.newState.isKnownPackage(packageName)) {
							if (JavaBuilder.DEBUG)
								System.out.println("Found added package " + packageName); //$NON-NLS-1$
							addDependentsOf(packagePath, false);
							return;
						}
						if (JavaBuilder.DEBUG)
							System.out.println("Skipped dependents of added package " + packageName); //$NON-NLS-1$
					} else {
						// see if the package still exists on the classpath
						if (!this.nameEnvironment.isPackage(packageName, ModuleBinding.ANY)) {
							if (JavaBuilder.DEBUG)
								System.out.println("Found removed package " + packageName); //$NON-NLS-1$
							addDependentsOf(packagePath, false);
							return;
						}
						if (JavaBuilder.DEBUG)
							System.out.println("Skipped dependents of removed package " + packageName); //$NON-NLS-1$
					}
					//$FALL-THROUGH$ traverse the sub-packages and .class files
				case IResourceDelta.CHANGED :
					IResourceDelta[] children = binaryDelta.getAffectedChildren();
					for (int i = 0, l = children.length; i < l; i++)
						findAffectedSourceFiles(children[i], segmentCount, structurallyChangedTypes);
			}
			return;
		case IResource.FILE :
			if (org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(resource.getName())) {
				IPath typePath = resource.getFullPath().removeFirstSegments(segmentCount).removeFileExtension();
				switch (binaryDelta.getKind()) {
					case IResourceDelta.ADDED :
					case IResourceDelta.REMOVED :
						if (JavaBuilder.DEBUG)
							System.out.println("Found added/removed class file " + typePath); //$NON-NLS-1$
						addDependentsOf(typePath, false);
						return;
					case IResourceDelta.CHANGED :
						if ((binaryDelta.getFlags() & IResourceDelta.CONTENT) == 0)
							return; // skip it since it really isn't changed
						if (structurallyChangedTypes != null && !structurallyChangedTypes.includes(typePath.toString()))
							return; // skip since it wasn't a structural change
						if (JavaBuilder.DEBUG)
							System.out.println("Found changed class file " + typePath); //$NON-NLS-1$
						addDependentsOf(typePath, false);
				}
				return;
			}
	}
}

protected boolean findSourceFiles(IResourceDelta delta) throws CoreException {
	ArrayList visited = this.makeOutputFolderConsistent ? new ArrayList(this.sourceLocations.length) : null;
	for (int i = 0, l = this.sourceLocations.length; i < l; i++) {
		ClasspathMultiDirectory md = this.sourceLocations[i];
		if (this.makeOutputFolderConsistent && md.hasIndependentOutputFolder && !visited.contains(md.binaryFolder)) {
			// even a project which acts as its own source folder can have an independent/nested output folder
			visited.add(md.binaryFolder);
			IResourceDelta binaryDelta = delta.findMember(md.binaryFolder.getProjectRelativePath());
			if (binaryDelta != null) {
				int segmentCount = binaryDelta.getFullPath().segmentCount();
				IResourceDelta[] children = binaryDelta.getAffectedChildren();
				for (int j = 0, m = children.length; j < m; j++)
					if (!checkForClassFileChanges(children[j], md, segmentCount))
						return false;
			}
		}
		if (md.sourceFolder.equals(this.javaBuilder.currentProject)) {
			// skip nested source & output folders when the project is a source folder
			int segmentCount = delta.getFullPath().segmentCount();
			IResourceDelta[] children = delta.getAffectedChildren();
			for (int j = 0, m = children.length; j < m; j++)
				if (!isExcludedFromProject(children[j].getFullPath()))
					if (!findSourceFiles(children[j], md, segmentCount))
						return false;
		} else {
			IResourceDelta sourceDelta = delta.findMember(md.sourceFolder.getProjectRelativePath());
			if (sourceDelta != null) {
				if (sourceDelta.getKind() == IResourceDelta.REMOVED) {
					if (JavaBuilder.DEBUG)
						System.out.println("ABORTING incremental build... found removed source folder"); //$NON-NLS-1$
					return false; // removed source folder should not make it here, but handle anyways (ADDED is supported)
				}
				int segmentCount = sourceDelta.getFullPath().segmentCount();
				IResourceDelta[] children = sourceDelta.getAffectedChildren();
				try {
					for (int j = 0, m = children.length; j < m; j++)
						if (!findSourceFiles(children[j], md, segmentCount))
							return false;
				} catch (CoreException e) {
					// catch the case that a package has been renamed and collides on disk with an as-yet-to-be-deleted package
					if (e.getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
						if (JavaBuilder.DEBUG)
							System.out.println("ABORTING incremental build... found renamed package"); //$NON-NLS-1$
						return false;
					}
					throw e; // rethrow
				}
			}
		}
		this.notifier.checkCancel();
	}
	return true;
}

protected boolean findSourceFiles(IResourceDelta sourceDelta, ClasspathMultiDirectory md, int segmentCount) throws CoreException {
	// When a package becomes a type or vice versa, expect 2 deltas,
	// one on the folder & one on the source file
	IResource resource = sourceDelta.getResource();
	// remember that if inclusion & exclusion patterns change then a full build is done
	boolean isExcluded = (md.exclusionPatterns != null || md.inclusionPatterns != null)
		&& Util.isExcluded(resource, md.inclusionPatterns, md.exclusionPatterns);
	switch(resource.getType()) {
		case IResource.FOLDER :
			if (isExcluded && md.inclusionPatterns == null)
		        return true; // no need to go further with this delta since its children cannot be included

			switch (sourceDelta.getKind()) {
				case IResourceDelta.ADDED :
				    if (!isExcluded) {
						IPath addedPackagePath = resource.getFullPath().removeFirstSegments(segmentCount);
						createFolder(addedPackagePath, md.binaryFolder); // ensure package exists in the output folder
						// see if any known source file is from the same package... classpath already includes new package
						if (this.sourceLocations.length > 1 && this.newState.isKnownPackage(addedPackagePath.toString())) {
							if (JavaBuilder.DEBUG)
								System.out.println("Skipped dependents of added package " + addedPackagePath); //$NON-NLS-1$
						} else {
							if (JavaBuilder.DEBUG)
								System.out.println("Found added package " + addedPackagePath); //$NON-NLS-1$
							addDependentsOf(addedPackagePath, true);
						}
				    }
					//$FALL-THROUGH$ collect all the source files
				case IResourceDelta.CHANGED :
					IResourceDelta[] children = sourceDelta.getAffectedChildren();
					for (int i = 0, l = children.length; i < l; i++)
						if (!findSourceFiles(children[i], md, segmentCount))
							return false;
					return true;
				case IResourceDelta.REMOVED :
				    if (isExcluded) {
				    	// since this folder is excluded then there is nothing to delete (from this md), but must walk any included subfolders
						children = sourceDelta.getAffectedChildren();
						for (int i = 0, l = children.length; i < l; i++)
							if (!findSourceFiles(children[i], md, segmentCount))
								return false;
						return true;
				    }
					IPath removedPackagePath = resource.getFullPath().removeFirstSegments(segmentCount);
					if (this.sourceLocations.length > 1) {
						for (int i = 0, l = this.sourceLocations.length; i < l; i++) {
							if (this.sourceLocations[i].sourceFolder.getFolder(removedPackagePath).exists()) {
								// only a package fragment was removed, same as removing multiple source files
								if (md.hasIndependentOutputFolder)
									createFolder(removedPackagePath, md.binaryFolder); // ensure package exists in the output folder
								IResourceDelta[] removedChildren = sourceDelta.getAffectedChildren();
								for (int j = 0, m = removedChildren.length; j < m; j++)
									if (!findSourceFiles(removedChildren[j], md, segmentCount))
										return false;
								return true;
							}
						}
					}
					if ((sourceDelta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
						// same idea as moving a source file
						// see bug 163200
						IResource movedFolder = this.javaBuilder.workspaceRoot.getFolder(sourceDelta.getMovedToPath());
						JavaBuilder.removeProblemsAndTasksFor(movedFolder);
					}
					IFolder removedPackageFolder = md.binaryFolder.getFolder(removedPackagePath);
					if (removedPackageFolder.exists())
						removedPackageFolder.delete(IResource.FORCE, null);
					// add dependents even when the package thinks it does not exist to be on the safe side
					if (JavaBuilder.DEBUG)
						System.out.println("Found removed package " + removedPackagePath); //$NON-NLS-1$
					addDependentsOf(removedPackagePath, true);
					this.newState.removePackage(sourceDelta);
			}
			return true;
		case IResource.FILE :
			if (isExcluded) return true;

			String resourceName = resource.getName();
			if (org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(resourceName)) {
				IPath typePath = resource.getFullPath().removeFirstSegments(segmentCount).removeFileExtension();
				String typeLocator = resource.getProjectRelativePath().toString();
				switch (sourceDelta.getKind()) {
					case IResourceDelta.ADDED :
						if (JavaBuilder.DEBUG)
							System.out.println("Compile this added source file " + typeLocator); //$NON-NLS-1$
						this.sourceFiles.add(new SourceFile((IFile) resource, md, true));
						String typeName = typePath.toString();
						if (!this.newState.isDuplicateLocator(typeName, typeLocator)) { // adding dependents results in 2 duplicate errors
							if (JavaBuilder.DEBUG)
								System.out.println("Found added source file " + typeName); //$NON-NLS-1$
							addDependentsOf(typePath, true);
						}
						return true;
					case IResourceDelta.REMOVED :
						char[][] definedTypeNames = this.newState.getDefinedTypeNamesFor(typeLocator);
						if (definedTypeNames == null) { // defined a single type matching typePath
							removeClassFile(typePath, md.binaryFolder);
							if ((sourceDelta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
								// remove problems and tasks for a compilation unit that is being moved (to another package or renamed)
								// if the target file is a compilation unit, the new cu will be recompiled
								// if the target file is a non-java resource, then markers are removed
								// see bug 2857
								IResource movedFile = this.javaBuilder.workspaceRoot.getFile(sourceDelta.getMovedToPath());
								JavaBuilder.removeProblemsAndTasksFor(movedFile);
							}
						} else {
							if (JavaBuilder.DEBUG)
								System.out.println("Found removed source file " + typePath.toString()); //$NON-NLS-1$
							addDependentsOf(typePath, true); // add dependents of the source file since it may be involved in a name collision
							if (definedTypeNames.length > 0) { // skip it if it failed to successfully define a type
								IPath packagePath = typePath.removeLastSegments(1);
								for (int i = 0, l = definedTypeNames.length; i < l; i++)
									removeClassFile(packagePath.append(new String(definedTypeNames[i])), md.binaryFolder);
							}
						}
						this.newState.removeLocator(typeLocator);
						return true;
					case IResourceDelta.CHANGED :
						if ((sourceDelta.getFlags() & IResourceDelta.CONTENT) == 0
								&& (sourceDelta.getFlags() & IResourceDelta.ENCODING) == 0)
							return true; // skip it since it really isn't changed
						if (JavaBuilder.DEBUG)
							System.out.println("Compile this changed source file " + typeLocator); //$NON-NLS-1$
						SourceFile unit = new SourceFile((IFile) resource, md, true);
						this.sourceFiles.add(unit);
				}
				return true;
			} else if (org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(resourceName)) {
				// perform full build if a managed class file has been changed
				if (this.makeOutputFolderConsistent) {
					IPath typePath = resource.getFullPath().removeFirstSegments(segmentCount).removeFileExtension();
					if (this.newState.isKnownType(typePath.toString())) {
						if (JavaBuilder.DEBUG)
							System.out.println("MUST DO FULL BUILD. Found change to class file " + typePath); //$NON-NLS-1$
						return false;
					}
				}
				return true;
			} else if (md.hasIndependentOutputFolder) {
				if (this.javaBuilder.filterExtraResource(resource)) return true;

				// copy all other resource deltas to the output folder
				IPath resourcePath = resource.getFullPath().removeFirstSegments(segmentCount);
				IResource outputFile = md.binaryFolder.getFile(resourcePath);
				switch (sourceDelta.getKind()) {
					case IResourceDelta.ADDED :
						if (outputFile.exists()) {
							if (JavaBuilder.DEBUG)
								System.out.println("Deleting existing file " + resourcePath); //$NON-NLS-1$
							outputFile.delete(IResource.FORCE, null);
						}
						if (JavaBuilder.DEBUG)
							System.out.println("Copying added file " + resourcePath); //$NON-NLS-1$
						createFolder(resourcePath.removeLastSegments(1), md.binaryFolder); // ensure package exists in the output folder
						copyResource(resource, outputFile);
						return true;
					case IResourceDelta.REMOVED :
						if (outputFile.exists()) {
							if (JavaBuilder.DEBUG)
								System.out.println("Deleting removed file " + resourcePath); //$NON-NLS-1$
							outputFile.delete(IResource.FORCE, null);
						}
						return true;
					case IResourceDelta.CHANGED :
						if ((sourceDelta.getFlags() & IResourceDelta.CONTENT) == 0
								&& (sourceDelta.getFlags() & IResourceDelta.ENCODING) == 0)
							return true; // skip it since it really isn't changed
						if (outputFile.exists()) {
							if (JavaBuilder.DEBUG)
								System.out.println("Deleting existing file " + resourcePath); //$NON-NLS-1$
							outputFile.delete(IResource.FORCE, null);
						}
						if (JavaBuilder.DEBUG)
							System.out.println("Copying changed file " + resourcePath); //$NON-NLS-1$
						createFolder(resourcePath.removeLastSegments(1), md.binaryFolder); // ensure package exists in the output folder
						copyResource(resource, outputFile);
				}
				return true;
			}
	}
	return true;
}

@Override
protected void finishedWith(String sourceLocator, CompilationResult result, char[] mainTypeName, ArrayList definedTypeNames, ArrayList duplicateTypeNames) {
	char[][] previousTypeNames = this.newState.getDefinedTypeNamesFor(sourceLocator);
	if (previousTypeNames == null)
		previousTypeNames = new char[][] {mainTypeName};
	IPath packagePath = null;
	next : for (int i = 0, l = previousTypeNames.length; i < l; i++) {
		char[] previous = previousTypeNames[i];
		for (int j = 0, m = definedTypeNames.size(); j < m; j++)
			if (CharOperation.equals(previous, (char[]) definedTypeNames.get(j)))
				continue next;

		SourceFile sourceFile = (SourceFile) result.getCompilationUnit();
		if (packagePath == null) {
			int count = sourceFile.sourceLocation.sourceFolder.getFullPath().segmentCount();
			packagePath = sourceFile.resource.getFullPath().removeFirstSegments(count).removeLastSegments(1);
		}
		if (this.secondaryTypesToRemove == null)
			this.secondaryTypesToRemove = new SimpleLookupTable();
		ArrayList types = (ArrayList) this.secondaryTypesToRemove.get(sourceFile.sourceLocation.binaryFolder);
		if (types == null)
			types = new ArrayList(definedTypeNames.size());
		types.add(packagePath.append(new String(previous)));
		this.secondaryTypesToRemove.put(sourceFile.sourceLocation.binaryFolder, types);
	}
	super.finishedWith(sourceLocator, result, mainTypeName, definedTypeNames, duplicateTypeNames);
}

@Override
protected void processAnnotationResults(CompilationParticipantResult[] results) {
	for (int i = results.length; --i >= 0;) {
		CompilationParticipantResult result = results[i];
		if (result == null) continue;

		IFile[] deletedGeneratedFiles = result.deletedFiles;
		if (deletedGeneratedFiles != null)
			deleteGeneratedFiles(deletedGeneratedFiles);

		IFile[] addedGeneratedFiles = result.addedFiles;
		if (addedGeneratedFiles != null) {
			for (int j = addedGeneratedFiles.length; --j >= 0;) {
				SourceFile sourceFile = findSourceFile(addedGeneratedFiles[j], true);
				if (sourceFile != null && !this.sourceFiles.contains(sourceFile))
					this.sourceFiles.add(sourceFile);
			}
		}

		recordParticipantResult(result);
	}
}

protected void removeClassFile(IPath typePath, IContainer outputFolder) throws CoreException {
	if (typePath.lastSegment().indexOf('$') == -1) { // is not a nested type
		this.newState.removeQualifiedTypeName(typePath.toString());
		// add dependents even when the type thinks it does not exist to be on the safe side
		if (JavaBuilder.DEBUG)
			System.out.println("Found removed type " + typePath); //$NON-NLS-1$
		addDependentsOf(typePath, true); // when member types are removed, their enclosing type is structurally changed
	}
	IFile classFile = outputFolder.getFile(typePath.addFileExtension(SuffixConstants.EXTENSION_class));
	if (classFile.exists()) {
		if (JavaBuilder.DEBUG)
			System.out.println("Deleting class file of removed type " + typePath); //$NON-NLS-1$
		classFile.delete(IResource.FORCE, null);
	}
}

protected void removeSecondaryTypes() throws CoreException {
	if (this.secondaryTypesToRemove != null) { // delayed deleting secondary types until the end of the compile loop
		Object[] keyTable = this.secondaryTypesToRemove.keyTable;
		Object[] valueTable = this.secondaryTypesToRemove.valueTable;
		for (int i = 0, l = keyTable.length; i < l; i++) {
			IContainer outputFolder = (IContainer) keyTable[i];
			if (outputFolder != null) {
				ArrayList paths = (ArrayList) valueTable[i];
				for (int j = 0, m = paths.size(); j < m; j++)
					removeClassFile((IPath) paths.get(j), outputFolder);
			}
		}
		this.secondaryTypesToRemove = null;
		if (this.previousSourceFiles != null)
			this.previousSourceFiles = null; // cannot optimize recompile case when a secondary type is deleted, see 181269
	}
}

protected void resetCollections() {
	if (this.sourceFiles == null) {
		this.sourceFiles = new LinkedHashSet<>(33);
		this.previousSourceFiles = null;
		this.qualifiedStrings = new HashSet<>(3);
		this.simpleStrings = new HashSet<>(3);
		this.rootStrings = new HashSet<>(3);
		this.hasStructuralChanges = false;
	} else {
		this.previousSourceFiles = this.sourceFiles.isEmpty() ? null : (LinkedHashSet) this.sourceFiles.clone();

		this.sourceFiles.clear();
		this.qualifiedStrings.clear();
		this.simpleStrings.clear();
		this.rootStrings.clear();
		this.workQueue.clear();
	}
}

@Override
protected void updateProblemsFor(SourceFile sourceFile, CompilationResult result) throws CoreException {
	if (CharOperation.equals(sourceFile.getMainTypeName(), TypeConstants.PACKAGE_INFO_NAME)) {
		IResource pkgResource = sourceFile.resource.getParent();
		IMarker[] findMarkers = pkgResource.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, false,
				IResource.DEPTH_ZERO);
		if (findMarkers.length > 0) {
			// markers must be from the time when no package-info.java existed.
			// trigger a full build, so marker is cleared also from packages in other source folders
			throw new AbortCompilation(true, new AbortIncrementalBuildException(new String(TypeConstants.PACKAGE_INFO_NAME)));
		}
	}
	IMarker[] markers = JavaBuilder.getProblemsFor(sourceFile.resource);
	CategorizedProblem[] problems = result.getProblems();
	if (problems == null && markers.length == 0) return;

	this.notifier.updateProblemCounts(markers, problems);
	JavaBuilder.removeProblemsFor(sourceFile.resource);
	storeProblemsFor(sourceFile, problems);
}

@Override
protected void updateTasksFor(SourceFile sourceFile, CompilationResult result) throws CoreException {
	IMarker[] markers = JavaBuilder.getTasksFor(sourceFile.resource);
	CategorizedProblem[] tasks = result.getTasks();
	if (tasks == null && markers.length == 0) return;

	JavaBuilder.removeTasksFor(sourceFile.resource);
	storeTasksFor(sourceFile, tasks);
}

/**
 * @see org.eclipse.jdt.internal.core.builder.AbstractImageBuilder#writeClassFileContents(org.eclipse.jdt.internal.compiler.ClassFile, org.eclipse.core.resources.IFile, java.lang.String, boolean, org.eclipse.jdt.internal.core.builder.SourceFile)
 */
@Override
protected void writeClassFileContents(ClassFile classfile, IFile file, String qualifiedFileName, boolean isTopLevelType, SourceFile compilationUnit) throws CoreException {
	// Before writing out the class file, compare it to the previous file
	// If structural changes occurred then add dependent source files
	byte[] bytes = classfile.getBytes();
	if (file.exists()) {
		if (writeClassFileCheck(file, qualifiedFileName, bytes) || compilationUnit.updateClassFile) { // see 46093
			if (JavaBuilder.DEBUG)
				System.out.println("Writing changed class file " + file.getName());//$NON-NLS-1$
			if (!file.isDerived())
				file.setDerived(true, null);
			file.setContents(new ByteArrayInputStream(bytes), true, false, null);
		} else if (JavaBuilder.DEBUG) {
			System.out.println("Skipped over unchanged class file " + file.getName());//$NON-NLS-1$
		}
	} else {
		if (isTopLevelType)
			addDependentsOf(new Path(qualifiedFileName), true); // new type
		if (JavaBuilder.DEBUG)
			System.out.println("Writing new class file " + file.getName());//$NON-NLS-1$
		try {
			file.create(new ByteArrayInputStream(bytes), IResource.FORCE | IResource.DERIVED, null);
		} catch (CoreException e) {
			if (e.getStatus().getCode() == IResourceStatus.CASE_VARIANT_EXISTS) {
				IStatus status = e.getStatus();
				if (status instanceof IResourceStatus) {
					IPath oldFilePath = ((IResourceStatus) status).getPath();
					char[] oldTypeName = oldFilePath.removeFileExtension().lastSegment().toCharArray();
					char[][] previousTypeNames = this.newState.getDefinedTypeNamesFor(compilationUnit.typeLocator());
					boolean fromSameFile = false;
					if (previousTypeNames == null) {
						fromSameFile = CharOperation.equals(compilationUnit.getMainTypeName(), oldTypeName);
					} else {
						for (int i = 0, l = previousTypeNames.length; i < l; i++) {
							if (CharOperation.equals(previousTypeNames[i], oldTypeName)) {
								fromSameFile = true;
								break;
							}
						}
					}
					if (fromSameFile) {
						// file is defined by the same compilationUnit, but won't be deleted until later so do it now
						IFile collision = file.getParent().getFile(new Path(oldFilePath.lastSegment()));
						collision.delete(true, false, null);
						boolean success = false;
						try {
							file.create(new ByteArrayInputStream(bytes), IResource.FORCE | IResource.DERIVED, null);
							success = true;
						} catch (CoreException ignored) {
							// ignore the second exception
						}
						if (success) return;
					}
				}
				// catch the case that a type has been renamed and collides on disk with an as-yet-to-be-deleted type
				throw new AbortCompilation(true, new AbortIncrementalBuildException(qualifiedFileName));
			}
			throw e; // rethrow
		}
	}
}

protected boolean writeClassFileCheck(IFile file, String fileName, byte[] newBytes) throws CoreException {
	try {
		byte[] oldBytes = Util.getResourceContentsAsByteArray(file);
		notEqual : if (newBytes.length == oldBytes.length) {
			for (int i = newBytes.length; --i >= 0;)
				if (newBytes[i] != oldBytes[i]) break notEqual;
			return false; // bytes are identical so skip them
		}
		URI location = file.getLocationURI();
		if (location == null) return false; // unable to determine location of this class file
		String filePath = location.getSchemeSpecificPart();
		ClassFileReader reader = new ClassFileReader(oldBytes, filePath.toCharArray());
		// ignore local types since they're only visible inside a single method
		if (!(reader.isLocal() || reader.isAnonymous()) && reader.hasStructuralChanges(newBytes)) {
			if (JavaBuilder.DEBUG)
				System.out.println("Type has structural changes " + fileName); //$NON-NLS-1$
			addDependentsOf(new Path(fileName), true);
			this.newState.wasStructurallyChanged(fileName);
		}
	} catch (JavaModelException jme) {
		Throwable e = jme.getCause();
		if (e instanceof CoreException) {
			// assuming a ResourceException during IFile.getContents(), treat it like a corrupt file
			addDependentsOf(new Path(fileName), true);
			this.newState.wasStructurallyChanged(fileName);
		} else {
			throw jme;
		}
	} catch (ClassFormatException e) {
		addDependentsOf(new Path(fileName), true);
		this.newState.wasStructurallyChanged(fileName);
	}
	return true;
}

@Override
public String toString() {
	return "incremental image builder for:\n\tnew state: " + this.newState; //$NON-NLS-1$
}


/* Debug helper

static void dump(IResourceDelta delta) {
	StringBuffer buffer = new StringBuffer();
	IPath path = delta.getFullPath();
	for (int i = path.segmentCount(); --i > 0;)
		buffer.append("  ");
	switch (delta.getKind()) {
		case IResourceDelta.ADDED:
			buffer.append('+');
			break;
		case IResourceDelta.REMOVED:
			buffer.append('-');
			break;
		case IResourceDelta.CHANGED:
			buffer.append('*');
			break;
		case IResourceDelta.NO_CHANGE:
			buffer.append('=');
			break;
		default:
			buffer.append('?');
			break;
	}
	buffer.append(path);
	System.out.println(buffer.toString());
	IResourceDelta[] children = delta.getAffectedChildren();
	for (int i = 0, l = children.length; i < l; i++)
		dump(children[i]);
}
*/
}
