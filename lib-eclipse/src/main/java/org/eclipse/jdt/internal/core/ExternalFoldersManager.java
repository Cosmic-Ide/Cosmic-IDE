/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - inconsistent initialization of classpath container backed by external class folder, see https://bugs.eclipse.org/320618
 *     Thirumala Reddy Mutchukota <thirumala@google.com> - Contribution to bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=411423
 *     Terry Parker <tparker@google.com> - [performance] Low hit rates in JavaModel caches - https://bugs.eclipse.org/421165
 *     Andrey Loskutov <loskutov@gmx.de> - ExternalFoldersManager.RefreshJob interrupts auto build job - https://bugs.eclipse.org/476059
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.DeltaProcessor.RootInfo;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

public class ExternalFoldersManager {
	private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");  //$NON-NLS-1$//$NON-NLS-2$
	private static final String EXTERNAL_PROJECT_NAME = ".org.eclipse.jdt.core.external.folders"; //$NON-NLS-1$
	private static final String LINKED_FOLDER_NAME = ".link"; //$NON-NLS-1$
	private Map<IPath, IFolder> folders;
	private Set<IPath> pendingFolders; // subset of keys of 'folders', for which linked folders haven't been created yet.
	private final AtomicInteger counter = new AtomicInteger(0);
	/* Singleton instance */
	private static ExternalFoldersManager MANAGER;
	private RefreshJob refreshJob;

	private ExternalFoldersManager() {
		// Prevent instantiation
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=377806
		if (Platform.isRunning()) {
			/*
			 * The code here runs during JavaCore start-up.
			 * So if we need to open the external folders project, we do this from a job.
			 * Otherwise workspace jobs that attempt to access JDT core functionality can cause a deadlock.
			 *
			 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=542860.
			 */
			class InitializeFolders extends WorkspaceJob {
				public InitializeFolders() {
					super("Initialize external folders"); //$NON-NLS-1$
				}

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) {
					getFolders();
					return Status.OK_STATUS;
				}

				@Override
				public boolean belongsTo(Object family) {
					return family == InitializeFolders.class;
				}
			}
			InitializeFolders initializeFolders = new InitializeFolders();
			IProject project = getExternalFoldersProject();
			initializeFolders.setRule(project);
			initializeFolders.schedule();
		}
	}

	public static synchronized ExternalFoldersManager getExternalFoldersManager() {
		if (MANAGER == null) {
			 MANAGER = new ExternalFoldersManager();
		}
		return MANAGER;
	}

	/**
	 * Returns a set of external paths to external folders referred to on the given classpath.
	 * Returns <code>null</code> if there are none.
	 */
	public static Set<IPath> getExternalFolders(IClasspathEntry[] classpath) {
		if (classpath == null)
			return null;
		Set<IPath> folders = null;
		for (int i = 0; i < classpath.length; i++) {
			IClasspathEntry entry = classpath[i];
			if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				IPath entryPath = entry.getPath();
				if (isExternalFolderPath(entryPath)) {
					if (folders == null)
						folders = new LinkedHashSet<>();
					folders.add(entryPath);
				}
				IPath attachmentPath = entry.getSourceAttachmentPath();
				if (isExternalFolderPath(attachmentPath)) {
					if (folders == null)
						folders = new LinkedHashSet<>();
					folders.add(attachmentPath);
				}
			}
		}
		return folders;
	}

	/**
	 * Returns <code>true</code> if the provided path is a folder external to the project.
	 * The path is expected to be one matching the {@link IClasspathEntry#CPE_LIBRARY} case in
	 * {@link IClasspathEntry#getPath()} definition.
	 */
	public static boolean isExternalFolderPath(IPath externalPath) {
		if (externalPath == null || externalPath.isEmpty()) {
			return false;
		}

		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		if (manager.isExternalFile(externalPath) || manager.isAssumedExternalFile(externalPath)) {
			return false;
		}
		if (!externalPath.isAbsolute()
				|| (WINDOWS && (externalPath.getDevice() == null && !externalPath.isUNC()))) {
			// can be only project relative path
			return false;
		}
		// Test if this an absolute path in local file system (not the workspace path)
		File externalFolder = externalPath.toFile();
		if (Files.isRegularFile(externalFolder.toPath())) {
			manager.addExternalFile(externalPath);
			return false;
		}
		if (Files.isDirectory(externalFolder.toPath())) {
			return true;
		}
		// this can be now only full workspace path or an external path to a not existing file or folder
		if (isInternalFilePath(externalPath)) {
			return false;
		}
		if (isInternalContainerPath(externalPath)) {
			return false;
		}
		// From here on the legacy code assumes that not existing resource must be external.
		// We just follow the old assumption.
		if (externalPath.getFileExtension() != null/*likely a .jar, .zip, .rar or other file*/) {
			manager.addAssumedExternalFile(externalPath);
			// assume not existing external (?) file (?) (can also be a folder with dotted name!)
			return false;
		}
		// assume not existing external (?) folder (?)
		return true;
	}

	/**
	 * @param path full absolute workspace path
	 */
	private static boolean isInternalFilePath(IPath path) {
		IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
		// in case this is full workspace path it should start with project segment
		if(path.segmentCount() > 1 && wsRoot.getFile(path).exists()) {
			return true;
		}
		return false;
	}

	/**
	 * @param path full absolute workspace path
	 */
	private static boolean isInternalContainerPath(IPath path) {
		IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
		// in case this is full workspace path it should start with project segment
		int segmentCount = path.segmentCount();
		if(segmentCount == 1 && wsRoot.getProject(path.segment(0)).exists()) {
			return true;
		}
		if(segmentCount > 1 && wsRoot.getFolder(path).exists()) {
			return true;
		}
		return false;
	}

	public static boolean isInternalPathForExternalFolder(IPath resourcePath) {
		return EXTERNAL_PROJECT_NAME.equals(resourcePath.segment(0));
	}

	public IFolder addFolder(IPath externalFolderPath, boolean scheduleForCreation) {
		return addFolder(externalFolderPath, getExternalFoldersProject(), scheduleForCreation);
	}

	private IFolder addFolder(IPath externalFolderPath, IProject externalFoldersProject, boolean scheduleForCreation) {
		Map<IPath, IFolder> knownFolders = getFolders();

		IFolder existing;
		synchronized (this) {
			existing = knownFolders.get(externalFolderPath);
			if (existing != null) {
				return existing;
			}
		}

		IFolder result;
		do {
			result = externalFoldersProject.getFolder(LINKED_FOLDER_NAME + this.counter.incrementAndGet());
		} while (result.exists());

		synchronized (this) {
			if (scheduleForCreation) {
				if (this.pendingFolders == null)
					this.pendingFolders = new LinkedHashSet<>();
				this.pendingFolders.add(externalFolderPath);
			}
			existing = knownFolders.get(externalFolderPath);
			if (existing != null) {
				return existing;
			}
			knownFolders.put(externalFolderPath, result);
		}
		return result;
	}

	/**
	 * Try to remove the argument from the list of folders pending for creation.
	 * @param externalPath to link to
	 * @return true if the argument was found in the list of pending folders and could be removed from it.
	 */
	public synchronized boolean removePendingFolder(Object externalPath) {
		if (this.pendingFolders == null)
			return false;
		return this.pendingFolders.remove(externalPath);
	}

	public IFolder createLinkFolder(IPath externalFolderPath, boolean refreshIfExistAlready, IProgressMonitor monitor) throws CoreException {
		IProject externalFoldersProject = createExternalFoldersProject(monitor); // run outside synchronized as this can create a resource
		return createLinkFolder(externalFolderPath, refreshIfExistAlready, externalFoldersProject, monitor);
	}

	private IFolder createLinkFolder(IPath externalFolderPath, boolean refreshIfExistAlready,
									IProject externalFoldersProject, IProgressMonitor monitor) throws CoreException {

		IFolder result = addFolder(externalFolderPath, externalFoldersProject, false);
		if (!result.exists()) {
			try {
				result.createLink(externalFolderPath, IResource.ALLOW_MISSING_LOCAL, monitor);
			} catch (CoreException e) {
				// If we managed to create the folder in the meantime, don't complain
				if (!result.exists()) {
					throw e;
				}
			}
		} else if (refreshIfExistAlready) {
			result.refreshLocal(IResource.DEPTH_INFINITE,  monitor);
		}
		return result;
	}

	public void createPendingFolders(IProgressMonitor monitor) throws JavaModelException{
		synchronized (this) {
			if (this.pendingFolders == null || this.pendingFolders.isEmpty()) return;
		}

		IProject externalFoldersProject = null;
		try {
			externalFoldersProject = createExternalFoldersProject(monitor);
		}
		catch(CoreException e) {
			throw new JavaModelException(e);
		}
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=368152
		// To avoid race condition (from addFolder and removeFolder, load the map elements into an array and clear the map immediately.
		// The createLinkFolder being in the synchronized block can cause a deadlock and hence keep it out of the synchronized block.
		Object[] arrayOfFolders = null;
		synchronized (this) {
			arrayOfFolders = this.pendingFolders.toArray();
			this.pendingFolders.clear();
		}

		for (int i=0; i < arrayOfFolders.length; i++) {
			try {
				createLinkFolder((IPath) arrayOfFolders[i], false, externalFoldersProject, monitor);
			} catch (CoreException e) {
				Util.log(e, "Error while creating a link for external folder :" + arrayOfFolders[i]); //$NON-NLS-1$
			}
		}
	}

	public void cleanUp(IProgressMonitor monitor) throws CoreException {
		List<Entry<IPath, IFolder>> toDelete = getFoldersToCleanUp(monitor);
		if (toDelete == null)
			return;
		for (Entry<IPath, IFolder> entry : toDelete) {
			IFolder folder = entry.getValue();
			folder.delete(true, monitor);
			IPath key = entry.getKey();
			this.folders.remove(key);
		}
		IProject project = getExternalFoldersProject();
		if (project.isAccessible() && project.members().length == 1/*remaining member is .project*/)
			project.delete(true, monitor);
	}

	private List<Entry<IPath, IFolder>> getFoldersToCleanUp(IProgressMonitor monitor) throws CoreException {
		DeltaProcessingState state = JavaModelManager.getDeltaState();
		Map<IPath, RootInfo> roots = state.roots;
		Map<IPath, IPath> sourceAttachments = state.sourceAttachments;
		if (roots == null && sourceAttachments == null)
			return null;
		Map<IPath, IFolder> knownFolders = getFolders();
		List<Entry<IPath, IFolder>> result = null;
		synchronized (knownFolders) {
			Iterator<Entry<IPath, IFolder>> iterator = knownFolders.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<IPath, IFolder> entry = iterator.next();
				IPath path = entry.getKey();
				if ((roots != null && !roots.containsKey(path))
						&& (sourceAttachments != null && !sourceAttachments.containsKey(path))) {
					if (entry.getValue() != null) {
						if (result == null)
							result = new ArrayList<>();
						result.add(entry);
					}
				}
			}
		}
		return result;
	}

	public IProject getExternalFoldersProject() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(EXTERNAL_PROJECT_NAME);
	}

	public IProject createExternalFoldersProject(IProgressMonitor monitor) throws CoreException {
		IProject project = getExternalFoldersProject();
		if (!project.isAccessible()) {
			if (!project.exists()) {
				createExternalFoldersProject(project, monitor);
			}
			openExternalFoldersProject(project, monitor);
		}
		return project;
	}

	/*
	 * Attempt to open the given project (assuming it exists).
	 * If failing to open, make all attempts to recreate the missing pieces.
	 */
	private void openExternalFoldersProject(IProject project, IProgressMonitor monitor) throws CoreException {
		try {
			project.open(monitor);
		} catch (CoreException e1) {
			if (e1.getStatus().getCode() == IResourceStatus.FAILED_READ_METADATA) {
				// workspace was moved
				// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=241400 and https://bugs.eclipse.org/bugs/show_bug.cgi?id=252571 )
				project.delete(false/*don't delete content*/, true/*force*/, monitor);
				createExternalFoldersProject(project, monitor);
			} else {
				// .project or folder on disk have been deleted, recreate them
				IPath stateLocation = JavaCore.getPlugin().getStateLocation();
				IPath projectPath = stateLocation.append(EXTERNAL_PROJECT_NAME);
				try {
					Files.createDirectories(projectPath.toFile().toPath());
					try (FileOutputStream output = new FileOutputStream(projectPath.append(".project").toOSString())){ //$NON-NLS-1$
				        output.write((
				        		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //$NON-NLS-1$
				        		"<projectDescription>\n" + //$NON-NLS-1$
				        		"	<name>" + EXTERNAL_PROJECT_NAME + "</name>\n" + //$NON-NLS-1$ //$NON-NLS-2$
				        		"	<comment></comment>\n" + //$NON-NLS-1$
				        		"	<projects>\n" + //$NON-NLS-1$
				        		"	</projects>\n" + //$NON-NLS-1$
				        		"	<buildSpec>\n" + //$NON-NLS-1$
				        		"	</buildSpec>\n" + //$NON-NLS-1$
				        		"	<natures>\n" + //$NON-NLS-1$
				        		"	</natures>\n" + //$NON-NLS-1$
				        		"</projectDescription>").getBytes()); //$NON-NLS-1$
				    }
				} catch (IOException e) {
					// fallback to re-creating the project
					project.delete(false/*don't delete content*/, true/*force*/, monitor);
					createExternalFoldersProject(project, monitor);
				}
			}
			project.open(monitor);
		}
	}


	private void createExternalFoldersProject(IProject project, IProgressMonitor monitor) throws CoreException {
		IProjectDescription desc = project.getWorkspace().newProjectDescription(project.getName());
		IPath stateLocation = JavaCore.getPlugin().getStateLocation();
		desc.setLocation(stateLocation.append(EXTERNAL_PROJECT_NAME));
		try {
			project.create(desc, IResource.HIDDEN, monitor);
		} catch (CoreException e) {
			// If we managed to create the project in the meantime, don't complain
			if (!project.exists()) {
				throw e;
			}
		}
	}

	public IFolder getFolder(IPath externalFolderPath) {
		return getFolders().get(externalFolderPath);
	}

	Map<IPath, IFolder> getFolders() {
		if (this.folders == null) {
			Map<IPath, IFolder> tempFolders = new LinkedHashMap<>();
			IProject project = getExternalFoldersProject();
			try {
				if (!project.isAccessible()) {
					if (project.exists()) {
						// workspace was moved (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=252571 )
						openExternalFoldersProject(project, null/*no progress*/);
					} else {
						// if project doesn't exist, do not open and recreate it as it means that there are no external folders
						return this.folders = Collections.synchronizedMap(tempFolders);
					}
				}
				IResource[] members = project.members();
				for (IResource member : members) {
					if (member.getType() == IResource.FOLDER && member.isLinked() && member.getName().startsWith(LINKED_FOLDER_NAME)) {
						IPath externalFolderPath = member.getLocation();
						tempFolders.put(externalFolderPath, (IFolder) member);
					}
				}
			} catch (CoreException e) {
				Util.log(e, "Exception while initializing external folders"); //$NON-NLS-1$
			}
			synchronized (this) {
				if (this.folders == null) {
					this.folders = Collections.synchronizedMap(tempFolders);
				}
			}
		}
		return this.folders;
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=313153
	// Use the same RefreshJob if the job is still available
	private synchronized void runRefreshJob(Collection<IPath> paths) {
		if (paths == null || paths.isEmpty()) {
			return;
		}
		if (this.refreshJob == null) {
			this.refreshJob = new RefreshJob();
		}
		this.refreshJob.addFoldersToRefresh(paths);
	}

	/*
	 * Refreshes the external folders referenced on the classpath of the given source project
	 */
	public void refreshReferences(final IProject[] sourceProjects, IProgressMonitor monitor) {
		IProject externalProject = getExternalFoldersProject();
		try {
			Set<IPath> externalFolders = null;
			for (int index = 0; index < sourceProjects.length; index++) {
				if (sourceProjects[index].equals(externalProject))
					continue;
				if (!JavaProject.hasJavaNature(sourceProjects[index]))
					continue;

				Set<IPath> foldersInProject = getExternalFolders(((JavaProject) JavaCore.create(sourceProjects[index])).getResolvedClasspath());

				if (foldersInProject == null || foldersInProject.size() == 0)
					continue;
				if (externalFolders == null)
					externalFolders = new LinkedHashSet<>();

				externalFolders.addAll(foldersInProject);
			}
			runRefreshJob(externalFolders);

		} catch (CoreException e) {
			Util.log(e, "Exception while refreshing external project"); //$NON-NLS-1$
		}
	}

	public void refreshReferences(IProject source, IProgressMonitor monitor) {
		IProject externalProject = getExternalFoldersProject();
		if (source.equals(externalProject))
			return;
		if (!JavaProject.hasJavaNature(source))
			return;
		try {
			Set<IPath> externalFolders = getExternalFolders(((JavaProject) JavaCore.create(source)).getResolvedClasspath());
			runRefreshJob(externalFolders);
		} catch (CoreException e) {
			Util.log(e, "Exception while refreshing external project"); //$NON-NLS-1$
		}
	}

	public IFolder removeFolder(IPath externalFolderPath) {
		return getFolders().remove(externalFolderPath);
	}

	static class RefreshJob extends Job {

		final LinkedHashSet<IPath> externalFolders;

		RefreshJob(){
			super(Messages.refreshing_external_folders);
			// bug 476059: don't interrupt autobuild by using rule and system flag.
			setSystem(true);
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			setRule(workspace.getRuleFactory().refreshRule(workspace.getRoot()));
			this.externalFolders = new LinkedHashSet<>();
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == ResourcesPlugin.FAMILY_MANUAL_REFRESH;
		}

		/*
		 * Add the collection of paths to be refreshed to the already
		 * existing set of paths and schedules the job
		 */
		public void addFoldersToRefresh(Collection<IPath> paths) {
			boolean shouldSchedule;
			synchronized (this.externalFolders) {
				this.externalFolders.addAll(paths);
				shouldSchedule = !this.externalFolders.isEmpty();
			}
			if (shouldSchedule) {
				schedule();
			}
		}

		@Override
		protected IStatus run(IProgressMonitor pm) {
			MultiStatus errors = new MultiStatus(JavaCore.PLUGIN_ID, IStatus.OK,
					"Exception while refreshing external folders", null); //$NON-NLS-1$
			while (true) {
				IPath externalPath;
				synchronized (this.externalFolders) {
					if (this.externalFolders.isEmpty()) {
						return errors.isOK()? Status.OK_STATUS : errors;
					}
					// keep the path in the list to avoid re-adding it while we are working
					externalPath = this.externalFolders.iterator().next();
				}

				try {
					IFolder folder = getExternalFoldersManager().getFolder(externalPath);
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=321358
					if (folder != null) {
						folder.refreshLocal(IResource.DEPTH_INFINITE, pm);
					}
				} catch (CoreException e) {
					errors.merge(e.getStatus());
				} finally {
					// we should always remove the path to avoid endless loop trying to refresh it
					synchronized (this.externalFolders) {
						this.externalFolders.remove(externalPath);
					}
				}
			}
		}
	}

}
