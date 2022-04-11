/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *         bug 407191 - [1.8] Binary access support for type annotations
 *     Stephan Herrmann - Contribution for
 *								Bug 440474 - [null] textual encoding of external null annotations
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.env;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding.ExternalAnnotationStatus;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;

public interface IBinaryType extends IGenericType {

	char[][] NoInterface = CharOperation.NO_CHAR_CHAR;
	IBinaryNestedType[] NoNestedType = new IBinaryNestedType[0];
	IBinaryField[] NoField = new IBinaryField[0];
	IBinaryMethod[] NoMethod = new IBinaryMethod[0];
/**
 * Answer the runtime visible and invisible annotations for this type or null if none.
 */

IBinaryAnnotation[] getAnnotations();

/**
 * Answer the runtime visible and invisible type annotations for this type or null if none.
 */

IBinaryTypeAnnotation[] getTypeAnnotations();

/**
 * Answer the enclosing method (including method selector and method descriptor), or
 * null if none.
 *
 * For example, "foo()Ljava/lang/Object;V"
 */

char[] getEnclosingMethod();
/**
 * Answer the resolved name of the enclosing type in the
 * class file format as specified in section 4.2 of the Java 2 VM spec
 * or null if the receiver is a top level type.
 *
 * For example, java.lang.String is java/lang/String.
 */

char[] getEnclosingTypeName();
/**
 * Answer the receiver's fields or null if the array is empty.
 */

IBinaryField[] getFields();
/**
 * Answer the receiver's record components or null if the array is empty.
 */

IRecordComponent[] getRecordComponents();
/**
 * Answer the module to which this type belongs.
 * {@code null} if the type is associated to the unnamed module.
 *
 * @return the module name or {@code null}
 */
char[] getModule();
/**
 * Answer the receiver's ClassSignature, which describes the type parameters,
 * super class, and super interfaces as specified in section "4.7.9.1 Signatures"
 * of the Java SE 8 VM spec.
 * Returns null if none.
 *
 * @return the receiver's signature, null if none
 */
char[] getGenericSignature();
/**
 * Answer the resolved names of the receiver's interfaces in the
 * class file format as specified in section 4.2 of the Java 2 VM spec
 * or null if the array is empty.
 *
 * For example, java.lang.String is java/lang/String.
 */

char[][] getInterfaceNames();

/**
 * Answer the unresolved names of the receiver's permitted sub types
 * or null if the array is empty.
 *
 * A name is a simple name or a qualified, dot separated name.
 * For example, Hashtable or java.util.Hashtable.
 */
default char[][] getPermittedSubtypeNames() {
	return null;
}

/**
 * Answer the receiver's nested types or null if the array is empty.
 *
 * This nested type info is extracted from the inner class attributes.
 * Ask the name environment to find a member type using its compound name.
 */

// NOTE: The compiler examines the nested type info & ignores the local types
// so the local types do not have to be included.

IBinaryNestedType[] getMemberTypes();
/**
 * Answer the receiver's methods or null if the array is empty.
 */

IBinaryMethod[] getMethods();

/**
 * Answer the list of missing type names which were referenced from
 * the problem classfile. This list is encoded via an extra attribute.
 */
char[][][] getMissingTypeNames();

/**
 * Answer the resolved name of the type in the
 * class file format as specified in section 4.2 of the Java 2 VM spec.
 *
 * For example, java.lang.String is java/lang/String.
 */
char[] getName();

/**
 * Answer the simple name of the type in the class file.
 * For member A$B, will answer B.
 * For anonymous will answer null.
 */
char[] getSourceName();

/**
 * Answer the resolved name of the receiver's superclass in the
 * class file format as specified in section 4.2 of the Java 2 VM spec
 * or null if it does not have one.
 *
 * For example, java.lang.String is java/lang/String.
 */

char[] getSuperclassName();
/**
 * Answer the tagbits set according to the bits for annotations.
 */
long getTagBits();
/**
 * Answer true if the receiver is an anonymous class.
 * false otherwise
 */
boolean isAnonymous();

/**
 * Answer true if the receiver is a local class.
 * false otherwise
 */
boolean isLocal();

/**
 * Answer true if the receiver is a record.
 * false otherwise
 */
boolean isRecord();

/**
 * Answer true if the receiver is a member class.
 * false otherwise
 */
boolean isMember();

/**
 * Answer the source file attribute, or null if none.
 *
 * For example, "String.java"
 */

char[] sourceFileName();

/**
 * Answer a type annotation walker that takes into consideration also external annotations.
 * @param walker previous walker, may be empty, otherwise it will be returned unchanged
 * @param member if either a IBinaryField or a IBinaryMethod is provided, answer a walker specifically for that member
 * @param environment for use by the walker
 * @return either a matching walker with data from external annotations or the walker provided via argument 'walker'.
 */
ITypeAnnotationWalker enrichWithExternalAnnotationsFor(ITypeAnnotationWalker walker, Object member, LookupEnvironment environment);

/**
 * Answer whether a provider for external annotations is associated with this binary type.
 */
ExternalAnnotationStatus getExternalAnnotationStatus();

}
