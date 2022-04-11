/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.util.Util;
import org.osgi.service.prefs.BackingStoreException;

/**
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class UserLibraryManager {

	public final static String CP_USERLIBRARY_PREFERENCES_PREFIX = JavaCore.PLUGIN_ID+".userLibrary."; //$NON-NLS-1$

	private Map userLibraries;

	public UserLibraryManager() {
		initialize();
	}

	/*
	 * Gets the library for a given name or <code>null</code> if no such library exists.
	 */
	public synchronized UserLibrary getUserLibrary(String libName) {
		return (UserLibrary) this.userLibraries.get(libName);
	}

	/*
	 * Returns the names of all defined user libraries. The corresponding classpath container path
	 * is the name appended to the CONTAINER_ID.
	 */
	public synchronized String[] getUserLibraryNames() {
		Set set = this.userLibraries.keySet();
		return (String[]) set.toArray(new String[set.size()]);
	}

	private void initialize() {
		this.userLibraries = new HashMap();
		IEclipsePreferences instancePreferences = JavaModelManager.getJavaModelManager().getInstancePreferences();
		String[] propertyNames;
		try {
			propertyNames = instancePreferences.keys();
		} catch (BackingStoreException e) {
			Util.log(e, "Exception while initializing user libraries"); //$NON-NLS-1$
			return;
		}

		boolean preferencesNeedFlush = false;
		for (int i = 0, length = propertyNames.length; i < length; i++) {
			String propertyName = propertyNames[i];
			if (propertyName.startsWith(CP_USERLIBRARY_PREFERENCES_PREFIX)) {
				String propertyValue = instancePreferences.get(propertyName, null);
				if (propertyValue != null) {
					String libName= propertyName.substring(CP_USERLIBRARY_PREFERENCES_PREFIX.length());
					StringReader reader = new StringReader(propertyValue);
					UserLibrary library;
					try {
						library = UserLibrary.createFromString(reader);
					} catch (IOException | ClasspathEntry.AssertionFailedException e) {
						Util.log(e, "Exception while initializing user library " + libName); //$NON-NLS-1$
						instancePreferences.remove(propertyName);
						preferencesNeedFlush = true;
						continue;
					}
					this.userLibraries.put(libName, library);
				}
			}
		}
		if (preferencesNeedFlush) {
			try {
				instancePreferences.flush();
			} catch (BackingStoreException e) {
				Util.log(e, "Exception while flusing instance preferences"); //$NON-NLS-1$
			}
		}
	}

	public void updateUserLibrary(String libName, String encodedUserLibrary) {
		try {
			// find affected projects
			IPath containerPath = new Path(JavaCore.USER_LIBRARY_CONTAINER_ID).append(libName);
			IJavaProject[] allJavaProjects = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
			ArrayList affectedProjects = new ArrayList();
			for (int i= 0; i < allJavaProjects.length; i++) {
				IJavaProject javaProject = allJavaProjects[i];
				IClasspathEntry[] entries= javaProject.getRawClasspath();
				for (int j= 0; j < entries.length; j++) {
					IClasspathEntry entry = entries[j];
					if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
						if (containerPath.equals(entry.getPath())) {
							affectedProjects.add(javaProject);
							break;
						}
					}
				}
			}

			// decode user library
			UserLibrary userLibrary = encodedUserLibrary == null ? null : UserLibrary.createFromString(new StringReader(encodedUserLibrary));

			synchronized (this) {
				// update user libraries map
				if (userLibrary != null) {
					this.userLibraries.put(libName, userLibrary);
				} else {
					this.userLibraries.remove(libName);
				}
			}

			// update affected projects
			int length = affectedProjects.size();
			if (length == 0)
				return;
			IJavaProject[] projects = new IJavaProject[length];
			affectedProjects.toArray(projects);
			IClasspathContainer[] containers = new IClasspathContainer[length];
			if (userLibrary != null) {
				UserLibraryClasspathContainer container = new UserLibraryClasspathContainer(libName);
				for (int i = 0; i < length; i++) {
					containers[i] = container;
				}
			}
			JavaCore.setClasspathContainer(containerPath, projects, containers, null);
		} catch (JavaModelException e) {
			Util.log(e, "Exception while setting user library '"+ libName +"'."); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IOException | ClasspathEntry.AssertionFailedException ase) {
			Util.log(ase, "Exception while decoding user library '"+ libName +"'."); //$NON-NLS-1$ //$NON-NLS-2$
		}

	}

	public void removeUserLibrary(String libName)  {
		synchronized (this.userLibraries) {
			IEclipsePreferences instancePreferences = JavaModelManager.getJavaModelManager().getInstancePreferences();
			String propertyName = CP_USERLIBRARY_PREFERENCES_PREFIX+libName;
			instancePreferences.remove(propertyName);
			try {
				instancePreferences.flush();
			} catch (BackingStoreException e) {
				Util.log(e, "Exception while removing user library " + libName); //$NON-NLS-1$
			}
		}
		// this.userLibraries was updated during the PreferenceChangeEvent (see preferenceChange(...))
	}

	public void setUserLibrary(String libName, IClasspathEntry[] entries, boolean isSystemLibrary)  {
		synchronized (this.userLibraries) {
			IEclipsePreferences instancePreferences = JavaModelManager.getJavaModelManager().getInstancePreferences();
			String propertyName = CP_USERLIBRARY_PREFERENCES_PREFIX+libName;
			try {
				String propertyValue = UserLibrary.serialize(entries, isSystemLibrary);
				instancePreferences.put(propertyName, propertyValue); // sends out a PreferenceChangeEvent (see preferenceChange(...))
			} catch (IOException e) {
				Util.log(e, "Exception while serializing user library " + libName); //$NON-NLS-1$
				return;
			}
			try {
				instancePreferences.flush();
			} catch (BackingStoreException e) {
				Util.log(e, "Exception while saving user library " + libName); //$NON-NLS-1$
			}
		}
		// this.userLibraries was updated during the PreferenceChangeEvent (see preferenceChange(...))
	}

}
