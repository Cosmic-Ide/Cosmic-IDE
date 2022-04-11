/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryModule;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.core.JavaModelManager.PerProjectInfo;

public class BinaryModule extends BinaryMember implements AbstractModule {

	private IBinaryModule info;

	/** For creating a pure handle from its memento. */
	public BinaryModule(JavaElement parent, String name) {
		super(parent, name);
	}
	/** For creating a populated handle from a class file. */
	public BinaryModule(JavaElement parent, IBinaryModule info) {
		super(parent, String.valueOf(info.name()));
		this.info = info;
	}
	@Override
	public IModule getModuleInfo() throws JavaModelException {
		if (this.info == null) {
			ModularClassFile classFile = (ModularClassFile) this.getParent();
			this.info = classFile.getBinaryModuleInfo();
		}
		return this.info;
	}
	@Override
	public IAnnotation[] getAnnotations() throws JavaModelException {
		IBinaryModule moduleInfo = (IBinaryModule) getModuleInfo();
		IBinaryAnnotation[] binaryAnnotations = moduleInfo.getAnnotations();
		long tagBits = moduleInfo.getTagBits() & ~TagBits.AnnotationDeprecated; // TODO: kludge to avoid duplication of real annotation and tagBit induced standard annotation
		return getAnnotations(binaryAnnotations, tagBits);
	}
	@Override
	public IJavaElement[] getChildren() throws JavaModelException {
		return NO_ELEMENTS;
	}
	@Override
	public boolean isBinary() {
		return true;
	}
	@Override
	public boolean isSystemModule() {
		IPackageFragmentRoot pfr = (IPackageFragmentRoot) getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		return pfr instanceof JrtPackageFragmentRoot;
	}
	@Override
	public int getFlags() throws JavaModelException {
		if (getModuleInfo().isOpen())
			return ClassFileConstants.ACC_OPEN;
		return 0;
	}
	@Override
	public char getHandleMementoDelimiter() {
		return JavaElement.JEM_MODULE;
	}
	@Override
	public String getKey(boolean forceOpen) throws JavaModelException {
		return getKey(this, forceOpen);
	}
	@Override
	public ISourceRange getSourceRange() throws JavaModelException {
		SourceMapper mapper= getSourceMapper();
		if (mapper != null) {
			// ensure the class file's buffer is open so that source ranges are computed
			((ModularClassFile)getClassFile()).getBuffer();

			return mapper.getSourceRange(this);
		} else {
			return SourceMapper.UNKNOWN_RANGE;
		}
	}
	@Override
	public String getAttachedJavadoc(IProgressMonitor monitor) throws JavaModelException {
		JavadocContents javadocContents = getJavadocContents(monitor);
		if (javadocContents == null) return null;
		return javadocContents.getModuleDoc();
	}
	public JavadocContents getJavadocContents(IProgressMonitor monitor) throws JavaModelException {
		PerProjectInfo projectInfo = JavaModelManager.getJavaModelManager().getPerProjectInfoCheckExistence(getJavaProject().getProject());
		JavadocContents cachedJavadoc = null;
		synchronized (projectInfo.javadocCache) {
			cachedJavadoc = (JavadocContents) projectInfo.javadocCache.get(this);
		}

		if (cachedJavadoc != null && cachedJavadoc != BinaryType.EMPTY_JAVADOC) {
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
		pathBuffer.append(getElementName()).append(JavadocConstants.MODULE_FILE_SUFFIX);
		if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
		String contents = getURLContents(baseLocation, String.valueOf(pathBuffer));
		JavadocContents javadocContents = new JavadocContents(contents);
		synchronized (projectInfo.javadocCache) {
			projectInfo.javadocCache.put(this, javadocContents);
		}
		return javadocContents;
	}
	@Override
	public String toString(String lineDelimiter) {
		StringBuffer buffer = new StringBuffer();
		try {
			toStringContent(buffer, lineDelimiter);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buffer.toString();
	}
}