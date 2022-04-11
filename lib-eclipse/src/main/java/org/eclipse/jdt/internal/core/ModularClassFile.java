/*******************************************************************************
 * Copyright (c) 2017, 2018 GK Software AG, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.IBinaryModule;
import org.eclipse.jdt.internal.compiler.env.IDependent;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.core.nd.java.model.BinaryModuleDescriptor;
import org.eclipse.jdt.internal.core.nd.java.model.BinaryModuleFactory;
import org.eclipse.jdt.internal.core.util.MementoTokenizer;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * A handle to a modular class file.
 */
public class ModularClassFile extends AbstractClassFile implements IModularClassFile {

	protected ModularClassFile(PackageFragment parent) {
		super(parent, TypeConstants.MODULE_INFO_NAME_STRING);
	}

	/**
	 * Creates the single child element for this class file adding the resulting
	 * new handle (of type {@link IBinaryModule}) and info object to the newElements table.
	 * Returns true if successful, or false if an error is encountered parsing the class file.
	 *
	 * @see Openable
	 * @see Signature
	 */
	@Override
	protected boolean buildStructure(OpenableElementInfo info, IProgressMonitor pm, Map newElements, IResource underlyingResource) throws JavaModelException {
		IBinaryModule moduleInfo = getBinaryModuleInfo();
		if (moduleInfo == null) {
			// The structure of a class file is unknown if a class file format errors occurred
			//during the creation of the diet class file representative of this ClassFile.
			info.setChildren(JavaElement.NO_ELEMENTS);
			return false;
		}

		// Create & link a handle:
		BinaryModule module = new BinaryModule(this, moduleInfo);
		newElements.put(module, moduleInfo);
		info.setChildren(new IJavaElement[] {module});
		((ClassFileInfo) info).setModule(module);
		((PackageFragmentRootInfo) getPackageFragmentRoot().getElementInfo()).setModule(module);
		return true;
	}

	@Override
	public void codeComplete(int offset, CompletionRequestor requestor, WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException {
		String source = getSource();
		if (source != null) {
			BasicCompilationUnit cu =
				new BasicCompilationUnit(
					getSource().toCharArray(),
					null,
					TypeConstants.MODULE_INFO_FILE_NAME_STRING,
					getJavaProject()); // use project to retrieve corresponding .java IFile
			codeComplete(cu, cu, offset, requestor, owner, null/*extended context isn't computed*/, monitor);
		}
	}

	@Override
	public IJavaElement[] codeSelect(int offset, int length, WorkingCopyOwner owner) throws JavaModelException {
		IBuffer buffer = getBuffer();
		char[] contents;
		if (buffer != null && (contents = buffer.getCharacters()) != null) {
			BasicCompilationUnit cu = new BasicCompilationUnit(contents, null, TypeConstants.MODULE_INFO_FILE_NAME_STRING, this);
			return super.codeSelect(cu, offset, length, owner);
		} else {
			// has no associated source
			return new IJavaElement[] {};
		}
	}

	@Override
	public IType findPrimaryType() {
		return null;
	}

	@Override
	public boolean isClass() throws JavaModelException {
		return false;
	}

	@Override
	public boolean isInterface() throws JavaModelException {
		return false;
	}

	/**
	 * @return never returns.
	 * @throws UnsupportedOperationException
	 *             always.
	 * @deprecated should only be used as {@link IOrdinaryClassFile#getType()}.
	 * @see IClassFile#getType()
	 */

	@Deprecated
	@Override
	public IType getType() {
		throw new UnsupportedOperationException("IClassFile#getType() cannot be used on an IModularClassFile"); //$NON-NLS-1$
	}

	/**
	 * Returns the <code>IBinaryModule</code> specific for this IClassFile, based
	 * on its underlying resource, or <code>null</code> if unable to create
	 * the diet class file.
	 * There are two cases to consider:<ul>
	 * <li>a class file corresponding to an IFile resource</li>
	 * <li>a class file corresponding to a zip entry in a JAR</li>
	 * </ul>
	 *
	 * @exception JavaModelException when the IFile resource or JAR is not available
	 * or when this class file is not present in the JAR
	 */
	public IBinaryModule getBinaryModuleInfo() throws JavaModelException {
		try {
			IBinaryModule info = getJarBinaryModuleInfo();
			if (info == null) {
				throw newNotPresentException();
			}
			return info;
		} catch (ClassFormatException cfe) {
			//the structure remains unknown
			if (JavaCore.getPlugin().isDebugging()) {
				cfe.printStackTrace(System.err);
			}
			return null;
		} catch (IOException ioe) {
			throw new JavaModelException(ioe, IJavaModelStatusConstants.IO_EXCEPTION);
		} catch (CoreException e) {
			if (e instanceof JavaModelException) {
				throw (JavaModelException)e;
			} else {
				throw new JavaModelException(e);
			}
		}
	}

	private IBinaryModule getJarBinaryModuleInfo() throws CoreException, IOException, ClassFormatException {
		BinaryModuleDescriptor descriptor = BinaryModuleFactory.createDescriptor(this);

		if (descriptor == null) {
			return null;
		}
		IBinaryModule result = null;
		IPackageFragmentRoot root = getPackageFragmentRoot();
		if (getPackageFragmentRoot() instanceof JarPackageFragmentRoot) {
			if (root instanceof JrtPackageFragmentRoot || this.name.equals(IModule.MODULE_INFO)) {
				PackageFragment pkg = (PackageFragment) getParent();
				JarPackageFragmentRoot jarRoot = (JarPackageFragmentRoot) getPackageFragmentRoot();
				String entryName = jarRoot.getClassFilePath(Util.concatWith(pkg.names, getElementName(), '/'));
				byte[] contents = getClassFileContent(jarRoot, entryName);
				if (contents != null) {
					String fileName = root.getHandleIdentifier() + IDependent.JAR_FILE_ENTRY_SEPARATOR + entryName;
					ClassFileReader classFileReader = new ClassFileReader(contents, fileName.toCharArray(), false);
					return classFileReader.getModuleDeclaration();
				}
			} else {
				result = BinaryModuleFactory.readModule(descriptor, null);
			}
		} else {
			result = BinaryModuleFactory.readModule(descriptor, null);
		}

		return result;
	}

	/**
	 * @see ITypeRoot
	 */
	@Override
	public IJavaElement getElementAt(int position) throws JavaModelException {
		IJavaElement parentElement = getParent();
		while (parentElement.getElementType() != IJavaElement.PACKAGE_FRAGMENT_ROOT) {
			parentElement = parentElement.getParent();
		}
		PackageFragmentRoot root = (PackageFragmentRoot) parentElement;
		SourceMapper mapper = root.getSourceMapper();
		if (mapper == null) {
			return null;
		} else {
			// ensure this class file's buffer is open so that source ranges are computed
			getBuffer();

			IModuleDescription module = getModule();
			return findElement(module, position, mapper);
		}
	}
	@Override
	public IJavaElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner owner) {
		switch (token.charAt(0)) {
			case JEM_MODULE:
				if (!memento.hasMoreTokens()) return this;
				String modName = memento.nextToken();
				JavaElement mod = new BinaryModule(this, modName);
				return mod.getHandleFromMemento(memento, owner);
		}
		return null;
	}
	/**
	 * @see JavaElement#getHandleMemento()
	 */
	@Override
	protected char getHandleMementoDelimiter() {
		return JavaElement.JEM_MODULAR_CLASSFILE;
	}
	@Override
	protected void escapeMementoName(StringBuffer buffer, String mementoName) {
		// nop, name is irrelevant
	}
	@Override
	public ICompilationUnit getWorkingCopy(WorkingCopyOwner owner, IProgressMonitor monitor) throws JavaModelException {
		CompilationUnit workingCopy = new ClassFileWorkingCopy(this, owner == null ? DefaultWorkingCopyOwner.PRIMARY : owner);
		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		JavaModelManager.PerWorkingCopyInfo perWorkingCopyInfo =
			manager.getPerWorkingCopyInfo(workingCopy, false/*don't create*/, true/*record usage*/, null/*not used since don't create*/);
		if (perWorkingCopyInfo != null) {
			return perWorkingCopyInfo.getWorkingCopy(); // return existing handle instead of the one created above
		}
		BecomeWorkingCopyOperation op = new BecomeWorkingCopyOperation(workingCopy, null);
		op.runOperation(monitor);
		return workingCopy;
	}
	/**
	 * Opens and returns buffer on the source code associated with this class file.
	 * Maps the source code to the children elements of this class file.
	 * If no source code is associated with this class file,
	 * <code>null</code> is returned.
	 *
	 * @see Openable
	 */
	@Override
	protected IBuffer openBuffer(IProgressMonitor pm, Object info) throws JavaModelException {
		SourceMapper mapper = getSourceMapper();
		if (mapper != null) {
			return mapSource(mapper);
		}
		return null;
	}

	/** Loads the buffer via SourceMapper, and maps it in SourceMapper */
	private IBuffer mapSource(SourceMapper mapper) throws JavaModelException {
		char[] contents = mapper.findSource(getModule());
		if (contents != null) {
			// create buffer
			IBuffer buffer = BufferManager.createBuffer(this);
			if (buffer == null) return null;
			BufferManager bufManager = getBufferManager();
			bufManager.addBuffer(buffer);

			// set the buffer source
			if (buffer.getCharacters() == null){
				buffer.setContents(contents);
			}

			// listen to buffer changes
			buffer.addBufferChangedListener(this);

			// do the source mapping
			mapper.mapSource((NamedMember) getModule(), contents, null);

			return buffer;
		} else {
			// create buffer
			IBuffer buffer = BufferManager.createNullBuffer(this);
			if (buffer == null) return null;
			BufferManager bufManager = getBufferManager();
			bufManager.addBuffer(buffer);

			// listen to buffer changes
			buffer.addBufferChangedListener(this);
			return buffer;
		}
	}

	@Override
	public IModuleDescription getModule() throws JavaModelException {
		BinaryModule module = (BinaryModule) ((ClassFileInfo) getElementInfo()).getModule();
		if (module == null) {
			throw newNotPresentException();
		}
		return module;
	}

	private static final JavaElementInfo notExists = new JavaElementInfo();

	@Override
	public boolean exists() {
		Object info = JavaModelManager.getJavaModelManager().getInfo(this);
		if (info == notExists)
			return false;
		if (info != null)
			return true;
		boolean exists = super.exists();
		if (!exists)
			JavaModelManager.getJavaModelManager().putInfos(this, notExists, false, Map.of(this, notExists));
		return exists;
	}
}
