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
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Common functionality for Binary member handles.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class BinaryMember extends NamedMember {

/*
 * Constructs a binary member.
 */
protected BinaryMember(JavaElement parent, String name) {
	super(parent, name);
}
/*
 * @see ISourceManipulation
 */
@Override
public void copy(IJavaElement container, IJavaElement sibling, String rename, boolean force, IProgressMonitor monitor) throws JavaModelException {
	throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.READ_ONLY, this));
}
protected IAnnotation[] getAnnotations(IBinaryAnnotation[] binaryAnnotations, long tagBits) {
	IAnnotation[] standardAnnotations = getStandardAnnotations(tagBits);
	if (binaryAnnotations == null)
		return standardAnnotations;
	int length = binaryAnnotations.length;
	int standardLength = standardAnnotations.length;
	int fullLength = length + standardLength;
	if (fullLength == 0) {
		return Annotation.NO_ANNOTATIONS;
	}
	IAnnotation[] annotations = new IAnnotation[fullLength];
	for (int i = 0; i < length; i++) {
		annotations[i] = Util.getAnnotation(this, binaryAnnotations[i], null);
	}
	System.arraycopy(standardAnnotations, 0, annotations, length, standardLength);
	return annotations;
}
private IAnnotation getAnnotation(char[][] annotationName) {
	return new Annotation(this, new String(CharOperation.concatWith(annotationName, '.')));
}
protected IAnnotation[] getStandardAnnotations(long tagBits) {
	if ((tagBits & TagBits.AllStandardAnnotationsMask) == 0)
		return Annotation.NO_ANNOTATIONS;
	ArrayList annotations = new ArrayList();

	if ((tagBits & TagBits.AnnotationRetentionMASK) != 0) {
		annotations.add(getAnnotation(TypeConstants.JAVA_LANG_ANNOTATION_RETENTION));
	}
	if ((tagBits & TagBits.AnnotationDeprecated) != 0) {
		annotations.add(getAnnotation(TypeConstants.JAVA_LANG_DEPRECATED));
	}
	if ((tagBits & TagBits.AnnotationDocumented) != 0) {
		annotations.add(getAnnotation(TypeConstants.JAVA_LANG_ANNOTATION_DOCUMENTED));
	}
	if ((tagBits & TagBits.AnnotationInherited) != 0) {
		annotations.add(getAnnotation(TypeConstants.JAVA_LANG_ANNOTATION_INHERITED));
	}
	if ((tagBits & TagBits.AnnotationPolymorphicSignature) != 0) {
		annotations.add(getAnnotation(TypeConstants.JAVA_LANG_INVOKE_METHODHANDLE_$_POLYMORPHICSIGNATURE));
	}
	if ((tagBits & TagBits.AnnotationSafeVarargs) != 0) {
		annotations.add(getAnnotation(TypeConstants.JAVA_LANG_SAFEVARARGS));
	}
	// note that JAVA_LANG_SUPPRESSWARNINGS and JAVA_LANG_OVERRIDE cannot appear in binaries
	return (IAnnotation[]) annotations.toArray(new IAnnotation[annotations.size()]);
}

@Override
public String[] getCategories() throws JavaModelException {
	SourceMapper mapper= getSourceMapper();
	if (mapper != null) {
		// ensure the class file's buffer is open so that categories are computed
		getClassFile().getBuffer();

		if (mapper.categories != null) {
			String[] categories = (String[]) mapper.categories.get(this);
			if (categories != null)
				return categories;
		}
	}
	return CharOperation.NO_STRINGS;
}
public String getKey() {
	try {
		return getKey(false/*don't open*/);
	} catch (JavaModelException e) {
		// happen only if force open is true
		return null;
	}
}
/**
 * @see org.eclipse.jdt.internal.compiler.lookup.Binding#computeUniqueKey()
 */
public abstract String getKey(boolean forceOpen) throws JavaModelException;
/*
 * @see ISourceReference
 */
@Override
public ISourceRange getNameRange() throws JavaModelException {
	SourceMapper mapper= getSourceMapper();
	if (mapper != null) {
		// ensure the class file's buffer is open so that source ranges are computed
		getClassFile().getBuffer();

		return mapper.getNameRange(this);
	} else {
		return SourceMapper.UNKNOWN_RANGE;
	}
}
/*
 * @see ISourceReference
 */
@Override
public ISourceRange getSourceRange() throws JavaModelException {
	SourceMapper mapper= getSourceMapper();
	if (mapper != null) {
		// ensure the class file's buffer is open so that source ranges are computed
		getClassFile().getBuffer();

		return mapper.getSourceRange(this);
	} else {
		return SourceMapper.UNKNOWN_RANGE;
	}
}
/*
 * @see IMember
 */
@Override
public boolean isBinary() {
	return true;
}
/*
 * @see IJavaElement
 */
@Override
public boolean isStructureKnown() throws JavaModelException {
	return ((IJavaElement)getOpenableParent()).isStructureKnown();
}
/*
 * @see ISourceManipulation
 */
@Override
public void move(IJavaElement container, IJavaElement sibling, String rename, boolean force, IProgressMonitor monitor) throws JavaModelException {
	throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.READ_ONLY, this));
}
/*
 * @see ISourceManipulation
 */
@Override
public void rename(String newName, boolean force, IProgressMonitor monitor) throws JavaModelException {
	throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.READ_ONLY, this));
}
/*
 * Sets the contents of this element.
 * Throws an exception as this element is read only.
 */
public void setContents(String contents, IProgressMonitor monitor) throws JavaModelException {
	throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.READ_ONLY, this));
}
}
