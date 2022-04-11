/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation.
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
package org.eclipse.jdt.internal.compiler.batch;

import java.util.Arrays;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ExportsStatement;
import org.eclipse.jdt.internal.compiler.ast.ModuleDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ModuleReference;
import org.eclipse.jdt.internal.compiler.ast.OpensStatement;
import org.eclipse.jdt.internal.compiler.ast.ProvidesStatement;
import org.eclipse.jdt.internal.compiler.ast.RequiresStatement;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.UsesStatement;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.compiler.env.IModulePathEntry;
import org.eclipse.jdt.internal.compiler.env.ISourceModule;
import org.eclipse.jdt.internal.compiler.env.ModuleReferenceImpl;
import org.eclipse.jdt.internal.compiler.env.PackageExportImpl;

/**
 * Retrofit a {@link ModuleDeclaration} into an {@link ISourceModule}.
 * It remembers the underlying {@link ICompilationUnit} so the full structure
 * can be recreated if needed.
 */
public class BasicModule implements ISourceModule {
	static class Service implements IModule.IService {
		char[] provides;
		char[][] with;
		@Override
		public char[] name() {
			return this.provides;
		}

		@Override
		public char[][] with() {
			return this.with;
		}
		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append("provides"); //$NON-NLS-1$
			buffer.append(this.provides);
			buffer.append(" with "); //$NON-NLS-1$
			buffer.append(this.with);
			buffer.append(';');
			return buffer.toString();
		}
	}
	private static PackageExportImpl createPackageExport(ExportsStatement[] refs, int i) {
		ExportsStatement ref = refs[i];
		PackageExportImpl exp = new PackageExportImpl();
		exp.pack = ref.pkgName;
		ModuleReference[] imp = ref.targets;
		if (imp != null) {
			exp.exportedTo = new char[imp.length][];
			for(int j = 0; j < imp.length; j++) {
				exp.exportedTo = imp[j].tokens;
			}
		}
		return exp;
	}
	private static Service createService(TypeReference service, TypeReference[] with) {
		Service ser = new Service();
		ser.provides = CharOperation.concatWith(service.getTypeName(), '.');
		ser.with = new char[with.length][];
		for (int i = 0; i < with.length; i++) {
			ser.with[i] = CharOperation.concatWith(with[i].getTypeName(), '.');
		}
		return ser;
	}
	private static PackageExportImpl createPackageOpen(OpensStatement ref) {
		PackageExportImpl exp = new PackageExportImpl();
		exp.pack = ref.pkgName;
		ModuleReference[] imp = ref.targets;
		if (imp != null) {
			exp.exportedTo = new char[imp.length][];
			for(int j = 0; j < imp.length; j++) {
				exp.exportedTo = imp[j].tokens;
			}
		}
		return exp;
	}

	private boolean isOpen = false;
	char[] name;
	IModule.IModuleReference[] requires;
	IModule.IPackageExport[] exports;
	char[][] uses;
	Service[] provides;
	IModule.IPackageExport[] opens;
	private ICompilationUnit compilationUnit;

	public BasicModule(ModuleDeclaration descriptor, IModulePathEntry root) {
		this.compilationUnit = descriptor.compilationResult().compilationUnit;
		this.name = descriptor.moduleName;
		if (descriptor.requiresCount > 0) {
			RequiresStatement[] refs = descriptor.requires;
			this.requires = new ModuleReferenceImpl[refs.length];
			for (int i = 0; i < refs.length; i++) {
				ModuleReferenceImpl ref = new ModuleReferenceImpl();
				ref.name = CharOperation.concatWith(refs[i].module.tokens, '.');
				ref.modifiers = refs[i].modifiers;
				this.requires[i] = ref;
			}
		} else {
			this.requires = new ModuleReferenceImpl[0];
		}
		if (descriptor.exportsCount > 0) {
			ExportsStatement[] refs = descriptor.exports;
			this.exports = new PackageExportImpl[refs.length];
			for (int i = 0; i < refs.length; i++) {
				PackageExportImpl exp = createPackageExport(refs, i);
				this.exports[i] = exp;
			}
		} else {
			this.exports = new PackageExportImpl[0];
		}
		if (descriptor.usesCount > 0) {
			UsesStatement[] u = descriptor.uses;
			this.uses = new char[u.length][];
			for(int i = 0; i < u.length; i++) {
				this.uses[i] = CharOperation.concatWith(u[i].serviceInterface.getTypeName(), '.');
			}
		}
		if (descriptor.servicesCount > 0) {
			ProvidesStatement[] services = descriptor.services;
			this.provides = new Service[descriptor.servicesCount];
			for (int i = 0; i < descriptor.servicesCount; i++) {
				this.provides[i] = createService(services[i].serviceInterface, services[i].implementations);
			}
		}
		if (descriptor.opensCount > 0) {
			OpensStatement[] refs = descriptor.opens;
			this.opens = new PackageExportImpl[refs.length];
			for (int i = 0; i < refs.length; i++) {
				PackageExportImpl exp = createPackageOpen(refs[i]);
				this.opens[i] = exp;
			}
		} else {
			this.opens = new PackageExportImpl[0];
		}
		this.isOpen = descriptor.isOpen();
	}
	@Override
	public ICompilationUnit getCompilationUnit() {
		return this.compilationUnit;
	}
	@Override
	public char[] name() {
		return this.name;
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
	public boolean isOpen() {
		return this.isOpen;
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
		if (this.requires != null) {
			for(int i = 0; i < this.requires.length; i++) {
				buffer.append("\trequires "); //$NON-NLS-1$
				if (this.requires[i].isTransitive()) {
					buffer.append(" public "); //$NON-NLS-1$
				}
				buffer.append(this.requires[i].name());
				buffer.append(';').append('\n');
			}
		}
		if (this.exports != null) {
			buffer.append('\n');
			for(int i = 0; i < this.exports.length; i++) {
				buffer.append("\texports "); //$NON-NLS-1$
				buffer.append(this.exports[i].toString());
			}
		}
		if (this.uses != null) {
			buffer.append('\n');
			for (char[] cs : this.uses) {
				buffer.append(cs);
				buffer.append(';').append('\n');
			}
		}
		if (this.provides != null) {
			buffer.append('\n');
			for(Service ser : this.provides) {
				buffer.append(ser.toString());
			}
		}
		buffer.append('\n').append('}').toString();
	}
}