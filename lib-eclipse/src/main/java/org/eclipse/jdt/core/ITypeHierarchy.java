/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.core;

import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A type hierarchy provides navigations between a type and its resolved
 * supertypes and subtypes for a specific type or for all types within a region.
 * Supertypes may extend outside of the type hierarchy's region in which it was
 * created such that the root of the hierarchy is always included. For example, if a type
 * hierarchy is created for a <code>java.io.File</code>, and the region the hierarchy was
 * created in is the package fragment <code>java.io</code>, the supertype
 * <code>java.lang.Object</code> will still be included. As a historical quirk,
 * <code>java.lang.Object</code> has always been included in type hierarchies
 * created on interface types.
 * <p>
 * A type hierarchy is static and can become stale. Although consistent when
 * created, it does not automatically track changes in the model.
 * As changes in the model potentially invalidate the hierarchy, change notifications
 * are sent to registered <code>ITypeHierarchyChangedListener</code>s. Listeners should
 * use the <code>exists</code> method to determine if the hierarchy has become completely
 * invalid (for example, when the type or project the hierarchy was created on
 * has been removed). To refresh a hierarchy, use the <code>refresh</code> method.
 * </p>
 * <p>
 * The type hierarchy may contain cycles due to malformed supertype declarations.
 * Most type hierarchy queries are oblivious to cycles; the <code>getAll* </code>
 * methods are implemented such that they are unaffected by cycles.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ITypeHierarchy {
/**
 * Adds the given listener for changes to this type hierarchy. Listeners are
 * notified when this type hierarchy changes and needs to be refreshed.
 * Has no effect if an identical listener is already registered.
 *
 * @param listener the listener
 */
void addTypeHierarchyChangedListener(ITypeHierarchyChangedListener listener);
/**
 * Returns whether the given type is part of this hierarchy.
 *
 * @param type the given type
 * @return true if the given type is part of this hierarchy, false otherwise
 */
boolean contains(IType type);
/**
 * Returns whether the type and project this hierarchy was created on exist.
 * @return true if the type and project this hierarchy was created on exist, false otherwise
 */
boolean exists();
/**
 * Returns all classes in this type hierarchy's graph, in no particular
 * order. Any classes in the creation region which were not resolved to
 * have any subtypes or supertypes are not included in the result.
 *
 * @return all classes in this type hierarchy's graph
 */
IType[] getAllClasses();
/**
 * Returns all interfaces in this type hierarchy's graph, in no particular
 * order. Any interfaces in the creation region which were not resolved to
 * have any subtypes or supertypes are not included in the result.
 *
 * @return all interfaces in this type hierarchy's graph
 */
IType[] getAllInterfaces();
/**
 * Returns all resolved subtypes (direct and indirect) of the
 * given type, in no particular order, limited to the
 * types in this type hierarchy's graph. An empty array
 * is returned if there are no resolved subtypes for the
 * given type.
 *
 * @param type the given type
 * @return all resolved subtypes (direct and indirect) of the given type
 */
IType[] getAllSubtypes(IType type);
/**
 * Returns all resolved superclasses of the
 * given class, in bottom-up order. An empty array
 * is returned if there are no resolved superclasses for the
 * given class.
 *
 * <p>NOTE: once a type hierarchy has been created, it is more efficient to
 * query the hierarchy for superclasses than to query a class recursively up
 * the superclass chain. Querying an element performs a dynamic resolution,
 * whereas the hierarchy returns a pre-computed result.
 *
 * @param type the given type
 * @return all resolved superclasses of the given class, in bottom-up order, an empty
 * array if none.
 */
IType[] getAllSuperclasses(IType type);
/**
 * Returns all resolved superinterfaces (direct and indirect) of the given type.
 * If the given type is a class, this includes all superinterfaces of all superclasses.
 * An empty array is returned if there are no resolved superinterfaces for the
 * given type.
 *
 * <p>NOTE: once a type hierarchy has been created, it is more efficient to
 * query the hierarchy for superinterfaces than to query a type recursively.
 * Querying an element performs a dynamic resolution,
 * whereas the hierarchy returns a pre-computed result.
 *
 * @param type the given type
 * @return all resolved superinterfaces (direct and indirect) of the given type, an empty array if none
 */
IType[] getAllSuperInterfaces(IType type);
/**
 * Returns all resolved supertypes of the
 * given type, in bottom-up order. An empty array
 * is returned if there are no resolved supertypes for the
 * given type.
 * <p>
 * Note that <code>java.lang.Object</code> is NOT considered to be a supertype
 * of any interface type.
 * </p><p>NOTE: once a type hierarchy has been created, it is more efficient to
 * query the hierarchy for supertypes than to query a type recursively up
 * the supertype chain. Querying an element performs a dynamic resolution,
 * whereas the hierarchy returns a pre-computed result.
 *
 * @param type the given type
 * @return all resolved supertypes of the given class, in bottom-up order, an empty array
 * if none
 */
IType[] getAllSupertypes(IType type);
/**
 * Returns all types in this type hierarchy's graph, in no particular
 * order. Any types in the creation region which were not resolved to
 * have any subtypes or supertypes are not included in the result.
 *
 * @return all types in this type hierarchy's graph
 */
IType[] getAllTypes();

/**
 * Return the flags associated with the given type (would be equivalent to <code>IMember.getFlags()</code>),
 * or <code>-1</code> if this information wasn't cached on the hierarchy during its computation.
 *
 * @param type the given type
 * @return the modifier flags for this member
 * @see Flags
 * @since 2.0
 */
int getCachedFlags(IType type);

/**
 * Returns all interfaces resolved to extend the given interface,
 * in no particular order, limited to the interfaces in this
 * hierarchy's graph.
 * Returns an empty collection if the given type is a class, or
 * if no interfaces were resolved to extend the given interface.
 *
 * @param type the given type
 * @return all interfaces resolved to extend the given interface limited to the interfaces in this
 * hierarchy's graph, an empty array if none.
 */
IType[] getExtendingInterfaces(IType type);
/**
 * Returns all classes resolved to implement the given interface,
 * in no particular order, limited to the classes in this type
 * hierarchy's  graph. Returns an empty collection if the given type is a
 * class, or if no classes were resolved to implement the given
 * interface.
 *
 * @param type the given type
 * @return all classes resolved to implement the given interface limited to the classes in this type
 * hierarchy's  graph, an empty array if none
 */
IType[] getImplementingClasses(IType type);
/**
 * Returns all classes in the graph which have no resolved superclass,
 * in no particular order.
 *
 * @return all classes in the graph which have no resolved superclass
 */
IType[] getRootClasses();
/**
 * Returns all interfaces in the graph which have no resolved superinterfaces,
 * in no particular order.
 *
 * @return all interfaces in the graph which have no resolved superinterfaces
 */
IType[] getRootInterfaces();
/**
 * Returns the direct resolved subclasses of the given class,
 * in no particular order, limited to the classes in this
 * type hierarchy's graph.
 * Returns an empty collection if the given type is an interface,
 * or if no classes were resolved to be subclasses of the given
 * class.
 *
 * @param type the given type
 * @return the direct resolved subclasses of the given class limited to the classes in this
 * type hierarchy's graph, an empty collection if none.
 */
IType[] getSubclasses(IType type);
/**
 * Returns the direct resolved subtypes of the given type,
 * in no particular order, limited to the types in this
 * type hierarchy's graph.
 * If the type is a class, this returns the resolved subclasses.
 * If the type is an interface, this returns both the classes which implement
 * the interface and the interfaces which extend it.
 *
 * @param type the given type
 * @return the direct resolved subtypes of the given type limited to the types in this
 * type hierarchy's graph
 */
IType[] getSubtypes(IType type);
/**
 * Returns the resolved superclass of the given class,
 * or <code>null</code> if the given class has no superclass,
 * the superclass could not be resolved, or if the given
 * type is an interface.
 *
 * @param type the given type
 * @return the resolved superclass of the given class,
 * or <code>null</code> if the given class has no superclass,
 * the superclass could not be resolved, or if the given
 * type is an interface
 */
IType getSuperclass(IType type);
/**
 * Returns the direct resolved interfaces that the given type implements or extends,
 * in no particular order, limited to the interfaces in this type
 * hierarchy's graph.
 * For classes, this gives the interfaces that the class implements.
 * For interfaces, this gives the interfaces that the interface extends.
 *
 * @param type the given type
 * @return the direct resolved interfaces that the given type implements or extends limited to the interfaces in this type
 * hierarchy's graph
 */
IType[] getSuperInterfaces(IType type);
/**
 * Returns the resolved supertypes of the given type,
 * in no particular order, limited to the types in this
 * type hierarchy's graph.
 * For classes, this returns its superclass and the interfaces that the class implements.
 * For interfaces, this returns the interfaces that the interface extends. As a consequence
 * <code>java.lang.Object</code> is NOT considered to be a supertype of any interface
 * type.
 *
 * @param type the given type
 * @return the resolved supertypes of the given type limited to the types in this
 * type hierarchy's graph
 */
IType[] getSupertypes(IType type);
/**
 * Returns the type this hierarchy was computed for.
 * Returns <code>null</code> if this hierarchy was computed for a region.
 *
 * @return the type this hierarchy was computed for
 */
IType getType();
/**
 * Re-computes the type hierarchy reporting progress.
 *
 * @param monitor the given progress monitor
 * @exception JavaModelException if unable to refresh the hierarchy
 */
void refresh(IProgressMonitor monitor) throws JavaModelException;
/**
 * Removes the given listener from this type hierarchy.
 * Has no effect if an identical listener is not registered.
 *
 * @param listener the listener
 */
void removeTypeHierarchyChangedListener(ITypeHierarchyChangedListener listener);
/**
 * Stores the type hierarchy in an output stream. This stored hierarchy can be load by
 * IType#loadTypeHierachy(IJavaProject, InputStream, IProgressMonitor).
 * Listeners of this hierarchy are not stored.
 *
 * Only hierarchies created by the following methods can be store:
 * <ul>
 * <li>IType#newSupertypeHierarchy(IProgressMonitor)</li>
 * <li>IType#newTypeHierarchy(IJavaProject, IProgressMonitor)</li>
 * <li>IType#newTypeHierarchy(IProgressMonitor)</li>
 * </ul>
 *
 * @param outputStream output stream where the hierarchy will be stored
 * @param monitor the given progress monitor
 * @exception JavaModelException if unable to store the hierarchy in the ouput stream
 * @see IType#loadTypeHierachy(java.io.InputStream, IProgressMonitor)
 * @since 2.1
 */
void store(OutputStream outputStream, IProgressMonitor monitor) throws JavaModelException;
}
