/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation.
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
package org.eclipse.jdt.internal.compiler.classfmt;

import java.util.Arrays;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryModule;
import org.eclipse.jdt.internal.compiler.env.IModule;

public class ModuleInfo extends ClassFileStruct implements IBinaryModule {
	protected int flags;
	protected int requiresCount;
	protected int exportsCount;
	protected int usesCount;
	protected int providesCount;
	protected int opensCount;
	protected char[] name;
	protected char[] version;
	protected ModuleReferenceInfo[] requires;
	protected PackageExportInfo[] exports;
	protected PackageExportInfo[] opens;
	char[][] uses;
	IModule.IService[] provides;

	protected AnnotationInfo[] annotations;
	private long tagBits;


	@Override
	public boolean isOpen() {
		return (this.flags & ClassFileConstants.ACC_OPEN) != 0;
	}
	public int requiresCount() {
		return this.requiresCount;
	}
	public int exportsCount() {
		return this.exportsCount;
	}
	public int usesCount() {
		return this.usesCount;
	}
	public int providesCount() {
		return this.providesCount;
	}
	@Override
	public char[] name() {
		return this.name;
	}
	public void setName(char[] name) {
		this.name = name;
	}
	@Override
	public IModule.IModuleReference[] requires() {
		return this.requires;
	}
	@Override
	public IModule.IPackageExport[] exports() {
		return this.exports;
	}
	@Override
	public char[][] uses() {
		return this.uses;
	}
	@Override
	public IService[] provides() {
		return this.provides;
	}
	@Override
	public IModule.IPackageExport[] opens() {
		return this.opens;
	}
	@Override
	public IBinaryAnnotation[] getAnnotations() {
		return this.annotations;
	}
	@Override
	public long getTagBits() {
		return this.tagBits;
	}

	/**
	 * @param classFileBytes byte[]
	 * @param offsets int[]
	 * @param offset int
	 */
	protected ModuleInfo (byte classFileBytes[], int offsets[], int offset) {
		super(classFileBytes, offsets, offset);
	}

	/**
	 * @param classFileBytes bytes of the enclosing class file
	 * @param offsets constant pool offsets
	 * @param offset offset to the "Module" attribute
	 * @return a module info initialized from the "Module" attribute, which was already detected by the caller
	 */
	public static ModuleInfo createModule(byte classFileBytes[], int offsets[], int offset) {

		ModuleInfo module = new ModuleInfo(classFileBytes, offsets, 0);

		module.readModuleAttribute(offset+6);

		return module;
	}

	private void readModuleAttribute(int moduleOffset) {
		int utf8Offset;
		int name_index = this.constantPoolOffsets[u2At(moduleOffset)];
		utf8Offset = this.constantPoolOffsets[u2At(name_index + 1)];
		this.name = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
		CharOperation.replace(this.name, '/', '.');
		moduleOffset += 2;
		this.flags = u2At(moduleOffset);
		moduleOffset += 2;
		int version_index = u2At(moduleOffset);
		if (version_index > 0) {
			utf8Offset = this.constantPoolOffsets[version_index];
			this.version = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
		}
		moduleOffset += 2;

		int count = u2At(moduleOffset);
		this.requiresCount = count;
		this.requires = new ModuleReferenceInfo[count];
		moduleOffset += 2;
		for (int i = 0; i < count; i++) {
			name_index = this.constantPoolOffsets[u2At(moduleOffset)];
			utf8Offset = this.constantPoolOffsets[u2At(name_index + 1)];
			char[] requiresNames = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
			this.requires[i] = this.new ModuleReferenceInfo();
			CharOperation.replace(requiresNames, '/', '.');
			this.requires[i].refName = requiresNames;
			moduleOffset += 2;
			int modifiers = u2At(moduleOffset);
			this.requires[i].modifiers = modifiers;
			this.requires[i].isTransitive = (ClassFileConstants.ACC_TRANSITIVE & modifiers) != 0; // Access modifier
			moduleOffset += 2;
			version_index = u2At(moduleOffset);
			if (version_index > 0) {
				utf8Offset = this.constantPoolOffsets[version_index];
				this.requires[i].required_version = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
			}
			moduleOffset += 2;
		}
		count = u2At(moduleOffset);
		moduleOffset += 2;
		this.exportsCount = count;
		this.exports = new PackageExportInfo[count];
		for (int i = 0; i < count; i++) {
			name_index = this.constantPoolOffsets[u2At(moduleOffset)];
			utf8Offset = this.constantPoolOffsets[u2At(name_index + 1)];
			char[] exported = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
			CharOperation.replace(exported, '/', '.');
			PackageExportInfo pack = this.new PackageExportInfo();
			this.exports[i] = pack;
			pack.packageName = exported;
			moduleOffset += 2;
			pack.modifiers = u2At(moduleOffset);
			moduleOffset += 2;
			int exportedtoCount = u2At(moduleOffset);
			moduleOffset += 2;
			if (exportedtoCount > 0) {
				pack.exportedTo = new char[exportedtoCount][];
				pack.exportedToCount = exportedtoCount;
				for(int k = 0; k < exportedtoCount; k++) {
					name_index = this.constantPoolOffsets[u2At(moduleOffset)];
					utf8Offset = this.constantPoolOffsets[u2At(name_index + 1)];
					char[] exportedToName = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
					CharOperation.replace(exportedToName, '/', '.');
					pack.exportedTo[k] = exportedToName;
					moduleOffset += 2;
				}
			}
		}
		count = u2At(moduleOffset);
		moduleOffset += 2;
		this.opensCount = count;
		this.opens = new PackageExportInfo[count];
		for (int i = 0; i < count; i++) {
			name_index = this.constantPoolOffsets[u2At(moduleOffset)];
			utf8Offset = this.constantPoolOffsets[u2At(name_index + 1)];
			char[] exported = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
			CharOperation.replace(exported, '/', '.');
			PackageExportInfo pack = this.new PackageExportInfo();
			this.opens[i] = pack;
			pack.packageName = exported;
			moduleOffset += 2;
			pack.modifiers = u2At(moduleOffset);
			moduleOffset += 2;
			int exportedtoCount = u2At(moduleOffset);
			moduleOffset += 2;
			if (exportedtoCount > 0) {
				pack.exportedTo = new char[exportedtoCount][];
				pack.exportedToCount = exportedtoCount;
				for(int k = 0; k < exportedtoCount; k++) {
					name_index = this.constantPoolOffsets[u2At(moduleOffset)];
					utf8Offset = this.constantPoolOffsets[u2At(name_index + 1)];
					char[] exportedToName = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
					CharOperation.replace(exportedToName, '/', '.');
					pack.exportedTo[k] = exportedToName;
					moduleOffset += 2;
				}
			}
		}
		count = u2At(moduleOffset);
		moduleOffset += 2;
		this.usesCount = count;
		this.uses = new char[count][];
		for (int i = 0; i < count; i++) {
			int classIndex = this.constantPoolOffsets[u2At(moduleOffset)];
			utf8Offset = this.constantPoolOffsets[u2At(classIndex + 1)];
			char[] inf = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
			CharOperation.replace(inf, '/', '.');
			this.uses[i] = inf;
			moduleOffset += 2;
		}
		count = u2At(moduleOffset);
		moduleOffset += 2;
		this.providesCount = count;
		this.provides = new ServiceInfo[count];
		for (int i = 0; i < count; i++) {
			int classIndex = this.constantPoolOffsets[u2At(moduleOffset)];
			utf8Offset = this.constantPoolOffsets[u2At(classIndex + 1)];
			char[] inf = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
			CharOperation.replace(inf, '/', '.');
			ServiceInfo service = this.new ServiceInfo();
			this.provides[i] = service;
			service.serviceName = inf;
			moduleOffset += 2;
			int implCount = u2At(moduleOffset);
			moduleOffset += 2;
			service.with = new char[implCount][];
			if (implCount > 0) {
				service.with = new char[implCount][];
				for(int k = 0; k < implCount; k++) {
					classIndex = this.constantPoolOffsets[u2At(moduleOffset)];
					utf8Offset = this.constantPoolOffsets[u2At(classIndex + 1)];
					char[] implName = utf8At(utf8Offset + 3, u2At(utf8Offset + 1));
					CharOperation.replace(implName, '/', '.');
					service.with[k] = implName;
					moduleOffset += 2;
				}
			}
		}
	}
	void setAnnotations(AnnotationInfo[] annotationInfos, long tagBits, boolean fullyInitialize) {
		this.annotations = annotationInfos;
		this.tagBits = tagBits;
		if (fullyInitialize) {
			for (int i = 0, max = annotationInfos.length; i < max; i++) {
				annotationInfos[i].initialize();
			}
		}
	}

	class ModuleReferenceInfo implements IModule.IModuleReference {
		char[] refName;
		boolean isTransitive = false;
		int modifiers;
		char[] required_version;
		@Override
		public char[] name() {
			return this.refName;
		}
		@Override
		public boolean isTransitive() {
			return this.isTransitive;
		}
		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof IModule.IModuleReference))
				return false;
			IModule.IModuleReference mod = (IModule.IModuleReference) o;
			if (this.modifiers != mod.getModifiers())
				return false;
			return CharOperation.equals(this.refName, mod.name(), false);
		}
		@Override
		public int hashCode() {
			return CharOperation.hashCode(this.refName);
		}
		@Override
		public int getModifiers() {
			return this.modifiers;
		}
	}
	class PackageExportInfo implements IModule.IPackageExport {
		char[] packageName;
		char[][] exportedTo;
		int exportedToCount;
		int modifiers;
		@Override
		public char[] name() {
			return this.packageName;
		}

		@Override
		public char[][] targets() {
			return this.exportedTo;
		}
		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			toStringContent(buffer);
			return buffer.toString();
		}
		protected void toStringContent(StringBuffer buffer) {
			buffer.append(this.packageName);
			if (this.exportedToCount > 0) {
				buffer.append(" to "); //$NON-NLS-1$
				for(int i = 0; i < this.exportedToCount; i++) {
					buffer.append(this.exportedTo[i]);
					buffer.append(',').append(' ');
				}
			}
			buffer.append(';').append('\n');
		}
	}
	class ServiceInfo implements IModule.IService {
		char[] serviceName;
		char[][] with;
		@Override
		public char[] name() {
			return this.serviceName;
		}

		@Override
		public char[][] with() {
			return this.with;
		}
	}
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof IModule))
			return false;
		IModule mod = (IModule) o;
		if (!CharOperation.equals(this.name, mod.name()))
			return false;
		return Arrays.equals(this.requires, mod.requires());
	}
	@Override
	public int hashCode() {
		int result = 17;
		int c = CharOperation.hashCode(this.name);
		result = 31 * result + c;
		c =  Arrays.hashCode(this.requires);
		result = 31 * result + c;
		return result;
	}
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer(getClass().getName());
		toStringContent(buffer);
		return buffer.toString();
	}
	protected void toStringContent(StringBuffer buffer) {
		buffer.append("\nmodule "); //$NON-NLS-1$
		buffer.append(this.name).append(' ');
		buffer.append('{').append('\n');
		if (this.requiresCount > 0) {
			for(int i = 0; i < this.requiresCount; i++) {
				buffer.append("\trequires "); //$NON-NLS-1$
				if (this.requires[i].isTransitive) {
					buffer.append(" public "); //$NON-NLS-1$
				}
				buffer.append(this.requires[i].refName);
				buffer.append(';').append('\n');
			}
		}
		if (this.exportsCount > 0) {
			buffer.append('\n');
			for(int i = 0; i < this.exportsCount; i++) {
				buffer.append("\texports "); //$NON-NLS-1$
				buffer.append(this.exports[i].toString());
			}
		}
		buffer.append('\n').append('}').toString();
	}
}
