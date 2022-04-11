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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class OnDemandComputer {
	private int typeOnDemandThreshold;
	private int staticOnDemandThreshold;

	OnDemandComputer(int typeOnDemandThreshold, int staticOnDemandThreshold) {
		this.typeOnDemandThreshold = typeOnDemandThreshold;
		this.staticOnDemandThreshold = staticOnDemandThreshold;
	}

	/**
	 * Identifies on-demand reductions (additions of on-demand imports with corresponding removal of
	 * single imports) satisfying the type and static on-demand import thresholds.
	 * <p>
	 * Only the containers imports which have been added or removed as part of the rewrite
	 * will be considered for on-demand reductions.
	 *
	 * @param imports
	 *            the imports in the compilation unit
	 * @param touchedContainers
	 *            the containers of all imports being added or removed as part of this rewrite,
	 *            which should be considered for on-demand reductions (specified as an on-demand
	 *            ImportName for each container)
	 * @param typeExplicitSimpleNames
	 *            simple names of non-static single imports which must be preserved as single
	 *            imports and not reduced into on-demand imports
	 * @param staticExplicitSimpleNames
	 *            simple names of static single imports which must be preserved as single
	 *            imports and not reduced into on-demand imports
	 */
	Collection<OnDemandReduction> identifyPossibleReductions(
			Set<ImportName> imports,
			Set<ImportName> touchedContainers,
			Set<String> typeExplicitSimpleNames,
			Set<String> staticExplicitSimpleNames) {
		Collection<OnDemandReduction> candidates = new ArrayList<OnDemandReduction>();

		Map<ImportName, Collection<ImportName>> importsByContainer = mapByContainer(imports);

		for (Map.Entry<ImportName, Collection<ImportName>> containerAndImports : importsByContainer.entrySet()) {
			ImportName containerOnDemand = containerAndImports.getKey();

			// Imports from an unnamed package should not be reduced (see bug 461863).
			boolean isUnnamedPackage = containerOnDemand.containerName.isEmpty();

			if (touchedContainers.contains(containerOnDemand) && !isUnnamedPackage) {
				Collection<ImportName> containerImports = containerAndImports.getValue();

				Set<String> explicitSimpleNames =
						containerOnDemand.isStatic ? staticExplicitSimpleNames : typeExplicitSimpleNames;

				int onDemandThreshold =
						containerOnDemand.isStatic ? this.staticOnDemandThreshold : this.typeOnDemandThreshold;

				OnDemandReduction candidate = maybeReduce(
						containerOnDemand, containerImports, onDemandThreshold, explicitSimpleNames);
				if (candidate != null) {
					candidates.add(candidate);
				}
			}
		}

		return candidates;
	}

	private Map<ImportName, Collection<ImportName>> mapByContainer(Collection<ImportName> imports) {
		Map<ImportName, Collection<ImportName>> importsByContainer = new HashMap<ImportName, Collection<ImportName>>();
		for (ImportName importName : imports) {
			ImportName containerOnDemand = importName.getContainerOnDemand();

			Collection<ImportName> containerImports = importsByContainer.get(containerOnDemand);
			if (containerImports == null) {
				containerImports = new ArrayList<ImportName>();
				importsByContainer.put(containerOnDemand, containerImports);
			}

			containerImports.add(importName);
		}

		return importsByContainer;
	}

	private OnDemandReduction maybeReduce(
			ImportName containerOnDemand,
			Collection<ImportName> containerImports,
			int onDemandThreshold,
			Set<String> explicitSimpleNames) {
		boolean containerHasOnDemand = false;
		Collection<ImportName> reducibleImports = new ArrayList<ImportName>();

		for (ImportName currentImport : containerImports) {
			if (currentImport.isOnDemand()) {
				containerHasOnDemand = true;
			} else {
				if (!explicitSimpleNames.contains(currentImport.simpleName)) {
					reducibleImports.add(currentImport);
				}
			}
		}

		if (containerHasOnDemand || reducibleImports.size() >= onDemandThreshold) {
			return new OnDemandReduction(containerOnDemand, reducibleImports);
		}

		return null;
	}
}
