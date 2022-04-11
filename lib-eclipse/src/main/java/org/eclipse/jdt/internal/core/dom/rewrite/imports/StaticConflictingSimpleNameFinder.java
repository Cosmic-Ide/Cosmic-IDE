/*******************************************************************************
 * Copyright (c) 2015 Google Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     John Glassmyer <jogl@google.com> - import group sorting is broken - https://bugs.eclipse.org/430303
 *******************************************************************************/
package org.eclipse.jdt.internal.core.dom.rewrite.imports;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import java.util.HashSet;
import java.util.Set;

/**
 * Finds conflicts among importable static members declared within the specified on-demand-imported
 * containers.
 */
final class StaticConflictingSimpleNameFinder implements ConflictingSimpleNameFinder {
	private static boolean isStaticImportableMember(int memberFlags) {
		return (Flags.isStatic(memberFlags) || Flags.isEnum(memberFlags)) && !Flags.isPrivate(memberFlags);
	}

	private final IJavaProject project;

	StaticConflictingSimpleNameFinder(IJavaProject project) {
		this.project = project;
	}

	@Override
	public Set<String> findConflictingSimpleNames(
			Set<String> simpleNames,
			Set<String> onDemandAndImplicitContainerNames,
			IProgressMonitor monitor) throws JavaModelException {
		Set<String> memberNamesFoundInMultipleTypes = new HashSet<String>();

		Set<String> foundMemberNames = new HashSet<String>();
		for (String containerName : onDemandAndImplicitContainerNames) {
			IType containingType = this.project.findType(containerName, monitor);
			if (containingType != null) {
				if (!containingType.exists()) { // workaround for https://bugs.eclipse.org/483887
					continue;
				}
				for (String memberName : extractStaticMemberNames(containingType)) {
					if (simpleNames.contains(memberName)) {
						if (foundMemberNames.contains(memberName)) {
							memberNamesFoundInMultipleTypes.add(memberName);
						} else {
							foundMemberNames.add(memberName);
						}
					}
				}
			}
		}

		return memberNamesFoundInMultipleTypes;
	}

	private Set<String> extractStaticMemberNames(IType type) throws JavaModelException {
		Set<String> memberNames = new HashSet<String>();

		for (IField field : type.getFields()) {
			if (isStaticImportableMember(field.getFlags())) {
				memberNames.add(field.getElementName());
			}
		}

		for (IMethod method : type.getMethods()) {
			if (isStaticImportableMember(method.getFlags())) {
				memberNames.add(method.getElementName());
			}
		}

		return memberNames;
	}
}