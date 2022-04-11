/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.core.util.IConstantPool;
import org.eclipse.jdt.core.util.IConstantPoolConstant;
import org.eclipse.jdt.core.util.IConstantPoolEntry;
import org.eclipse.jdt.core.util.IConstantPoolEntry3;
import org.eclipse.jdt.core.util.IModuleAttribute;
import org.eclipse.jdt.core.util.IPackageVisibilityInfo;
import org.eclipse.jdt.core.util.IProvidesInfo;
import org.eclipse.jdt.core.util.IRequiresInfo;

/**
 * @since 3.14
 */
public class ModuleAttribute extends ClassFileAttribute implements IModuleAttribute {

	static final IRequiresInfo[] NO_REQUIRES = new IRequiresInfo[0];
	static final IPackageVisibilityInfo[] NO_PACKAGE_VISIBILITY_INFOS = new IPackageVisibilityInfo[0];
	static final int[] NO_USES = new int[0];
	static final IProvidesInfo[] NO_PROVIDES_INFOS = new IProvidesInfo[0];
	private int moduleNameIndex;
	private char[] moduleName;
	private int moduleFlags;
	private int moduleVersionIndex;
	private char[] moduleVersionValue;
	private int requiresCount;
	private IRequiresInfo[] requiresInfo;
	private int exportsCount;
	private IPackageVisibilityInfo[] exportsInfo;
	private int opensCount;
	private IPackageVisibilityInfo[] opensInfo;
	private int usesCount;
	private int[] usesIndices;
	private char[][] usesNames;
	private int providesCount;
	private IProvidesInfo[] providesInfo;

	ModuleAttribute(byte[] classFileBytes, IConstantPool constantPool, int offset) throws ClassFormatException {
		super(classFileBytes, constantPool, offset);
		int readOffset = 6; // skip attribute_name_index & attribute_length
		this.moduleNameIndex = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		IConstantPoolEntry constantPoolEntry = constantPool.decodeEntry(this.moduleNameIndex);
		if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Module) {
			throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
		}
		this.moduleName = ((IConstantPoolEntry3) constantPoolEntry).getModuleName();
		this.moduleFlags = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		this.moduleVersionIndex = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		if (this.moduleVersionIndex != 0) {
			constantPoolEntry = constantPool.decodeEntry(this.moduleVersionIndex);
			if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Utf8) {
				throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
			}
			this.moduleVersionValue = constantPoolEntry.getUtf8Value();
		} else {
			this.moduleVersionValue = CharOperation.NO_CHAR;
		}

		this.requiresCount = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		if (this.requiresCount != 0) {
			this.requiresInfo = new RequiresInfo[this.requiresCount];
			for (int i = 0; i < this.requiresCount; i++) {
				this.requiresInfo [i] = new RequiresInfo(classFileBytes, constantPool, offset + readOffset);
				readOffset += 6;
			}
		} else {
			this.requiresInfo = NO_REQUIRES;
		}

		this.exportsCount = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		if (this.exportsCount != 0) {
			this.exportsInfo = new PackageVisibilityInfo[this.exportsCount];
			for (int i = 0; i < this.exportsCount; i++) {
				this.exportsInfo [i] = new PackageVisibilityInfo(classFileBytes, constantPool, offset + readOffset);
				readOffset += 6 + 2 * this.exportsInfo[i].getTargetsCount();
			}
		} else {
			this.exportsInfo = NO_PACKAGE_VISIBILITY_INFOS;
		}

		this.opensCount = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		if (this.opensCount != 0) {
			this.opensInfo = new PackageVisibilityInfo[this.opensCount];
			for (int i = 0; i < this.opensCount; i++) {
				this.opensInfo [i] = new PackageVisibilityInfo(classFileBytes, constantPool, offset + readOffset);
				readOffset += 6 + 2 * this.opensInfo[i].getTargetsCount();
			}
		} else {
			this.opensInfo = NO_PACKAGE_VISIBILITY_INFOS;
		}

		this.usesCount = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		if (this.usesCount != 0) {
			this.usesIndices = new int[this.usesCount];
			this.usesNames = new char[this.usesCount][];
			for (int i = 0; i < this.usesCount; i++) {
				this.usesIndices[i] = u2At(classFileBytes, readOffset, offset);
				readOffset += 2;
				constantPoolEntry = constantPool.decodeEntry(this.usesIndices[i]);
				if (constantPoolEntry.getKind() != IConstantPoolConstant.CONSTANT_Class) {
					throw new ClassFormatException(ClassFormatException.INVALID_CONSTANT_POOL_ENTRY);
				}
				this.usesNames[i] = constantPoolEntry.getClassInfoName();
			}
		} else {
			this.usesIndices = NO_USES;
			this.usesNames = CharOperation.NO_CHAR_CHAR;
		}

		this.providesCount = u2At(classFileBytes, readOffset, offset);
		readOffset += 2;
		if (this.providesCount != 0) {
			this.providesInfo = new ProvidesInfo[this.providesCount];
			for (int i = 0; i < this.providesCount; i++) {
				this.providesInfo[i] = new ProvidesInfo(classFileBytes, constantPool, offset + readOffset);
				readOffset += 4 + 2 * this.providesInfo[i].getImplementationsCount();
			}
		} else {
			this.providesInfo = NO_PROVIDES_INFOS;
		}
	}

	@Override
	public int getModuleNameIndex() {
		return this.moduleNameIndex;
	}

	@Override
	public char[] getModuleName() {
		return this.moduleName;
	}

	@Override
	public int getModuleFlags() {
		return this.moduleFlags;
	}

	@Override
	public int getModuleVersionIndex() {
		return this.moduleVersionIndex;
	}

	@Override
	public char[] getModuleVersionValue() {
		return this.moduleVersionValue;
	}

	@Override
	public int getRequiresCount() {
		return this.requiresCount;
	}

	@Override
	public IRequiresInfo[] getRequiresInfo() {
		return this.requiresInfo;
	}

	@Override
	public int getExportsCount() {
		return this.exportsCount;
	}

	@Override
	public IPackageVisibilityInfo[] getExportsInfo() {
		return this.exportsInfo;
	}

	@Override
	public int getOpensCount() {
		return this.opensCount;
	}

	@Override
	public IPackageVisibilityInfo[] getOpensInfo() {
		return this.opensInfo;
	}

	@Override
	public int getUsesCount() {
		return this.usesCount;
	}

	@Override
	public int[] getUsesIndices() {
		return this.usesIndices;
	}

	@Override
	public char[][] getUsesClassNames() {
		return this.usesNames;
	}

	@Override
	public int getProvidesCount() {
		return this.providesCount;
	}

	@Override
	public IProvidesInfo[] getProvidesInfo() {
		return this.providesInfo;
	}
}
