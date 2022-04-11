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
 *     Stephan Herrmann - Contribution for
 *								Bug 458577 - IClassFile.getWorkingCopy() may lead to NPE in BecomeWorkingCopyOperation
 *								Bug 440477 - [null] Infrastructure for feeding external annotations into compilation
 *								Bug 462768 - [null] NPE when using linked folder for external annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * Common parts of ClassFile (containing a BinaryType) and ModularClassFile (containing a BinaryModule).
 * Prior to Java 9, most of this content was directly in ClassFile.
 */
public abstract class AbstractClassFile extends Openable implements IClassFile, SuffixConstants  {

	protected String name;

	protected AbstractClassFile(PackageFragment parent, String nameWithoutExtension) {
		super(parent);
		this.name = nameWithoutExtension;
	}

	/*
	 * @see IClassFile#becomeWorkingCopy(IProblemRequestor, WorkingCopyOwner, IProgressMonitor)
	 */
	@Override
	public ICompilationUnit becomeWorkingCopy(IProblemRequestor problemRequestor, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException {
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		CompilationUnit workingCopy = new ClassFileWorkingCopy(this, owner == null ? DefaultWorkingCopyOwner.PRIMARY : owner);
		JavaModelManager.PerWorkingCopyInfo perWorkingCopyInfo = manager.getPerWorkingCopyInfo(workingCopy, false/*don't create*/, true /*record usage*/, null/*no problem requestor needed*/);
		if (perWorkingCopyInfo == null) {
			// close cu and its children
			close();

			BecomeWorkingCopyOperation operation = new BecomeWorkingCopyOperation(workingCopy, problemRequestor);
			operation.runOperation(monitor);

			return workingCopy;
		}
		return perWorkingCopyInfo.workingCopy;
	}

	/**
	 * @see ICodeAssist#codeComplete(int, ICompletionRequestor)
	 * @deprecated
	 */
	@Override
	@Deprecated
	public void codeComplete(int offset, ICompletionRequestor requestor) throws JavaModelException {
		codeComplete(offset, requestor, DefaultWorkingCopyOwner.PRIMARY);
	}
	/**
	 * @see ICodeAssist#codeComplete(int, ICompletionRequestor, WorkingCopyOwner)
	 * @deprecated
	 */
	@Override
	@Deprecated
	public void codeComplete(int offset, ICompletionRequestor requestor, WorkingCopyOwner owner) throws JavaModelException {
		if (requestor == null) {
			throw new IllegalArgumentException("Completion requestor cannot be null"); //$NON-NLS-1$
		}
		codeComplete(offset, new org.eclipse.jdt.internal.codeassist.CompletionRequestorWrapper(requestor), owner);
	}

	@Override
	public void codeComplete(int offset, CompletionRequestor requestor) throws JavaModelException {
		codeComplete(offset, requestor, DefaultWorkingCopyOwner.PRIMARY);
	}

	@Override
	public void codeComplete(int offset, CompletionRequestor requestor, IProgressMonitor monitor) throws JavaModelException {
		codeComplete(offset, requestor, DefaultWorkingCopyOwner.PRIMARY, monitor);
	}

	@Override
	public void codeComplete(int offset, CompletionRequestor requestor, WorkingCopyOwner owner) throws JavaModelException {
		codeComplete(offset, requestor, owner, null);
	}
	@Override
	public abstract void codeComplete(int offset, CompletionRequestor requestor, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException;

	/**
	 * @see ICodeAssist#codeSelect(int, int)
	 */
	@Override
	public IJavaElement[] codeSelect(int offset, int length) throws JavaModelException {
		return codeSelect(offset, length, DefaultWorkingCopyOwner.PRIMARY);
	}
	@Override
	public abstract IJavaElement[] codeSelect(int offset, int length, WorkingCopyOwner owner) throws JavaModelException;

	/**
	 * Returns a new element info for this element.
	 */
	@Override
	protected Object createElementInfo() {
		return new ClassFileInfo();
	}
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AbstractClassFile)) return false;
		AbstractClassFile other = (AbstractClassFile) o;
		return this.name.equals(other.name) && this.getParent().equals(other.getParent());
	}

	/**
	 * Finds the deepest <code>IJavaElement</code> in the hierarchy of
	 * <code>elt</elt>'s children (including <code>elt</code> itself)
	 * which has a source range that encloses <code>position</code>
	 * according to <code>mapper</code>.
	 */
	protected IJavaElement findElement(IJavaElement elt, int position, SourceMapper mapper) {
		SourceRange range = mapper.getSourceRange(elt);
		if (range == null || position < range.getOffset() || range.getOffset() + range.getLength() - 1 < position) {
			return null;
		}
		if (elt instanceof IParent) {
			try {
				IJavaElement[] children = ((IParent) elt).getChildren();
				for (int i = 0; i < children.length; i++) {
					IJavaElement match = findElement(children[i], position, mapper);
					if (match != null) {
						return match;
					}
				}
			} catch (JavaModelException npe) {
				// elt doesn't exist: return the element
			}
		}
		return elt;
	}

	/**
	 * Provide a way for clients (like debugger) to determine if two non-equal {@link AbstractClassFile} objects point
	 * to the same physical storage. The return value is constructed form the container path (if there is any) and the
	 * path of the class file itself (that could be either absolute or relative if it is inside container).
	 *
	 * @return some kind of unique class file identifier based on path information only. The return value may look like
	 *         a path in a file system, but is not guaranteed to be a valid path that could be resolved via NIO API.
	 */
	public String getPathIdentifier() {
		JavaElement pkg = getParent();
		if (pkg instanceof JarPackageFragment) {
			JarPackageFragmentRoot root = (JarPackageFragmentRoot) pkg.getParent();
			String entryName = Util.concatWith(((PackageFragment) pkg).names, getElementName(), '/');
			entryName = root.getClassFilePath(entryName);
			String rootPath = root.getPath().toOSString();
			if (org.eclipse.jdt.internal.compiler.util.Util.isJrt(rootPath)) {
				// container + module + class
				return rootPath + '/' + root.getElementName() + '/' +entryName;
			} else {
				// container + class
				return rootPath + '/' + entryName;
			}
		} else {
			IFile file = (IFile) resource();
			IPath location = file.getLocation();
			return location == null? file.getFullPath().toPortableString() : location.toOSString();
		}
	}

	@Override
	public byte[] getBytes() throws JavaModelException {
		JavaElement pkg = getParent();
		if (pkg instanceof JarPackageFragment) {
			JarPackageFragmentRoot root = (JarPackageFragmentRoot) pkg.getParent();
			try {
				String entryName = Util.concatWith(((PackageFragment) pkg).names, getElementName(), '/');
				entryName = root.getClassFilePath(entryName);
				return getClassFileContent(root, entryName);
				// Java 9 - The below exception is not thrown in new scheme of things. Could cause issues?
	//			throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST, this));
			} catch (IOException ioe) {
				throw new JavaModelException(ioe, IJavaModelStatusConstants.IO_EXCEPTION);
			} catch (CoreException e) {
				if (e instanceof JavaModelException) {
					throw (JavaModelException)e;
				} else {
					throw new JavaModelException(e);
				}
			}
		} else {
			IFile file = (IFile) resource();
			return Util.getResourceContentsAsByteArray(file);
		}
	}
	protected byte[] getClassFileContent(JarPackageFragmentRoot root, String className) throws CoreException, IOException {
		byte[] contents = null;
		String rootPath = root.getPath().toOSString();
		if (org.eclipse.jdt.internal.compiler.util.Util.isJrt(rootPath)) {
			contents = org.eclipse.jdt.internal.compiler.util.JRTUtil.getClassfileContent(
					new File(rootPath),
					className,
					root.getElementName());
		} else {
			ZipFile zip = root.getJar();
			try {
				ZipEntry ze = zip.getEntry(className);
				if (ze != null) {
					contents = org.eclipse.jdt.internal.compiler.util.Util.getZipEntryByteContent(ze, zip);
				}
			} finally {
				JavaModelManager.getJavaModelManager().closeZipFile(zip);
			}
		}
		if (contents == null && Thread.interrupted()) // reading from JRT is interruptible
			throw new OperationCanceledException();
		return contents;
	}

	@Override
	public IBuffer getBuffer() throws JavaModelException {
		IStatus status = validateClassFile();
		if (status.isOK()) {
			return super.getBuffer();
		} else {
			switch (status.getCode()) {
			case IJavaModelStatusConstants.ELEMENT_NOT_ON_CLASSPATH: // don't throw a JavaModelException to be able to open .class file outside the classpath (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=138507 )
			case IJavaModelStatusConstants.INVALID_ELEMENT_TYPES: // don't throw a JavaModelException to be able to open .class file in proj==src case without source (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=221904 )
				return null;
			default:
				throw new JavaModelException(status);
			}
		}
	}
	/**
	 * @see IMember#getTypeRoot()
	 */
	public ITypeRoot getTypeRoot() {
		return this;
	}

	/**
	 * A class file has a corresponding resource unless it is contained
	 * in a jar.
	 *
	 * @see IJavaElement
	 */
	@Override
	public IResource getCorrespondingResource() throws JavaModelException {
		IPackageFragmentRoot root= (IPackageFragmentRoot)getParent().getParent();
		if (root.isArchive()) {
			return null;
		} else {
			return getUnderlyingResource();
		}
	}
	public IJavaElement getElementAtConsideringSibling(int position) throws JavaModelException {
		IPackageFragment fragment = (IPackageFragment)getParent();
		PackageFragmentRoot root = (PackageFragmentRoot) fragment.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		SourceMapper mapper = root.getSourceMapper();
		if (mapper == null) {
			return null;
		} else {
			int index = this.name.indexOf('$');
			int prefixLength = index < 0 ? this.name.length() : index;

			IType type = null;
			int start = -1;
			int end = Integer.MAX_VALUE;
			IJavaElement[] children = fragment.getChildren();
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof IOrdinaryClassFile) {
					IOrdinaryClassFile classFile = (IOrdinaryClassFile) children[i];
					String childName = classFile.getElementName();

					int childIndex = childName.indexOf('$');
					int childPrefixLength = childIndex < 0 ? childName.indexOf('.') : childIndex;
					if (prefixLength == childPrefixLength && this.name.regionMatches(0, childName, 0, prefixLength)) {

						// ensure this class file's buffer is open so that source ranges are computed
						classFile.getBuffer();

						SourceRange range = mapper.getSourceRange(classFile.getType());
						if (range == SourceMapper.UNKNOWN_RANGE) continue;
						int newStart = range.getOffset();
						int newEnd = newStart + range.getLength() - 1;
						if(newStart > start && newEnd < end
								&& newStart <= position && newEnd >= position) {
							type = classFile.getType();
							start = newStart;
							end = newEnd;
						}
					}
				}
			}
			if(type != null) {
				return findElement(type, position, mapper);
			}
			return null;
		}
	}
	@Override
	public String getElementName() {
		return this.name + SuffixConstants.SUFFIX_STRING_class;
	}
	/**
	 * @see IJavaElement
	 */
	@Override
	public int getElementType() {
		return CLASS_FILE;
	}

	/*
	 * @see IJavaElement
	 */
	@Override
	public IPath getPath() {
		PackageFragmentRoot root = getPackageFragmentRoot();
		if (root.isArchive()) {
			return root.getPath();
		} else {
			return getParent().getPath().append(getElementName());
		}
	}

	/*
	 * @see IJavaElement
	 */
	@Override
	public IResource resource(PackageFragmentRoot root) {
		return ((IContainer) ((Openable) this.getParent()).resource(root)).getFile(new Path(getElementName()));
	}
	/**
	 * @see ISourceReference
	 */
	@Override
	public String getSource() throws JavaModelException {
		IBuffer buffer = getBuffer();
		if (buffer == null) {
			return null;
		}
		return buffer.getContents();
	}
	/**
	 * @see ISourceReference
	 */
	@Override
	public ISourceRange getSourceRange() throws JavaModelException {
		IBuffer buffer = getBuffer();
		if (buffer != null) {
			String contents = buffer.getContents();
			if (contents == null) return null;
			return new SourceRange(0, contents.length());
		} else {
			return null;
		}
	}
	/**
	 * @see IClassFile
	 * @deprecated
	 */
	@Override
	@Deprecated
	public IJavaElement getWorkingCopy(IProgressMonitor monitor, org.eclipse.jdt.core.IBufferFactory factory) throws JavaModelException {
		return getWorkingCopy(BufferFactoryWrapper.create(factory), monitor);
	}
	/**
	 * @see Openable
	 */
	@Override
	protected boolean hasBuffer() {
		return true;
	}
	@Override
	public int hashCode() {
		return Util.combineHashCodes(this.name.hashCode(), this.getParent().hashCode());
	}
	/**
	 * Returns true - class files are always read only.
	 */
	@Override
	public boolean isReadOnly() {
		return true;
	}
	private IStatus validateClassFile() {
		IPackageFragmentRoot root = getPackageFragmentRoot();
		try {
			if (root.getKind() != IPackageFragmentRoot.K_BINARY)
				return new JavaModelStatus(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, root);
		} catch (JavaModelException e) {
			return e.getJavaModelStatus();
		}
		IJavaProject project = getJavaProject();
		return JavaConventions.validateClassFileName(getElementName(), project.getOption(JavaCore.COMPILER_SOURCE, true), project.getOption(JavaCore.COMPILER_COMPLIANCE, true));
	}


	/**
	 * @see ICodeAssist#codeComplete(int, ICodeCompletionRequestor)
	 * @deprecated - should use codeComplete(int, ICompletionRequestor) instead
	 */
	@Override
	@Deprecated
	public void codeComplete(int offset, final org.eclipse.jdt.core.ICodeCompletionRequestor requestor) throws JavaModelException {

		if (requestor == null){
			codeComplete(offset, (ICompletionRequestor)null);
			return;
		}
		codeComplete(
			offset,
			new ICompletionRequestor(){
				@Override
				public void acceptAnonymousType(char[] superTypePackageName,char[] superTypeName, char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance) {
					// ignore
				}
				@Override
				public void acceptClass(char[] packageName, char[] className, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
					requestor.acceptClass(packageName, className, completionName, modifiers, completionStart, completionEnd);
				}
				@Override
				public void acceptError(IProblem error) {
					// was disabled in 1.0
				}
				@Override
				public void acceptField(char[] declaringTypePackageName, char[] declaringTypeName, char[] fieldName, char[] typePackageName, char[] typeName, char[] completionName, int modifiers, int completionStart, int completionEnd, int relevance) {
					requestor.acceptField(declaringTypePackageName, declaringTypeName, fieldName, typePackageName, typeName, completionName, modifiers, completionStart, completionEnd);
				}
				@Override
				public void acceptInterface(char[] packageName,char[] interfaceName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance) {
					requestor.acceptInterface(packageName, interfaceName, completionName, modifiers, completionStart, completionEnd);
				}
				@Override
				public void acceptKeyword(char[] keywordName,int completionStart,int completionEnd, int relevance){
					requestor.acceptKeyword(keywordName, completionStart, completionEnd);
				}
				@Override
				public void acceptLabel(char[] labelName,int completionStart,int completionEnd, int relevance){
					requestor.acceptLabel(labelName, completionStart, completionEnd);
				}
				@Override
				public void acceptLocalVariable(char[] localVarName,char[] typePackageName,char[] typeName,int modifiers,int completionStart,int completionEnd, int relevance){
					// ignore
				}
				@Override
				public void acceptMethod(char[] declaringTypePackageName,char[] declaringTypeName,char[] selector,char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] returnTypePackageName,char[] returnTypeName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance){
					// skip parameter names
					requestor.acceptMethod(declaringTypePackageName, declaringTypeName, selector, parameterPackageNames, parameterTypeNames, returnTypePackageName, returnTypeName, completionName, modifiers, completionStart, completionEnd);
				}
				@Override
				public void acceptMethodDeclaration(char[] declaringTypePackageName,char[] declaringTypeName,char[] selector,char[][] parameterPackageNames,char[][] parameterTypeNames,char[][] parameterNames,char[] returnTypePackageName,char[] returnTypeName,char[] completionName,int modifiers,int completionStart,int completionEnd, int relevance){
					// ignore
				}
				@Override
				public void acceptModifier(char[] modifierName,int completionStart,int completionEnd, int relevance){
					requestor.acceptModifier(modifierName, completionStart, completionEnd);
				}
				@Override
				public void acceptPackage(char[] packageName,char[] completionName,int completionStart,int completionEnd, int relevance){
					requestor.acceptPackage(packageName, completionName, completionStart, completionEnd);
				}
				@Override
				public void acceptType(char[] packageName,char[] typeName,char[] completionName,int completionStart,int completionEnd, int relevance){
					requestor.acceptType(packageName, typeName, completionName, completionStart, completionEnd);
				}
				@Override
				public void acceptVariableName(char[] typePackageName,char[] typeName,char[] varName,char[] completionName,int completionStart,int completionEnd, int relevance){
					// ignore
				}
			});
	}

	@Override
	protected IStatus validateExistence(IResource underlyingResource) {
		// check whether the class file can be opened
		IStatus status = validateClassFile();
		if (!status.isOK())
			return status;
		if (underlyingResource != null) {
			if (!underlyingResource.isAccessible())
				return newDoesNotExistStatus();
			PackageFragmentRoot root;
			if ((underlyingResource instanceof IFolder) && (root = getPackageFragmentRoot()).isArchive()) { // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=204652
				return root.newDoesNotExistStatus();
			}
		}
		return JavaModelStatus.VERIFIED_OK;
	}

	@Override
	public ISourceRange getNameRange() {
		return null;
	}
}
