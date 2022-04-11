/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModularClassFile;
import org.eclipse.jdt.core.IOrdinaryClassFile;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.JavaModelManager.PerProjectInfo;
import org.eclipse.jdt.internal.core.util.MementoTokenizer;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * @see IPackageFragment
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PackageFragment extends Openable implements IPackageFragment, SuffixConstants {
	/**
	 * Constant empty list of class files
	 */
	protected static final IClassFile[] NO_CLASSFILES = new IClassFile[] {};
	protected static final IOrdinaryClassFile[] NO_ORDINARY_CLASSFILES = new IOrdinaryClassFile[] {};
	/**
	 * Constant empty list of compilation units
	 */
	protected static final ICompilationUnit[] NO_COMPILATION_UNITS = new ICompilationUnit[] {};

	public String[] names;

	private boolean isValidPackageName;

protected PackageFragment(PackageFragmentRoot root, String[] names) {
	super(root);
	this.names = names;
	this.isValidPackageName = internalIsValidPackageName();
}
/**
 * @see Openable
 */
@Override
protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm, Map newElements, IResource underlyingResource) throws JavaModelException {
	// add compilation units/class files from resources
	HashSet vChildren = new HashSet();
	int kind = getKind();
	try {
	    PackageFragmentRoot root = getPackageFragmentRoot();
		char[][] inclusionPatterns = root.fullInclusionPatternChars();
		char[][] exclusionPatterns = root.fullExclusionPatternChars();
		IResource[] members = ((IContainer) underlyingResource).members();
		int length = members.length;
		if (length > 0) {
			IJavaProject project = getJavaProject();
			String sourceLevel = project.getOption(JavaCore.COMPILER_SOURCE, true);
			String complianceLevel = project.getOption(JavaCore.COMPILER_COMPLIANCE, true);
			for (int i = 0; i < length; i++) {
				IResource child = members[i];
				if (child.getType() != IResource.FOLDER
						&& !Util.isExcluded(child, inclusionPatterns, exclusionPatterns)) {
					IJavaElement childElement;
					if (kind == IPackageFragmentRoot.K_SOURCE && Util.isValidCompilationUnitName(child.getName(), sourceLevel, complianceLevel)) {
						childElement = new CompilationUnit(this, child.getName(), DefaultWorkingCopyOwner.PRIMARY);
						vChildren.add(childElement);
					} else if (kind == IPackageFragmentRoot.K_BINARY && Util.isValidClassFileName(child.getName(), sourceLevel, complianceLevel)) {
						childElement = getClassFile(child.getName());
						vChildren.add(childElement);
					}
				}
			}
		}
	} catch (CoreException e) {
		throw new JavaModelException(e);
	}

	if (kind == IPackageFragmentRoot.K_SOURCE) {
		// add primary compilation units
		ICompilationUnit[] primaryCompilationUnits = getCompilationUnits(DefaultWorkingCopyOwner.PRIMARY);
		for (int i = 0, length = primaryCompilationUnits.length; i < length; i++) {
			ICompilationUnit primary = primaryCompilationUnits[i];
			vChildren.add(primary);
		}
	}

	if (!vChildren.isEmpty()) {
		IJavaElement[] children = new IJavaElement[vChildren.size()];
		vChildren.toArray(children);
		info.setChildren(children);
	} else {
		info.setChildren(JavaElement.NO_ELEMENTS);
	}
	return true;
}
/**
 * Returns true if this fragment contains at least one java resource.
 * Returns false otherwise.
 */
@Override
public boolean containsJavaResources() throws JavaModelException {
	return ((PackageFragmentInfo) getElementInfo()).containsJavaResources();
}
/**
 * @see ISourceManipulation
 */
@Override
public void copy(IJavaElement container, IJavaElement sibling, String rename, boolean force, IProgressMonitor monitor) throws JavaModelException {
	if (container == null) {
		throw new IllegalArgumentException(Messages.operation_nullContainer);
	}
	IJavaElement[] elements= new IJavaElement[] {this};
	IJavaElement[] containers= new IJavaElement[] {container};
	IJavaElement[] siblings= null;
	if (sibling != null) {
		siblings= new IJavaElement[] {sibling};
	}
	String[] renamings= null;
	if (rename != null) {
		renamings= new String[] {rename};
	}
	getJavaModel().copy(elements, containers, siblings, renamings, force, monitor);
}
/**
 * @see IPackageFragment
 */
@Override
public ICompilationUnit createCompilationUnit(String cuName, String contents, boolean force, IProgressMonitor monitor) throws JavaModelException {
	CreateCompilationUnitOperation op= new CreateCompilationUnitOperation(this, cuName, contents, force);
	op.runOperation(monitor);
	return new CompilationUnit(this, cuName, DefaultWorkingCopyOwner.PRIMARY);
}
/**
 * @see JavaElement
 */
@Override
protected Object createElementInfo() {
	return new PackageFragmentInfo();
}
/**
 * @see ISourceManipulation
 */
@Override
public void delete(boolean force, IProgressMonitor monitor) throws JavaModelException {
	IJavaElement[] elements = new IJavaElement[] {this};
	getJavaModel().delete(elements, force, monitor);
}
@Override
public boolean equals(Object o) {
	if (this == o) return true;
	if (!(o instanceof PackageFragment)) return false;

	PackageFragment other = (PackageFragment) o;
	return Util.equalArraysOrNull(this.names, other.names) &&
			this.getParent().equals(other.getParent());
}
@Override
public boolean exists() {
	// super.exist() only checks for the parent and the resource existence
	// so also ensure that:
	//  - the package is not excluded (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=138577)
	//  - its name is valide (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=108456)
	return super.exists() && !Util.isExcluded(this) && isValidPackageName();
}
/**
 * @see IPackageFragment#getOrdinaryClassFile(String)
 * @exception IllegalArgumentException if the name does not end with ".class" or if the name is "module-info.class".
 */
@Override
public IOrdinaryClassFile getOrdinaryClassFile(String classFileName) {
	if (!org.eclipse.jdt.internal.compiler.util.Util.isClassFileName(classFileName)) {
		throw new IllegalArgumentException(Messages.bind(Messages.element_invalidClassFileName, classFileName));
	}
	if (TypeConstants.MODULE_INFO_CLASS_NAME_STRING.equals(classFileName)) {
		throw new IllegalArgumentException(Messages.element_moduleInfoNotSupported);
	}
	// don't hold on the .class file extension to save memory
	// also make sure to not use substring as the resulting String may hold on the underlying char[] which might be much bigger than necessary
	int length = classFileName.length() - 6;
	char[] nameWithoutExtension = new char[length];
	classFileName.getChars(0, length, nameWithoutExtension, 0);
	return new ClassFile(this, new String(nameWithoutExtension));
}
/**
 * @see IPackageFragment#getClassFile(String)
 * @exception IllegalArgumentException if the name does not end with ".class".
 */
@Override
public IClassFile getClassFile(String classFileName) {
	if (TypeConstants.MODULE_INFO_CLASS_NAME_STRING.equals(classFileName))
		return getModularClassFile();
	return getOrdinaryClassFile(classFileName);
}
@Override
public IModularClassFile getModularClassFile() {
	// don't hold on the .class file extension to save memory
	// also make sure to not use substring as the resulting String may hold on the underlying char[] which might be much bigger than necessary
	return new ModularClassFile(this);
}

/**
 * Returns a collection of ordinary class files in this - a folder package fragment which has a root
 * that has its kind set to <code>IPackageFragmentRoot.K_Source</code> does not
 * recognize class files.
 *
 * @see IPackageFragment#getOrdinaryClassFiles()
 */
@Override
public IOrdinaryClassFile[] getOrdinaryClassFiles() throws JavaModelException {
	if (getKind() == IPackageFragmentRoot.K_SOURCE) {
		return NO_ORDINARY_CLASSFILES;
	}

	ArrayList list = getChildrenOfType(CLASS_FILE);
	for (Iterator iterator = list.iterator(); iterator.hasNext();) {
		if (iterator.next() instanceof ModularClassFile)
			iterator.remove();
	}
	IOrdinaryClassFile[] array= new IOrdinaryClassFile[list.size()];
	list.toArray(array);
	return array;
}
/**
 * Returns a collection of all class files in this - a folder package fragment which has a root
 * that has its kind set to <code>IPackageFragmentRoot.K_Source</code> does not
 * recognize class files.
 *
 * @see IPackageFragment#getAllClassFiles()
 */
@Override
public IClassFile[] getAllClassFiles() throws JavaModelException {
	if (getKind() == IPackageFragmentRoot.K_SOURCE) {
		return NO_CLASSFILES;
	}

	ArrayList list = getChildrenOfType(CLASS_FILE);
	IClassFile[] array= new IClassFile[list.size()];
	list.toArray(array);
	return array;
}

@Deprecated
@Override
public IClassFile[] getClassFiles() throws JavaModelException {
	return getOrdinaryClassFiles();
}

/**
 * @see IPackageFragment#getCompilationUnit(String)
 * @exception IllegalArgumentException if the name does not end with ".java"
 */
@Override
public ICompilationUnit getCompilationUnit(String cuName) {
	if (!org.eclipse.jdt.internal.core.util.Util.isJavaLikeFileName(cuName)) {
		throw new IllegalArgumentException(Messages.convention_unit_notJavaName);
	}
	return new CompilationUnit(this, cuName, DefaultWorkingCopyOwner.PRIMARY);
}
/**
 * @see IPackageFragment#getCompilationUnits()
 */
@Override
public ICompilationUnit[] getCompilationUnits() throws JavaModelException {
	if (getKind() == IPackageFragmentRoot.K_BINARY) {
		return NO_COMPILATION_UNITS;
	}

	ArrayList list = getChildrenOfType(COMPILATION_UNIT);
	ICompilationUnit[] array= new ICompilationUnit[list.size()];
	list.toArray(array);
	return array;
}
/**
 * @see IPackageFragment#getCompilationUnits(WorkingCopyOwner)
 */
@Override
public ICompilationUnit[] getCompilationUnits(WorkingCopyOwner owner) {
	ICompilationUnit[] workingCopies = JavaModelManager.getJavaModelManager().getWorkingCopies(owner, false/*don't add primary*/);
	if (workingCopies == null) return JavaModelManager.NO_WORKING_COPY;
	int length = workingCopies.length;
	ICompilationUnit[] result = new ICompilationUnit[length];
	int index = 0;
	for (int i = 0; i < length; i++) {
		ICompilationUnit wc = workingCopies[i];
		if (equals(wc.getParent()) && !Util.isExcluded(wc)) { // 59933 - excluded wc shouldn't be answered back
			result[index++] = wc;
		}
	}
	if (index != length) {
		System.arraycopy(result, 0, result = new ICompilationUnit[index], 0, index);
	}
	return result;
}
@Override
public String getElementName() {
	if (this.names.length == 0)
		return DEFAULT_PACKAGE_NAME;
	return Util.concatWith(this.names, '.');
}
/**
 * @see IJavaElement
 */
@Override
public int getElementType() {
	return PACKAGE_FRAGMENT;
}
/*
 * @see JavaElement
 */
@Override
public IJavaElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner owner) {
	switch (token.charAt(0)) {
		case JEM_CLASSFILE:
			if (!memento.hasMoreTokens()) return this;
			String classFileName = memento.nextToken();
			JavaElement classFile = (JavaElement) getClassFile(classFileName);
			return classFile.getHandleFromMemento(memento, owner);
		case JEM_MODULAR_CLASSFILE:
			classFile = (JavaElement) getModularClassFile();
			return classFile.getHandleFromMemento(memento, owner);
		case JEM_COMPILATIONUNIT:
			if (!memento.hasMoreTokens()) return this;
			String cuName = memento.nextToken();
			JavaElement cu = new CompilationUnit(this, cuName, owner);
			return cu.getHandleFromMemento(memento, owner);
	}
	return null;
}
/**
 * @see JavaElement#getHandleMementoDelimiter()
 */
@Override
protected char getHandleMementoDelimiter() {
	return JavaElement.JEM_PACKAGEFRAGMENT;
}
/**
 * @see IPackageFragment#getKind()
 */
@Override
public int getKind() throws JavaModelException {
	return ((IPackageFragmentRoot)getParent()).getKind();
}
/**
 * Returns an array of non-java resources contained in the receiver.
 */
@Override
public Object[] getNonJavaResources() throws JavaModelException {
	if (isDefaultPackage()) {
		// We don't want to show non java resources of the default package (see PR #1G58NB8)
		return JavaElementInfo.NO_NON_JAVA_RESOURCES;
	} else {
		return ((PackageFragmentInfo) getElementInfo()).getNonJavaResources(resource(), getPackageFragmentRoot());
	}
}
/**
 * @see IJavaElement#getPath()
 */
@Override
public IPath getPath() {
	PackageFragmentRoot root = getPackageFragmentRoot();
	if (root.isArchive()) {
		return root.getPath();
	} else {
		IPath path = root.getPath();
		for (int i = 0, length = this.names.length; i < length; i++) {
			String name = this.names[i];
			path = path.append(name);
		}
		return path;
	}
}
/**
 * @see JavaElement#resource()
 */
@Override
public IResource resource(PackageFragmentRoot root) {
	int length = this.names.length;
	if (length == 0) {
		return root.resource(root);
	} else {
		IPath path = new Path(this.names[0]);
		for (int i = 1; i < length; i++)
			path = path.append(this.names[i]);
		return ((IContainer)root.resource(root)).getFolder(path);
	}
}
/**
 * @see IJavaElement#getUnderlyingResource()
 */
@Override
public IResource getUnderlyingResource() throws JavaModelException {
	IResource rootResource = this.getParent().getUnderlyingResource();
	if (rootResource == null) {
		//jar package fragment root that has no associated resource
		return null;
	}
	// the underlying resource may be a folder or a project (in the case that the project folder
	// is atually the package fragment root)
	if (rootResource.getType() == IResource.FOLDER || rootResource.getType() == IResource.PROJECT) {
		IContainer folder = (IContainer) rootResource;
		String[] segs = this.names;
		for (int i = 0; i < segs.length; ++i) {
			IResource child = folder.findMember(segs[i]);
			if (child == null || child.getType() != IResource.FOLDER) {
				throw newNotPresentException();
			}
			folder = (IFolder) child;
		}
		return folder;
	} else {
		return rootResource;
	}
}
@Override
public int hashCode() {
	int hash = this.getParent().hashCode();
	for (int i = 0, length = this.names.length; i < length; i++)
		hash = Util.combineHashCodes(this.names[i].hashCode(), hash);
	return hash;
}
/**
 * @see IParent
 */
@Override
public boolean hasChildren() throws JavaModelException {
	return getChildren().length > 0;
}
/**
 * @see IPackageFragment#hasSubpackages()
 */
@Override
public boolean hasSubpackages() throws JavaModelException {
	IJavaElement[] packages= ((IPackageFragmentRoot)getParent()).getChildren();
	int namesLength = this.names.length;
	nextPackage: for (int i= 0, length = packages.length; i < length; i++) {
		String[] otherNames = ((PackageFragment) packages[i]).names;
		if (otherNames.length <= namesLength) continue nextPackage;
		for (int j = 0; j < namesLength; j++)
			if (!this.names[j].equals(otherNames[j]))
				continue nextPackage;
		return true;
	}
	return false;
}
protected boolean internalIsValidPackageName() {
	// if package fragment refers to folder in another IProject, then
	// resource().getProject() is different than getJavaProject().getProject()
	// use the other java project's options to verify the name
	IJavaProject javaProject = JavaCore.create(resource().getProject());
	String sourceLevel = javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
	String complianceLevel = javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
	for (int i = 0, length = this.names.length; i < length; i++) {
		if (!Util.isValidFolderNameForPackage(this.names[i], sourceLevel, complianceLevel))
			return false;
	}
	return true;
}
/**
 * @see IPackageFragment#isDefaultPackage()
 */
@Override
public boolean isDefaultPackage() {
	return this.names.length == 0;
}
protected final boolean isValidPackageName() {
	return this.isValidPackageName;
}
/**
 * @see ISourceManipulation#move(IJavaElement, IJavaElement, String, boolean, IProgressMonitor)
 */
@Override
public void move(IJavaElement container, IJavaElement sibling, String rename, boolean force, IProgressMonitor monitor) throws JavaModelException {
	if (container == null) {
		throw new IllegalArgumentException(Messages.operation_nullContainer);
	}
	IJavaElement[] elements= new IJavaElement[] {this};
	IJavaElement[] containers= new IJavaElement[] {container};
	IJavaElement[] siblings= null;
	if (sibling != null) {
		siblings= new IJavaElement[] {sibling};
	}
	String[] renamings= null;
	if (rename != null) {
		renamings= new String[] {rename};
	}
	getJavaModel().move(elements, containers, siblings, renamings, force, monitor);
}
/**
 * @see ISourceManipulation#rename(String, boolean, IProgressMonitor)
 */
@Override
public void rename(String newName, boolean force, IProgressMonitor monitor) throws JavaModelException {
	if (newName == null) {
		throw new IllegalArgumentException(Messages.element_nullName);
	}
	IJavaElement[] elements= new IJavaElement[] {this};
	IJavaElement[] dests= new IJavaElement[] {getParent()};
	String[] renamings= new String[] {newName};
	getJavaModel().rename(elements, dests, renamings, force, monitor);
}
/**
 * Debugging purposes
 */
@Override
protected void toStringChildren(int tab, StringBuffer buffer, Object info) {
	if (tab == 0) {
		super.toStringChildren(tab, buffer, info);
	}
}
/**
 * Debugging purposes
 */
@Override
protected void toStringInfo(int tab, StringBuffer buffer, Object info, boolean showResolvedInfo) {
	buffer.append(tabString(tab));
	if (this.names.length == 0) {
		buffer.append("<default>"); //$NON-NLS-1$
	} else {
		toStringName(buffer);
	}
	if (info == null) {
		buffer.append(" (not open)"); //$NON-NLS-1$
	} else {
		if (tab > 0) {
			buffer.append(" (...)"); //$NON-NLS-1$
		}
	}
}

@Override
public String getAttachedJavadoc(IProgressMonitor monitor) throws JavaModelException {
	PerProjectInfo projectInfo = JavaModelManager.getJavaModelManager().getPerProjectInfoCheckExistence(getJavaProject().getProject());
	String cachedJavadoc = null;
	synchronized (projectInfo.javadocCache) {
		cachedJavadoc = (String) projectInfo.javadocCache.get(this);
	}
	if (cachedJavadoc != null) {
		return cachedJavadoc;
	}
	URL baseLocation= getJavadocBaseLocation();
	if (baseLocation == null) {
		return null;
	}
	StringBuffer pathBuffer = new StringBuffer(baseLocation.toExternalForm());

	if (!(pathBuffer.charAt(pathBuffer.length() - 1) == '/')) {
		pathBuffer.append('/');
	}
	String packPath= getElementName().replace('.', '/');
	pathBuffer.append(packPath).append('/').append(JavadocConstants.PACKAGE_FILE_NAME);

	if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
	String contents = getURLContents(baseLocation, String.valueOf(pathBuffer));
	if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
	if (contents == null) return null;

	contents = (new JavadocContents(contents)).getPackageDoc();
	if (contents == null) contents = ""; //$NON-NLS-1$
	synchronized (projectInfo.javadocCache) {
		projectInfo.javadocCache.put(this, contents);
	}
	return contents;
}

@Override
protected IStatus validateExistence(IResource underlyingResource) {
	// check that the name of the package is valid (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=108456)
	if (!isValidPackageName())
		return newDoesNotExistStatus();

	// check whether this pkg can be opened
	if (underlyingResource != null && !resourceExists(underlyingResource))
		return newDoesNotExistStatus();

	// check that it is not excluded (https://bugs.eclipse.org/bugs/show_bug.cgi?id=138577)
	int kind;
	try {
		kind = getKind();
	} catch (JavaModelException e) {
		return e.getStatus();
	}
	if (kind == IPackageFragmentRoot.K_SOURCE && Util.isExcluded(this))
		return newDoesNotExistStatus();

	return JavaModelStatus.VERIFIED_OK;
}
}
