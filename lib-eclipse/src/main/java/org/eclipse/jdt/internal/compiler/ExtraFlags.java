/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.env.IBinaryNestedType;

public final class ExtraFlags {
	public final static int HasNonPrivateStaticMemberTypes = 0x0001;
	public final static int IsMemberType = 0x0002;
	public final static int IsLocalType = 0x0004;

	//internal flags
	public final static int ParameterTypesStoredAsSignature = 0x0010;

	public static int getExtraFlags(ClassFileReader reader) {
		int extraFlags = 0;

		if (reader.isNestedType()) {
			extraFlags |= ExtraFlags.IsMemberType;
		}

		if (reader.isLocal()) {
			extraFlags |= ExtraFlags.IsLocalType;
		}

		IBinaryNestedType[] memberTypes = reader.getMemberTypes();
		int memberTypeCounter = memberTypes == null ? 0 : memberTypes.length;
		if (memberTypeCounter > 0) {
			done : for (int i = 0; i < memberTypeCounter; i++) {
				int modifiers = memberTypes[i].getModifiers();
				// if the member type is static and not private
				if ((modifiers & ClassFileConstants.AccStatic) != 0 && (modifiers & ClassFileConstants.AccPrivate) == 0) {
					extraFlags |= ExtraFlags.HasNonPrivateStaticMemberTypes;
					break done;
				}
			}

		}

		return extraFlags;
	}

	public static int getExtraFlags(IType type) throws JavaModelException {
		int extraFlags = 0;

		if (type.isMember()) {
			extraFlags |= ExtraFlags.IsMemberType;
		}

		if (type.isLocal()) {
			extraFlags |= ExtraFlags.IsLocalType;
		}

		IType[] memberTypes = type.getTypes();
		int memberTypeCounter = memberTypes == null ? 0 : memberTypes.length;
		if (memberTypeCounter > 0) {
			done : for (int i = 0; i < memberTypeCounter; i++) {
				int flags = memberTypes[i].getFlags();
				// if the member type is static and not private
				if ((flags & ClassFileConstants.AccStatic) != 0 && (flags & ClassFileConstants.AccPrivate) == 0 ) {
					extraFlags |= ExtraFlags.HasNonPrivateStaticMemberTypes;
					break done;
				}
			}
		}

		return extraFlags;
	}

	public static int getExtraFlags(TypeDeclaration typeDeclaration) {
		int extraFlags = 0;

		if (typeDeclaration.enclosingType != null) {
			extraFlags |= ExtraFlags.IsMemberType;
		}
		TypeDeclaration[] memberTypes = typeDeclaration.memberTypes;
		int memberTypeCounter = memberTypes == null ? 0 : memberTypes.length;
		if (memberTypeCounter > 0) {
			done : for (int i = 0; i < memberTypeCounter; i++) {
				int modifiers = memberTypes[i].modifiers;
				// if the member type is static and not private
				if ((modifiers & ClassFileConstants.AccStatic) != 0 && (modifiers & ClassFileConstants.AccPrivate) == 0) {
					extraFlags |= ExtraFlags.HasNonPrivateStaticMemberTypes;
					break done;
				}
			}
		}

		return extraFlags;
	}
}
