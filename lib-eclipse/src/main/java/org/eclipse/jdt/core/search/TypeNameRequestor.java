/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.jdt.core.search;

/**
 * A <code>TypeNameRequestor</code> collects search results from a <code>searchAllTypeNames</code>
 * query to a <code>SearchEngine</code>. Clients must subclass this abstract class and pass
 * an instance to the <code>SearchEngine.searchAllTypeNames(...)</code> method. Only top-level and
 * member types are reported. Local types are not reported.
 * <p>
 * This class may be subclassed by clients.
 * </p>
 * @since 3.1
 */
public abstract class TypeNameRequestor {
	/**
	 * Accepts a top-level or a member type.
	 * <p>
	 * The default implementation of this method does nothing.
	 * Subclasses should override.
	 * </p>
	 *
	 * @param modifiers the modifier flags of the type. Note that for source type,
	 *		these flags may slightly differ from those get after resolution.
	 *		For example an interface defined by <code>interface A {}</code>,
	 *		although obviously public, will be returned false by <code>Flags.isPublic(modifiers)</code>
	 *		due to the fact that its declaration does not explicitly define public flag.
	 *		@see org.eclipse.jdt.core.Flags
	 * @param packageName the package name as specified in the package declaration (i.e. a dot-separated name)
	 * @param simpleTypeName the simple name of the type
	 * @param enclosingTypeNames if the type is a member type,
	 *          the simple names of the enclosing types from the outer-most to the
	 *          direct parent of the type (for example, if the class is x.y.A$B$C then
	 *          the enclosing types are [A, B]. This is an empty array if the type
	 *          is a top-level type.
	 * @param path the full path to the resource containing the type. If the resource is a .class file
	 *          or a source file, this is the full path in the workspace to this resource. If the
	 *          resource is an archive (that is, a .zip or .jar file), the path is composed of 2 paths separated
	 *		 	 by <code>IJavaSearchScope.JAR_FILE_ENTRY_SEPARATOR</code>:
	 *			 the first path is the full OS path to the archive (if it is an external archive),
	 *			 or the workspace relative <code>IPath</code> to the archive (if it is an internal archive),
	 * 		 the second path is the path to the resource inside the archive.
	 */
	public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
		// do nothing
	}
}
