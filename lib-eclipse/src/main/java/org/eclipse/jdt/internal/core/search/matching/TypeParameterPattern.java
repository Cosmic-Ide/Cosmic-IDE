/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Microsoft Corporation - adapt to the new index match API
 *******************************************************************************/
package org.eclipse.jdt.internal.core.search.matching;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.index.Index;
import org.eclipse.jdt.internal.core.search.IndexQueryRequestor;
import org.eclipse.jdt.internal.core.search.JavaSearchScope;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Pattern to search type parameters.
 *
 * @since 3.1
 */
public class TypeParameterPattern extends JavaSearchPattern {

	protected boolean findDeclarations;
	protected boolean findReferences;
	protected char[] name;
	protected ITypeParameter typeParameter;
	protected char[] declaringMemberName;
	protected char[] methodDeclaringClassName;
	protected char[][] methodArgumentTypes;

	/**
	 * @param findDeclarations
	 * @param findReferences
	 * @param typeParameter
	 * @param matchRule
	 */
	public TypeParameterPattern(boolean findDeclarations, boolean findReferences, ITypeParameter typeParameter, int matchRule) {
		super(TYPE_PARAM_PATTERN, matchRule);

		this.findDeclarations = findDeclarations; // set to find declarations & all occurences
		this.findReferences = findReferences; // set to find references & all occurences
		this.typeParameter = typeParameter;
		this.name = typeParameter.getElementName().toCharArray(); // store type parameter name
		IMember member = typeParameter.getDeclaringMember();
		this.declaringMemberName = member.getElementName().toCharArray(); // store type parameter declaring member name

		// For method type parameter, store also declaring class name and parameters type names
		if (member instanceof IMethod) {
			IMethod method = (IMethod) member;
			this.methodDeclaringClassName = method.getParent().getElementName().toCharArray();
			String[] parameters = method.getParameterTypes();
			int length = parameters.length;
			this.methodArgumentTypes = new char[length][];
			for (int i=0; i<length; i++) {
				this.methodArgumentTypes[i] = Signature.toCharArray(parameters[i].toCharArray());
			}
		}
	}

	/*
	 * Same than LocalVariablePattern.
	 */
	@Override
	public void findIndexMatches(Index index, IndexQueryRequestor requestor, SearchParticipant participant, IJavaSearchScope scope, IProgressMonitor progressMonitor) {
	    IPackageFragmentRoot root = (IPackageFragmentRoot) this.typeParameter.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		String documentPath;
		String relativePath;
	    if (root.isArchive()) {
 	    	IType type = (IType) this.typeParameter.getAncestor(IJavaElement.TYPE);
    	    relativePath = (type.getFullyQualifiedName('$')).replace('.', '/') + SuffixConstants.SUFFIX_STRING_class;
    	    IModuleDescription md = root.getModuleDescription();
            if(md != null) {
            	String module = md.getElementName();
            	documentPath = root.getPath() + IJavaSearchScope.JAR_FILE_ENTRY_SEPARATOR
            			+ module + IJavaSearchScope.JAR_FILE_ENTRY_SEPARATOR + relativePath;
            } else {
            	documentPath = root.getPath() + IJavaSearchScope.JAR_FILE_ENTRY_SEPARATOR + relativePath;
            }
	    } else {
			IPath path = this.typeParameter.getPath();
	        documentPath = path.toString();
			relativePath = Util.relativePath(path, 1/*remove project segment*/);
	    }

		if (scope instanceof JavaSearchScope) {
			JavaSearchScope javaSearchScope = (JavaSearchScope) scope;
			// Get document path access restriction from java search scope
			// Note that requestor has to verify if needed whether the document violates the access restriction or not
			AccessRuleSet access = javaSearchScope.getAccessRuleSet(relativePath, index.containerPath);
			if (access != JavaSearchScope.NOT_ENCLOSED) { // scope encloses the path
				if (!requestor.acceptIndexMatch(documentPath, this, participant, access))
					throw new OperationCanceledException();
			}
		} else if (scope.encloses(documentPath)) {
			if (!requestor.acceptIndexMatch(documentPath, this, participant, null))
				throw new OperationCanceledException();
		}
	}

	@Override
	public void findIndexMatches(Index index, IndexQueryRequestor requestor, SearchParticipant participant, IJavaSearchScope scope, boolean resolveDocumentName, IProgressMonitor progressMonitor) {
		findIndexMatches(index, requestor, participant, scope, progressMonitor);
	}

	@Override
	protected StringBuffer print(StringBuffer output) {
		if (this.findDeclarations) {
			output.append(this.findReferences
				? "TypeParamCombinedPattern: " //$NON-NLS-1$
				: "TypeParamDeclarationPattern: "); //$NON-NLS-1$
		} else {
			output.append("TypeParamReferencePattern: "); //$NON-NLS-1$
		}
		output.append(this.typeParameter.toString());
		return super.print(output);
	}

}
