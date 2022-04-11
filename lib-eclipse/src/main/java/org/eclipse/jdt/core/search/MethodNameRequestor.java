/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
  *
  * A <code> MethodDeclarationRequestor</code> collects search results from a <code> searchAllMethodDeclarations</code>
  * query to a <code>SearchEngine</code>. Clients must subclass this abstract class and pass an instance to the
  * <code>SearchEngine.searchAllMethodDeclarations</code> method.
  *
  * <p>
  * This class may be subclassed by clients
  * </p>
 * @since 3.12
  *
  */
public abstract class MethodNameRequestor {

	/**
	 * Accepts a method.
	 *
	 * <p>
	 * The default implementation of this method does nothing.
	 * Subclasses should override.
	 * </p>
	 *
	 * @param methodName name of the method.
	 * @param parameterCount number of parameters in this method.
	 * @param declaringQualifier the qualified name of parent of the enclosing type of this method.
	 * @param simpleTypeName name of the enclosing type of this method.
	 * @param typeModifiers modifiers of the type
	 * @param packageName the package name as specified in the package declaration (i.e. a dot-separated name).
	 * @param signature signature of the method - this would be null for methods in source files.
	 * @param parameterTypes types of all the parameters.
	 * @param parameterNames names of all the parameters.
	 * @param returnType return type of the method.
	 * @param modifiers modifiers of the method.
	 * @param path the full path to the resource containing the type. If the resource is a .class file
	 *          or a source file, this is the full path in the workspace to this resource. If the
	 *          resource is an archive (that is, a .zip or .jar file), the path is composed of 2 paths separated
	 *		 	 by <code>IJavaSearchScope.JAR_FILE_ENTRY_SEPARATOR</code>:
	 *			 the first path is the full OS path to the archive (if it is an external archive),
	 *			 or the workspace relative <code>IPath</code> to the archive (if it is an internal archive),
	 * 		 the second path is the path to the resource inside the archive.
	 */
	public void acceptMethod(
			char[] methodName,
			int parameterCount,
			char[] declaringQualifier,
			char[] simpleTypeName,
			int typeModifiers,
			char[] packageName,
			char[] signature,
			char[][] parameterTypes,
			char[][] parameterNames,
			char[] returnType,
			int modifiers,
			String path,
			int methodIndex) {
		//do nothing
	}
}