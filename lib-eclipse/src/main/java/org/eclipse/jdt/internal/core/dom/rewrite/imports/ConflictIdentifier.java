/*******************************************************************************
 * Copyright (c) 2014, 2015 Google Inc and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Contributions for
 *     						Bug 473178
 *******************************************************************************/
package org.eclipse.jdt.internal.core.dom.rewrite.imports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;

public class ConflictIdentifier {
	/**
	 * Encapsulates those simple names (of type imports and of static imports) which would be
	 * imported from multiple on-demand or implicit import containers.
	 */
	static final class Conflicts {
		final Set<String> typeConflicts;
		final Set<String> staticConflicts;

		Conflicts(Set<String> typeConflicts, Set<String> staticConflicts) {
			this.typeConflicts = Collections.unmodifiableSet(new HashSet<>(typeConflicts));
			this.staticConflicts = Collections.unmodifiableSet(new HashSet<>(staticConflicts));
		}

		@Override
		public String toString() {
			return String.format(
					"Conflicts(type: %s; static: %s)", this.typeConflicts, this.staticConflicts); //$NON-NLS-1$
		}
	}

	private final OnDemandComputer onDemandComputer;
	private final TypeConflictingSimpleNameFinder typeConflictFinder;
	private final StaticConflictingSimpleNameFinder staticConflictFinder;
	private final Set<String> implicitImportContainers;

	ConflictIdentifier(
			OnDemandComputer onDemandComputer,
			TypeConflictingSimpleNameFinder typeConflictFinder,
			StaticConflictingSimpleNameFinder staticConflictFinder,
			Set<String> implicitImportContainers) {
		this.onDemandComputer = onDemandComputer;
		this.typeConflictFinder = typeConflictFinder;
		this.staticConflictFinder = staticConflictFinder;
		this.implicitImportContainers = implicitImportContainers;
	}

	/**
	 * Identifies the simple names (of the elements of {@code imports}) which would be imported from
	 * multiple on-demand or implicit import containers.
	 *
	 * @param imports
	 *            imports whose simple names are to be considered for conflicts
	 * @param addedImports
	 *            imports which have been added as part of the rewrite (and could therefore trigger
	 *            on-demand reductions; a subset of {@code imports}
	 * @param typeExplicitSimpleNames
	 *            simple names of types which are already known to require explicit imports
	 * @param staticExplicitSimpleNames
	 *            simple names of statics which are already known to require explicit imports
	 * @param progressMonitor
	 *            a progress monitor used to track time spent searching for conflicts
	 * @return a {@link Conflicts} object encapsulating the found conflicting type and static names
	 * @throws JavaModelException if an error occurs while searching for declarations
	 */
	Conflicts identifyConflicts(
			Set<ImportName> imports,
			Set<ImportName> addedImports,
			Set<String> typeExplicitSimpleNames,
			Set<String> staticExplicitSimpleNames,
			IProgressMonitor progressMonitor) throws JavaModelException {
		Collection<OnDemandReduction> onDemandCandidates = this.onDemandComputer.identifyPossibleReductions(
				imports, addedImports, typeExplicitSimpleNames, staticExplicitSimpleNames);

		Set<String> typeOnDemandContainers = new HashSet<>(extractContainerNames(onDemandCandidates, false));
		Set<String> staticOnDemandContainers = new HashSet<>(extractContainerNames(onDemandCandidates, true));

		if (!typeOnDemandContainers.isEmpty()) {
			// Existing on-demands might conflict with new or existing on-demands.
			typeOnDemandContainers.addAll(extractOnDemandContainerNames(imports, false));

			// Implicitly imported types might conflict with type on-demands.
			typeOnDemandContainers.addAll(this.implicitImportContainers);

			// Member types imported by static on-demands might conflict with type on-demands.
			typeOnDemandContainers.addAll(staticOnDemandContainers);
		}

		if (!staticOnDemandContainers.isEmpty()) {
			// Existing on-demands might conflict with new or existing on-demands.
			staticOnDemandContainers.addAll(extractOnDemandContainerNames(imports, true));
		}

		Set<String> typeConflicts = findConflictingSimpleNames(
				this.typeConflictFinder, imports, false, typeOnDemandContainers, progressMonitor);

		Set<String> staticConflicts = findConflictingSimpleNames(
				this.staticConflictFinder, imports, true, staticOnDemandContainers, progressMonitor);

		return new Conflicts(typeConflicts, staticConflicts);
	}

	private Collection<String> extractContainerNames(
			Collection<OnDemandReduction> onDemandCandidates, boolean isStatic) {
		Collection<String> containerNames = new ArrayList<>(onDemandCandidates.size());
		for (OnDemandReduction onDemandCandidate : onDemandCandidates) {
			ImportName containerOnDemand = onDemandCandidate.containerOnDemand;
			if (containerOnDemand.isStatic == isStatic) {
				containerNames.add(containerOnDemand.containerName);
			}
		}

		return containerNames;
	}

	private Collection<String> extractOnDemandContainerNames(
			Collection<ImportName> imports, boolean isStatic) {
		Collection<String> onDemandContainerNames = new ArrayList<>(imports.size());
		for (ImportName importName : imports) {
			if (importName.isOnDemand() && importName.isStatic == isStatic) {
				onDemandContainerNames.add(importName.containerName);
			}
		}

		return onDemandContainerNames;
	}

	private Set<String> findConflictingSimpleNames(
			ConflictingSimpleNameFinder conflictFinder,
			Set<ImportName> imports,
			boolean isStatic,
			Set<String> onDemandImportedContainers,
			IProgressMonitor monitor) throws JavaModelException {
		if (onDemandImportedContainers.isEmpty() || imports.isEmpty()) {
			return Collections.emptySet();
		}

		Set<String> simpleNames = new HashSet<>();
		for (ImportName currentImport : imports) {
			if (currentImport.isStatic == isStatic) {
				simpleNames.add(currentImport.simpleName);
			}
		}

		return conflictFinder.findConflictingSimpleNames(simpleNames, onDemandImportedContainers, monitor);
	}
}
