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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Contributions for
 *     						Bug 473178
 *******************************************************************************/
package org.eclipse.jdt.internal.core.dom.rewrite.imports;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Sorts imports according to the order of import groups defined on the Organize Imports preference
 * page. Considers equal any two imports matching the same import group.
 */
final class ImportGroupComparator implements Comparator<ImportName>{
	private static final class ImportGroup {
		private final String name;
		private final int index;
		private final ImportGroup prefix;

		public ImportGroup(String name, int index, ImportGroup prefix) {
			this.name = name;
			this.index = index;
			this.prefix = prefix;
		}

		@Override
		public String toString() {
			return String.format("ImportGroup(%d:%s)", getIndex(), getName()); //$NON-NLS-1$
		}

		String getName() {
			return this.name;
		}

		int getIndex() {
			return this.index;
		}

		ImportGroup getPrefix() {
			return this.prefix;
		}
	}

	private static final class IndexedImportGroups {
		final NavigableMap<String, ImportGroup> typeImportGroupsByName;
		final NavigableMap<String, ImportGroup> staticImportGroupByName;

		IndexedImportGroups(
				NavigableMap<String, ImportGroup> typeImportGroupsByName,
				NavigableMap<String, ImportGroup> staticImportGroupsByName) {
			this.typeImportGroupsByName = typeImportGroupsByName;
			this.staticImportGroupByName = staticImportGroupsByName;
		}
	}

	private static final String MATCH_ALL = ""; //$NON-NLS-1$
	private static final String STATIC_PREFIX = "#"; //$NON-NLS-1$
	private static final String STATIC_MATCH_ALL = STATIC_PREFIX + MATCH_ALL;

	private static List<String> memoizedImportOrder = null;
	private static IndexedImportGroups memoizedIndexedImportGroups = null;

	private static List<String> includeMatchAllImportGroups(List<String> importOrder) {
		boolean needsTypeMatchAll = !importOrder.contains(MATCH_ALL);
		boolean needsStaticMatchAll = !importOrder.contains(STATIC_MATCH_ALL);

		if (!needsTypeMatchAll && !needsStaticMatchAll) {
			return importOrder;
		}

		List<String> augmentedOrder = new ArrayList<>(importOrder.size() + 2);

		if (needsStaticMatchAll) {
			augmentedOrder.add(STATIC_MATCH_ALL);
		}

		augmentedOrder.addAll(importOrder);

		if (needsTypeMatchAll) {
			augmentedOrder.add(MATCH_ALL);
		}

		return augmentedOrder;
	}

	private static synchronized IndexedImportGroups indexImportOrder(List<String> importOrder) {
		if (importOrder.equals(memoizedImportOrder)) {
			return memoizedIndexedImportGroups;
		}

		Map<String, Integer> typeGroupsAndIndices = new HashMap<>();
		Map<String, Integer> staticGroupsAndIndices = new HashMap<>();
		for (int i = 0; i < importOrder.size(); i++) {
			String importGroupString = importOrder.get(i);

			final Map<String, Integer> groupsAndIndices;
			if (importGroupString.startsWith(STATIC_PREFIX)) {
				groupsAndIndices = staticGroupsAndIndices;
				importGroupString = importGroupString.substring(1);
			} else {
				groupsAndIndices = typeGroupsAndIndices;
			}

			groupsAndIndices.put(importGroupString, i);
		}

		memoizedImportOrder = importOrder;

		memoizedIndexedImportGroups = new IndexedImportGroups(
				mapImportGroups(typeGroupsAndIndices),
				mapImportGroups(staticGroupsAndIndices));

		return memoizedIndexedImportGroups;
	}

	private static NavigableMap<String, ImportGroup> mapImportGroups(Map<String, Integer> importGroupNamesAndIndices) {
		if (importGroupNamesAndIndices.isEmpty()) {
			importGroupNamesAndIndices = Collections.singletonMap(MATCH_ALL, 0);
		}

		List<String> sortedNames = new ArrayList<>(importGroupNamesAndIndices.keySet());
		Collections.sort(sortedNames);

		ArrayList<ImportGroup> importGroups = new ArrayList<>(sortedNames.size());

		Deque<ImportGroup> prefixingGroups = new ArrayDeque<>();
		for (String name : sortedNames) {
			while (!prefixingGroups.isEmpty()
					&& !isWholeSegmentPrefix(prefixingGroups.getLast().getName(), name)) {
				prefixingGroups.removeLast();
			}
			ImportGroup prefix = prefixingGroups.peekLast();

			ImportGroup group = new ImportGroup(name, importGroupNamesAndIndices.get(name), prefix);

			importGroups.add(group);

			prefixingGroups.addLast(group);
		}

		NavigableMap<String, ImportGroup> groupsByName = new TreeMap<>();
		for (ImportGroup group : importGroups) {
			groupsByName.put(group.getName(), group);
		}

		return groupsByName;
	}

	private static boolean isWholeSegmentPrefix(String prefix, String name) {
		if (!name.startsWith(prefix)) {
			return false;
		}

		return prefix.isEmpty() || name.length() == prefix.length() || name.charAt(prefix.length()) == '.';
	}

	private final IndexedImportGroups indexedImportGroups;

	ImportGroupComparator(List<String> importOrder) {
		List<String> importOrderWithMatchAllGroups = includeMatchAllImportGroups(importOrder);
		this.indexedImportGroups = indexImportOrder(importOrderWithMatchAllGroups);
	}

	@Override
	public int compare(ImportName o1, ImportName o2) {
		return determineSortPosition(o1) - determineSortPosition(o2);
	}

	private int determineSortPosition(ImportName importName) {
		String name = (importName.isOnDemand() ? importName.containerName : importName.qualifiedName);

		NavigableMap<String, ImportGroup> groupsByName = importName.isStatic
				? this.indexedImportGroups.staticImportGroupByName
						: this.indexedImportGroups.typeImportGroupsByName;

		ImportGroup prefixingGroup = groupsByName.floorEntry(name).getValue();
		while (!isWholeSegmentPrefix(prefixingGroup.getName(), name)) {
			prefixingGroup = prefixingGroup.getPrefix();
		}

		return prefixingGroup.getIndex();
	}
}
