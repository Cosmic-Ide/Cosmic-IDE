/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

/**
 * @see IJavaElementRequestor
 */

@SuppressWarnings({ "rawtypes", "unchecked" })
public class JavaElementRequestor implements IJavaElementRequestor {
	/**
	 * True if this requestor no longer wants to receive
	 * results from its <code>IRequestorNameLookup</code>.
	 */
	protected boolean canceled= false;

	/**
	 * A collection of the resulting fields, or <code>null</code>
	 * if no field results have been received.
	 */
	protected ArrayList fields= null;

	/**
	 * A collection of the resulting initializers, or <code>null</code>
	 * if no initializer results have been received.
	 */
	protected ArrayList initializers= null;

	/**
	 * A collection of the resulting member types, or <code>null</code>
	 * if no member type results have been received.
	 */
	protected ArrayList memberTypes= null;

	/**
	 * A collection of the resulting methods, or <code>null</code>
	 * if no method results have been received.
	 */
	protected ArrayList methods= null;

	/**
	 * A collection of the resulting package fragments, or <code>null</code>
	 * if no package fragment results have been received.
	 */
	protected ArrayList packageFragments= null;

	/**
	 * A collection of the resulting types, or <code>null</code>
	 * if no type results have been received.
	 */
	protected ArrayList types= null;

	/**
	 * A collection of the resulting modules, or <code>null</code>
	 * if no module results have been received
	 */
	protected ArrayList<IModuleDescription> modules = null;

	/**
	 * Empty arrays used for efficiency
	 */
	protected static final IField[] EMPTY_FIELD_ARRAY= new IField[0];
	protected static final IInitializer[] EMPTY_INITIALIZER_ARRAY= new IInitializer[0];
	protected static final IType[] EMPTY_TYPE_ARRAY= new IType[0];
	protected static final IPackageFragment[] EMPTY_PACKAGE_FRAGMENT_ARRAY= new IPackageFragment[0];
	protected static final IMethod[] EMPTY_METHOD_ARRAY= new IMethod[0];
	protected static final IModuleDescription[] EMPTY_MODULE_ARRAY= new IModuleDescription[0];
/**
 * @see IJavaElementRequestor
 */
@Override
public void acceptField(IField field) {
	if (this.fields == null) {
		this.fields= new ArrayList();
	}
	this.fields.add(field);
}
/**
 * @see IJavaElementRequestor
 */
@Override
public void acceptInitializer(IInitializer initializer) {
	if (this.initializers == null) {
		this.initializers= new ArrayList();
	}
	this.initializers.add(initializer);
}
/**
 * @see IJavaElementRequestor
 */
@Override
public void acceptMemberType(IType type) {
	if (this.memberTypes == null) {
		this.memberTypes= new ArrayList();
	}
	this.memberTypes.add(type);
}
/**
 * @see IJavaElementRequestor
 */
@Override
public void acceptMethod(IMethod method) {
	if (this.methods == null) {
		this.methods = new ArrayList();
	}
	this.methods.add(method);
}
/**
 * @see IJavaElementRequestor
 */
@Override
public void acceptPackageFragment(IPackageFragment packageFragment) {
	if (this.packageFragments== null) {
		this.packageFragments= new ArrayList();
	}
	this.packageFragments.add(packageFragment);
}
/**
 * @see IJavaElementRequestor
 */
@Override
public void acceptType(IType type) {
	if (this.types == null) {
		this.types= new ArrayList();
	}
	this.types.add(type);
}
/**
 * @see IJavaElementRequestor
 */
@Override
public void acceptModule(IModuleDescription module) {
	if (this.modules == null) {
		this.modules= new ArrayList();
	}
	this.modules.add(module);
}

/**
 * @see IJavaElementRequestor
 */
public IField[] getFields() {
	if (this.fields == null) {
		return EMPTY_FIELD_ARRAY;
	}
	int size = this.fields.size();
	IField[] results = new IField[size];
	this.fields.toArray(results);
	return results;
}
/**
 * @see IJavaElementRequestor
 */
public IInitializer[] getInitializers() {
	if (this.initializers == null) {
		return EMPTY_INITIALIZER_ARRAY;
	}
	int size = this.initializers.size();
	IInitializer[] results = new IInitializer[size];
	this.initializers.toArray(results);
	return results;
}
/**
 * @see IJavaElementRequestor
 */
public IType[] getMemberTypes() {
	if (this.memberTypes == null) {
		return EMPTY_TYPE_ARRAY;
	}
	int size = this.memberTypes.size();
	IType[] results = new IType[size];
	this.memberTypes.toArray(results);
	return results;
}
/**
 * @see IJavaElementRequestor
 */
public IMethod[] getMethods() {
	if (this.methods == null) {
		return EMPTY_METHOD_ARRAY;
	}
	int size = this.methods.size();
	IMethod[] results = new IMethod[size];
	this.methods.toArray(results);
	return results;
}
/**
 * @see IJavaElementRequestor
 */
public IPackageFragment[] getPackageFragments() {
	if (this.packageFragments== null) {
		return EMPTY_PACKAGE_FRAGMENT_ARRAY;
	}
	int size = this.packageFragments.size();
	IPackageFragment[] results = new IPackageFragment[size];
	this.packageFragments.toArray(results);
	return results;
}
/**
 * @see IJavaElementRequestor
 */
public IType[] getTypes() {
	if (this.types== null) {
		return EMPTY_TYPE_ARRAY;
	}
	int size = this.types.size();
	IType[] results = new IType[size];
	this.types.toArray(results);
	return results;
}
/**
 * @see IJavaElementRequestor
 */
public IModuleDescription[] getModules() {
	if (this.modules == null) {
		return EMPTY_MODULE_ARRAY;
	}
	int size = this.modules.size();
	IModuleDescription[] results = new IModuleDescription[size];
	this.modules.toArray(results);
	return results;
}
/**
 * @see IJavaElementRequestor
 */
@Override
public boolean isCanceled() {
	return this.canceled;
}
/**
 * Reset the state of this requestor.
 */
public void reset() {
	this.canceled = false;
	this.fields = null;
	this.initializers = null;
	this.memberTypes = null;
	this.methods = null;
	this.packageFragments = null;
	this.types = null;
}
/**
 * Sets the #isCanceled state of this requestor to true or false.
 */
public void setCanceled(boolean b) {
	this.canceled= b;
}
}
