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
package org.eclipse.jdt.internal.core.util;

import java.util.ArrayList;

import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.util.SuffixConstants;
import org.eclipse.jdt.internal.core.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class JavaElementFinder extends BindingKeyParser {

	private JavaProject project;
	private WorkingCopyOwner owner;
	public IJavaElement element;
	public JavaModelException exception;
	private ArrayList types = new ArrayList();

	public JavaElementFinder(String key, JavaProject project, WorkingCopyOwner owner) {
		super(key);
		this.project = project;
		this.owner = owner;
	}

	private JavaElementFinder(BindingKeyParser parser, JavaProject project, WorkingCopyOwner owner) {
		super(parser);
		this.project = project;
		this.owner = owner;
	}

	@Override
	public void consumeAnnotation() {
		if (!(this.element instanceof IAnnotatable)) return;
		int size = this.types.size();
		if (size == 0) return;
		IJavaElement annotationType = ((JavaElementFinder) this.types.get(size-1)).element;
		this.element = ((IAnnotatable) this.element).getAnnotation(annotationType.getElementName());
	}

	@Override
	public void consumeField(char[] fieldName) {
		if (!(this.element instanceof IType)) return;
		this.element = ((IType) this.element).getField(new String(fieldName));
	}

	@Override
	public void consumeFullyQualifiedName(char[] fullyQualifiedName) {
		try {
			this.element = this.project.findType(new String(CharOperation.replaceOnCopy(fullyQualifiedName, '/', '.')), this.owner);
		} catch (JavaModelException e) {
			this.exception = e;
		}
	}

	@Override
	public void consumeLocalType(char[] uniqueKey) {
		if (this.element == null) return;
		if (this.element instanceof BinaryType) {
			int lastSlash = CharOperation.lastIndexOf('/', uniqueKey);
			int end = CharOperation.indexOf(';', uniqueKey, lastSlash+1);
			char[] localName = CharOperation.subarray(uniqueKey, lastSlash+1, end);
			IPackageFragment pkg = (IPackageFragment) this.element.getAncestor(IJavaElement.PACKAGE_FRAGMENT);
			this.element = pkg.getClassFile(new String(localName) + SuffixConstants.SUFFIX_STRING_class);
		} else {
			int firstDollar = CharOperation.indexOf('$', uniqueKey);
			int end = CharOperation.indexOf('$', uniqueKey, firstDollar+1);
			if (end == -1)
				end = CharOperation.indexOf(';', uniqueKey, firstDollar+1);
			char[] sourceStart = CharOperation.subarray(uniqueKey, firstDollar+1, end);
			int position = Integer.parseInt(new String(sourceStart));
			try {
				this.element = ((ITypeRoot) this.element.getOpenable()).getElementAt(position);
			} catch (JavaModelException e) {
				this.exception = e;
			}
		}
	}

	@Override
	public void consumeMemberType(char[] simpleTypeName) {
		if (!(this.element instanceof IType)) return;
		this.element = ((IType) this.element).getType(new String(simpleTypeName));
	}

	@Override
	public void consumeMethod(char[] selector, char[] signature) {
		if (!(this.element instanceof IType)) return;
		String[] parameterTypes = Signature.getParameterTypes(new String(signature));
		IType type = (IType) this.element;
		IMethod method = type.getMethod(new String(selector), parameterTypes);
		IMethod[] methods = type.findMethods(method);
		if (methods.length > 0)
			this.element = methods[0];
	}

	@Override
	public void consumePackage(char[] pkgName) {
		pkgName = CharOperation.replaceOnCopy(pkgName, '/', '.');
		try {
			this.element = this.project.findPackageFragment(new String(pkgName));
		} catch (JavaModelException e) {
			this.exception = e;
		}
	}

	@Override
	public void consumeParser(BindingKeyParser parser) {
		this.types.add(parser);
	}

	@Override
	public void consumeSecondaryType(char[] simpleTypeName) {
		if (this.element == null) return;
		IOpenable openable = this.element.getOpenable();
		if (!(openable instanceof ICompilationUnit)) return;
		this.element = ((ICompilationUnit) openable).getType(new String(simpleTypeName));
	}

	@Override
	public void consumeTypeVariable(char[] position, char[] typeVariableName) {
		if (this.element == null) return;
		switch (this.element.getElementType()) {
		case IJavaElement.TYPE:
			this.element = ((IType) this.element).getTypeParameter(new String(typeVariableName));
			break;
		case IJavaElement.METHOD:
			this.element = ((IMethod) this.element).getTypeParameter(new String(typeVariableName));
			break;
		}
	}

	@Override
	public void consumeModule(char[] moduleName) {
		try {
			this.element = this.project.findModule(new String(moduleName), null);
		} catch (JavaModelException e) {
			this.exception = e;
		}
	}

	@Override
	public BindingKeyParser newParser() {
		return new JavaElementFinder(this, this.project, this.owner);
	}

}
