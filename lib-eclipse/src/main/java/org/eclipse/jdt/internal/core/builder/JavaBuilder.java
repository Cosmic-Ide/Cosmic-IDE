/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Terry Parker <tparker@google.com> - [performance] Low hit rates in JavaModel caches - https://bugs.eclipse.org/421165
 *******************************************************************************/
package org.eclipse.jdt.internal.core.builder;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.*;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.core.*;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

import java.io.*;
import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class JavaBuilder extends IncrementalProjectBuilder {

IProject currentProject;
JavaProject javaProject;
IWorkspaceRoot workspaceRoot;
CompilationParticipant[] participants;
NameEnvironment nameEnvironment;
NameEnvironment testNameEnvironment;
SimpleLookupTable binaryLocationsPerProject; // maps a project to its binary resources (output folders, class folders, zip/jar files)
public State lastState;
BuildNotifier notifier;
char[][] extraResourceFileFilters;
String[] extraResourceFolderFilters;
public static final String SOURCE_ID = "JDT"; //$NON-NLS-1$

public static boolean DEBUG = false;
public static boolean SHOW_STATS = false;

/**
 * Bug 549457: In case auto-building on a JDT core settings change (e.g. compiler compliance) is not desired,
 * specify VM property: {@code -Dorg.eclipse.disableAutoBuildOnSettingsChange=true}
 */
private static final boolean DISABLE_AUTO_BUILDING_ON_SETTINGS_CHANGE = Boolean.getBoolean("org.eclipse.disableAutoBuildOnSettingsChange"); //$NON-NLS-1$
private static final IPath JDT_CORE_SETTINGS_PATH = Path.fromPortableString(JavaProject.DEFAULT_PREFERENCES_DIRNAME + IPath.SEPARATOR + JavaProject.JAVA_CORE_PREFS_FILE);

/**
 * A list of project names that have been built.
 * This list is used to reset the JavaModel.existingExternalFiles cache when a build cycle begins
 * so that deleted external jars are discovered.
 */
static LinkedHashSet<String> builtProjects;

public static IMarker[] getProblemsFor(IResource resource) {
	try {
		if (resource != null && resource.exists()) {
			IMarker[] markers = resource.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
			Set markerTypes = JavaModelManager.getJavaModelManager().compilationParticipants.managedMarkerTypes();
			if (markerTypes.isEmpty()) return markers;
			ArrayList markerList = new ArrayList(5);
			for (int i = 0, length = markers.length; i < length; i++) {
				markerList.add(markers[i]);
			}
			Iterator iterator = markerTypes.iterator();
			while (iterator.hasNext()) {
				markers = resource.findMarkers((String) iterator.next(), false, IResource.DEPTH_INFINITE);
				for (int i = 0, length = markers.length; i < length; i++) {
					markerList.add(markers[i]);
				}
			}
			IMarker[] result;
			markerList.toArray(result = new IMarker[markerList.size()]);
			return result;
		}
	} catch (CoreException e) {
		// assume there are no problems
	}
	return new IMarker[0];
}

public static IMarker[] getTasksFor(IResource resource) {
	try {
		if (resource != null && resource.exists())
			return resource.findMarkers(IJavaModelMarker.TASK_MARKER, false, IResource.DEPTH_INFINITE);
	} catch (CoreException e) {
		// assume there are no tasks
	}
	return new IMarker[0];
}

/**
 * Hook allowing to initialize some static state before a complete build iteration.
 * This hook is invoked during PRE_AUTO_BUILD notification
 */
public static void buildStarting() {
	// build is about to start
}

/**
 * Hook allowing to reset some static state after a complete build iteration.
 * This hook is invoked during POST_AUTO_BUILD notification
 */
public static void buildFinished() {
	BuildNotifier.resetProblemCounters();
}

public static void removeProblemsFor(IResource resource) {
	try {
		if (resource != null && resource.exists()) {
			resource.deleteMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);

			// delete managed markers
			Set markerTypes = JavaModelManager.getJavaModelManager().compilationParticipants.managedMarkerTypes();
			if (markerTypes.size() == 0) return;
			Iterator iterator = markerTypes.iterator();
			while (iterator.hasNext())
				resource.deleteMarkers((String) iterator.next(), false, IResource.DEPTH_INFINITE);
		}
	} catch (CoreException e) {
		// assume there were no problems
	}
}

public static void removeTasksFor(IResource resource) {
	try {
		if (resource != null && resource.exists())
			resource.deleteMarkers(IJavaModelMarker.TASK_MARKER, false, IResource.DEPTH_INFINITE);
	} catch (CoreException e) {
		// assume there were no problems
	}
}

public static void removeProblemsAndTasksFor(IResource resource) {
	try {
		if (resource != null && resource.exists()) {
			resource.deleteMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
			resource.deleteMarkers(IJavaModelMarker.TASK_MARKER, false, IResource.DEPTH_INFINITE);

			// delete managed markers
			Set markerTypes = JavaModelManager.getJavaModelManager().compilationParticipants.managedMarkerTypes();
			if (markerTypes.size() == 0) return;
			Iterator iterator = markerTypes.iterator();
			while (iterator.hasNext())
				resource.deleteMarkers((String) iterator.next(), false, IResource.DEPTH_INFINITE);
		}
	} catch (CoreException e) {
		// assume there were no problems
	}
}

public static State readState(IProject project, DataInputStream in) throws IOException, CoreException {
	return State.read(project, in);
}

public static void writeState(Object state, DataOutputStream out) throws IOException {
	((State) state).write(out);
}

@Override
protected IProject[] build(int kind, Map ignored, IProgressMonitor monitor) throws CoreException {
	this.currentProject = getProject();
	if (this.currentProject == null || !this.currentProject.isAccessible()) return new IProject[0];

	if (DEBUG)
		System.out.println("\nJavaBuilder: Starting build of " + this.currentProject.getName() //$NON-NLS-1$
			+ " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$
	this.notifier = new BuildNotifier(monitor, this.currentProject);
	this.notifier.begin();
	boolean ok = false;
	try {
		this.notifier.checkCancel();
		kind = initializeBuilder(kind, true);

		if (isWorthBuilding()) {
			if (kind == FULL_BUILD) {
				if (DEBUG)
					System.out.println("JavaBuilder: Performing full build as requested"); //$NON-NLS-1$
				buildAll();
			} else {
				if ((this.lastState = getLastState(this.currentProject)) == null) {
					if (DEBUG)
						System.out.println("JavaBuilder: Performing full build since last saved state was not found"); //$NON-NLS-1$
					buildAll();
				} else if (hasClasspathChanged()) {
					// if the output location changes, do not delete the binary files from old location
					// the user may be trying something
					if (DEBUG)
						System.out.println("JavaBuilder: Performing full build since classpath has changed"); //$NON-NLS-1$
					buildAll();
				} else if (this.nameEnvironment.sourceLocations.length > 0 || this.testNameEnvironment.sourceLocations.length > 0) {
					// if there is no source to compile & no classpath changes then we are done
					SimpleLookupTable deltas = findDeltas();
					if (deltas == null) {
						if (DEBUG)
							System.out.println("JavaBuilder: Performing full build since deltas are missing after incremental request"); //$NON-NLS-1$
						buildAll();
					} else if (deltas.elementSize > 0) {
						if (hasJdtCoreSettingsChange(deltas) && !DISABLE_AUTO_BUILDING_ON_SETTINGS_CHANGE) {
							if (DEBUG)
								System.out.println("JavaBuilder: Performing full build since project settings have changed"); //$NON-NLS-1$
							buildAll();
						} else {
							buildDeltas(deltas);
						}
					} else if (DEBUG) {
						System.out.println("JavaBuilder: Nothing to build since deltas were empty"); //$NON-NLS-1$
					}
				} else {
					if (hasStructuralDelta()) { // double check that a jar file didn't get replaced in a binary project
						if (DEBUG)
							System.out.println("JavaBuilder: Performing full build since there are structural deltas"); //$NON-NLS-1$
						buildAll();
					} else {
						if (DEBUG)
							System.out.println("JavaBuilder: Nothing to build since there are no source folders and no deltas"); //$NON-NLS-1$
						this.lastState.tagAsNoopBuild();
					}
				}
			}
			ok = true;
		}
	} catch (CoreException e) {
		Util.log(e, "JavaBuilder handling CoreException while building: " + this.currentProject.getName()); //$NON-NLS-1$
		createInconsistentBuildMarker(e);
	} catch (ImageBuilderInternalException e) {
		Util.log(e.getThrowable(), "JavaBuilder handling ImageBuilderInternalException while building: " + this.currentProject.getName()); //$NON-NLS-1$
		createInconsistentBuildMarker(e.coreException);
	} catch (MissingSourceFileException e) {
		// do not log this exception since its thrown to handle aborted compiles because of missing source files
		if (DEBUG)
			System.out.println(Messages.bind(Messages.build_missingSourceFile, e.missingSourceFile));
		removeProblemsAndTasksFor(this.currentProject); // make this the only problem for this project

		Map<String, Object> attributes = new HashMap<>();
		attributes.put(IMarker.MESSAGE, Messages.bind(Messages.build_missingSourceFile, e.missingSourceFile));
		attributes.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_ERROR));
		attributes.put(IMarker.SOURCE_ID, JavaBuilder.SOURCE_ID);
		this.currentProject.createMarker(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, attributes);
	} finally {
		for (int i = 0, l = this.participants == null ? 0 : this.participants.length; i < l; i++)
			this.participants[i].buildFinished(this.javaProject);
		if (!ok)
			// If the build failed, clear the previously built state, forcing a full build next time.
			clearLastState();
		this.notifier.done();
		cleanup();
	}
	IProject[] requiredProjects = getRequiredProjects(true);
	if (DEBUG)
		System.out.println("JavaBuilder: Finished build of " + this.currentProject.getName() //$NON-NLS-1$
			+ " @ " + new Date(System.currentTimeMillis()) + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
	return requiredProjects;
}

private void buildAll() {
	this.notifier.checkCancel();
	this.notifier.subTask(Messages.bind(Messages.build_preparingBuild, this.currentProject.getName()));
	if (DEBUG && this.lastState != null)
		System.out.println("JavaBuilder: Clearing last state : " + this.lastState); //$NON-NLS-1$
	clearLastState();
	BatchImageBuilder imageBuilder = new BatchImageBuilder(this, true, CompilationGroup.MAIN);
	BatchImageBuilder testImageBuilder = new BatchImageBuilder(imageBuilder, true, CompilationGroup.TEST);
	imageBuilder.build();
	if (testImageBuilder.sourceLocations.length > 0) {
		// Note: testImageBuilder *MUST* have a separate output folder, or it will delete the files created by imageBuilder.build()
		testImageBuilder.build();
	} else {
		testImageBuilder.cleanUp();
	}
	recordNewState(imageBuilder.newState);
}

private void buildDeltas(SimpleLookupTable deltas) {
	this.notifier.checkCancel();
	this.notifier.subTask(Messages.bind(Messages.build_preparingBuild, this.currentProject.getName()));
	if (DEBUG && this.lastState != null)
		System.out.println("JavaBuilder: Clearing last state : " + this.lastState); //$NON-NLS-1$
	clearLastState(); // clear the previously built state so if the build fails, a full build will occur next time
	IncrementalImageBuilder imageBuilder = new IncrementalImageBuilder(this);
	if (imageBuilder.build(deltas)) {
		recordNewState(imageBuilder.newState);
	} else {
		if (DEBUG)
			System.out.println("JavaBuilder: Performing full build since incremental build failed"); //$NON-NLS-1$
		buildAll();
	}
}

@Override
protected void clean(IProgressMonitor monitor) throws CoreException {
	this.currentProject = getProject();
	if (this.currentProject == null || !this.currentProject.isAccessible()) return;

	if (DEBUG)
		System.out.println("\nJavaBuilder: Cleaning " + this.currentProject.getName() //$NON-NLS-1$
			+ " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$
	this.notifier = new BuildNotifier(monitor, this.currentProject);
	this.notifier.begin();
	try {
		this.notifier.checkCancel();

		initializeBuilder(CLEAN_BUILD, true);
		if (DEBUG)
			System.out.println("JavaBuilder: Clearing last state as part of clean : " + this.lastState); //$NON-NLS-1$
		clearLastState();
		removeProblemsAndTasksFor(this.currentProject);
		new BatchImageBuilder(this, false, CompilationGroup.MAIN).cleanOutputFolders(false);
		new BatchImageBuilder(this, false, CompilationGroup.TEST).cleanOutputFolders(false);
	} catch (CoreException e) {
		Util.log(e, "JavaBuilder handling CoreException while cleaning: " + this.currentProject.getName()); //$NON-NLS-1$
		createInconsistentBuildMarker(e);
	} finally {
		this.notifier.done();
		cleanup();
	}
	if (DEBUG)
		System.out.println("JavaBuilder: Finished cleaning " + this.currentProject.getName() //$NON-NLS-1$
			+ " @ " + new Date(System.currentTimeMillis())); //$NON-NLS-1$
}

private void createInconsistentBuildMarker(CoreException coreException) throws CoreException {
	String message = null;
	IStatus status = coreException.getStatus();
 	if (status.isMultiStatus()) {
 		IStatus[] children = status.getChildren();
 		if (children != null && children.length > 0)
 		    message = children[0].getMessage();
 	}
 	if (message == null)
 		message = coreException.getMessage();

	Map<String, Object> attributes = new HashMap<>();
	attributes.put(IMarker.MESSAGE, Messages.bind(Messages.build_inconsistentProject, message));
	attributes.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_ERROR));
	attributes.put(IJavaModelMarker.CATEGORY_ID, Integer.valueOf(CategorizedProblem.CAT_BUILDPATH));
	attributes.put(IMarker.SOURCE_ID, JavaBuilder.SOURCE_ID);
	this.currentProject.createMarker(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, attributes);
}

private void cleanup() {
	this.participants = null;
	if(this.nameEnvironment != null) {
		this.nameEnvironment.cleanup();
		this.nameEnvironment = null;
	}
	if(this.testNameEnvironment != null) {
		this.testNameEnvironment.cleanup();
		this.testNameEnvironment = null;
	}
	this.binaryLocationsPerProject = null;
	this.lastState = null;
	this.notifier = null;
	this.extraResourceFileFilters = null;
	this.extraResourceFolderFilters = null;
}

private void clearLastState() {
	JavaModelManager.getJavaModelManager().setLastBuiltState(this.currentProject, null);
}

boolean filterExtraResource(IResource resource) {
	if (this.extraResourceFileFilters != null) {
		char[] name = resource.getName().toCharArray();
		for (int i = 0, l = this.extraResourceFileFilters.length; i < l; i++)
			if (CharOperation.match(this.extraResourceFileFilters[i], name, true))
				return true;
	}
	if (this.extraResourceFolderFilters != null) {
		IPath path = resource.getProjectRelativePath();
		String pathName = path.toString();
		int count = path.segmentCount();
		if (resource.getType() == IResource.FILE) count--;
		for (int i = 0, l = this.extraResourceFolderFilters.length; i < l; i++)
			if (pathName.indexOf(this.extraResourceFolderFilters[i]) != -1)
				for (int j = 0; j < count; j++)
					if (this.extraResourceFolderFilters[i].equals(path.segment(j)))
						return true;
	}
	return false;
}

private SimpleLookupTable findDeltas() {
	this.notifier.subTask(Messages.bind(Messages.build_readingDelta, this.currentProject.getName()));
	IResourceDelta delta = getDelta(this.currentProject);
	SimpleLookupTable deltas = new SimpleLookupTable(3);
	if (delta != null) {
		if (delta.getKind() != IResourceDelta.NO_CHANGE) {
			if (DEBUG)
				System.out.println("JavaBuilder: Found source delta for: " + this.currentProject.getName()); //$NON-NLS-1$
			deltas.put(this.currentProject, delta);
		}
	} else {
		if (DEBUG)
			System.out.println("JavaBuilder: Missing delta for: " + this.currentProject.getName()); //$NON-NLS-1$
		this.notifier.subTask(""); //$NON-NLS-1$
		return null;
	}

	Object[] keyTable = this.binaryLocationsPerProject.keyTable;
	Object[] valueTable = this.binaryLocationsPerProject.valueTable;
	nextProject : for (int i = 0, l = keyTable.length; i < l; i++) {
		IProject p = (IProject) keyTable[i];
		if (p != null && p != this.currentProject) {
			State s = getLastState(p);
			if (!this.lastState.wasStructurallyChanged(p, s)) { // see if we can skip its delta
				if (s.wasNoopBuild())
					continue nextProject; // project has no source folders and can be skipped
				ClasspathLocation[] classFoldersAndJars = (ClasspathLocation[]) valueTable[i];
				boolean canSkip = true;
				for (int j = 0, m = classFoldersAndJars.length; j < m; j++) {
					if (classFoldersAndJars[j].isOutputFolder())
						classFoldersAndJars[j] = null; // can ignore output folder since project was not structurally changed
					else
						canSkip = false;
				}
				if (canSkip) continue nextProject; // project has no structural changes in its output folders
			}

			this.notifier.subTask(Messages.bind(Messages.build_readingDelta, p.getName()));
			delta = getDelta(p);
			if (delta != null) {
				if (delta.getKind() != IResourceDelta.NO_CHANGE) {
					if (DEBUG)
						System.out.println("JavaBuilder: Found binary delta for: " + p.getName()); //$NON-NLS-1$
					deltas.put(p, delta);
				}
			} else {
				if (DEBUG)
					System.out.println("JavaBuilder: Missing delta for: " + p.getName());	 //$NON-NLS-1$
				this.notifier.subTask(""); //$NON-NLS-1$
				return null;
			}
		}
	}
	this.notifier.subTask(""); //$NON-NLS-1$
	return deltas;
}

public State getLastState(IProject project) {
	return (State) JavaModelManager.getJavaModelManager().getLastBuiltState(project, this.notifier.monitor);
}

/* Return the list of projects for which it requires a resource delta. This builder's project
* is implicitly included and need not be specified. Builders must re-specify the list
* of interesting projects every time they are run as this is not carried forward
* beyond the next build. Missing projects should be specified but will be ignored until
* they are added to the workspace.
*/
private IProject[] getRequiredProjects(boolean includeBinaryPrerequisites) {
	if (this.javaProject == null || this.workspaceRoot == null) return new IProject[0];

	LinkedHashSet<IProject> projects = new LinkedHashSet<>();
	ExternalFoldersManager externalFoldersManager = JavaModelManager.getExternalManager();
	try {
		IClasspathEntry[] entries = this.javaProject.getExpandedClasspath();
		for (int i = 0, l = entries.length; i < l; i++) {
			IClasspathEntry entry = entries[i];
			IPath path = entry.getPath();
			IProject p = null;
			switch (entry.getEntryKind()) {
				case IClasspathEntry.CPE_PROJECT :
					p = this.workspaceRoot.getProject(path.lastSegment()); // missing projects are considered too
					if (((ClasspathEntry) entry).isOptional() && !JavaProject.hasJavaNature(p)) // except if entry is optional
						p = null;
					break;
				case IClasspathEntry.CPE_LIBRARY :
					if (includeBinaryPrerequisites && path.segmentCount() > 0) {
						// some binary resources on the class path can come from projects that are not included in the project references
						IResource resource = this.workspaceRoot.findMember(path.segment(0));
						if (resource instanceof IProject) {
							p = (IProject) resource;
						} else {
							resource = externalFoldersManager.getFolder(path);
							if (resource != null)
								p = resource.getProject();
						}
					}
			}
			if (p != null && !projects.contains(p))
				projects.add(p);
		}
	} catch(JavaModelException e) {
		return new IProject[0];
	}
	IProject[] result = new IProject[projects.size()];
	projects.toArray(result);
	return result;
}

boolean hasBuildpathErrors() throws CoreException {
	IMarker[] markers = this.currentProject.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
	for (int i = 0, l = markers.length; i < l; i++)
		if (markers[i].getAttribute(IJavaModelMarker.CATEGORY_ID, -1) == CategorizedProblem.CAT_BUILDPATH)
			return true;
	return false;
}

private boolean hasJdtCoreSettingsChange(SimpleLookupTable deltas) {
	Object resourceDelta = deltas.get(this.currentProject);
	if (resourceDelta instanceof IResourceDelta) {
		return ((IResourceDelta) resourceDelta).findMember(JDT_CORE_SETTINGS_PATH) != null;
	}
	return false;
}

private boolean hasClasspathChanged() {
	return hasClasspathChanged(CompilationGroup.MAIN) || hasClasspathChanged(CompilationGroup.TEST);
}

private boolean hasClasspathChanged(CompilationGroup compilationGroup) {
	ClasspathMultiDirectory[] newSourceLocations = (compilationGroup == CompilationGroup.MAIN ? this.nameEnvironment : this.testNameEnvironment).sourceLocations;
	ClasspathMultiDirectory[] oldSourceLocations = compilationGroup == CompilationGroup.MAIN ? this.lastState.sourceLocations : this.lastState.testSourceLocations;
	int newLength = newSourceLocations.length;
	int oldLength = oldSourceLocations.length;
	int n, o;
	for (n = o = 0; n < newLength && o < oldLength; n++, o++) {
		if (newSourceLocations[n].equals(oldSourceLocations[o])) continue; // checks source & output folders
		try {
			if (newSourceLocations[n].sourceFolder.members().length == 0) { // added new empty source folder
				o--;
				continue;
			} else if (this.lastState.isSourceFolderEmpty(oldSourceLocations[o].sourceFolder)) {
				n--;
				continue;
			}
		} catch (CoreException ignore) { // skip it
		}
		if (DEBUG) {
			System.out.println("JavaBuilder: New location: " + newSourceLocations[n] + "\n!= old location: " + oldSourceLocations[o]); //$NON-NLS-1$ //$NON-NLS-2$
			printLocations(newSourceLocations, oldSourceLocations);
		}
		return true;
	}
	while (n < newLength) {
		try {
			if (newSourceLocations[n].sourceFolder.members().length == 0) { // added new empty source folder
				n++;
				continue;
			}
		} catch (CoreException ignore) { // skip it
		}
		if (DEBUG) {
			System.out.println("JavaBuilder: Added non-empty source folder"); //$NON-NLS-1$
			printLocations(newSourceLocations, oldSourceLocations);
		}
		return true;
	}
	while (o < oldLength) {
		if (this.lastState.isSourceFolderEmpty(oldSourceLocations[o].sourceFolder)) {
			o++;
			continue;
		}
		if (DEBUG) {
			System.out.println("JavaBuilder: Removed non-empty source folder"); //$NON-NLS-1$
			printLocations(newSourceLocations, oldSourceLocations);
		}
		return true;
	}

	ClasspathLocation[] newBinaryLocations = (compilationGroup == CompilationGroup.MAIN ? this.nameEnvironment : this.testNameEnvironment).binaryLocations;
	ClasspathLocation[] oldBinaryLocations = compilationGroup == CompilationGroup.MAIN ? this.lastState.binaryLocations : this.lastState.testBinaryLocations;
	newLength = newBinaryLocations.length;
	oldLength = oldBinaryLocations.length;
	for (n = o = 0; n < newLength && o < oldLength; n++, o++) {
		if (newBinaryLocations[n].equals(oldBinaryLocations[o])) continue;
		if (DEBUG) {
			System.out.println("JavaBuilder: New test location: " + newBinaryLocations[n] + "\n!= old test location: " + oldBinaryLocations[o]); //$NON-NLS-1$ //$NON-NLS-2$
			printLocations(newBinaryLocations, oldBinaryLocations);
		}
		return true;
	}
	if (n < newLength || o < oldLength) {
		if (DEBUG) {
			System.out.println("JavaBuilder: Number of test binary folders/jar files has changed:"); //$NON-NLS-1$
			printLocations(newBinaryLocations, oldBinaryLocations);
		}
		return true;
	}
	return false;
}

private boolean hasJavaBuilder(IProject project) throws CoreException {
	ICommand[] buildCommands = project.getDescription().getBuildSpec();
	for (int i = 0, l = buildCommands.length; i < l; i++)
		if (buildCommands[i].getBuilderName().equals(JavaCore.BUILDER_ID))
			return true;
	return false;
}

private boolean hasStructuralDelta() {
	// handle case when currentProject has only .class file folders and/or jar files... no source/output folders
	IResourceDelta delta = getDelta(this.currentProject);
	if (delta != null && delta.getKind() != IResourceDelta.NO_CHANGE) {
		ClasspathLocation[] classFoldersAndJars = (ClasspathLocation[]) this.binaryLocationsPerProject.get(this.currentProject);
		if (classFoldersAndJars != null) {
			for (int i = 0, l = classFoldersAndJars.length; i < l; i++) {
				ClasspathLocation classFolderOrJar = classFoldersAndJars[i]; // either a .class file folder or a zip/jar file
				if (classFolderOrJar != null) {
					IPath p = classFolderOrJar.getProjectRelativePath();
					if (p != null) {
						IResourceDelta binaryDelta = delta.findMember(p);
						if (binaryDelta != null && binaryDelta.getKind() != IResourceDelta.NO_CHANGE)
							return true;
					}
				}
			}
		}
	}
	return false;
}

private int initializeBuilder(int kind, boolean forBuild) throws CoreException {
	// some calls just need the nameEnvironment initialized so skip the rest
	this.javaProject = (JavaProject) JavaCore.create(this.currentProject);
	this.workspaceRoot = this.currentProject.getWorkspace().getRoot();

	if (forBuild) {
		// cache the known participants for this project
		this.participants = JavaModelManager.getJavaModelManager().compilationParticipants.getCompilationParticipants(this.javaProject);
		if (this.participants != null)
			for (int i = 0, l = this.participants.length; i < l; i++)
				if (this.participants[i].aboutToBuild(this.javaProject) == CompilationParticipant.NEEDS_FULL_BUILD)
					kind = FULL_BUILD;

		// Flush the existing external files cache if this is the beginning of a build cycle
		String projectName = this.currentProject.getName();
		if (builtProjects == null || builtProjects.contains(projectName)) {
			builtProjects = new LinkedHashSet();
		}
		builtProjects.add(projectName);
	}

	this.binaryLocationsPerProject = new SimpleLookupTable(3);
	this.nameEnvironment = new NameEnvironment(this.workspaceRoot, this.javaProject, this.binaryLocationsPerProject, this.notifier, CompilationGroup.MAIN);
	this.testNameEnvironment = new NameEnvironment(this.workspaceRoot, this.javaProject, this.binaryLocationsPerProject, this.notifier, CompilationGroup.TEST);

	if (forBuild) {
		String filterSequence = this.javaProject.getOption(JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER, true);
		char[][] filters = filterSequence != null && filterSequence.length() > 0
			? CharOperation.splitAndTrimOn(',', filterSequence.toCharArray())
			: null;
		if (filters == null) {
			this.extraResourceFileFilters = null;
			this.extraResourceFolderFilters = null;
		} else {
			int fileCount = 0, folderCount = 0;
			for (int i = 0, l = filters.length; i < l; i++) {
				char[] f = filters[i];
				if (f.length == 0) continue;
				if (f[f.length - 1] == '/') folderCount++; else fileCount++;
			}
			this.extraResourceFileFilters = new char[fileCount][];
			this.extraResourceFolderFilters = new String[folderCount];
			for (int i = 0, l = filters.length; i < l; i++) {
				char[] f = filters[i];
				if (f.length == 0) continue;
				if (f[f.length - 1] == '/')
					this.extraResourceFolderFilters[--folderCount] = new String(f, 0, f.length - 1);
				else
					this.extraResourceFileFilters[--fileCount] = f;
			}
		}
	}
	return kind;
}

private boolean isClasspathBroken(JavaProject jProj, boolean tryRepair) throws CoreException {
	IMarker[] markers = jProj.getProject().findMarkers(IJavaModelMarker.BUILDPATH_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
	for (int i = 0, l = markers.length; i < l; i++) {
		if (markers[i].getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR) {
			if (tryRepair) {
				Object code = markers[i].getAttribute(IJavaModelMarker.ID);
				if (code instanceof Integer && ((Integer)code) == IJavaModelStatusConstants.CP_INVALID_EXTERNAL_ANNOTATION_PATH) {
					new ClasspathValidation(jProj).validate();
					return isClasspathBroken(jProj, false);
				}
			}
			return true;
		}
	}
	return false;
}

private boolean isWorthBuilding() throws CoreException {
	boolean abortBuilds =
		JavaCore.ABORT.equals(this.javaProject.getOption(JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH, true));
	if (!abortBuilds) {
		if (DEBUG)
			System.out.println("JavaBuilder: Ignoring invalid classpath"); //$NON-NLS-1$
		return true;
	}

	// Abort build only if there are classpath errors
	if (isClasspathBroken(this.javaProject, true)) {
		if (DEBUG)
			System.out.println("JavaBuilder: Aborted build because project has classpath errors (incomplete or involved in cycle)"); //$NON-NLS-1$

		removeProblemsAndTasksFor(this.currentProject); // remove all compilation problems

		Map<String, Object> attributes = new HashMap<>();
		attributes.put(IMarker.MESSAGE, Messages.build_abortDueToClasspathProblems);
		attributes.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_ERROR));
		attributes.put(IJavaModelMarker.CATEGORY_ID, Integer.valueOf(CategorizedProblem.CAT_BUILDPATH));
		attributes.put(IMarker.SOURCE_ID, JavaBuilder.SOURCE_ID);

		this.currentProject.createMarker(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, attributes);
		return false;
	}

	if (JavaCore.WARNING.equals(this.javaProject.getOption(JavaCore.CORE_INCOMPLETE_CLASSPATH, true)))
		return true;

	// make sure all prereq projects have valid build states... only when aborting builds since projects in cycles do not have build states
	// except for projects involved in a 'warning' cycle (see below)
	IProject[] requiredProjects = getRequiredProjects(false);
	for (int i = 0, l = requiredProjects.length; i < l; i++) {
		IProject p = requiredProjects[i];
		if (getLastState(p) == null)  {
			// The prereq project has no build state: if this prereq project has a 'warning' cycle marker then allow build (see bug id 23357)
			JavaProject prereq = (JavaProject) JavaCore.create(p);
			if (prereq.hasCycleMarker() && JavaCore.WARNING.equals(this.javaProject.getOption(JavaCore.CORE_CIRCULAR_CLASSPATH, true))) {
				if (DEBUG)
					System.out.println("JavaBuilder: Continued to build even though prereq project " + p.getName() //$NON-NLS-1$
						+ " was not built since its part of a cycle"); //$NON-NLS-1$
				continue;
			}
			if (!hasJavaBuilder(p)) {
				if (DEBUG)
					System.out.println("JavaBuilder: Continued to build even though prereq project " + p.getName() //$NON-NLS-1$
						+ " is not built by JavaBuilder"); //$NON-NLS-1$
				continue;
			}
			if (DEBUG)
				System.out.println("JavaBuilder: Aborted build because prereq project " + p.getName() //$NON-NLS-1$
					+ " was not built"); //$NON-NLS-1$

			removeProblemsAndTasksFor(this.currentProject); // make this the only problem for this project

			Map<String, Object> attributes = new HashMap<>();
			attributes.put(IMarker.MESSAGE,
					isClasspathBroken(prereq, true)
							? Messages.bind(Messages.build_prereqProjectHasClasspathProblems, p.getName())
							: Messages.bind(Messages.build_prereqProjectMustBeRebuilt, p.getName()));
			attributes.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_ERROR));
			attributes.put(IJavaModelMarker.CATEGORY_ID, Integer.valueOf(CategorizedProblem.CAT_BUILDPATH));
			attributes.put(IMarker.SOURCE_ID, JavaBuilder.SOURCE_ID);
			this.currentProject.createMarker(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, attributes);
			return false;
		}
	}
	return true;
}

/*
 * Instruct the build manager that this project is involved in a cycle and
 * needs to propagate structural changes to the other projects in the cycle.
 */
void mustPropagateStructuralChanges() {
	LinkedHashSet cycleParticipants = new LinkedHashSet(3);
	this.javaProject.updateCycleParticipants(new ArrayList(), cycleParticipants, new HashMap<>(), this.workspaceRoot, new HashSet(3), null);
	IPath currentPath = this.javaProject.getPath();
	Iterator i= cycleParticipants.iterator();
	while (i.hasNext()) {
		IPath participantPath = (IPath) i.next();
		if (participantPath != currentPath) {
			IProject project = this.workspaceRoot.getProject(participantPath.segment(0));
			if (hasBeenBuilt(project)) {
				if (DEBUG)
					System.out.println("JavaBuilder: Requesting another build iteration since cycle participant " + project.getName() //$NON-NLS-1$
						+ " has not yet seen some structural changes"); //$NON-NLS-1$
				needRebuild();
				return;
			}
		}
	}
}

private void printLocations(ClasspathLocation[] newLocations, ClasspathLocation[] oldLocations) {
	System.out.println("JavaBuilder: New locations:"); //$NON-NLS-1$
	for (int i = 0, length = newLocations.length; i < length; i++)
		System.out.println("    " + newLocations[i].debugPathString()); //$NON-NLS-1$
	System.out.println("JavaBuilder: Old locations:"); //$NON-NLS-1$
	for (int i = 0, length = oldLocations.length; i < length; i++)
		System.out.println("    " + oldLocations[i].debugPathString()); //$NON-NLS-1$
}

private void recordNewState(State state) {
	Object[] keyTable = this.binaryLocationsPerProject.keyTable;
	for (int i = 0, l = keyTable.length; i < l; i++) {
		IProject prereqProject = (IProject) keyTable[i];
		if (prereqProject != null && prereqProject != this.currentProject)
			state.recordStructuralDependency(prereqProject, getLastState(prereqProject));
	}

	if (DEBUG)
		System.out.println("JavaBuilder: Recording new state : " + state); //$NON-NLS-1$
	// state.dump();
	JavaModelManager.getJavaModelManager().setLastBuiltState(this.currentProject, state);
}

/**
 * String representation for debugging purposes
 */
@Override
public String toString() {
	return this.currentProject == null
		? "JavaBuilder for unknown project" //$NON-NLS-1$
		: "JavaBuilder for " + this.currentProject.getName(); //$NON-NLS-1$
}
}
