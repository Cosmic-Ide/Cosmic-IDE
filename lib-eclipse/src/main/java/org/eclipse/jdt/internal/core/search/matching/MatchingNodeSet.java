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
package org.eclipse.jdt.internal.core.search.matching;

import java.util.ArrayList;

import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.util.HashtableOfLong;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * A set of matches and possible matches, which need to be resolved.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MatchingNodeSet {

/**
 * Map of matching ast nodes that don't need to be resolved to their accuracy level.
 * Each node is removed as it is reported.
 */
SimpleLookupTable matchingNodes = new SimpleLookupTable(3); // node -> accuracy
private HashtableOfLong matchingNodesKeys = new HashtableOfLong(3); // sourceRange -> node
static Integer EXACT_MATCH = Integer.valueOf(SearchMatch.A_ACCURATE);
static Integer POTENTIAL_MATCH = Integer.valueOf(SearchMatch.A_INACCURATE);
static Integer ERASURE_MATCH = Integer.valueOf(SearchPattern.R_ERASURE_MATCH);

/**
 * Tell whether locators need to resolve or not for current matching node set.
 */
public boolean mustResolve;

/**
 * Set of possible matching ast nodes. They need to be resolved
 * to determine if they really match the search pattern.
 */
SimpleSet possibleMatchingNodesSet = new SimpleSet(7);
private HashtableOfLong possibleMatchingNodesKeys = new HashtableOfLong(7);


public MatchingNodeSet(boolean mustResolvePattern) {
	super();
	this.mustResolve = mustResolvePattern;
}

public int addMatch(ASTNode node, int matchLevel) {
	int maskedLevel = matchLevel & PatternLocator.MATCH_LEVEL_MASK;
	switch (maskedLevel) {
		case PatternLocator.INACCURATE_MATCH:
			if (matchLevel != maskedLevel) {
				addTrustedMatch(node, Integer.valueOf(SearchMatch.A_INACCURATE+(matchLevel & PatternLocator.FLAVORS_MASK)));
			} else {
				addTrustedMatch(node, POTENTIAL_MATCH);
			}
			break;
		case PatternLocator.POSSIBLE_MATCH:
			addPossibleMatch(node);
			break;
		case PatternLocator.ERASURE_MATCH:
			if (matchLevel != maskedLevel) {
				addTrustedMatch(node, Integer.valueOf(SearchPattern.R_ERASURE_MATCH+(matchLevel & PatternLocator.FLAVORS_MASK)));
			} else {
				addTrustedMatch(node, ERASURE_MATCH);
			}
			break;
		case PatternLocator.ACCURATE_MATCH:
			if (matchLevel != maskedLevel) {
				addTrustedMatch(node, Integer.valueOf(SearchMatch.A_ACCURATE+(matchLevel & PatternLocator.FLAVORS_MASK)));
			} else {
				addTrustedMatch(node, EXACT_MATCH);
			}
			break;
	}
	return matchLevel;
}
public void addPossibleMatch(ASTNode node) {
	// remove existing node at same position from set
	// (case of recovery that created the same node several time
	// see http://bugs.eclipse.org/bugs/show_bug.cgi?id=29366)
	long key = (((long) node.sourceStart) << 32) + node.sourceEnd;
	ASTNode existing = (ASTNode) this.possibleMatchingNodesKeys.get(key);
	if (existing != null && existing.getClass().equals(node.getClass()))
		this.possibleMatchingNodesSet.remove(existing);

	// add node to set
	this.possibleMatchingNodesSet.add(node);
	this.possibleMatchingNodesKeys.put(key, node);
}
public void addTrustedMatch(ASTNode node, boolean isExact) {
	addTrustedMatch(node, isExact ? EXACT_MATCH : POTENTIAL_MATCH);

}
void addTrustedMatch(ASTNode node, Integer level) {
	// remove existing node at same position from set
	// (case of recovery that created the same node several time
	// see http://bugs.eclipse.org/bugs/show_bug.cgi?id=29366)
	long key = (((long) node.sourceStart) << 32) + node.sourceEnd;
	ASTNode existing = (ASTNode) this.matchingNodesKeys.get(key);
	if (existing != null && existing.getClass().equals(node.getClass()))
		this.matchingNodes.removeKey(existing);

	// map node to its accuracy level
	this.matchingNodes.put(node, level);
	this.matchingNodesKeys.put(key, node);
}
protected boolean hasPossibleNodes(int start, int end) {
	Object[] nodes = this.possibleMatchingNodesSet.values;
	for (int i = 0, l = nodes.length; i < l; i++) {
		ASTNode node = (ASTNode) nodes[i];
		if (node != null && start <= node.sourceStart && node.sourceEnd <= end)
			return true;
	}
	nodes = this.matchingNodes.keyTable;
	for (int i = 0, l = nodes.length; i < l; i++) {
		ASTNode node = (ASTNode) nodes[i];
		if (node != null && start <= node.sourceStart && node.sourceEnd <= end)
			return true;
	}
	return false;
}
/**
 * Returns the matching nodes that are in the given range in the source order.
 */
protected ASTNode[] matchingNodes(int start, int end) {
	ArrayList nodes = null;
	Object[] keyTable = this.matchingNodes.keyTable;
	for (int i = 0, l = keyTable.length; i < l; i++) {
		ASTNode node = (ASTNode) keyTable[i];
		if (node != null && start <= node.sourceStart && node.sourceEnd <= end) {
			if (nodes == null) nodes = new ArrayList();
			nodes.add(node);
		}
	}
	if (nodes == null) return null;

	ASTNode[] result = new ASTNode[nodes.size()];
	nodes.toArray(result);

	// sort nodes by source starts
	Util.Comparer comparer = new Util.Comparer() {
		@Override
		public int compare(Object o1, Object o2) {
			return ((ASTNode) o1).sourceStart - ((ASTNode) o2).sourceStart;
		}
	};
	Util.sort(result, comparer);
	return result;
}
public Object removePossibleMatch(ASTNode node) {
	long key = (((long) node.sourceStart) << 32) + node.sourceEnd;
	ASTNode existing = (ASTNode) this.possibleMatchingNodesKeys.get(key);
	if (existing == null) return null;

	this.possibleMatchingNodesKeys.put(key, null);
	return this.possibleMatchingNodesSet.remove(node);
}
public Object removeTrustedMatch(ASTNode node) {
	long key = (((long) node.sourceStart) << 32) + node.sourceEnd;
	ASTNode existing = (ASTNode) this.matchingNodesKeys.get(key);
	if (existing == null) return null;

	this.matchingNodesKeys.put(key, null);
	return this.matchingNodes.removeKey(node);
}
@Override
public String toString() {
	// TODO (jerome) should show both tables
	StringBuffer result = new StringBuffer();
	result.append("Exact matches:"); //$NON-NLS-1$
	Object[] keyTable = this.matchingNodes.keyTable;
	Object[] valueTable = this.matchingNodes.valueTable;
	for (int i = 0, l = keyTable.length; i < l; i++) {
		ASTNode node = (ASTNode) keyTable[i];
		if (node == null) continue;
		result.append("\n\t"); //$NON-NLS-1$
		switch (((Integer)valueTable[i]).intValue()) {
			case SearchMatch.A_ACCURATE:
				result.append("ACCURATE_MATCH: "); //$NON-NLS-1$
				break;
			case SearchMatch.A_INACCURATE:
				result.append("INACCURATE_MATCH: "); //$NON-NLS-1$
				break;
			case SearchPattern.R_ERASURE_MATCH:
				result.append("ERASURE_MATCH: "); //$NON-NLS-1$
				break;
		}
		node.print(0, result);
	}

	result.append("\nPossible matches:"); //$NON-NLS-1$
	Object[] nodes = this.possibleMatchingNodesSet.values;
	for (int i = 0, l = nodes.length; i < l; i++) {
		ASTNode node = (ASTNode) nodes[i];
		if (node == null) continue;
		result.append("\nPOSSIBLE_MATCH: "); //$NON-NLS-1$
		node.print(0, result);
	}
	return result.toString();
}
}
