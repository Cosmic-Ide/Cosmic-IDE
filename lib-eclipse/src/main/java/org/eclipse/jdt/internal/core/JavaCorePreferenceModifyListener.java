/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.PreferenceModifyListener;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class JavaCorePreferenceModifyListener extends PreferenceModifyListener {

	static int PREFIX_LENGTH = JavaModelManager.CP_CONTAINER_PREFERENCES_PREFIX.length();
	JavaModel javaModel = JavaModelManager.getJavaModelManager().getJavaModel();

	@Override
	public IEclipsePreferences preApply(IEclipsePreferences node) {
		// the node does not need to be the root of the hierarchy
		Preferences root = node.node("/"); //$NON-NLS-1$
		try {
			// we must not create empty preference nodes, so first check if the node exists
			if (root.nodeExists(InstanceScope.SCOPE)) {
				Preferences instance = root.node(InstanceScope.SCOPE);
				// we must not create empty preference nodes, so first check if the node exists
				if (instance.nodeExists(JavaCore.PLUGIN_ID)) {
					cleanJavaCore(instance.node(JavaCore.PLUGIN_ID));
				}
			}
		} catch (BackingStoreException e) {
			// do nothing
		}
		return super.preApply(node);
	}

	/**
	 * Clean imported preferences from obsolete keys.
	 *
	 * @param preferences JavaCore preferences.
	 */
	void cleanJavaCore(Preferences preferences) {
		try {
			String[] keys = preferences.keys();
			for (int k = 0, kl= keys.length; k<kl; k++) {
				String key = keys[k];
				if (key.startsWith(JavaModelManager.CP_CONTAINER_PREFERENCES_PREFIX) && !isJavaProjectAccessible(key)) {
					preferences.remove(key);
				}
			}
		} catch (BackingStoreException e) {
			// do nothing
		}
	}

	/**
	 * Returns whether a java project referenced in property key
	 * is still longer accessible or not.
	 *
	 * @param propertyName
	 * @return true if a project is referenced in given key and this project
	 * 	is still accessible, false otherwise.
	 */
	boolean isJavaProjectAccessible(String propertyName) {
		int index = propertyName.indexOf('|', PREFIX_LENGTH);
		if (index > 0) {
			final String projectName = propertyName.substring(PREFIX_LENGTH, index).trim();
			JavaProject project = this.javaModel.getJavaProject(projectName);
			if (project.getProject().isAccessible()) {
				return true;
			}
		}
		return false;
	}

}
