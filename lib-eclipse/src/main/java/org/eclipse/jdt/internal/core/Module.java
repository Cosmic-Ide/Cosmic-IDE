/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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

//import org.eclipse.jdt.core.IType;
//import org.eclipse.jdt.core.JavaModelException;
//import org.eclipse.jdt.core.compiler.CharOperation;
//import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
//import org.eclipse.jdt.internal.compiler.env.IModuleDeclaration;

//public class Module implements org.eclipse.jdt.internal.compiler.env.IModuleDeclaration {
//
//	public ClassFile binaryDecl;
//	public IModuleDeclaration declaration = null;
//	public boolean isBinary = false;
//	public SourceType sourceDecl;
//
//	public Module(SourceType sourceDecl) {
//		this.sourceDecl = sourceDecl;
//	}
//	public Module(ClassFile binaryDecl) {
//		this.binaryDecl = binaryDecl;
//		this.isBinary = true;
//	}
//	@Override
//	public char[] name() {
//		//
//		IModuleDeclaration decl = getDeclaration();
//		return decl == null ? CharOperation.NO_CHAR : decl.name();
//	}
//	@Override
//	public IModuleDeclaration getDeclaration() {
//		//
//		if (this.declaration == null) {
//			try {
//				if (this.isBinary) {
//					IType type = this.binaryDecl.getType();
//					this.declaration = ((ClassFileReader) (((BinaryType)type).getElementInfo())).getModuleDeclaration();
//				} else {
//					this.declaration = (ModuleInfo) this.sourceDecl.getElementInfo();
//				}
//			}catch (JavaModelException e) {
//				// do nothing
//			}
//		}
//		return this.declaration;
//	}
//
//	public String toString() {
//		return new String(this.name());
//
//	}
//}
