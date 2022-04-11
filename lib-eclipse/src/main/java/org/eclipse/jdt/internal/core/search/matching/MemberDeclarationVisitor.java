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
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search.matching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.ast.*;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ProblemMethodBinding;
import org.eclipse.jdt.internal.compiler.util.HashtableOfIntValues;
import org.eclipse.jdt.internal.core.search.matching.MatchLocator.WrappedCoreException;

/**
 * Specific visitor of field or method declaration which can identify and store
 * the local and  other elements of one or several matching nodes.
 * <p>
 * This visitor can also peek up local or anonymous type declaration and restart
 * a new {@link MatchLocator} traverse on this type.
 * </p>
 */
class MemberDeclarationVisitor extends ASTVisitor {
	// Matches information
	private final MatchLocator locator;
	private final IJavaElement enclosingElement;
	private final MatchingNodeSet nodeSet;
	private final ASTNode[] matchingNodes;
	private final ASTNode matchingNode;

	// Local type storage
	HashtableOfIntValues occurrencesCounts;
	HashtableOfIntValues inTypeOccurrencesCounts;
	int nodesCount = 0;

	// Local and other elements storage
	private Annotation annotation;
	private LocalDeclaration localDeclaration;
	IJavaElement localElement;
	IJavaElement[] localElements, otherElements;
	IJavaElement[][] allOtherElements;
	int ptr = -1;
	int[] ptrs;
	private boolean typeInHierarchy;

public MemberDeclarationVisitor(IJavaElement element, ASTNode[] nodes, MatchingNodeSet set, MatchLocator locator, boolean typeInHierarchy) {
	this.enclosingElement = element;
	this.typeInHierarchy = typeInHierarchy;
	this.nodeSet = set;
	this.locator = locator;
	this.occurrencesCounts = new HashtableOfIntValues();
	this.inTypeOccurrencesCounts = this.locator.inTypeOccurrencesCounts;
	if (nodes == null) {
		this.matchingNode = null;
		this.matchingNodes = null;
	} else {
		this.nodesCount = nodes.length;
		if (nodes.length == 1) {
			this.matchingNode = nodes[0];
			this.matchingNodes = null;
		} else {
			this.matchingNode = null;
			this.matchingNodes = nodes;
			this.localElements = new IJavaElement[this.nodesCount];
			this.ptrs = new int[this.nodesCount];
			this.allOtherElements = new IJavaElement[this.nodesCount][];
		}
	}
}
@Override
public void endVisit(Argument argument, BlockScope scope) {
    this.localDeclaration = null;
}
@Override
public void endVisit(LocalDeclaration declaration, BlockScope scope) {
    this.localDeclaration = null;
}
@Override
public void endVisit(MarkerAnnotation markerAnnotation, BlockScope unused) {
	this.annotation = null;
}
@Override
public void endVisit(NormalAnnotation normalAnnotation, BlockScope unused) {
	this.annotation = null;
}
@Override
public void endVisit(SingleMemberAnnotation singleMemberAnnotation, BlockScope unused) {
	this.annotation = null;
}
IJavaElement getLocalElement(int idx) {
	if (this.nodesCount == 1) {
		return this.localElement;
	}
	if (this.localElements != null) {
		return this.localElements[idx];
	}
	return null;
}
IJavaElement[] getOtherElements(int idx) {
	if (this.nodesCount == 1) {
		if (this.otherElements != null) {
			int length = this.otherElements.length;
			if (this.ptr < (length-1)) {
				System.arraycopy(this.otherElements, 0, this.otherElements = new IJavaElement[this.ptr+1], 0, this.ptr+1);
			}
		}
		return this.otherElements;
	}
	IJavaElement[] elements = this.allOtherElements == null ? null : this.allOtherElements[idx];
	if (elements != null) {
		int length = elements.length;
		if (this.ptrs[idx] < (length-1)) {
			System.arraycopy(elements, 0, elements = this.allOtherElements[idx] = new IJavaElement[this.ptrs[idx]+1], 0, this.ptrs[idx]+1);
		}
	}
	return elements;
}
private int matchNode(ASTNode reference) {
	if (this.matchingNode != null) {
		if (this.matchingNode == reference) return 0;
	} else {
	    int length = this.matchingNodes.length;
		for (int i=0; i<length; i++) {
			if (this.matchingNodes[i] == reference)  { // == is intentional
				return i;
			}
		}
	}
	return -1;
}
/*
 * Store the handle for the reference of the given index (e.g. peek in #matchingNodes
 * or #matchingNode).
 * Note that for performance reason, matching node and associated handles are
 * not stored in array when there's only one reference to identify.
 */
private void storeHandle(int idx) {
	if (this.localDeclaration == null) return;
	IJavaElement handle = this.locator.createHandle(this.localDeclaration, this.enclosingElement);
    if (this.nodesCount == 1) {
    	if (this.localElement == null) {
    		if (this.annotation == null) {
		    	this.localElement =  handle;
    		} else {
		    	IJavaElement annotHandle = this.locator.createHandle(this.annotation, (IAnnotatable) handle);
		    	if (annotHandle == null) {
			    	annotHandle = this.locator.createHandle(this.annotation, (IAnnotatable) this.enclosingElement);
		    	}
		    	this.localElement = annotHandle == null ? handle : annotHandle;
    		}
    	} else {
	    	if (++this.ptr == 0) {
	    		this.otherElements = new IJavaElement[10];
	    	} else {
	            int length = this.otherElements.length;
	            if (this.ptr == length) {
	            	System.arraycopy(this.otherElements, 0, this.otherElements = new IJavaElement[length+10], 0, length);
	            }
            }
    		if (this.annotation == null) {
		    	this.otherElements[this.ptr] = handle;
    		} else {
		    	IJavaElement annotHandle = this.locator.createHandle(this.annotation, (IAnnotatable) handle);
		    	if (annotHandle == null) {
			    	annotHandle = this.locator.createHandle(this.annotation, (IAnnotatable) this.enclosingElement);
		    	}
		    	this.otherElements[this.ptr] = annotHandle == null ? handle : annotHandle;
    		}
    	}
    } else {
    	if (this.localElements[idx] == null) {
	    	if (this.annotation == null) {
		    	this.localElements[idx] =  handle;
    		} else {
		    	IJavaElement annotHandle = this.locator.createHandle(this.annotation, (IAnnotatable) handle);
		    	if (annotHandle == null) {
			    	annotHandle = this.locator.createHandle(this.annotation, (IAnnotatable) this.enclosingElement);
		    	}
		    	this.localElements[idx] = annotHandle == null ? handle : annotHandle;
    		}
			this.ptrs[idx] = -1;
	    } else {
	        int oPtr = ++this.ptrs[idx];
	    	if (oPtr== 0) {
    			this.allOtherElements[idx] = new IJavaElement[10];
    		} else {
            	int length = this.allOtherElements[idx].length;
	            if (oPtr == length) {
	            	System.arraycopy(this.allOtherElements[idx], 0, this.allOtherElements[idx] = new IJavaElement[length+10], 0, length);
        	    }
	        }
	    	if (this.annotation == null) {
	 		   	this.allOtherElements[idx][oPtr] = handle;
    		} else {
		    	IJavaElement annotHandle = this.locator.createHandle(this.annotation, (IAnnotatable) handle);
		    	if (annotHandle == null) {
			    	annotHandle = this.locator.createHandle(this.annotation, (IAnnotatable) this.enclosingElement);
		    	}
	 		   	this.allOtherElements[idx][oPtr] = annotHandle == null ? handle : annotHandle;
    		}
    	}
    }
}
@Override
public boolean visit(Argument argument, BlockScope scope) {
    this.localDeclaration = argument;
    return true;
}
@Override
public boolean visit(LambdaExpression lambdaExpression, BlockScope scope) {
	Integer level = (Integer) this.nodeSet.matchingNodes.removeKey(lambdaExpression);
	try {
		if (lambdaExpression.resolvedType != null && lambdaExpression.resolvedType.isValidBinding() &&
				!(lambdaExpression.descriptor instanceof ProblemMethodBinding))
			this.locator.reportMatching(lambdaExpression, this.enclosingElement, level != null ? level.intValue() : -1, this.nodeSet, this.typeInHierarchy);
		else
			return true;
	} catch (CoreException e) {
		throw new WrappedCoreException(e);
	}
	return false; // Don't visit the children as they get traversed under control of reportMatching.
}
@Override
public boolean visit(LocalDeclaration declaration, BlockScope scope) {
    this.localDeclaration = declaration;
    return true;
}
@Override
public boolean visit(MarkerAnnotation markerAnnotation, BlockScope unused) {
	this.annotation = markerAnnotation;
	return true;
}
@Override
public boolean visit(NormalAnnotation normalAnnotation, BlockScope unused) {
	this.annotation = normalAnnotation;
	return true;
}
@Override
public boolean visit(QualifiedNameReference nameReference, BlockScope unused) {
	if (this.nodesCount > 0){
		int idx = matchNode(nameReference);
		if (idx >= 0) {
			storeHandle(idx);
		}
	}
	return false;
}
@Override
public boolean visit(QualifiedTypeReference typeReference, BlockScope unused) {
	if (this.nodesCount > 0){
		int idx = matchNode(typeReference);
		if (idx >= 0) {
			storeHandle(idx);
		}
	}
	return false;
}
@Override
public boolean visit(SingleMemberAnnotation singleMemberAnnotation, BlockScope unused) {
	this.annotation = singleMemberAnnotation;
	return true;
}
@Override
public boolean visit(SingleNameReference nameReference, BlockScope unused) {
	if (this.nodesCount > 0){
		int idx = matchNode(nameReference);
		if (idx >= 0) {
			storeHandle(idx);
		}
	}
	return false;
}
@Override
public boolean visit(SingleTypeReference typeReference, BlockScope unused) {
	if (this.nodesCount > 0){
		int idx = matchNode(typeReference);
		if (idx >= 0) {
			storeHandle(idx);
		}
	}
	return false;
}

int getInTypeOccurrenceCountForBinaryAnonymousType(TypeDeclaration typeDeclaration, char[] name,
		IJavaElement parent, int occurrenceCount) {
	int ret = occurrenceCount;
	if ((typeDeclaration.bits & ASTNode.IsAnonymousType) != 0 && this.enclosingElement instanceof IMember) {
	    IMember member = (IMember) parent;
		if (member.isBinary())  {
			int tmp = this.inTypeOccurrencesCounts.get(name);
			ret =  (tmp == HashtableOfIntValues.NO_VALUE) ? 1 : tmp + 1;
			this.inTypeOccurrencesCounts.put(name, ret);
		}
	}
	return ret;
}
@Override
public boolean visit(TypeDeclaration typeDeclaration, BlockScope unused) {
	try {
		char[] simpleName;
		if ((typeDeclaration.bits & ASTNode.IsAnonymousType) != 0) {
			simpleName = CharOperation.NO_CHAR;
		} else {
			simpleName = typeDeclaration.name;
		}
		int occurrenceCount = this.occurrencesCounts.get(simpleName);
		if (occurrenceCount == HashtableOfIntValues.NO_VALUE) {
			occurrenceCount = 1;
		} else {
			occurrenceCount = occurrenceCount + 1;
		}
		occurrenceCount = getInTypeOccurrenceCountForBinaryAnonymousType(typeDeclaration, simpleName, this.enclosingElement, occurrenceCount);
		this.occurrencesCounts.put(simpleName, occurrenceCount);
		HashtableOfIntValues oldOccurencesCount = this.inTypeOccurrencesCounts;
		this.locator.inTypeOccurrencesCounts = new HashtableOfIntValues();
		if ((typeDeclaration.bits & ASTNode.IsAnonymousType) != 0) {
			this.locator.reportMatching(typeDeclaration, this.enclosingElement, -1, this.nodeSet, occurrenceCount);
		} else {
			Integer level = (Integer) this.nodeSet.matchingNodes.removeKey(typeDeclaration);
			this.locator.reportMatching(typeDeclaration, this.enclosingElement, level != null ? level.intValue() : -1, this.nodeSet, occurrenceCount);
		}
		this.locator.inTypeOccurrencesCounts = oldOccurencesCount;
		return false; // don't visit members as this was done during reportMatching(...)
	} catch (CoreException e) {
		throw new WrappedCoreException(e);
	}
}
}
