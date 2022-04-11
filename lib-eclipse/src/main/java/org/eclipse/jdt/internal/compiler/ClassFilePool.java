/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.jdt.internal.compiler;

import java.util.Arrays;

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.ModuleBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;

public class ClassFilePool {
	public static final int POOL_SIZE = 25; // need to have enough for 2 units
	ClassFile[] classFiles;

private ClassFilePool() {
	// prevent instantiation
	this.classFiles = new ClassFile[POOL_SIZE];
}

public static ClassFilePool newInstance() {
	return new ClassFilePool();
}

public synchronized ClassFile acquire(SourceTypeBinding typeBinding) {
	for (int i = 0; i < POOL_SIZE; i++) {
		ClassFile classFile = this.classFiles[i];
		if (classFile == null) {
			ClassFile newClassFile = new ClassFile(typeBinding);
			this.classFiles[i] = newClassFile;
			newClassFile.isShared = true;
			return newClassFile;
		}
		if (!classFile.isShared) {
			classFile.reset(typeBinding, typeBinding.scope.compilerOptions());
			classFile.isShared = true;
			return classFile;
		}
	}
	return new ClassFile(typeBinding);
}
public synchronized ClassFile acquireForModule(ModuleBinding moduleBinding, CompilerOptions options) {
	for (int i = 0; i < POOL_SIZE; i++) {
		ClassFile classFile = this.classFiles[i];
		if (classFile == null) {
			ClassFile newClassFile = new ClassFile(moduleBinding, options);
			this.classFiles[i] = newClassFile;
			newClassFile.isShared = true;
			return newClassFile;
		}
		if (!classFile.isShared) {
			classFile.reset(null, options);
			classFile.isShared = true;
			return classFile;
		}
	}
	return new ClassFile(moduleBinding, options);
}
public synchronized void release(ClassFile classFile) {
	classFile.isShared = false;
}
public void reset() {
	Arrays.fill(this.classFiles, null);
}
}
