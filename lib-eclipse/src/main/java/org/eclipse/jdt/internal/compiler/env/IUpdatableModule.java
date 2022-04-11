/*******************************************************************************
 * Copyright (c) 2017, 2018 GK Software AG, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.env;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.util.SimpleSetOfCharArray;

/**
 * Interface to a module as needed to implement the updates for {@code --add-reads}
 * and {@code --add-exports} command line options (or corresponding classpath attributes).
 */
public interface IUpdatableModule {

	/**
	 * The compiler first wires modules only, before then wiring packages based on the module graph.
	 * This enum selects one of these phases when asking the environment to perform its updates.
	 */
	enum UpdateKind { MODULE, PACKAGE }

	static class AddExports implements Consumer<IUpdatableModule> {

		char[] name;
		char[][] targets;
		public AddExports(char[] pkgName, char[][] targets) {
			this.name = pkgName;
			this.targets = targets;
		}
		@Override
		public void accept(IUpdatableModule t) {
			t.addExports(this.name, this.targets);
		}

		public char[] getName() {
			return this.name;
		}

		public char[][] getTargetModules() {
			return this.targets;
		}

		public UpdateKind getKind() {
			return UpdateKind.PACKAGE;
		}
		@Override
		public boolean equals(Object other) {
			if (this == other) return true;
			if (!(other instanceof AddExports)) return false;
			AddExports pu = (AddExports) other;

			if (!CharOperation.equals(this.name, pu.name))
				return false;
			if (!CharOperation.equals(this.targets, pu.targets))
				return false;
			return true;
		}
		@Override
		public int hashCode() {
			int hash = CharOperation.hashCode(this.name);
			if (this.targets != null) {
				for (int i = 0; i < this.targets.length; i++) {
					hash += 17 * CharOperation.hashCode(this.targets[i]);
				}
			}
			return hash;
		}
		@Override
		public String toString() {
			return "add-exports " + CharOperation.charToString(this.name) + "=" + CharOperation.charToString(CharOperation.concatWith(this.targets, ','));  //$NON-NLS-1$//$NON-NLS-2$
		}

	}

	static class AddReads implements Consumer<IUpdatableModule> {

		char[] targetModule;

		public AddReads(char[] target) {
			this.targetModule = target;
		}
		@Override
		public void accept(IUpdatableModule t) {
			// TODO Auto-generated method stub
			t.addReads(this.targetModule);
		}

		public char[] getTarget() {
			return this.targetModule;
		}

		public UpdateKind getKind() {
			return UpdateKind.MODULE;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) return true;
			if (!(other instanceof AddReads)) return false;
			AddReads mu = (AddReads) other;
			return CharOperation.equals(this.targetModule, mu.targetModule);
		}
		@Override
		public int hashCode() {
			return CharOperation.hashCode(this.targetModule);
		}
		@Override
		public String toString() {
			return "add-read " + CharOperation.charToString(this.targetModule); //$NON-NLS-1$
		}
	}
	/** Structure for update operations, sorted by {@link UpdateKind}. */
	static class UpdatesByKind {
		List<Consumer<IUpdatableModule>> moduleUpdates = Collections.emptyList();
		List<Consumer<IUpdatableModule>> packageUpdates = Collections.emptyList();
		public List<Consumer<IUpdatableModule>> getList(UpdateKind kind, boolean create) {
			switch (kind) {
				case MODULE:
					if (this.moduleUpdates == Collections.EMPTY_LIST && create)
						this.moduleUpdates = new ArrayList<>();
					return this.moduleUpdates;
				case PACKAGE:
					if (this.packageUpdates == Collections.EMPTY_LIST && create)
						this.packageUpdates = new ArrayList<>();
					return this.packageUpdates;
				default:
					throw new IllegalArgumentException("Unknown enum value "+kind); //$NON-NLS-1$
			}
		}
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			for (Consumer<IUpdatableModule> consumer : this.moduleUpdates) {
				if(result.length() > 0)
					result.append("\n"); //$NON-NLS-1$
				result.append(consumer);
			}
			for (Consumer<IUpdatableModule> consumer : this.packageUpdates) {
				if(result.length() > 0)
					result.append("\n"); //$NON-NLS-1$
				result.append(consumer);
			}
			return result.toString();
		}
	}

	/** Answer the name of the module to update. */
	char[] name();
	/** Perform an --add-reads update on the module. */
	void addReads(char[] moduleName);
	/** Perform an --add-exports update on the module. */
	void addExports(char[] packageName, char[][] targetModules);
	/** Define the ModuleMainClass to be recorded in the generated module-info.class. */
	void setMainClassName(char[] mainClassName);
	/** Passes names of packages to be recorded in the ModulePackages classfile attribute. */
	void setPackageNames(SimpleSetOfCharArray packageNames);
}
