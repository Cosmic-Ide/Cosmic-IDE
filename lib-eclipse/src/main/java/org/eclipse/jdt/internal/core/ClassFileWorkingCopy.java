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
 *     Stephan Herrmann - Contribution for Bug 337935 - Test failures when run as an IDE (org.eclipse.sdk.ide)
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.util.ClassFileBytesDisassembler;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipse.jdt.internal.core.util.Disassembler;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * A working copy on an <code>IClassFile</code>.
 */
public class ClassFileWorkingCopy extends CompilationUnit {

	public AbstractClassFile classFile;

public ClassFileWorkingCopy(AbstractClassFile classFile, WorkingCopyOwner owner) {
	super((PackageFragment) classFile.getParent(), sourceFileName(classFile), owner);
	this.classFile = classFile;
}
private static String sourceFileName(AbstractClassFile classFile) {
	if (classFile instanceof ModularClassFile)
		return TypeConstants.MODULE_INFO_FILE_NAME_STRING;
	else
		return ((BinaryType) ((ClassFile) classFile).getType()).getSourceFileName(null/*no info available*/);
}

@Override
public void commitWorkingCopy(boolean force, IProgressMonitor monitor) throws JavaModelException {
	throw new JavaModelException(new JavaModelStatus(IJavaModelStatusConstants.INVALID_ELEMENT_TYPES, this));
}

@Override
public IBuffer getBuffer() throws JavaModelException {
	if (isWorkingCopy())
		return super.getBuffer();
	else
		return this.classFile.getBuffer();
}

@Override
public char[] getContents() {
	try {
		IBuffer buffer = getBuffer();
		if (buffer == null) return CharOperation.NO_CHAR;
		char[] characters = buffer.getCharacters();
		if (characters == null) return CharOperation.NO_CHAR;
		return characters;
	} catch (JavaModelException e) {
		return CharOperation.NO_CHAR;
	}
}

@Override
public IPath getPath() {
	return this.classFile.getPath();
}

@Override
public JavaElement getPrimaryElement(boolean checkOwner) {
	if (checkOwner && isPrimary()) return this;
	return new ClassFileWorkingCopy(this.classFile, DefaultWorkingCopyOwner.PRIMARY);
}

@Override
public IResource resource(PackageFragmentRoot root) {
	if (root.isArchive())
		return root.resource(root);
	return this.classFile.resource(root);
}

/**
 * @see Openable#openBuffer(IProgressMonitor, Object)
 */
@Override
protected IBuffer openBuffer(IProgressMonitor pm, Object info) throws JavaModelException {

	// create buffer
	IBuffer buffer = BufferManager.createBuffer(this);

	// set the buffer source
	IBuffer classFileBuffer = this.classFile.getBuffer();
	if (classFileBuffer != null) {
		buffer.setContents(classFileBuffer.getCharacters());
	} else {
		// Disassemble
		IClassFileReader reader = ToolFactory.createDefaultClassFileReader(this.classFile, IClassFileReader.ALL);
		Disassembler disassembler = new Disassembler();
		String contents = disassembler.disassemble(reader, Util.getLineSeparator("", getJavaProject()), ClassFileBytesDisassembler.WORKING_COPY); //$NON-NLS-1$
		buffer.setContents(contents);
	}

	// add buffer to buffer cache
	BufferManager bufManager = getBufferManager();
	bufManager.addBuffer(buffer);

	// listen to buffer changes
	buffer.addBufferChangedListener(this);

	return buffer;
}

@Override
protected void toStringName(StringBuffer buffer) {
	buffer.append(this.classFile.getElementName());
}

}
