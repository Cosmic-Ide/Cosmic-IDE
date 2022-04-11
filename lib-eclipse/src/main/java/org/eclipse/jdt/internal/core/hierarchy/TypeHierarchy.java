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
package org.eclipse.jdt.internal.core.hierarchy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeHierarchyChangedListener;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.core.ClassFile;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.core.JavaModelStatus;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.Openable;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.Region;
import org.eclipse.jdt.internal.core.TypeVector;
import org.eclipse.jdt.internal.core.util.Messages;
import org.eclipse.jdt.internal.core.util.Util;

/**
 * @see ITypeHierarchy
 */
public class TypeHierarchy implements ITypeHierarchy, IElementChangedListener {

	public static boolean DEBUG = false;

	static final byte VERSION = 0x0000;
	// SEPARATOR
	static final byte SEPARATOR1 = '\n';
	static final byte SEPARATOR2 = ',';
	static final byte SEPARATOR3 = '>';
	static final byte SEPARATOR4 = '\r';
	// general info
	static final byte COMPUTE_SUBTYPES = 0x0001;

	// type info
	static final byte CLASS = 0x0000;
	static final byte INTERFACE = 0x0001;
	static final byte COMPUTED_FOR = 0x0002;
	static final byte ROOT = 0x0004;

	// cst
	static final byte[] NO_FLAGS = new byte[]{};
	static final int SIZE = 10;

	/**
	 * The Java Project in which the hierarchy is being built - this
	 * provides the context for determining a classpath and namelookup rules.
	 * Possibly null.
	 */
	protected IJavaProject project;
	/**
	 * The type the hierarchy was specifically computed for,
	 * possibly null.
	 */
	protected IType focusType;

	/*
	 * The working copies that take precedence over original compilation units
	 */
	protected ICompilationUnit[] workingCopies;

	protected Map<IType, IType> classToSuperclass;
	protected Map<IType, IType[]> typeToSuperInterfaces;
	protected Map<IType, TypeVector> typeToSubtypes;
	protected Map<IType, Integer> typeFlags;
	protected TypeVector rootClasses = new TypeVector();
	protected ArrayList<IType> interfaces = new ArrayList<IType>(10);
	public ArrayList<String> missingTypes = new ArrayList<String>(4);

	protected static final IType[] NO_TYPE = new IType[0];

	/**
	 * The progress monitor to report work completed too.
	 */
	protected SubMonitor progressMonitor = SubMonitor.convert(null);

	/**
	 * Change listeners - null if no one is listening.
	 */
	protected ArrayList<ITypeHierarchyChangedListener> changeListeners = null;

	/*
	 * A map from Openables to ArrayLists of ITypes
	 */
	public Map<IOpenable, ArrayList<IType>> files = null;

	/**
	 * A region describing the packages considered by this
	 * hierarchy. Null if not activated.
	 */
	protected Region packageRegion = null;

	/**
	 * A region describing the projects considered by this
	 * hierarchy. Null if not activated.
	 */
	protected Region projectRegion = null;

	/**
	 * Whether this hierarchy should contains subtypes.
	 */
	protected boolean computeSubtypes;

	/**
	 * The scope this hierarchy should restrain itsef in.
	 */
	IJavaSearchScope scope;

	/*
	 * Whether this hierarchy needs refresh
	 */
	public boolean needsRefresh = true;

	/*
	 * Collects changes to types
	 */
	protected ChangeCollector changeCollector;

/**
 * Creates an empty TypeHierarchy
 */
public TypeHierarchy() {
	// Creates an empty TypeHierarchy
}
/**
 * Creates a TypeHierarchy on the given type.
 */
public TypeHierarchy(IType type, ICompilationUnit[] workingCopies, IJavaProject project, boolean computeSubtypes) {
	this(type, workingCopies, SearchEngine.createJavaSearchScope(new IJavaElement[] {project}), computeSubtypes);
	this.project = project;
}
/**
 * Creates a TypeHierarchy on the given type.
 */
public TypeHierarchy(IType type, ICompilationUnit[] workingCopies, IJavaSearchScope scope, boolean computeSubtypes) {
	this.focusType = type == null ? null : (IType) ((JavaElement) type).unresolved(); // unsure the focus type is unresolved (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=92357)
	this.workingCopies = workingCopies;
	this.computeSubtypes = computeSubtypes;
	this.scope = scope;
}
/**
 * Initializes the file, package and project regions
 */
protected void initializeRegions() {

	IType[] allTypes = getAllTypes();
	for (int i = 0; i < allTypes.length; i++) {
		IType type = allTypes[i];
		Openable o = (Openable) ((JavaElement) type).getOpenableParent();
		if (o != null) {
			ArrayList<IType> types = this.files.get(o);
			if (types == null) {
				types = new ArrayList<>();
				this.files.put(o, types);
			}
			types.add(type);
		}
		IPackageFragment pkg = type.getPackageFragment();
		this.packageRegion.add(pkg);
		IJavaProject declaringProject = type.getJavaProject();
		if (declaringProject != null) {
			this.projectRegion.add(declaringProject);
		}
		checkCanceled();
	}
}
/**
 * Adds the type to the collection of interfaces.
 */
protected void addInterface(IType type) {
	this.interfaces.add(type);
}
/**
 * Adds the type to the collection of root classes
 * if the classes is not already present in the collection.
 */
protected void addRootClass(IType type) {
	if (this.rootClasses.contains(type)) return;
	this.rootClasses.add(type);
}
/**
 * Adds the given subtype to the type.
 */
protected void addSubtype(IType type, IType subtype) {
	TypeVector subtypes = this.typeToSubtypes.get(type);
	if (subtypes == null) {
		subtypes = new TypeVector();
		this.typeToSubtypes.put(type, subtypes);
	}
	if (!subtypes.contains(subtype)) {
		subtypes.add(subtype);
	}
}
/**
 * @see ITypeHierarchy
 */
@Override
public synchronized void addTypeHierarchyChangedListener(ITypeHierarchyChangedListener listener) {
	ArrayList<ITypeHierarchyChangedListener> listeners = this.changeListeners;
	if (listeners == null) {
		this.changeListeners = listeners = new ArrayList<>();
	}

	// register with JavaCore to get Java element delta on first listener added
	if (listeners.size() == 0) {
		JavaCore.addElementChangedListener(this);
	}

	// add listener only if it is not already present
	if (listeners.indexOf(listener) == -1) {
		listeners.add(listener);
	}
}
private static Integer bytesToFlags(byte[] bytes){
	if(bytes != null && bytes.length > 0) {
		return Integer.valueOf(new String(bytes));
	} else {
		return null;
	}
}
/**
 * cacheFlags.
 */
public void cacheFlags(IType type, int flags) {
	this.typeFlags.put(type, Integer.valueOf(flags));
}
/**
 * Caches the handle of the superclass for the specified type.
 * As a side effect cache this type as a subtype of the superclass.
 */
protected void cacheSuperclass(IType type, IType superclass) {
	if (superclass != null) {
		if (superclass.equals(type)) {
			Util.log(IStatus.ERROR, "Type "+type.getFullyQualifiedName()+" is it's own superclass");  //$NON-NLS-1$//$NON-NLS-2$
			return; // refuse to enter what could lead to a stackoverflow later
		}
		this.classToSuperclass.put(type, superclass);
		addSubtype(superclass, type);
	}
}
/**
 * Caches all of the superinterfaces that are specified for the
 * type.
 */
protected void cacheSuperInterfaces(IType type, IType[] superinterfaces) {
	this.typeToSuperInterfaces.put(type, superinterfaces);
	for (int i = 0; i < superinterfaces.length; i++) {
		IType superinterface = superinterfaces[i];
		if (superinterface != null) {
			addSubtype(superinterface, type);
		}
	}
}
/**
 * Checks with the progress monitor to see whether the creation of the type hierarchy
 * should be canceled. Should be regularly called
 * so that the user can cancel.
 *
 * @exception OperationCanceledException if cancelling the operation has been requested
 * @see IProgressMonitor#isCanceled
 */
protected void checkCanceled() {
	if (this.progressMonitor != null && this.progressMonitor.isCanceled()) {
		throw new OperationCanceledException();
	}
}
/**
 * Compute this type hierarchy.
 */
protected void compute() throws JavaModelException, CoreException {
	if (this.focusType != null) {
		HierarchyBuilder builder =
			new IndexBasedHierarchyBuilder(
				this,
				this.scope);
		builder.build(this.computeSubtypes);
	} // else a RegionBasedTypeHierarchy should be used
}
/**
 * @see ITypeHierarchy
 */
@Override
public boolean contains(IType type) {
	// classes
	if (this.classToSuperclass.get(type) != null) {
		return true;
	}

	// root classes
	if (this.rootClasses.contains(type)) return true;

	// interfaces
	if (this.interfaces.contains(type)) return true;

	return false;
}
/**
 * Determines if the change affects this hierarchy, and fires
 * change notification if required.
 */
@Override
public void elementChanged(ElementChangedEvent event) {
	// type hierarchy change has already been fired
	if (this.needsRefresh) return;

	if (isAffected(event.getDelta(), event.getType())) {
		this.needsRefresh = true;
		fireChange();
	}
}
/**
 * @see ITypeHierarchy
 */
@Override
public boolean exists() {
	if (!this.needsRefresh) return true;

	return (this.focusType == null || this.focusType.exists()) && javaProject().exists();
}
/**
 * Notifies listeners that this hierarchy has changed and needs
 * refreshing. Note that listeners can be removed as we iterate
 * through the list.
 */
public void fireChange() {
	ArrayList<ITypeHierarchyChangedListener> listeners = getClonedChangeListeners(); // clone so that a listener cannot have a side-effect on this list when being notified
	if (listeners == null) {
		return;
	}
	if (DEBUG) {
		System.out.println("FIRING hierarchy change ["+Thread.currentThread()+"]"); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.focusType != null) {
			System.out.println("    for hierarchy focused on " + ((JavaElement)this.focusType).toStringWithAncestors()); //$NON-NLS-1$
		}
	}

	for (int i= 0; i < listeners.size(); i++) {
		final ITypeHierarchyChangedListener listener= listeners.get(i);
		SafeRunner.run(new ISafeRunnable() {
			@Override
			public void handleException(Throwable exception) {
				Util.log(exception, "Exception occurred in listener of Type hierarchy change notification"); //$NON-NLS-1$
			}
			@Override
			public void run() throws Exception {
				listener.typeHierarchyChanged(TypeHierarchy.this);
			}
		});
	}
}
@SuppressWarnings("unchecked")
private synchronized ArrayList<ITypeHierarchyChangedListener> getClonedChangeListeners() {
	ArrayList<ITypeHierarchyChangedListener> listeners = this.changeListeners;
	if (listeners == null) {
		return null;
	}
	return (ArrayList<ITypeHierarchyChangedListener>) listeners.clone();
}
private static byte[] flagsToBytes(Integer flags){
	if(flags != null) {
		return flags.toString().getBytes();
	} else {
		return NO_FLAGS;
	}
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType[] getAllClasses() {

	TypeVector classes = this.rootClasses.copy();
	for (Iterator<IType> iter = this.classToSuperclass.keySet().iterator(); iter.hasNext();){
		classes.add(iter.next());
	}
	return classes.elements();
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType[] getAllInterfaces() {
	IType[] collection= new IType[this.interfaces.size()];
	this.interfaces.toArray(collection);
	return collection;
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType[]  getAllSubtypes(IType type) {
	return getAllSubtypesForType(type);
}
/**
 * @see #getAllSubtypes(IType)
 */
private IType[] getAllSubtypesForType(IType type) {
	ArrayList<IType> subTypes = new ArrayList<>();
	getAllSubtypesForType0(type, subTypes);
	IType[] subClasses = new IType[subTypes.size()];
	subTypes.toArray(subClasses);
	return subClasses;
}
/**
 */
private void getAllSubtypesForType0(IType type, ArrayList<IType> subs) {
	IType[] subTypes = getSubtypesForType(type);
	if (subTypes.length != 0) {
		for (int i = 0; i < subTypes.length; i++) {
			IType subType = subTypes[i];
			if (subs.contains(subType)) continue;
			subs.add(subType);
			getAllSubtypesForType0(subType, subs);
		}
	}
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType[] getAllSuperclasses(IType type) {
	IType superclass = getSuperclass(type);
	TypeVector supers = new TypeVector();
	while (superclass != null) {
		supers.add(superclass);
		superclass = getSuperclass(superclass);
	}
	return supers.elements();
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType[] getAllSuperInterfaces(IType type) {
	ArrayList<IType> supers = getAllSuperInterfaces0(type, null);
	if (supers == null)
		return NO_TYPE;
	IType[] superinterfaces = new IType[supers.size()];
	supers.toArray(superinterfaces);
	return superinterfaces;
}
private ArrayList<IType> getAllSuperInterfaces0(IType type, ArrayList<IType> supers) {
	IType[] superinterfaces = this.typeToSuperInterfaces.get(type);
	if (superinterfaces == null) // type is not part of the hierarchy
		return supers;
	if (superinterfaces.length != 0) {
		if (supers == null)
			supers = new ArrayList<IType>();
		for (int i1 = 0; i1 < superinterfaces.length; i1++) {
			IType element = superinterfaces[i1];
			if (supers.contains(element)) continue;
			supers.add(element);
			supers = getAllSuperInterfaces0(element, supers);
		}
	}
	IType superclass = this.classToSuperclass.get(type);
	if (superclass != null) {
		supers = getAllSuperInterfaces0(superclass, supers);
	}
	return supers;
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType[] getAllSupertypes(IType type) {
	ArrayList<IType> supers = getAllSupertypes0(type, null);
	if (supers == null)
		return NO_TYPE;
	IType[] supertypes = new IType[supers.size()];
	supers.toArray(supertypes);
	return supertypes;
}
private ArrayList<IType> getAllSupertypes0(IType type, ArrayList<IType> supers) {
	IType[] superinterfaces = this.typeToSuperInterfaces.get(type);
	if (superinterfaces == null) // type is not part of the hierarchy
		return supers;
	if (superinterfaces.length != 0) {
		if (supers == null)
			supers = new ArrayList<IType>();
		for (int i1 = 0; i1 < superinterfaces.length; i1++) {
			IType element = superinterfaces[i1];
			if (!supers.contains(element)) {
				supers.add(element);
				supers = getAllSuperInterfaces0(element, supers);
			}
		}
	}
	IType superclass = this.classToSuperclass.get(type);
	if (superclass != null) {
		if (supers == null)
			supers = new ArrayList<>();
		supers.add(superclass);
		supers = getAllSupertypes0(superclass, supers);
	}
	return supers;
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType[] getAllTypes() {
	IType[] classes = getAllClasses();
	int classesLength = classes.length;
	IType[] allInterfaces = getAllInterfaces();
	int interfacesLength = allInterfaces.length;
	IType[] all = new IType[classesLength + interfacesLength];
	System.arraycopy(classes, 0, all, 0, classesLength);
	System.arraycopy(allInterfaces, 0, all, classesLength, interfacesLength);
	return all;
}

/**
 * @see ITypeHierarchy#getCachedFlags(IType)
 */
@Override
public int getCachedFlags(IType type) {
	Integer flagObject = this.typeFlags.get(type);
	if (flagObject != null){
		return flagObject.intValue();
	}
	return -1;
}

/**
 * @see ITypeHierarchy
 */
@Override
public IType[] getExtendingInterfaces(IType type) {
	if (!isInterface(type)) return NO_TYPE;
	return getExtendingInterfaces0(type);
}
/**
 * Assumes that the type is an interface
 * @see #getExtendingInterfaces
 */
private IType[] getExtendingInterfaces0(IType extendedInterface) {
	Iterator<Entry<IType, IType[]>> iter = this.typeToSuperInterfaces.entrySet().iterator();
	ArrayList<IType> interfaceList = new ArrayList<>();
	while (iter.hasNext()) {
		Map.Entry<IType, IType[]> entry = iter.next();
		IType type = entry.getKey();
		if (!isInterface(type)) {
			continue;
		}
		IType[] superInterfaces = entry.getValue();
		if (superInterfaces != null) {
			for (int i = 0; i < superInterfaces.length; i++) {
				IType superInterface = superInterfaces[i];
				if (superInterface.equals(extendedInterface)) {
					interfaceList.add(type);
				}
			}
		}
	}
	IType[] extendingInterfaces = new IType[interfaceList.size()];
	interfaceList.toArray(extendingInterfaces);
	return extendingInterfaces;
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType[] getImplementingClasses(IType type) {
	if (!isInterface(type)) {
		return NO_TYPE;
	}
	return getImplementingClasses0(type);
}
/**
 * Assumes that the type is an interface
 * @see #getImplementingClasses
 */
private IType[] getImplementingClasses0(IType interfce) {

	Iterator<Map.Entry<IType,IType[]>> iter = this.typeToSuperInterfaces.entrySet().iterator();
	ArrayList<IType> iMenters = new ArrayList<>();
	while (iter.hasNext()) {
		Map.Entry<IType, IType[]> entry = iter.next();
		IType type = entry.getKey();
		if (isInterface(type)) {
			continue;
		}
		IType[] types = entry.getValue();
		for (int i = 0; i < types.length; i++) {
			IType iFace = types[i];
			if (iFace.equals(interfce)) {
				iMenters.add(type);
			}
		}
	}
	IType[] implementers = new IType[iMenters.size()];
	iMenters.toArray(implementers);
	return implementers;
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType[] getRootClasses() {
	return this.rootClasses.elements();
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType[] getRootInterfaces() {
	IType[] allInterfaces = getAllInterfaces();
	IType[] roots = new IType[allInterfaces.length];
	int rootNumber = 0;
	for (int i = 0; i < allInterfaces.length; i++) {
		IType[] superInterfaces = getSuperInterfaces(allInterfaces[i]);
		if (superInterfaces == null || superInterfaces.length == 0) {
			roots[rootNumber++] = allInterfaces[i];
		}
	}
	IType[] result = new IType[rootNumber];
	if (result.length > 0) {
		System.arraycopy(roots, 0, result, 0, rootNumber);
	}
	return result;
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType[] getSubclasses(IType type) {
	if (isInterface(type)) {
		return NO_TYPE;
	}
	TypeVector vector = this.typeToSubtypes.get(type);
	if (vector == null)
		return NO_TYPE;
	else
		return vector.elements();
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType[] getSubtypes(IType type) {
	return getSubtypesForType(type);
}
/**
 * Returns an array of subtypes for the given type - will never return null.
 */
private IType[] getSubtypesForType(IType type) {
	TypeVector vector = this.typeToSubtypes.get(type);
	if (vector == null)
		return NO_TYPE;
	else
		return vector.elements();
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType getSuperclass(IType type) {
	if (isInterface(type)) {
		return null;
	}
	return this.classToSuperclass.get(type);
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType[] getSuperInterfaces(IType type) {
	IType[] types = this.typeToSuperInterfaces.get(type);
	if (types == null) {
		return NO_TYPE;
	}
	return types;
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType[] getSupertypes(IType type) {
	IType superclass = getSuperclass(type);
	if (superclass == null) {
		return getSuperInterfaces(type);
	} else {
		TypeVector superTypes = new TypeVector(getSuperInterfaces(type));
		superTypes.add(superclass);
		return superTypes.elements();
	}
}
/**
 * @see ITypeHierarchy
 */
@Override
public IType getType() {
	return this.focusType;
}
/**
 * Adds the new elements to a new array that contains all of the elements of the old array.
 * Returns the new array.
 */
protected IType[] growAndAddToArray(IType[] array, IType[] additions) {
	if (array == null || array.length == 0) {
		return additions;
	}
	IType[] old = array;
	array = new IType[old.length + additions.length];
	System.arraycopy(old, 0, array, 0, old.length);
	System.arraycopy(additions, 0, array, old.length, additions.length);
	return array;
}
/**
 * Adds the new element to a new array that contains all of the elements of the old array.
 * Returns the new array.
 */
protected IType[] growAndAddToArray(IType[] array, IType addition) {
	if (array == null || array.length == 0) {
		return new IType[] {addition};
	}
	IType[] old = array;
	array = new IType[old.length + 1];
	System.arraycopy(old, 0, array, 0, old.length);
	array[old.length] = addition;
	return array;
}
/*
 * Whether fine-grained deltas where collected and affects this hierarchy.
 */
public boolean hasFineGrainChanges() {
    ChangeCollector collector = this.changeCollector;
	return collector != null && collector.needsRefresh();
}
/**
 * Returns whether this type or one of the subtypes in this hierarchy has the
 * same simple name as the given name.
 */
private boolean hasSubtypeNamed(String name) {
	int idx = -1;
	String rawName = (idx = name.indexOf('<')) > -1 ? name.substring(0, idx) : name;
	String simpleName = (idx = rawName.lastIndexOf('.')) > -1 ? rawName.substring(idx + 1) : rawName;

	if (this.focusType != null && this.focusType.getElementName().equals(simpleName)) {
		return true;
	}
	IType[] types = this.focusType == null ? getAllTypes() : getAllSubtypes(this.focusType);
	for (int i = 0, length = types.length; i < length; i++) {
		if (types[i].getElementName().equals(simpleName)) {
			return true;
		}
	}
	return false;
}

/**
 * Returns whether one of the types in this hierarchy has the given simple name.
 */
private boolean hasTypeNamed(String simpleName) {
	IType[] types = getAllTypes();
	for (int i = 0, length = types.length; i < length; i++) {
		if (types[i].getElementName().equals(simpleName)) {
			return true;
		}
	}
	return false;
}

/**
 * Returns whether the simple name of the given type or one of its supertypes is
 * the simple name of one of the types in this hierarchy.
 */
boolean includesTypeOrSupertype(IType type) {
	try {
		// check type
		if (hasTypeNamed(type.getElementName())) return true;

		// check superclass
		String superclassName = type.getSuperclassName();
		if (superclassName != null) {
			int lastSeparator = superclassName.lastIndexOf('.');
			String simpleName = superclassName.substring(lastSeparator+1);
			if (hasTypeNamed(simpleName)) return true;
		}

		// check superinterfaces
		String[] superinterfaceNames = type.getSuperInterfaceNames();
		if (superinterfaceNames != null) {
			for (int i = 0, length = superinterfaceNames.length; i < length; i++) {
				String superinterfaceName = superinterfaceNames[i];
				int lastSeparator = superinterfaceName.lastIndexOf('.');
				String simpleName = superinterfaceName.substring(lastSeparator+1);
				if (hasTypeNamed(simpleName)) return true;
			}
		}
	} catch (JavaModelException e) {
		// ignore
	}
	return false;
}
/**
 * Initializes this hierarchy's internal tables with the given size.
 */
protected void initialize(int size) {
	if (size < 10) {
		size = 10;
	}
	int smallSize = (size / 2);
	this.classToSuperclass = new HashMap<>(size);
	this.interfaces = new ArrayList<>(smallSize);
	this.missingTypes = new ArrayList<>(smallSize);
	this.rootClasses = new TypeVector();
	this.typeToSubtypes = new HashMap<>(smallSize);
	this.typeToSuperInterfaces = new HashMap<>(smallSize);
	this.typeFlags = new HashMap<>(smallSize);

	this.projectRegion = new Region();
	this.packageRegion = new Region();
	this.files = new HashMap<>(5);
}
/**
 * Returns true if the given delta could change this type hierarchy
 * @param eventType TODO
 */
public synchronized boolean isAffected(IJavaElementDelta delta, int eventType) {
	IJavaElement element= delta.getElement();
	switch (element.getElementType()) {
		case IJavaElement.JAVA_MODEL:
			return isAffectedByJavaModel(delta, element, eventType);
		case IJavaElement.JAVA_PROJECT:
			return isAffectedByJavaProject(delta, element, eventType);
		case IJavaElement.PACKAGE_FRAGMENT_ROOT:
			return isAffectedByPackageFragmentRoot(delta, element, eventType);
		case IJavaElement.PACKAGE_FRAGMENT:
			return isAffectedByPackageFragment(delta, (PackageFragment) element, eventType);
		case IJavaElement.CLASS_FILE:
		case IJavaElement.COMPILATION_UNIT:
			return isAffectedByOpenable(delta, element, eventType);
	}
	return false;
}
/**
 * Returns true if any of the children of a project, package
 * fragment root, or package fragment have changed in a way that
 * affects this type hierarchy.
 * @param eventType TODO
 */
private boolean isAffectedByChildren(IJavaElementDelta delta, int eventType) {
	if ((delta.getFlags() & IJavaElementDelta.F_CHILDREN) > 0) {
		IJavaElementDelta[] children= delta.getAffectedChildren();
		for (int i= 0; i < children.length; i++) {
			if (isAffected(children[i], eventType)) {
				return true;
			}
		}
	}
	return false;
}
/**
 * Returns true if the given java model delta could affect this type hierarchy
 * @param eventType TODO
 */
private boolean isAffectedByJavaModel(IJavaElementDelta delta, IJavaElement element, int eventType) {
	switch (delta.getKind()) {
		case IJavaElementDelta.ADDED :
		case IJavaElementDelta.REMOVED :
			return element.equals(javaProject().getJavaModel());
		case IJavaElementDelta.CHANGED :
			return isAffectedByChildren(delta, eventType);
	}
	return false;
}
/**
 * Returns true if the given java project delta could affect this type hierarchy
 * @param eventType TODO
 */
private boolean isAffectedByJavaProject(IJavaElementDelta delta, IJavaElement element, int eventType) {
    int kind = delta.getKind();
    int flags = delta.getFlags();
    if ((flags & IJavaElementDelta.F_OPENED) != 0) {
        kind = IJavaElementDelta.ADDED; // affected in the same way
    }
    if ((flags & IJavaElementDelta.F_CLOSED) != 0) {
        kind = IJavaElementDelta.REMOVED; // affected in the same way
    }
	switch (kind) {
		case IJavaElementDelta.ADDED :
			try {
				// if the added project is on the classpath, then the hierarchy has changed
				IClasspathEntry[] classpath = ((JavaProject)javaProject()).getExpandedClasspath();
				for (int i = 0; i < classpath.length; i++) {
					if (classpath[i].getEntryKind() == IClasspathEntry.CPE_PROJECT
							&& classpath[i].getPath().equals(element.getPath())) {
						return true;
					}
				}
				if (this.focusType != null) {
					// if the hierarchy's project is on the added project classpath, then the hierarchy has changed
					classpath = ((JavaProject)element).getExpandedClasspath();
					IPath hierarchyProject = javaProject().getPath();
					for (int i = 0; i < classpath.length; i++) {
						if (classpath[i].getEntryKind() == IClasspathEntry.CPE_PROJECT
								&& classpath[i].getPath().equals(hierarchyProject)) {
							return true;
						}
					}
				}
				return false;
			} catch (JavaModelException e) {
				return false;
			}
		case IJavaElementDelta.REMOVED :
			// removed project - if it contains packages we are interested in
			// then the type hierarchy has changed
			IJavaElement[] pkgs = this.packageRegion.getElements();
			for (int i = 0; i < pkgs.length; i++) {
				IJavaProject javaProject = pkgs[i].getJavaProject();
				if (javaProject != null && javaProject.equals(element)) {
					return true;
				}
			}
			return false;
		case IJavaElementDelta.CHANGED :
			return isAffectedByChildren(delta, eventType);
	}
	return false;
}
/**
 * Returns true if the given package fragment delta could affect this type hierarchy
 * @param eventType TODO
 */
private boolean isAffectedByPackageFragment(IJavaElementDelta delta, PackageFragment element, int eventType) {
	switch (delta.getKind()) {
		case IJavaElementDelta.ADDED :
			// if the package fragment is in the projects being considered, this could
			// introduce new types, changing the hierarchy
			return this.projectRegion.contains(element);
		case IJavaElementDelta.REMOVED :
			// is a change if the package fragment contains types in this hierarchy
			return packageRegionContainsSamePackageFragment(element);
		case IJavaElementDelta.CHANGED :
			// look at the files in the package fragment
			return isAffectedByChildren(delta, eventType);
	}
	return false;
}
/**
 * Returns true if the given package fragment root delta could affect this type hierarchy
 * @param eventType TODO
 */
private boolean isAffectedByPackageFragmentRoot(IJavaElementDelta delta, IJavaElement element, int eventType) {
	switch (delta.getKind()) {
		case IJavaElementDelta.ADDED :
			return this.projectRegion.contains(element);
		case IJavaElementDelta.REMOVED :
		case IJavaElementDelta.CHANGED :
			int flags = delta.getFlags();
			if ((flags & IJavaElementDelta.F_ADDED_TO_CLASSPATH) > 0) {
				// check if the root is in the classpath of one of the projects of this hierarchy
				if (this.projectRegion != null) {
					IPackageFragmentRoot root = (IPackageFragmentRoot)element;
					IPath rootPath = root.getPath();
					IJavaElement[] elements = this.projectRegion.getElements();
					for (int i = 0; i < elements.length; i++) {
						JavaProject javaProject = (JavaProject)elements[i];
						try {
							IClasspathEntry entry = javaProject.getClasspathEntryFor(rootPath);
							if (entry != null) {
								return true;
							}
						} catch (JavaModelException e) {
							// igmore this project
						}
					}
				}
			}
			if ((flags & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH) > 0 || (flags & IJavaElementDelta.F_ARCHIVE_CONTENT_CHANGED) > 0) {
				// 1. removed from classpath - if it contains packages we are interested in
				// the the type hierarchy has changed
				// 2. content of a jar changed - if it contains packages we are interested in
				// then the type hierarchy has changed
				IJavaElement[] pkgs = this.packageRegion.getElements();
				for (int i = 0; i < pkgs.length; i++) {
					if (pkgs[i].getParent().equals(element)) {
						return true;
					}
				}
				return false;
			}
	}
	return isAffectedByChildren(delta, eventType);
}
/**
 * Returns true if the given type delta (a compilation unit delta or a class file delta)
 * could affect this type hierarchy.
 * @param eventType TODO
 */
protected boolean isAffectedByOpenable(IJavaElementDelta delta, IJavaElement element, int eventType) {
	if (element instanceof CompilationUnit) {
		CompilationUnit cu = (CompilationUnit)element;
		ICompilationUnit focusCU =
			this.focusType != null ? this.focusType.getCompilationUnit() : null;
		if (focusCU != null && focusCU.getOwner() != cu.getOwner())
			return false;
		//ADDED delta arising from getWorkingCopy() should be ignored
		if (eventType != ElementChangedEvent.POST_RECONCILE && !cu.isPrimary() &&
				delta.getKind() == IJavaElementDelta.ADDED)
			return false;
		ChangeCollector collector = this.changeCollector;
		if (collector == null) {
		    collector = new ChangeCollector(this);
		}
		try {
			collector.addChange(cu, delta);
		} catch (JavaModelException e) {
			if (DEBUG)
				e.printStackTrace();
		}
		if (cu.isWorkingCopy() && eventType == ElementChangedEvent.POST_RECONCILE) {
			// changes to working copies are batched
			this.changeCollector = collector;
			return false;
		} else {
			return collector.needsRefresh();
		}
	} else if (element instanceof ClassFile) {
		switch (delta.getKind()) {
			case IJavaElementDelta.REMOVED:
				IOpenable o = (IOpenable) element;
				return this.files.get(o) != null;
			case IJavaElementDelta.ADDED:
				IType type = ((ClassFile)element).getType();
				String typeName = type.getElementName();
				if (hasSupertype(typeName)
					|| subtypesIncludeSupertypeOf(type)
					|| this.missingTypes.contains(typeName)) {

					return true;
				}
				break;
			case IJavaElementDelta.CHANGED:
				IJavaElementDelta[] children = delta.getAffectedChildren();
				for (int i = 0, length = children.length; i < length; i++) {
					IJavaElementDelta child = children[i];
					IJavaElement childElement = child.getElement();
					if (childElement instanceof IType) {
						type = (IType)childElement;
						boolean hasVisibilityChange = (delta.getFlags() & IJavaElementDelta.F_MODIFIERS) > 0;
						boolean hasSupertypeChange = (delta.getFlags() & IJavaElementDelta.F_SUPER_TYPES) > 0;
						if ((hasVisibilityChange && hasSupertype(type.getElementName()))
								|| (hasSupertypeChange && includesTypeOrSupertype(type))) {
							return true;
						}
					}
				}
				break;
		}
	}
	return false;
}
private boolean isInterface(IType type) {
	int flags = getCachedFlags(type);
	if (flags == -1) {
		try {
			return type.isInterface();
		} catch (JavaModelException e) {
			return false;
		}
	} else {
		return Flags.isInterface(flags);
	}
}
/**
 * Returns the java project this hierarchy was created in.
 */
public IJavaProject javaProject() {
	return this.focusType.getJavaProject();
}
protected static byte[] readUntil(InputStream input, byte separator) throws JavaModelException, IOException{
	return readUntil(input, separator, 0);
}
protected static byte[] readUntil(InputStream input, byte separator, int offset) throws IOException, JavaModelException{
	int length = 0;
	byte[] bytes = new byte[SIZE];
	byte b;
	while((b = (byte)input.read()) != separator && b != -1) {
		if(bytes.length == length) {
			System.arraycopy(bytes, 0, bytes = new byte[length*2], 0, length);
		}
		bytes[length++] = b;
	}
	if(b == -1) {
		throw new JavaModelException(new JavaModelStatus(IStatus.ERROR));
	}
	System.arraycopy(bytes, 0, bytes = new byte[length + offset], offset, length);
	return bytes;
}
public static ITypeHierarchy load(IType type, InputStream input, WorkingCopyOwner owner) throws JavaModelException {
	try {
		TypeHierarchy typeHierarchy = new TypeHierarchy();
		typeHierarchy.initialize(1);

		IType[] types = new IType[SIZE];
		int typeCount = 0;

		byte version = (byte)input.read();

		if(version != VERSION) {
			throw new JavaModelException(new JavaModelStatus(IStatus.ERROR));
		}
		byte generalInfo = (byte)input.read();
		if((generalInfo & COMPUTE_SUBTYPES) != 0) {
			typeHierarchy.computeSubtypes = true;
		}

		byte b;
		byte[] bytes;

		// read project
		bytes = readUntil(input, SEPARATOR1);
		if(bytes.length > 0) {
			typeHierarchy.project = (IJavaProject)JavaCore.create(new String(bytes));
			typeHierarchy.scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {typeHierarchy.project});
		} else {
			typeHierarchy.project = null;
			typeHierarchy.scope = SearchEngine.createWorkspaceScope();
		}

		// read missing type
		{
			bytes = readUntil(input, SEPARATOR1);
			byte[] missing;
			int j = 0;
			int length = bytes.length;
			for (int i = 0; i < length; i++) {
				b = bytes[i];
				if(b == SEPARATOR2) {
					missing = new byte[i - j];
					System.arraycopy(bytes, j, missing, 0, i - j);
					typeHierarchy.missingTypes.add(new String(missing));
					j = i + 1;
				}
			}
			System.arraycopy(bytes, j, missing = new byte[length - j], 0, length - j);
			typeHierarchy.missingTypes.add(new String(missing));
		}

		// read types
		while((b = (byte)input.read()) != SEPARATOR1 && b != -1) {
			bytes = readUntil(input, SEPARATOR4, 1);
			bytes[0] = b;
			IType element = (IType)JavaCore.create(new String(bytes), owner);

			if(types.length == typeCount) {
				System.arraycopy(types, 0, types = new IType[typeCount * 2], 0, typeCount);
			}
			types[typeCount++] = element;

			// read flags
			bytes = readUntil(input, SEPARATOR4);
			Integer flags = bytesToFlags(bytes);
			if(flags != null) {
				typeHierarchy.cacheFlags(element, flags.intValue());
			}

			// read info
			byte info = (byte)input.read();

			if((info & INTERFACE) != 0) {
				typeHierarchy.addInterface(element);
			}
			if((info & COMPUTED_FOR) != 0) {
				if(!element.equals(type)) {
					throw new JavaModelException(new JavaModelStatus(IStatus.ERROR));
				}
				typeHierarchy.focusType = element;
			}
			if((info & ROOT) != 0) {
				typeHierarchy.addRootClass(element);
			}
		}

		// read super class
		while((b = (byte)input.read()) != SEPARATOR1 && b != -1) {
			bytes = readUntil(input, SEPARATOR3, 1);
			bytes[0] = b;
			int subClass = Integer.parseInt(new String(bytes));

			// read super type
			bytes = readUntil(input, SEPARATOR1);
			int superClass = Integer.parseInt(new String(bytes));

			typeHierarchy.cacheSuperclass(
				types[subClass],
				types[superClass]);
		}

		// read super interface
		while((b = (byte)input.read()) != SEPARATOR1 && b != -1) {
			bytes = readUntil(input, SEPARATOR3, 1);
			bytes[0] = b;
			int subClass = Integer.parseInt(new String(bytes));

			// read super interface
			bytes = readUntil(input, SEPARATOR1);
			IType[] superInterfaces = new IType[(bytes.length / 2) + 1];
			int interfaceCount = 0;

			int j = 0;
			byte[] b2;
			for (int i = 0; i < bytes.length; i++) {
				if(bytes[i] == SEPARATOR2){
					b2 = new byte[i - j];
					System.arraycopy(bytes, j, b2, 0, i - j);
					j = i + 1;
					superInterfaces[interfaceCount++] = types[Integer.parseInt(new String(b2))];
				}
			}
			b2 = new byte[bytes.length - j];
			System.arraycopy(bytes, j, b2, 0, bytes.length - j);
			superInterfaces[interfaceCount++] = types[Integer.parseInt(new String(b2))];
			System.arraycopy(superInterfaces, 0, superInterfaces = new IType[interfaceCount], 0, interfaceCount);

			typeHierarchy.cacheSuperInterfaces(
				types[subClass],
				superInterfaces);
		}
		if(b == -1) {
			throw new JavaModelException(new JavaModelStatus(IStatus.ERROR));
		}
		return typeHierarchy;
	} catch(IOException e){
		throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
	}
}
/**
 * Returns <code>true</code> if an equivalent package fragment is included in the package
 * region. Package fragments are equivalent if they both have the same name.
 */
protected boolean packageRegionContainsSamePackageFragment(PackageFragment element) {
	IJavaElement[] pkgs = this.packageRegion.getElements();
	for (int i = 0; i < pkgs.length; i++) {
		PackageFragment pkg = (PackageFragment) pkgs[i];
		if (Util.equalArraysOrNull(pkg.names, element.names))
			return true;
	}
	return false;
}

/**
 * @see ITypeHierarchy
 * TODO (jerome) should use a PerThreadObject to build the hierarchy instead of synchronizing
 * (see also isAffected(IJavaElementDelta))
 */
@Override
public synchronized void refresh(IProgressMonitor monitor) throws JavaModelException {
	try {
		this.progressMonitor = SubMonitor.convert(monitor,
			this.focusType != null ?
					Messages.bind(Messages.hierarchy_creatingOnType, this.focusType.getFullyQualifiedName()) :
					Messages.hierarchy_creating,
			100);
		long start = -1;
		if (DEBUG) {
			start = System.currentTimeMillis();
			if (this.computeSubtypes) {
				System.out.println("CREATING TYPE HIERARCHY [" + Thread.currentThread() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				System.out.println("CREATING SUPER TYPE HIERARCHY [" + Thread.currentThread() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (this.focusType != null) {
				System.out.println("  on type " + ((JavaElement)this.focusType).toStringWithAncestors()); //$NON-NLS-1$
			}
		}

		compute();
		initializeRegions();
		this.needsRefresh = false;
		this.changeCollector = null;

		if (DEBUG) {
			if (this.computeSubtypes) {
				System.out.println("CREATED TYPE HIERARCHY in " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				System.out.println("CREATED SUPER TYPE HIERARCHY in " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			System.out.println(this.toString());
		}
	} catch (JavaModelException e) {
		throw e;
	} catch (CoreException e) {
		throw new JavaModelException(e);
	} finally {
		if (monitor != null) {
			monitor.done();
		}
		this.progressMonitor = null;
	}
}

/**
 * @see ITypeHierarchy
 */
@Override
public synchronized void removeTypeHierarchyChangedListener(ITypeHierarchyChangedListener listener) {
	ArrayList<ITypeHierarchyChangedListener> listeners = this.changeListeners;
	if (listeners == null) {
		return;
	}
	listeners.remove(listener);

	// deregister from JavaCore on last listener removed
	if (listeners.isEmpty()) {
		JavaCore.removeElementChangedListener(this);
	}
}
/**
 * @see ITypeHierarchy
 */
@Override
@SuppressWarnings("unchecked")
public void store(OutputStream output, IProgressMonitor monitor) throws JavaModelException {
	try {
		// compute types in hierarchy
		Hashtable<IType, Integer> hashtable = new Hashtable<>();
		Hashtable<Integer, IType> hashtable2 = new Hashtable<>();
		int count = 0;

		if(this.focusType != null) {
			Integer index = Integer.valueOf(count++);
			hashtable.put(this.focusType, index);
			hashtable2.put(index, this.focusType);
		}
		Object[] types = this.classToSuperclass.entrySet().toArray();
		for (int i = 0; i < types.length; i++) {
			Map.Entry<IType, IType> entry = (Map.Entry<IType, IType>) types[i];
			IType t = entry.getKey();
			if(hashtable.get(t) == null) {
				Integer index = Integer.valueOf(count++);
				hashtable.put(t, index);
				hashtable2.put(index, t);
			}
			IType superClass = entry.getValue();
			if(superClass != null && hashtable.get(superClass) == null) {
				Integer index = Integer.valueOf(count++);
				hashtable.put(superClass, index);
				hashtable2.put(index, superClass);
			}
		}
		Object[] intfs = this.typeToSuperInterfaces.entrySet().toArray();
		for (int i = 0; i < intfs.length; i++) {
			Map.Entry<IType, IType[]> entry = (Map.Entry<IType, IType[]>) intfs[i];
			IType t = entry.getKey();
			if(hashtable.get(t) == null) {
				Integer index = Integer.valueOf(count++);
				hashtable.put(t, index);
				hashtable2.put(index, t);
			}
			IType[] sp = entry.getValue();
			if(sp != null) {
				for (int j = 0; j < sp.length; j++) {
					IType superInterface = sp[j];
					if(sp[j] != null && hashtable.get(superInterface) == null) {
						Integer index = Integer.valueOf(count++);
						hashtable.put(superInterface, index);
						hashtable2.put(index, superInterface);
					}
				}
			}
		}
		// save version of the hierarchy format
		output.write(VERSION);

		// save general info
		byte generalInfo = 0;
		if(this.computeSubtypes) {
			generalInfo |= COMPUTE_SUBTYPES;
		}
		output.write(generalInfo);

		// save project
		if(this.project != null) {
			output.write(this.project.getHandleIdentifier().getBytes());
		}
		output.write(SEPARATOR1);

		// save missing types
		for (int i = 0; i < this.missingTypes.size(); i++) {
			if(i != 0) {
				output.write(SEPARATOR2);
			}
			output.write((this.missingTypes.get(i)).getBytes());

		}
		output.write(SEPARATOR1);

		// save types
		for (int i = 0; i < count ; i++) {
			IType t = hashtable2.get(Integer.valueOf(i));

			// n bytes
			output.write(t.getHandleIdentifier().getBytes());
			output.write(SEPARATOR4);
			output.write(flagsToBytes(this.typeFlags.get(t)));
			output.write(SEPARATOR4);
			byte info = CLASS;
			if(this.focusType != null && this.focusType.equals(t)) {
				info |= COMPUTED_FOR;
			}
			if(this.interfaces.contains(t)) {
				info |= INTERFACE;
			}
			if(this.rootClasses.contains(t)) {
				info |= ROOT;
			}
			output.write(info);
		}
		output.write(SEPARATOR1);

		// save superclasses
		types = this.classToSuperclass.entrySet().toArray();
		for (int i = 0; i < types.length; i++) {
			Map.Entry<IType, IType> entry = (Map.Entry<IType, IType>) types[i];
			IJavaElement key = entry.getKey();
			IJavaElement value = entry.getValue();

			output.write(hashtable.get(key).toString().getBytes());
			output.write('>');
			output.write(hashtable.get(value).toString().getBytes());
			output.write(SEPARATOR1);
		}
		output.write(SEPARATOR1);

		// save superinterfaces
		intfs = this.typeToSuperInterfaces.entrySet().toArray();
		for (int i = 0; i < intfs.length; i++) {
			Map.Entry<IType, IType[]> entry = (Map.Entry<IType, IType[]>) intfs[i];
			IJavaElement key = entry.getKey();
			IJavaElement[] values = entry.getValue();

			if(values.length > 0) {
				output.write(hashtable.get(key).toString().getBytes());
				output.write(SEPARATOR3);
				for (int j = 0; j < values.length; j++) {
					IJavaElement value = values[j];
					if(j != 0) output.write(SEPARATOR2);
					output.write(hashtable.get(value).toString().getBytes());
				}
				output.write(SEPARATOR1);
			}
		}
		output.write(SEPARATOR1);
	} catch(IOException e) {
		throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
	}
}
/**
 * Returns whether the simple name of a supertype of the given type is
 * the simple name of one of the subtypes in this hierarchy or the
 * simple name of this type.
 */
boolean subtypesIncludeSupertypeOf(IType type) {
	// look for superclass
	String superclassName = null;
	try {
		superclassName = type.getSuperclassName();
	} catch (JavaModelException e) {
		if (DEBUG) {
			e.printStackTrace();
		}
		return false;
	}
	if (superclassName == null) {
		superclassName = "Object"; //$NON-NLS-1$
	}
	if (hasSubtypeNamed(superclassName)) {
		return true;
	}

	// look for super interfaces
	String[] interfaceNames = null;
	try {
		interfaceNames = type.getSuperInterfaceNames();
	} catch (JavaModelException e) {
		if (DEBUG)
			e.printStackTrace();
		return false;
	}
	for (int i = 0, length = interfaceNames.length; i < length; i++) {
		String interfaceName = interfaceNames[i];
		if (hasSubtypeNamed(interfaceName)) {
			return true;
		}
	}

	return false;
}
/**
 * @see ITypeHierarchy
 */
@Override
public String toString() {
	StringBuffer buffer = new StringBuffer();
	buffer.append("Focus: "); //$NON-NLS-1$
	if (this.focusType == null) {
		buffer.append("<NONE>\n"); //$NON-NLS-1$
	} else {
		toString(buffer, this.focusType, 0);
	}
	if (exists()) {
		if (this.focusType != null) {
			buffer.append("Super types:\n"); //$NON-NLS-1$
			toString(buffer, this.focusType, 0, true);
			buffer.append("Sub types:\n"); //$NON-NLS-1$
			toString(buffer, this.focusType, 0, false);
		} else {
			if (this.rootClasses.size > 0) {
				IJavaElement[] roots = Util.sortCopy(getRootClasses());
				buffer.append("Super types of root classes:\n"); //$NON-NLS-1$
				int length = roots.length;
				for (int i = 0; i < length; i++) {
					IJavaElement root = roots[i];
					toString(buffer, root, 1);
					toString(buffer, root, 1, true);
				}
				buffer.append("Sub types of root classes:\n"); //$NON-NLS-1$
				for (int i = 0; i < length; i++) {
					IJavaElement root = roots[i];
					toString(buffer, root, 1);
					toString(buffer, root, 1, false);
				}
			} else if (this.rootClasses.size == 0) {
				// see http://bugs.eclipse.org/bugs/show_bug.cgi?id=24691
				buffer.append("No root classes"); //$NON-NLS-1$
			}
		}
	} else {
		buffer.append("(Hierarchy became stale)"); //$NON-NLS-1$
	}
	return buffer.toString();
}
/**
 * Append a String to the given buffer representing the hierarchy for the type,
 * beginning with the specified indentation level.
 * If ascendant, shows the super types, otherwise show the sub types.
 */
private void toString(StringBuffer buffer, IJavaElement type, int indent, boolean ascendant) {
	IType[] types= ascendant ? getSupertypes((IType) type) : getSubtypes((IType) type);
	IJavaElement[] sortedTypes = Util.sortCopy(types);
	for (int i= 0; i < sortedTypes.length; i++) {
		toString(buffer, sortedTypes[i], indent + 1);
		toString(buffer, sortedTypes[i], indent + 1, ascendant);
	}
}
private void toString(StringBuffer buffer, IJavaElement type, int indent) {
	for (int j= 0; j < indent; j++) {
		buffer.append("  "); //$NON-NLS-1$
	}
	buffer.append(((JavaElement) type).toStringWithAncestors(false/*don't show key*/));
	buffer.append('\n');
}
/**
 * Returns whether one of the types in this hierarchy has a supertype whose simple
 * name is the given simple name.
 */
boolean hasSupertype(String simpleName) {
	for(Iterator<IType> iter = this.classToSuperclass.values().iterator(); iter.hasNext();){
		IType superType = iter.next();
		if (superType.getElementName().equals(simpleName)) {
			return true;
		}
	}
	return false;
}
/**
 * @see IProgressMonitor
 */
protected void worked(int work) {
	if (this.progressMonitor != null) {
		this.progressMonitor.worked(work);
		checkCanceled();
	}
}
}
